package com.rental.mapper;

import com.rental.dto.PropertyRentHistoryResponse;
import com.rental.dto.PropertyRequest;
import com.rental.dto.PropertyResponse;
import com.rental.entity.Property;
import com.rental.entity.PropertyRentHistory;
import com.rental.entity.enums.PropertyStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PropertyMapper {

    public Property toEntity(PropertyRequest request) {
        return Property.builder()
                .propertyCode(request.propertyCode().trim())
                .type(request.type())
                .name(request.name().trim())
                .address(request.address().trim())
                .monthlyRent(request.monthlyRent())
                .deposit(request.deposit())
                .status(request.status() != null ? request.status() : PropertyStatus.AVAILABLE)
                .notes(request.notes())
                .build();
    }

    public void updateEntity(Property property, PropertyRequest request) {
        property.setPropertyCode(request.propertyCode().trim());
        property.setType(request.type());
        property.setName(request.name().trim());
        property.setAddress(request.address().trim());
        property.setMonthlyRent(request.monthlyRent());
        property.setDeposit(request.deposit());
        if (request.status() != null) {
            property.setStatus(request.status());
        }
        property.setNotes(request.notes());
    }

    public PropertyResponse toResponse(Property property) {
        return toResponse(property, List.of());
    }

    public PropertyResponse toResponse(Property property, List<PropertyRentHistory> rentHistory) {
        return new PropertyResponse(
                property.getId(),
                property.getPropertyCode(),
                property.getType(),
                property.getName(),
                property.getAddress(),
                property.getMonthlyRent(),
                property.getDeposit(),
                property.getStatus(),
                property.getNotes(),
                property.getCreatedAt(),
                property.getUpdatedAt(),
                rentHistory.stream().map(this::toRentHistoryResponse).toList()
        );
    }

    public PropertyRentHistoryResponse toRentHistoryResponse(PropertyRentHistory history) {
        return new PropertyRentHistoryResponse(
                history.getId(),
                history.getMonthlyRent(),
                history.getEffectiveFrom()
        );
    }
}
