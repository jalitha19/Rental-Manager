package com.rental.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateFirstAdminRequestTest {

    @Test
    void firstAdminRequestShouldCaptureUsernameAndPassword() {
        CreateFirstAdminRequest request = new CreateFirstAdminRequest("alice", "secret123");

        assertEquals("alice", request.username());
        assertEquals("secret123", request.password());
    }
}
