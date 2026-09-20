package com.rental.mapper;

import com.rental.dto.OccupantResponse;
import com.rental.dto.PropertySummary;
import com.rental.dto.RentalResponse;
import com.rental.dto.TenantSummary;
import com.rental.entity.Property;
import com.rental.entity.Rental;
import com.rental.entity.RentalTenant;
import com.rental.entity.Tenant;
import org.springframework.stereotype.Component;

@Component
public class RentalMapper {

    public RentalResponse toResponse(Rental rental) {
        return new RentalResponse(
                rental.getId(),
                toPropertySummary(rental.getProperty()),
                toTenantSummary(rental.getTenant()),
                rental.getTenant2() != null ? toTenantSummary(rental.getTenant2()) : null,
                rental.getStartDate(),
                rental.getEndDate(),
                rental.getMonthlyRent(),
                rental.getDeposit(),
                rental.getPaymentDueDay(),
                rental.getStatus(),
                rental.getNotes(),
                rental.getCreatedAt(),
                rental.getUpdatedAt(),
                rental.getOccupants().stream().map(this::toOccupantResponse).toList()
        );
    }

    public OccupantResponse toOccupantResponse(RentalTenant occupant) {
        return new OccupantResponse(
                occupant.getId(),
                toTenantSummary(occupant.getTenant()),
                occupant.getStartDate(),
                occupant.getEndDate(),
                occupant.getMonthlyRent()
        );
    }

    public PropertySummary toPropertySummary(Property property) {
        return new PropertySummary(property.getId(), property.getPropertyCode(), property.getName(), property.getType());
    }

    public TenantSummary toTenantSummary(Tenant tenant) {
        return new TenantSummary(tenant.getId(), tenant.getFullName(), tenant.getTenantType(), tenant.getPhotoUrl());
    }
}
