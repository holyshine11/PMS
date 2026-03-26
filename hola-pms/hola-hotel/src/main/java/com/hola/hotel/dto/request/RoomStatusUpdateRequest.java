package com.hola.hotel.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 객실 HK/FO 상태 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RoomStatusUpdateRequest {

    @Pattern(regexp = "^(CLEAN|DIRTY|OOO|OOS|INSPECTED|PICKUP)$", message = "유효하지 않은 HK 상태입니다")
    private String hkStatus;

    @Pattern(regexp = "^(VACANT|OCCUPIED)$", message = "유효하지 않은 FO 상태입니다")
    private String foStatus;

    @Size(max = 500, message = "메모는 500자 이내로 입력해주세요")
    private String memo;

    private Long assigneeId;
}
