package com.hola.common.auth.repository;

import com.hola.common.auth.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 대상 유형별 사용 중인 메뉴 목록 (정렬순)
     */
    List<Menu> findByTargetTypeAndUseYnTrueAndDeletedAtIsNullOrderBySortOrderAsc(String targetType);
}
