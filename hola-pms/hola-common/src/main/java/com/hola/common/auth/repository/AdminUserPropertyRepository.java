package com.hola.common.auth.repository;

import com.hola.common.auth.entity.AdminUserProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdminUserPropertyRepository extends JpaRepository<AdminUserProperty, Long> {

    List<AdminUserProperty> findByAdminUserId(Long adminUserId);

    List<AdminUserProperty> findByPropertyId(Long propertyId);

    /** 특정 사용자가 특정 프로퍼티에 접근 권한이 있는지 확인 (exists 쿼리) */
    boolean existsByAdminUserIdAndPropertyId(Long adminUserId, Long propertyId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AdminUserProperty p WHERE p.adminUserId = :adminUserId")
    void deleteByAdminUserId(Long adminUserId);
}
