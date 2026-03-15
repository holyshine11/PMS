package com.hola.reservation.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 부킹엔진 전용 응답 래퍼
 * - 산하정보통신 API 형식 호환
 * - 성공: {"result": {"RESULT_YN": "Y", "data": {...}}}
 * - 실패: {"result": {"RESULT_YN": "N", "RESULT_CODE": "...", "RESULT_MESSAGE": "..."}}
 */
@Getter
public class BookingResponse<T> {

    private final BookingResult<T> result;

    private BookingResponse(BookingResult<T> result) {
        this.result = result;
    }

    public static <T> BookingResponse<T> success(T data) {
        return new BookingResponse<>(BookingResult.success(data));
    }

    public static BookingResponse<Void> success() {
        return new BookingResponse<>(BookingResult.success(null));
    }

    public static <T> BookingResponse<T> error(String code, String message) {
        return new BookingResponse<>(BookingResult.error(code, message));
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookingResult<T> {

        @JsonProperty("RESULT_YN")
        private final String resultYn;

        @JsonProperty("RESULT_CODE")
        private final String resultCode;

        @JsonProperty("RESULT_MESSAGE")
        private final String resultMessage;

        private final T data;

        private BookingResult(String resultYn, String resultCode, String resultMessage, T data) {
            this.resultYn = resultYn;
            this.resultCode = resultCode;
            this.resultMessage = resultMessage;
            this.data = data;
        }

        static <T> BookingResult<T> success(T data) {
            return new BookingResult<>("Y", null, null, data);
        }

        static <T> BookingResult<T> error(String code, String message) {
            return new BookingResult<>("N", code, message, null);
        }
    }
}
