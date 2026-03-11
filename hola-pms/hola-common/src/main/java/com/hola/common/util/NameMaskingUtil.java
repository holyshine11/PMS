package com.hola.common.util;

/**
 * 이름 마스킹 유틸리티
 * - 2자: 첫 글자 + * (예: 김* )
 * - 3자: 첫 글자 + * + 마지막 글자 (예: 김*수)
 * - 4자 이상: 첫 글자 + ** + 마지막 글자 (예: 김**수)
 */
public final class NameMaskingUtil {

    private NameMaskingUtil() {
    }

    /**
     * 한글 이름 마스킹
     */
    public static String maskKoreanName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }

        String trimmed = name.trim();
        int length = trimmed.length();

        if (length == 1) {
            return trimmed;
        } else if (length == 2) {
            return trimmed.charAt(0) + "*";
        } else if (length == 3) {
            return String.valueOf(trimmed.charAt(0)) + "*" + trimmed.charAt(length - 1);
        } else {
            return String.valueOf(trimmed.charAt(0)) + "**" + trimmed.charAt(length - 1);
        }
    }
}
