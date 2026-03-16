package com.hola.room.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 트랜잭션 코드 그룹 (Main → Sub 2단계 계층)
 * Opera PMS의 Transaction Code Group에 해당
 */
@Entity
@Table(name = "rm_transaction_code_group",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "group_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCodeGroup extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "group_code", nullable = false, length = 20)
    private String groupCode;

    @Column(name = "group_name_ko", nullable = false, length = 100)
    private String groupNameKo;

    @Column(name = "group_name_en", length = 100)
    private String groupNameEn;

    // MAIN: 대분류, SUB: 소분류
    @Column(name = "group_type", nullable = false, length = 10)
    private String groupType;

    // SUB인 경우 상위 MAIN 그룹 ID
    @Column(name = "parent_group_id")
    private Long parentGroupId;

    public void update(String groupNameKo, String groupNameEn, Integer sortOrder) {
        this.groupNameKo = groupNameKo;
        this.groupNameEn = groupNameEn;
        if (sortOrder != null) {
            this.changeSortOrder(sortOrder);
        }
    }
}
