package com.hola.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransactionCodeGroupCreateRequest {

    @NotBlank @Size(max = 20)
    private String groupCode;

    @NotBlank @Size(max = 100)
    private String groupNameKo;

    @Size(max = 100)
    private String groupNameEn;

    @NotBlank
    private String groupType;       // MAIN / SUB

    private Long parentGroupId;     // SUB인 경우 필수

    private Integer sortOrder;
}
