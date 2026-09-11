package com.rental.controller;

import com.rental.dto.PhoneRequest;
import com.rental.dto.PhoneResponse;
import com.rental.dto.TenantRequest;
import com.rental.dto.TenantResponse;
import com.rental.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    public List<TenantResponse> list() {
        return tenantService.list();
    }

    @GetMapping("/search")
    public List<TenantResponse> search(@RequestParam String q) {
        return tenantService.search(q);
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable Long id) {
        return tenantService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody TenantRequest request) {
        return tenantService.create(request);
    }

    @PutMapping("/{id}")
    public TenantResponse update(@PathVariable Long id, @Valid @RequestBody TenantRequest request) {
        return tenantService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- phone numbers ----------

    @PostMapping("/{id}/phones")
    @ResponseStatus(HttpStatus.CREATED)
    public PhoneResponse addPhone(@PathVariable Long id, @Valid @RequestBody PhoneRequest request) {
        return tenantService.addPhone(id, request);
    }

    @PutMapping("/{id}/phones/{phoneId}")
    public PhoneResponse updatePhone(
            @PathVariable Long id,
            @PathVariable Long phoneId,
            @Valid @RequestBody PhoneRequest request
    ) {
        return tenantService.updatePhone(id, phoneId, request);
    }

    @DeleteMapping("/{id}/phones/{phoneId}")
    public ResponseEntity<Void> removePhone(@PathVariable Long id, @PathVariable Long phoneId) {
        tenantService.removePhone(id, phoneId);
        return ResponseEntity.noContent().build();
    }
}
