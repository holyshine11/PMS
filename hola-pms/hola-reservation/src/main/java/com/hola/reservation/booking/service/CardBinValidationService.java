package com.hola.reservation.booking.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.booking.dto.response.CardBinValidationResponse;
import org.springframework.stereotype.Service;

/**
 * Card BIN 검증 서비스
 * - 카드 번호 앞 6자리(BIN)로 네트워크 판별
 */
@Service
public class CardBinValidationService {

    /**
     * BIN 검증 및 카드 네트워크 판별
     * @param bin 카드 BIN (6자리)
     */
    public CardBinValidationResponse validate(String bin) {
        if (bin == null || bin.length() < 4 || bin.length() > 8 || !bin.matches("\\d+")) {
            throw new HolaException(ErrorCode.BOOKING_INVALID_CARD_BIN);
        }

        String network = detectNetwork(bin);
        String displayName = getDisplayName(network);
        boolean supported = !"UNKNOWN".equals(network);

        return CardBinValidationResponse.builder()
                .bin(bin)
                .network(network)
                .displayName(displayName)
                .cardType("CREDIT")
                .supported(supported)
                .build();
    }

    /**
     * BIN 접두사 기반 카드 네트워크 판별
     */
    private String detectNetwork(String bin) {
        int firstDigit = Character.getNumericValue(bin.charAt(0));

        // AMEX: 34, 37
        if (bin.startsWith("34") || bin.startsWith("37")) {
            return "AMEX";
        }

        // JCB: 3528-3589
        if (bin.length() >= 4) {
            int prefix4 = Integer.parseInt(bin.substring(0, 4));
            if (prefix4 >= 3528 && prefix4 <= 3589) {
                return "JCB";
            }

            // MASTERCARD: 2221-2720
            if (prefix4 >= 2221 && prefix4 <= 2720) {
                return "MASTERCARD";
            }
        }

        // MASTERCARD: 51-55
        if (bin.length() >= 2) {
            int prefix2 = Integer.parseInt(bin.substring(0, 2));
            if (prefix2 >= 51 && prefix2 <= 55) {
                return "MASTERCARD";
            }
        }

        // UNIONPAY: 62
        if (bin.startsWith("62")) {
            return "UNIONPAY";
        }

        // VISA: 4
        if (firstDigit == 4) {
            return "VISA";
        }

        return "UNKNOWN";
    }

    private String getDisplayName(String network) {
        return switch (network) {
            case "VISA" -> "Visa";
            case "MASTERCARD" -> "Mastercard";
            case "AMEX" -> "American Express";
            case "JCB" -> "JCB";
            case "UNIONPAY" -> "UnionPay";
            default -> "Unknown";
        };
    }
}
