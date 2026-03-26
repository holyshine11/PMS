package com.hola.reservation.booking.entity;

import com.hola.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * 간편결제 카드 엔티티 (KICC 빌키)
 * - 게스트 이메일 기반으로 카드 연결 (로그인 불필요)
 * - 이메일 당 최대 5개 카드 등록 가능
 */
@Entity
@Table(name = "rsv_easy_pay_card")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EasyPayCard extends BaseEntity {

    /** 게스트 이메일 (카드 소유자 식별) */
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /** KICC 빌키 (batchKey) */
    @Column(name = "batch_key", nullable = false, length = 100)
    private String batchKey;

    /** 마스킹 카드번호 (UI 표시용, 예: 1234-56**-****-7890) */
    @Column(name = "card_mask_no", length = 30)
    private String cardMaskNo;

    /** 발급사명 (삼성/현대/신한 등) */
    @Column(name = "issuer_name", length = 50)
    private String issuerName;

    /** 카드종류 (신용/체크) */
    @Column(name = "card_type", length = 20)
    private String cardType;

    /** PG 거래고유번호 (빌키 발급 시 pgCno) */
    @Column(name = "pg_cno", length = 20)
    private String pgCno;

    /** 카드 별칭 (선택) */
    @Column(name = "card_alias", length = 50)
    private String cardAlias;
}
