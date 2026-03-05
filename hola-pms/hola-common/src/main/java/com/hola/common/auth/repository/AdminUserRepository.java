package com.hola.common.auth.repository;

import com.hola.common.auth.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByLoginIdAndDeletedAtIsNull(String loginId);

    boolean existsByLoginIdAndDeletedAtIsNull(String loginId);

    /**
     * 호텔별 HOTEL_ADMIN 목록 (조건 필터링은 Service에서 처리)
     */
    List<AdminUser> findByHotelIdAndAccountTypeAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long hotelId, String accountType);

    /**
     * accountType 기반 관리자 목록 (블루웨이브 관리자 등 호텔 비종속)
     */
    List<AdminUser> findByAccountTypeAndDeletedAtIsNullOrderByCreatedAtDesc(String accountType);

    /**
     * 특정 권한을 사용하는 관리자 존재 여부
     */
    boolean existsByRoleIdAndDeletedAtIsNull(Long roleId);

    /**
     * 회원번호 시퀀스 조회
     */
    @Query(value = "SELECT nextval('sys_member_number_seq')", nativeQuery = true)
    Long getNextMemberNumberSequence();
}
