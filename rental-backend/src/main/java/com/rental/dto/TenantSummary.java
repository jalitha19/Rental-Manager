package com.rental.dto;

import com.rental.entity.enums.TenantType;

public record TenantSummary(
        Long id,
        String fullName,
        TenantType tenantType,
        String photoUrl
        ) {

}
