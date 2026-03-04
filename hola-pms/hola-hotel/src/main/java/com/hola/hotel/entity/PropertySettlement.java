package com.hola.hotel.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 프로퍼티 정산정보 엔티티
 */
@Entity
@Table(name = "htl_property_settlement", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"property_id", "country_type"})
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PropertySettlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "country_type", nullable = false, length = 2)
    private String countryType;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "account_holder", length = 100)
    private String accountHolder;

    @Column(name = "routing_number", length = 50)
    private String routingNumber;

    @Column(name = "swift_code", length = 50)
    private String swiftCode;

    @Column(name = "settlement_day", length = 10)
    private String settlementDay;

    @Column(name = "bank_book_path", length = 500)
    private String bankBookPath;

    /**
     * 정산정보 필드 일괄 수정
     */
    public void update(String accountNumber, String bankName, String bankCode,
                       String accountHolder, String routingNumber, String swiftCode,
                       String settlementDay, String bankBookPath) {
        this.accountNumber = accountNumber;
        this.bankName = bankName;
        this.bankCode = bankCode;
        this.accountHolder = accountHolder;
        this.routingNumber = routingNumber;
        this.swiftCode = swiftCode;
        this.settlementDay = settlementDay;
        this.bankBookPath = bankBookPath;
    }
}
