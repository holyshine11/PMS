package com.hola.hotel.repository;

import com.hola.hotel.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 프로퍼티 이미지 Repository
 */
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    /** 프로퍼티 전체 이미지 (타입별 정렬) */
    List<PropertyImage> findAllByPropertyIdOrderByImageTypeAscSortOrderAsc(Long propertyId);

    /** 특정 타입 이미지 */
    List<PropertyImage> findAllByPropertyIdAndImageTypeOrderBySortOrderAsc(Long propertyId, String imageType);

    /** 특정 참조 ID 이미지 (객실타입 등) */
    List<PropertyImage> findAllByPropertyIdAndImageTypeAndReferenceIdOrderBySortOrderAsc(
            Long propertyId, String imageType, Long referenceId);
}
