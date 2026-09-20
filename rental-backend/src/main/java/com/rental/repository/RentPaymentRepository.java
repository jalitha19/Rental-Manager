package com.rental.repository;

import com.rental.entity.RentPayment;
import com.rental.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {

    List<RentPayment> findByRentalIdOrderByPeriodMonthDesc(Long rentalId);

    List<RentPayment> findByPeriodMonth(LocalDate periodMonth);

    boolean existsByRentalIdAndPeriodMonth(Long rentalId, LocalDate periodMonth);

    boolean existsByRentalTenantIdAndPeriodMonth(Long rentalTenantId, LocalDate periodMonth);

    @Modifying(flushAutomatically = true)
    @Query("delete from RentPayment p where p.rental.id = :rentalId")
    void deleteByRentalId(@Param("rentalId") Long rentalId);

    @Query("""
            SELECT p FROM RentPayment p
            WHERE (:rentalId IS NULL OR p.rental.id = :rentalId)
              AND (:periodMonth IS NULL OR p.periodMonth = :periodMonth)
              AND (:status IS NULL OR p.status = :status)
            ORDER BY p.periodMonth DESC
            """)
    List<RentPayment> filter(
            @Param("rentalId") Long rentalId,
            @Param("periodMonth") LocalDate periodMonth,
            @Param("status") PaymentStatus status
    );
}
