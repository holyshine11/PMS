package com.hola.hotel.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하우스키핑 이슈/메모 등록 요청
 */
@Getter
@NoArgsConstructor
public class HkTaskIssueCreateRequest {

    @NotBlank(message = "이슈 유형을 선택해주세요.")
    private String issueType;  // MEMO, MAINTENANCE, SUPPLY_SHORT, LOST_FOUND, DAMAGE

    @NotBlank(message = "내용을 입력해주세요.")
    private String description;

    private String imagePath;
}
