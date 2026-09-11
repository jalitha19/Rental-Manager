package com.rental.controller;

import com.rental.dto.PropertyRequest;
import com.rental.dto.PropertyResponse;
import com.rental.entity.enums.PropertyType;
import com.rental.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Covers both Houses and Rooms -- they're the same underlying entity
 * (Property) distinguished by the `type` field. The frontend's separate
 * "Houses" and "Rooms" sidebar sections both call this controller with
 * ?type=HOUSE or ?type=ROOM.
 */
@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public List<PropertyResponse> list(@RequestParam(required = false) PropertyType type) {
        return propertyService.list(type);
    }

    @GetMapping("/search")
    public List<PropertyResponse> search(@RequestParam String q) {
        return propertyService.search(q);
    }

    @GetMapping("/{id}")
    public PropertyResponse get(@PathVariable Long id) {
        return propertyService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(@Valid @RequestBody PropertyRequest request) {
        return propertyService.create(request);
    }

    @PutMapping("/{id}")
    public PropertyResponse update(@PathVariable Long id, @Valid @RequestBody PropertyRequest request) {
        return propertyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
