package com.hola.reservation.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 예약 보증금 엔티티
 */
@Entity
@Table(name = "rsv_reservation_deposit")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReservationDeposit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_reservation_id", nullable = false)
    private MasterReservation masterReservation;

    @Column(name = "deposit_method", nullable = false, length = 20)
    @Builder.Default
    private String depositMethod = "CREDIT_CARD";

    @Column(name = "card_company", length = 50)
    private String cardCompany;

    @Column(name = "card_number_encrypted", length = 500)
    private String cardNumberEncrypted;

    @Column(name = "card_cvc_encrypted", length = 500)
    private String cardCvcEncrypted;

    @Column(name = "card_expiry_date", length = 10)
    private String cardExpiryDate;

    @Column(name = "card_password_encrypted", length = 500)
    private String cardPasswordEncrypted;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "KRW";

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * 보증금 정보 수정
     */
    public void update(String depositMethod, String cardCompany,
                       String cardNumberEncrypted, String cardCvcEncrypted,
                       String cardExpiryDate, String cardPasswordEncrypted,
                       String currency, BigDecimal amount) {
        this.depositMethod = depositMethod;
        this.cardCompany = cardCompany;
        this.cardNumberEncrypted = cardNumberEncrypted;
        this.cardCvcEncrypted = cardCvcEncrypted;
        this.cardExpiryDate = cardExpiryDate;
        this.cardPasswordEncrypted = cardPasswordEncrypted;
        this.currency = currency;
        this.amount = amount;
    }
}
