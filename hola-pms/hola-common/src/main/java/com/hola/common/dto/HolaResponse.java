package com.hola.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 통일된 API 응답 형식
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HolaResponse<T> {

    private static final String SUCCESS_CODE = "HOLA-0000";
    private static final String SUCCESS_MESSAGE = "성공";

    private boolean success;
    private String code;
    private String message;
    private T data;
    private PageInfo pagination;
    private LocalDateTime timestamp;

    public static <T> HolaResponse<T> success(T data) {
        return HolaResponse.<T>builder()
                .success(true)
                .code(SUCCESS_CODE)
                .message(SUCCESS_MESSAGE)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> HolaResponse<T> success(T data, PageInfo pageInfo) {
        return HolaResponse.<T>builder()
                .success(true)
                .code(SUCCESS_CODE)
                .message(SUCCESS_MESSAGE)
                .data(data)
                .pagination(pageInfo)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static HolaResponse<Void> success() {
        return HolaResponse.<Void>builder()
                .success(true)
                .code(SUCCESS_CODE)
                .message(SUCCESS_MESSAGE)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> HolaResponse<T> error(String code, String message) {
        return HolaResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
