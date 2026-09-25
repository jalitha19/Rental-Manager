package com.rental.repository;

import com.rental.entity.PropertyRentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyRentHistoryRepository extends JpaRepository<PropertyRentHistory, Long> {

    List<PropertyRentHistory> findByPropertyIdOrderByEffectiveFromDesc(Long propertyId);
}
