package com.hola.common.auth.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 권한 마스터 엔티티
 */
@Entity
@Table(name = "sys_role",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_name", "hotel_id", "property_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "hotel_id")
    private Long hotelId;

    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "target_type", nullable = false, length = 20)
    @Builder.Default
    private String targetType = "HOTEL_ADMIN";

    /**
     * 권한 정보 수정
     */
    public void update(String roleName, Boolean useYn) {
        this.roleName = roleName;
        if (useYn != null) {
            if (useYn) activate();
            else deactivate();
        }
    }
}
