package com.hola.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NameMaskingUtil - 한글 이름 마스킹")
class NameMaskingUtilTest {

    @Test
    @DisplayName("1글자 이름 - 마스킹 없음")
    void maskKoreanName_oneChar_unchanged() {
        assertThat(NameMaskingUtil.maskKoreanName("김")).isEqualTo("김");
    }

    @Test
    @DisplayName("2글자 이름 - 첫글자 + * (김*)")
    void maskKoreanName_twoChars_masksSecond() {
        assertThat(NameMaskingUtil.maskKoreanName("김수")).isEqualTo("김*");
    }

    @Test
    @DisplayName("3글자 이름 - 첫글자 + * + 끝글자 (김*수)")
    void maskKoreanName_threeChars_masksMiddle() {
        assertThat(NameMaskingUtil.maskKoreanName("김민수")).isEqualTo("김*수");
    }

    @Test
    @DisplayName("4글자 이름 - 첫글자 + ** + 끝글자 (김**수)")
    void maskKoreanName_fourChars_masksMiddleTwo() {
        assertThat(NameMaskingUtil.maskKoreanName("김민정수")).isEqualTo("김**수");
    }

    @Test
    @DisplayName("5글자 이름 - 첫글자 + ** + 끝글자")
    void maskKoreanName_fiveChars_masksMiddle() {
        assertThat(NameMaskingUtil.maskKoreanName("남궁민정수")).isEqualTo("남**수");
    }

    @Test
    @DisplayName("null 입력 - null 반환")
    void maskKoreanName_null_returnsNull() {
        assertThat(NameMaskingUtil.maskKoreanName(null)).isNull();
    }

    @Test
    @DisplayName("공백만 입력 - 원본 반환")
    void maskKoreanName_blank_returnsOriginal() {
        assertThat(NameMaskingUtil.maskKoreanName("   ")).isEqualTo("   ");
    }
}
