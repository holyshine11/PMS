package com.hola.common.auth.repository;

import com.hola.common.auth.entity.RoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoleMenuRepository extends JpaRepository<RoleMenu, Long> {

    /**
     * 특정 권한의 메뉴 매핑 목록
     */
    List<RoleMenu> findByRoleId(Long roleId);

    /**
     * 특정 권한의 메뉴 매핑 전체 삭제
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RoleMenu rm WHERE rm.roleId = :roleId")
    void deleteByRoleId(Long roleId);
}
