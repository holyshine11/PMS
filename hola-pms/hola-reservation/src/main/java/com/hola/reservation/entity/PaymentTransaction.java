package com.hola.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 거래 이력 엔티티 (경량 - BaseEntity 미상속)
 */
@Entity
@Table(name = "rsv_payment_transaction")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_reservation_id", nullable = false)
    private Long masterReservationId;

    @Column(name = "transaction_seq", nullable = false)
    private Integer transactionSeq;

    /** 거래 유형: PAYMENT, REFUND, CANCEL_FEE */
    @Column(name = "transaction_type", nullable = false, length = 20)
    @Builder.Default
    private String transactionType = "PAYMENT";

    /** 결제 수단: CARD, CASH */
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "KRW";

    @Column(name = "transaction_status", nullable = false, length = 20)
    @Builder.Default
    private String transactionStatus = "COMPLETED";

    /** VAN 승인번호 */
    @Column(name = "approval_no", length = 50)
    private String approvalNo;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    // === PG 확장 필드 ===

    /** PG 제공자 ("KICC", "MOCK") */
    @Column(name = "pg_provider", length = 20)
    private String pgProvider;

    /** PG 거래고유번호 */
    @Column(name = "pg_cno", length = 20)
    private String pgCno;

    /** 멱등성 키 (UUID) */
    @Column(name = "pg_transaction_id", length = 60)
    private String pgTransactionId;

    /** PG 거래상태 코드 */
    @Column(name = "pg_status_code", length = 10)
    private String pgStatusCode;

    /** PG 승인번호 */
    @Column(name = "pg_approval_no", length = 100)
    private String pgApprovalNo;

    /** PG 승인일시 (yyyyMMddHHmmss) */
    @Column(name = "pg_approval_date", length = 14)
    private String pgApprovalDate;

    /** 마스킹 카드번호 */
    @Column(name = "pg_card_no", length = 20)
    private String pgCardNo;

    /** 발급사명 */
    @Column(name = "pg_issuer_name", length = 50)
    private String pgIssuerName;

    /** 매입사명 */
    @Column(name = "pg_acquirer_name", length = 50)
    private String pgAcquirerName;

    /** 할부개월 (0=일시불) */
    @Column(name = "pg_installment_month")
    @Builder.Default
    private Integer pgInstallmentMonth = 0;

    /** 카드종류 (신용/체크/기프트) */
    @Column(name = "pg_card_type", length = 10)
    private String pgCardType;

    /** 원본 응답 JSON (감사 추적용) */
    @Column(name = "pg_raw_response", columnDefinition = "TEXT")
    private String pgRawResponse;

    // === PG 환불 재시도용 업데이트 메서드 ===

    /**
     * PG 환불 재시도 결과 업데이트
     */
    public void updatePgRefundResult(String transactionStatus, String pgCno,
                                      String pgApprovalNo, String pgStatusCode, String pgTransactionId) {
        this.transactionStatus = transactionStatus;
        this.pgCno = pgCno;
        this.pgApprovalNo = pgApprovalNo;
        this.pgStatusCode = pgStatusCode;
        this.pgTransactionId = pgTransactionId;
    }

    /**
     * 메모 업데이트 (재시도 실패 시 에러 메시지 추가용)
     */
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
}
