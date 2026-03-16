package com.hola.room.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCodeGroupResponse {

    private Long id;
    private Long propertyId;
    private String groupCode;
    private String groupNameKo;
    private String groupNameEn;
    private String groupType;
    private Long parentGroupId;
    private Integer sortOrder;
    private Boolean useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
