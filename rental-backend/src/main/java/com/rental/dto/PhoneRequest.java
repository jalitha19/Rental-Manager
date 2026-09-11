package com.rental.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneRequest(
        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        String label
) {
}
