package com.hola.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HolaResponse - API 응답 형식")
class HolaResponseTest {

    @Test
    @DisplayName("success(data) - 코드 HOLA-0000, success=true")
    void success_withData_correctCodeAndFlag() {
        HolaResponse<String> response = HolaResponse.success("테스트 데이터");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("HOLA-0000");
        assertThat(response.getMessage()).isEqualTo("성공");
        assertThat(response.getData()).isEqualTo("테스트 데이터");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("success() - 빈 응답, data null")
    void success_empty_noData() {
        HolaResponse<Void> response = HolaResponse.success();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("HOLA-0000");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("error - success=false, 커스텀 코드/메시지")
    void error_setsCodeAndMessage() {
        HolaResponse<Object> response = HolaResponse.error("HOLA-4000", "예약을 찾을 수 없습니다.");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("HOLA-4000");
        assertThat(response.getMessage()).isEqualTo("예약을 찾을 수 없습니다.");
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }
}
