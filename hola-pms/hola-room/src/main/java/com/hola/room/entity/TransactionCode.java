package com.hola.room.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 트랜잭션 코드 (부과 항목의 회계적 분류 단위)
 * Opera PMS의 Transaction Code에 해당
 */
@Entity
@Table(name = "rm_transaction_code",
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "transaction_code"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCode extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    // FK → TransactionCodeGroup (SUB 레벨)
    @Column(name = "transaction_group_id", nullable = false)
    private Long transactionGroupId;

    // 코드 번호 (예: "1000", "1010", "2000")
    @Column(name = "transaction_code", nullable = false, length = 10)
    private String transactionCode;

    @Column(name = "code_name_ko", nullable = false, length = 200)
    private String codeNameKo;

    @Column(name = "code_name_en", length = 200)
    private String codeNameEn;

    // 매출 분류: LODGING, FOOD_BEVERAGE, MISC, TAX, NON_REVENUE
    @Column(name = "revenue_category", nullable = false, length = 20)
    private String revenueCategory;

    // CHARGE: 부과, PAYMENT: 결제
    @Column(name = "code_type", nullable = false, length = 10)
    private String codeType;

    public void update(String codeNameKo, String codeNameEn, Long transactionGroupId,
                       String revenueCategory, Integer sortOrder) {
        this.codeNameKo = codeNameKo;
        this.codeNameEn = codeNameEn;
        this.transactionGroupId = transactionGroupId;
        this.revenueCategory = revenueCategory;
        if (sortOrder != null) {
            this.changeSortOrder(sortOrder);
        }
    }
}
