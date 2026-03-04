package com.hola.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * 페이징 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static PageInfo from(Page<?> page) {
        return PageInfo.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
