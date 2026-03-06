package com.hola.room.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 객실 타입 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class RoomTypeUpdateRequest {

    @Size(max = 2000, message = "설명은 2000자 이하입니다.")
    private String description;

    private BigDecimal roomSize;

    @Size(max = 2000, message = "객실 특징은 2000자 이하입니다.")
    private String features;

    @NotNull(message = "어른 최대 수용 인원을 입력해주세요.")
    @Min(value = 1, message = "어른 최대 수용 인원은 1명 이상이어야 합니다.")
    @Max(value = 99, message = "어른 최대 수용 인원은 99명 이하입니다.")
    private Integer maxAdults;

    @NotNull(message = "어린이 최대 수용 인원을 입력해주세요.")
    @Min(value = 0, message = "어린이 최대 수용 인원은 0명 이상이어야 합니다.")
    @Max(value = 99, message = "어린이 최대 수용 인원은 99명 이하입니다.")
    private Integer maxChildren;

    private Boolean extraBedYn;
    private Boolean useYn;
    private Integer sortOrder;

    /** 층/호수 매핑 데이터 */
    private List<RoomTypeCreateRequest.FloorRoomData> floors;

    /** 무료 서비스 옵션 매핑 */
    private List<RoomTypeCreateRequest.ServiceOptionData> freeServiceOptions;

    /** 유료 서비스 옵션 매핑 */
    private List<RoomTypeCreateRequest.ServiceOptionData> paidServiceOptions;
}
