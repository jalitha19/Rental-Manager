package com.rental.repository;

import com.rental.entity.TenantPhone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantPhoneRepository extends JpaRepository<TenantPhone, Long> {
}
