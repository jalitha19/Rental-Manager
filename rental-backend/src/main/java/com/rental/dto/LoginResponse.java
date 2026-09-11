package com.rental.dto;

public record LoginResponse(
        String token,
        String username,
        long expiresInMs
) {
}
