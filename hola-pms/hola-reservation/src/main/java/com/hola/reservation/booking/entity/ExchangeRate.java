package com.hola.reservation.booking.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 환율 테이블
 * - 기준통화: KRW
 * - 지원통화: USD, JPY, CNY
 * - 일자별 환율 관리
 */
@Entity
@Table(name = "rsv_exchange_rate",
        uniqueConstraints = @UniqueConstraint(columnNames = {"currency_code", "rate_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ExchangeRate extends BaseEntity {

    /** 통화 코드 (USD, JPY, CNY) */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    /** 적용 날짜 */
    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    /** KRW → 대상통화 환율 (1 KRW = ? 대상통화) */
    @Column(name = "rate_value", nullable = false, precision = 18, scale = 8)
    private BigDecimal rateValue;

    /** 활성 상태 */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
