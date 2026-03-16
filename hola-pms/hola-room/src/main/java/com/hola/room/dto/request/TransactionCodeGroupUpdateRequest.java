package com.hola.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransactionCodeGroupUpdateRequest {

    @NotBlank @Size(max = 100)
    private String groupNameKo;

    @Size(max = 100)
    private String groupNameEn;

    private Integer sortOrder;

    private Boolean useYn;
}
