package com.rental.controller;

import com.rental.dto.*;
import com.rental.entity.enums.RentalStatus;
import com.rental.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @GetMapping("/api/rentals")
    public List<RentalResponse> list(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) RentalStatus status
    ) {
        return rentalService.list(propertyId, tenantId, status);
    }

    @GetMapping("/api/rentals/{id}")
    public RentalResponse get(@PathVariable Long id) {
        return rentalService.get(id);
    }

    @DeleteMapping("/api/rentals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        rentalService.delete(id);
    }

    @PostMapping("/api/rentals")
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse create(@Valid @RequestBody RentalRequest request) {
        return rentalService.create(request);
    }

    @PutMapping("/api/rentals/{id}")
    public RentalResponse update(@PathVariable Long id, @Valid @RequestBody RentalUpdateRequest request) {
        return rentalService.update(id, request);
    }

    @PostMapping("/api/rentals/{id}/end")
    public RentalResponse end(@PathVariable Long id, @Valid @RequestBody EndRentalRequest request) {
        return rentalService.end(id, request);
    }

    /**
     * The "Change Tenant" action -- ends the current occupancy (if any) and
     * starts a new one.
     */
    @PostMapping("/api/properties/{propertyId}/change-tenant")
    public RentalResponse changeTenant(
            @PathVariable Long propertyId,
            @Valid @RequestBody ChangeTenantRequest request
    ) {
        return rentalService.changeTenant(propertyId, request);
    }

    @GetMapping("/api/properties/{propertyId}/rentals")
    public List<RentalResponse> propertyHistory(@PathVariable Long propertyId) {
        return rentalService.propertyHistory(propertyId);
    }

    @GetMapping("/api/tenants/{tenantId}/rentals")
    public List<RentalResponse> tenantHistory(@PathVariable Long tenantId) {
        return rentalService.tenantHistory(tenantId);
    }
}
