package com.rental.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalHouses,
        long totalRooms,
        long occupiedProperties,
        long availableProperties,
        long maintenanceProperties,
        long totalTenants,
        long currentMonthPaid,
        long currentMonthUnpaid,
        BigDecimal currentMonthPaidAmount,
        BigDecimal currentMonthUnpaidAmount,
        List<RentPendingItem> rentPending,
        long roomsOccupied,
        long roomsAvailable,
        long roomsTenants,
        long housesOccupied,
        long housesAvailable,
        long housesTenants
        ) {

}
