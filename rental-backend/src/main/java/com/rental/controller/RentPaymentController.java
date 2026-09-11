package com.rental.controller;

import com.rental.dto.MarkPaidRequest;
import com.rental.dto.RentPaymentRequest;
import com.rental.dto.RentPaymentResponse;
import com.rental.dto.RentPaymentUpdateRequest;
import com.rental.entity.enums.PaymentStatus;
import com.rental.service.RentPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RentPaymentController {

    private final RentPaymentService rentPaymentService;

    @GetMapping("/api/rent-payments")
    public List<RentPaymentResponse> list(
            @RequestParam(required = false) Long rentalId,
            @RequestParam(required = false) LocalDate periodMonth,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PaymentStatus status
    ) {
        return rentPaymentService.list(rentalId, periodMonth, year, status);
    }

    @GetMapping("/api/rent-payments/{id}")
    public RentPaymentResponse get(@PathVariable Long id) {
        return rentPaymentService.get(id);
    }

    @DeleteMapping("/api/rent-payments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        rentPaymentService.delete(id);
    }

    @PostMapping("/api/rent-payments")
    @ResponseStatus(HttpStatus.CREATED)
    public RentPaymentResponse create(@Valid @RequestBody RentPaymentRequest request) {
        return rentPaymentService.create(request);
    }

    @PutMapping("/api/rent-payments/{id}")
    public RentPaymentResponse update(@PathVariable Long id, @Valid @RequestBody RentPaymentUpdateRequest request) {
        return rentPaymentService.update(id, request);
    }

    @PostMapping("/api/rent-payments/{id}/mark-paid")
    public RentPaymentResponse markPaid(@PathVariable Long id, @Valid @RequestBody MarkPaidRequest request) {
        return rentPaymentService.markPaid(id, request);
    }

    /**
     * Create missing UNPAID payment rows for active rentals up to the chosen
     * month. If no month is supplied, the current month is used.
     */
    @PostMapping("/api/rent-payments/generate-current-month")
    public List<RentPaymentResponse> generateCurrentMonth(@RequestParam(required = false) LocalDate toMonth) {
        return rentPaymentService.generateCurrentMonthPayments(toMonth);
    }

    @GetMapping("/api/rentals/{rentalId}/payments")
    public List<RentPaymentResponse> rentalHistory(@PathVariable Long rentalId) {
        return rentPaymentService.rentalHistory(rentalId);
    }
}
