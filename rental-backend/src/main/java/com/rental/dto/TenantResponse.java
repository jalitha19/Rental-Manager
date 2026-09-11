package com.rental.dto;

import com.rental.entity.enums.TenantType;

import java.time.Instant;
import java.util.List;

public record TenantResponse(
        Long id,
        TenantType tenantType,
        String fullName,
        String nicNumber,
        String contactPerson,
        String address,
        String notes,
        String photoUrl,
        List<PhoneResponse> phones,
        Instant createdAt,
        Instant updatedAt
        ) {

}
