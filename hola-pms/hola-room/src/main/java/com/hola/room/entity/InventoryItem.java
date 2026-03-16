package com.hola.room.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 재고 아이템 마스터 (엑스트라 베드, 유아용 침대 등)
 * managementType에 따라 자체관리(INTERNAL) 또는 외부 ERP 연동(EXTERNAL)
 */
@Entity
@Table(name = "rm_inventory_item",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "item_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "item_code", nullable = false, length = 30)
    private String itemCode;

    @Column(name = "item_name_ko", nullable = false, length = 200)
    private String itemNameKo;

    @Column(name = "item_name_en", length = 200)
    private String itemNameEn;

    // EXTRA_BED, CRIB, ROLLAWAY, EQUIPMENT
    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    // INTERNAL: 자체관리, EXTERNAL: 외부 ERP 연동
    @Column(name = "management_type", nullable = false, length = 10)
    @Builder.Default
    private String managementType = "INTERNAL";

    // SAP 등 외부 시스템 아이템 코드 (EXTERNAL일 때 사용)
    @Column(name = "external_system_code", length = 50)
    private String externalSystemCode;

    // INTERNAL: 총 보유 수량, EXTERNAL: 참조용
    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private Integer totalQuantity = 0;

    public void update(String itemNameKo, String itemNameEn, String itemType,
                       String managementType, String externalSystemCode,
                       Integer totalQuantity) {
        this.itemNameKo = itemNameKo;
        this.itemNameEn = itemNameEn;
        this.itemType = itemType;
        this.managementType = managementType;
        this.externalSystemCode = externalSystemCode;
        this.totalQuantity = totalQuantity != null ? totalQuantity : 0;
    }
}
