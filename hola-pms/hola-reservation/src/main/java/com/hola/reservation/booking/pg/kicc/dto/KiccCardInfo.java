package com.hola.reservation.booking.pg.kicc.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KICC 카드 결제 상세 (paymentInfo.cardInfo)
 * - KICC API 필드명: issuerCode/issuerName/acquirerCode/acquirerName/cardGubun
 * - @JsonAlias로 KICC 원본 필드명과 기존 축약 필드명 모두 지원
 */
@Getter
@NoArgsConstructor
public class KiccCardInfo {

    /** 카드번호 (마스킹 또는 빌키) */
    private String cardNo;

    /** 마스킹 카드번호 (UI 표시용) */
    private String cardMaskNo;

    /** 발급사 코드 */
    @JsonAlias({"issuerCode", "issuerCd"})
    private String issuerCode;

    /** 발급사명 */
    @JsonAlias({"issuerName", "issuerNm"})
    private String issuerName;

    /** 매입사 코드 */
    @JsonAlias({"acquirerCode", "acquirerCd"})
    private String acquirerCode;

    /** 매입사명 */
    @JsonAlias({"acquirerName", "acquirerNm"})
    private String acquirerName;

    /** 할부개월 (0=일시불) */
    private Integer installmentMonth;

    /** 카드종류: 신용="N", 체크="Y", 기프트="G" */
    @JsonAlias({"cardGubun", "cardType"})
    private String cardGubun;

    /** 카드주체: 개인="P", 법인="C", 기타="N" */
    private String cardBizGubun;

    /** 부분취소 가능여부 (Y/N) */
    private String partCancelUsed;

    /** BC제휴사 카드코드 (빌키발급 시 응답) */
    private String subCardCd;

    // === 편의 메서드 (기존 코드 호환) ===

    public String getIssuerCd() { return issuerCode; }
    public String getIssuerNm() { return issuerName; }
    public String getAcquirerCd() { return acquirerCode; }
    public String getAcquirerNm() { return acquirerName; }
    public String getCardType() {
        if (cardGubun == null) return null;
        return switch (cardGubun) {
            case "N" -> "신용";
            case "Y" -> "체크";
            case "G" -> "기프트";
            default -> cardGubun;
        };
    }
}
