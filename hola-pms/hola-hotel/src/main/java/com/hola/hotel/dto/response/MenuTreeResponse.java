package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 메뉴 트리 응답 DTO (1depth + children)
 */
@Getter
@Builder
@AllArgsConstructor
public class MenuTreeResponse {

    private Long id;
    private String menuCode;
    private String menuName;
    private Integer depth;
    private List<MenuTreeResponse> children;
}
