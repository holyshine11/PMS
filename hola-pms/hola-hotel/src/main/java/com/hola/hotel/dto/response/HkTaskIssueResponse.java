package com.hola.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 하우스키핑 이슈/메모 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HkTaskIssueResponse {

    private Long id;
    private Long taskId;
    private Long propertyId;
    private Long roomNumberId;
    private String roomNumber;
    private String issueType;
    private String description;
    private String imagePath;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private LocalDateTime createdAt;
    private String createdBy;
    private String createdByName;  // 마스킹된 작성자 이름
}
