package com.rental.repository;

import com.rental.entity.Property;
import com.rental.entity.enums.PropertyStatus;
import com.rental.entity.enums.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByType(PropertyType type);

    long countByType(PropertyType type);

    long countByStatus(PropertyStatus status);

    boolean existsByPropertyCodeIgnoreCase(String propertyCode);

    boolean existsByPropertyCodeIgnoreCaseAndIdNot(String propertyCode, Long id);

    @Query("""
            SELECT p FROM Property p
            WHERE LOWER(p.propertyCode) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.address) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY p.propertyCode
            """)
    List<Property> search(@Param("q") String query);
}
