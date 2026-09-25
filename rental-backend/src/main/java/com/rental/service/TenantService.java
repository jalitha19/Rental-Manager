package com.rental.service;

import com.rental.dto.PhoneRequest;
import com.rental.dto.PhoneResponse;
import com.rental.dto.TenantRequest;
import com.rental.dto.TenantResponse;
import com.rental.entity.Rental;
import com.rental.entity.RentalTenant;
import com.rental.entity.Tenant;
import com.rental.entity.TenantPhone;
import com.rental.entity.enums.PropertyStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ResourceNotFoundException;
import com.rental.mapper.TenantMapper;
import com.rental.repository.RentPaymentRepository;
import com.rental.repository.RentalRepository;
import com.rental.repository.RentalTenantRepository;
import com.rental.repository.TenantPhoneRepository;
import com.rental.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantPhoneRepository tenantPhoneRepository;
    private final TenantMapper tenantMapper;
    private final RentalTenantRepository rentalTenantRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final RentalRepository rentalRepository;

    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream().map(tenantMapper::toResponse).toList();
    }

    public List<TenantResponse> search(String query) {
        return tenantRepository.search(query).stream().map(tenantMapper::toResponse).toList();
    }

    public TenantResponse get(Long id) {
        return tenantMapper.toResponse(getEntityOrThrow(id));
    }

    public TenantResponse create(TenantRequest request) {
        Tenant tenant = tenantMapper.toEntity(request);

        if (request.phones() != null) {
            for (PhoneRequest phoneRequest : request.phones()) {
                TenantPhone phone = TenantPhone.builder()
                        .tenant(tenant)
                        .phoneNumber(phoneRequest.phoneNumber().trim())
                        .label(phoneRequest.label())
                        .build();
                tenant.getPhones().add(phone);
            }
        }

        return tenantMapper.toResponse(tenantRepository.save(tenant));
    }

    /** Updates personal information only -- never touches phones or rental/payment history. */
    public TenantResponse update(Long id, TenantRequest request) {
        Tenant tenant = getEntityOrThrow(id);
        tenantMapper.updateEntity(tenant, request);
        return tenantMapper.toResponse(tenant);
    }

    public void delete(Long id) {
        Tenant tenant = getEntityOrThrow(id);

        if (!tenant.getRentals().isEmpty() || rentalTenantRepository.existsByTenantId(id)) {
            throw new BusinessRuleException(
                    "This tenant has rental history and cannot be deleted, to keep that history intact. "
                    + "Use \"Delete rental & payment history\" first if you want to remove it permanently.");
        }

        tenantRepository.delete(tenant);
    }

    /**
     * Permanently erases every rental and payment record this tenant is part
     * of, everywhere -- including rentals they only shared with another
     * tenant, where only this tenant's own record and payments are removed
     * and the other tenant's record is kept untouched. A rental left with no
     * occupants afterward is removed too, and its property is freed up. This
     * cannot be undone. It exists so a tenant's history can be cleared before
     * calling {@link #delete(Long)}.
     */
    public void deleteHistory(Long id) {
        getEntityOrThrow(id);

        List<RentalTenant> occupancies = rentalTenantRepository.findByTenantId(id);
        if (occupancies.isEmpty()) {
            return;
        }

        Set<Long> rentalIds = new LinkedHashSet<>();
        for (RentalTenant occupancy : occupancies) {
            rentalIds.add(occupancy.getRental().getId());
            rentPaymentRepository.deleteByRentalTenantId(occupancy.getId());
        }
        rentalTenantRepository.deleteAll(occupancies);
        rentalTenantRepository.flush();

        for (Long rentalId : rentalIds) {
            Rental rental = rentalRepository.findById(rentalId).orElse(null);
            if (rental == null) {
                continue;
            }
            List<RentalTenant> remaining = rental.getOccupants();
            if (remaining.isEmpty()) {
                var property = rental.getProperty();
                rentPaymentRepository.deleteByRentalId(rental.getId());
                rentalRepository.delete(rental);
                if (property.getStatus() == PropertyStatus.OCCUPIED) {
                    property.setStatus(PropertyStatus.AVAILABLE);
                }
            } else {
                rental.setTenant(remaining.get(0).getTenant());
                rental.setTenant2(remaining.size() > 1 ? remaining.get(1).getTenant() : null);
                rental.setStartDate(remaining.stream()
                        .map(RentalTenant::getStartDate)
                        .min(Comparator.naturalOrder())
                        .orElseThrow());
                rental.setMonthlyRent(remaining.stream()
                        .map(RentalTenant::getMonthlyRent)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
            }
        }
    }

    public PhoneResponse addPhone(Long tenantId, PhoneRequest request) {
        Tenant tenant = getEntityOrThrow(tenantId);

        TenantPhone phone = TenantPhone.builder()
                .tenant(tenant)
                .phoneNumber(request.phoneNumber().trim())
                .label(request.label())
                .build();

        tenant.getPhones().add(phone);
        tenantRepository.save(tenant);

        return tenantMapper.toPhoneResponse(phone);
    }

    public PhoneResponse updatePhone(Long tenantId, Long phoneId, PhoneRequest request) {
        TenantPhone phone = getPhoneOrThrow(tenantId, phoneId);
        phone.setPhoneNumber(request.phoneNumber().trim());
        phone.setLabel(request.label());
        return tenantMapper.toPhoneResponse(tenantPhoneRepository.save(phone));
    }

    public void removePhone(Long tenantId, Long phoneId) {
        Tenant tenant = getEntityOrThrow(tenantId);
        TenantPhone phone = getPhoneOrThrow(tenantId, phoneId);
        tenant.getPhones().remove(phone); // orphanRemoval on Tenant.phones deletes the row
    }

    private TenantPhone getPhoneOrThrow(Long tenantId, Long phoneId) {
        TenantPhone phone = tenantPhoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone number not found: " + phoneId));

        if (!phone.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Phone number not found for this tenant");
        }
        return phone;
    }

    private Tenant getEntityOrThrow(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
    }
}
