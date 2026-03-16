package com.hola.room.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCodeGroupTreeResponse {

    private Long id;
    private String groupCode;
    private String groupNameKo;
    private String groupNameEn;
    private String groupType;
    private Integer sortOrder;
    private Boolean useYn;
    private List<TransactionCodeGroupTreeResponse> children;
}
