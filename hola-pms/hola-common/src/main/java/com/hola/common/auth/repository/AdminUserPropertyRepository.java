package com.hola.common.auth.repository;

import com.hola.common.auth.entity.AdminUserProperty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminUserPropertyRepository extends JpaRepository<AdminUserProperty, Long> {

    List<AdminUserProperty> findByAdminUserId(Long adminUserId);

    List<AdminUserProperty> findByPropertyId(Long propertyId);

    void deleteByAdminUserId(Long adminUserId);
}
