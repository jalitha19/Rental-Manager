package com.rental.dto;

import com.rental.entity.enums.TenantType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TenantRequest(
        @NotNull(message = "Tenant type is required")
        TenantType tenantType,
        @NotBlank(message = "Name is required")
        String fullName,
        String nicNumber,
        String contactPerson,
        String address,
        String notes,
        String photoUrl,
        /**
         * Only used on creation -- lets the UI submit a tenant with their
         * initial phone number(s) in one request. Ignored on update; use the
         * dedicated /phones endpoints to add/edit/remove numbers after
         * creation.
         */
        @Valid
        List<PhoneRequest> phones
        ) {

}
