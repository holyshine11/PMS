package com.hola.common.auth.repository;

import com.hola.common.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 대상 유형별 전체 권한 목록 (정렬순)
     */
    List<Role> findByTargetTypeAndDeletedAtIsNullOrderBySortOrderAsc(String targetType);

    /**
     * 동일 호텔 내 권한명 중복 확인
     */
    boolean existsByRoleNameAndHotelIdAndDeletedAtIsNull(String roleName, Long hotelId);

    /**
     * 동일 호텔 내 권한명 중복 확인 (자기 자신 제외)
     */
    boolean existsByRoleNameAndHotelIdAndIdNotAndDeletedAtIsNull(String roleName, Long hotelId, Long id);

    /**
     * 해당 프로퍼티에 이미 권한이 존재하는지 확인
     */
    boolean existsByPropertyIdAndTargetTypeAndDeletedAtIsNull(Long propertyId, String targetType);

    /**
     * 동일 호텔+프로퍼티 내 권한명 중복 확인
     */
    boolean existsByRoleNameAndHotelIdAndPropertyIdAndDeletedAtIsNull(String roleName, Long hotelId, Long propertyId);

    /**
     * 동일 호텔+프로퍼티 내 권한명 중복 확인 (자기 자신 제외)
     */
    boolean existsByRoleNameAndHotelIdAndPropertyIdAndIdNotAndDeletedAtIsNull(String roleName, Long hotelId, Long propertyId, Long id);
}
