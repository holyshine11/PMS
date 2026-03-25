package com.hola.reservation.booking.pg.kicc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * KICC 메시지 인증값 (HmacSHA256) 유틸리티
 *
 * - 승인 응답 검증: pgCno + "|" + amount + "|" + transactionDate
 * - 취소/환불 요청: pgCno + "|" + shopTransactionId
 */
public class KiccHmacUtils {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String DELIMITER = "|";

    private KiccHmacUtils() {}

    /**
     * HmacSHA256 메시지 인증값 생성
     *
     * @param secretKey KICC 시크릿키
     * @param parts 구분자(|)로 연결할 값들
     * @return Base64 인코딩된 HMAC 해시
     */
    public static String generate(String secretKey, String... parts) {
        String data = String.join(DELIMITER, parts);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 생성 실패", e);
        }
    }

    /**
     * 승인 응답 메시지 인증값 생성
     * 형식: pgCno + "|" + amount + "|" + transactionDate
     */
    public static String generateForApproval(String secretKey, String pgCno, Long amount, String transactionDate) {
        return generate(secretKey, pgCno, String.valueOf(amount), transactionDate);
    }

    /**
     * 취소/환불 요청 메시지 인증값 생성
     * 형식: pgCno + "|" + shopTransactionId
     */
    public static String generateForRevise(String secretKey, String pgCno, String shopTransactionId) {
        return generate(secretKey, pgCno, shopTransactionId);
    }

    /**
     * 메시지 인증값 검증
     *
     * @param received 수신된 HMAC 값
     * @param secretKey KICC 시크릿키
     * @param parts 구분자(|)로 연결할 값들
     * @return 일치 여부
     */
    public static boolean verify(String received, String secretKey, String... parts) {
        if (received == null) {
            return false;
        }
        String expected = generate(secretKey, parts);
        return received.equals(expected);
    }

    /**
     * 승인 응답 메시지 인증값 검증
     */
    public static boolean verifyApproval(String received, String secretKey,
                                          String pgCno, Long amount, String transactionDate) {
        return verify(received, secretKey, pgCno, String.valueOf(amount), transactionDate);
    }
}
