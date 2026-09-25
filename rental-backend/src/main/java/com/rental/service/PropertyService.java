package com.rental.service;

import com.rental.dto.PropertyRequest;
import com.rental.dto.PropertyResponse;
import com.rental.entity.Property;
import com.rental.entity.PropertyRentHistory;
import com.rental.entity.enums.PropertyStatus;
import com.rental.entity.enums.PropertyType;
import com.rental.entity.enums.RentalStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ResourceNotFoundException;
import com.rental.mapper.PropertyMapper;
import com.rental.repository.PropertyRentHistoryRepository;
import com.rental.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyRentHistoryRepository propertyRentHistoryRepository;
    private final PropertyMapper propertyMapper;

    public List<PropertyResponse> list(PropertyType type) {
        List<Property> properties = type != null
                ? propertyRepository.findByType(type)
                : propertyRepository.findAll();
        return properties.stream().map(propertyMapper::toResponse).toList();
    }

    public List<PropertyResponse> search(String query) {
        return propertyRepository.search(query).stream().map(propertyMapper::toResponse).toList();
    }

    public PropertyResponse get(Long id) {
        Property property = getEntityOrThrow(id);
        return propertyMapper.toResponse(property, rentHistoryFor(property.getId()));
    }

    public PropertyResponse create(PropertyRequest request) {
        if (propertyRepository.existsByPropertyCodeIgnoreCase(request.propertyCode())) {
            throw new BusinessRuleException(
                    "A property with code '" + request.propertyCode() + "' already exists");
        }
        Property saved = propertyRepository.save(propertyMapper.toEntity(request));
        recordRentHistory(saved, request.rentEffectiveFrom());
        return propertyMapper.toResponse(saved, rentHistoryFor(saved.getId()));
    }

    public PropertyResponse update(Long id, PropertyRequest request) {
        Property property = getEntityOrThrow(id);

        if (propertyRepository.existsByPropertyCodeIgnoreCaseAndIdNot(request.propertyCode(), id)) {
            throw new BusinessRuleException(
                    "A property with code '" + request.propertyCode() + "' already exists");
        }

        boolean hasActiveRental = property.getRentals().stream()
                .anyMatch(rental -> rental.getStatus() == RentalStatus.ACTIVE);

        if (hasActiveRental && request.status() == PropertyStatus.AVAILABLE) {
            throw new BusinessRuleException(
                    "This property has an active rental. End the rental or change tenant before marking it available.");
        }

        boolean rentChanged = property.getMonthlyRent().compareTo(request.monthlyRent()) != 0;

        propertyMapper.updateEntity(property, request);

        if (rentChanged) {
            recordRentHistory(property, request.rentEffectiveFrom());
        }

        return propertyMapper.toResponse(property, rentHistoryFor(property.getId()));
    }

    /**
     * Adds a new standard-rent history entry. The property's earlier rate is
     * never overwritten -- it stays in property_rent_history at whatever date
     * it was recorded.
     */
    private void recordRentHistory(Property property, LocalDate effectiveFrom) {
        List<PropertyRentHistory> existing = rentHistoryFor(property.getId());
        LocalDate latestRecorded = existing.isEmpty() ? null : existing.get(0).getEffectiveFrom();
        LocalDate date = effectiveFrom != null ? effectiveFrom : LocalDate.now();

        if (latestRecorded != null && date.isBefore(latestRecorded)) {
            throw new BusinessRuleException(
                    "The effective date can't be before the last recorded rate change (" + latestRecorded + ")");
        }

        propertyRentHistoryRepository.save(PropertyRentHistory.builder()
                .property(property)
                .monthlyRent(property.getMonthlyRent())
                .effectiveFrom(date)
                .build());
    }

    private List<PropertyRentHistory> rentHistoryFor(Long propertyId) {
        return propertyRentHistoryRepository.findByPropertyIdOrderByEffectiveFromDesc(propertyId);
    }

    public void delete(Long id) {
        Property property = getEntityOrThrow(id);

        if (!property.getRentals().isEmpty()) {
            throw new BusinessRuleException(
                    "This property has rental history and cannot be deleted. " +
                    "Set its status to MAINTENANCE instead if it's no longer in use.");
        }

        propertyRepository.delete(property);
    }

    private Property getEntityOrThrow(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));
    }
}
