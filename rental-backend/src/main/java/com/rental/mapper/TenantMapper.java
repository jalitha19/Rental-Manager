package com.rental.mapper;

import com.rental.dto.PhoneResponse;
import com.rental.dto.TenantRequest;
import com.rental.dto.TenantResponse;
import com.rental.entity.Tenant;
import com.rental.entity.TenantPhone;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantMapper {

    public Tenant toEntity(TenantRequest request) {
        return Tenant.builder()
                .tenantType(request.tenantType())
                .fullName(request.fullName().trim())
                .nicNumber(blankToNull(request.nicNumber()))
                .contactPerson(blankToNull(request.contactPerson()))
                .address(request.address())
                .notes(request.notes())
                .photoUrl(blankToNull(request.photoUrl()))
                .build();
    }

    public void updateEntity(Tenant tenant, TenantRequest request) {
        tenant.setTenantType(request.tenantType());
        tenant.setFullName(request.fullName().trim());
        tenant.setNicNumber(blankToNull(request.nicNumber()));
        tenant.setContactPerson(blankToNull(request.contactPerson()));
        tenant.setAddress(request.address());
        tenant.setNotes(request.notes());
        tenant.setPhotoUrl(blankToNull(request.photoUrl()));
    }

    public TenantResponse toResponse(Tenant tenant) {
        List<PhoneResponse> phones = tenant.getPhones().stream()
                .map(this::toPhoneResponse)
                .toList();

        return new TenantResponse(
                tenant.getId(),
                tenant.getTenantType(),
                tenant.getFullName(),
                tenant.getNicNumber(),
                tenant.getContactPerson(),
                tenant.getAddress(),
                tenant.getNotes(),
                tenant.getPhotoUrl(),
                phones,
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }

    public PhoneResponse toPhoneResponse(TenantPhone phone) {
        return new PhoneResponse(phone.getId(), phone.getPhoneNumber(), phone.getLabel());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
