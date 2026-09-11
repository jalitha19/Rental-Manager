package com.rental.dto;

import com.rental.service.RentPaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardResponseTest {

    @Test
    void dashboardResponseCanCarryRoomAndHouseTypeBreakdown() {
        DashboardResponse response = new DashboardResponse(
                3,
                2,
                4,
                5,
                6,
                7,
                8,
                9,
                BigDecimal.TEN,
                BigDecimal.ONE,
                List.of(),
                12,
                13,
                14,
                15,
                16,
                17
        );

        assertEquals(2, response.totalRooms());
        assertEquals(12, response.roomsOccupied());
        assertEquals(13, response.roomsAvailable());
        assertEquals(14, response.roomsTenants());

        assertEquals(3, response.totalHouses());
        assertEquals(15, response.housesOccupied());
        assertEquals(16, response.housesAvailable());
        assertEquals(17, response.housesTenants());
    }

    @Test
    void rentPaymentServiceMonthRangeIncludesEveryMissingPeriodFromStartToCurrentMonth() {
        List<LocalDate> months = RentPaymentService.monthsBetweenInclusive(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 9, 1)
        );

        assertEquals(4, months.size());
        assertEquals(LocalDate.of(2026, 6, 1), months.get(0));
        assertEquals(LocalDate.of(2026, 7, 1), months.get(1));
        assertEquals(LocalDate.of(2026, 8, 1), months.get(2));
        assertEquals(LocalDate.of(2026, 9, 1), months.get(3));
    }

    @Test
    void rentPaymentServiceAccumulatesPartialPaymentsInsteadOfReplacingPaidAmount() {
        BigDecimal total = RentPaymentService.accumulatePaidAmount(
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(15000)
        );

        assertEquals(BigDecimal.valueOf(15000), total);
    }
}
