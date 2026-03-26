package com.hola.hotel.mapper;

import com.hola.hotel.dto.request.HkTaskCreateRequest;
import com.hola.hotel.dto.response.*;
import com.hola.hotel.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 하우스키핑 Entity <-> DTO 변환 매퍼
 */
@Component
public class HkTaskMapper {

    public HkTask toEntity(HkTaskCreateRequest request, Long propertyId) {
        return HkTask.builder()
                .propertyId(propertyId)
                .roomNumberId(request.getRoomNumberId())
                .taskType(request.getTaskType())
                .taskDate(LocalDate.now())
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .credit(request.getCredit() != null ? request.getCredit() : null)
                .note(request.getNote())
                .nextCheckinAt(request.getNextCheckinAt())
                .build();
    }

    public HkTaskResponse toResponse(HkTask task, String roomNumber, String floorNumber,
                                     String roomTypeName, String assignedToName, String inspectedByName) {
        return HkTaskResponse.builder()
                .id(task.getId())
                .propertyId(task.getPropertyId())
                .roomNumberId(task.getRoomNumberId())
                .roomNumber(roomNumber)
                .floorNumber(floorNumber)
                .roomTypeName(roomTypeName)
                .taskSheetId(task.getTaskSheetId())
                .taskType(task.getTaskType())
                .taskDate(task.getTaskDate())
                .status(task.getStatus())
                .priority(task.getPriority())
                .credit(task.getCredit())
                .assignedTo(task.getAssignedTo())
                .assignedToName(assignedToName)
                .assignedBy(task.getAssignedBy())
                .assignedAt(task.getAssignedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .inspectedAt(task.getInspectedAt())
                .inspectedByName(inspectedByName)
                .estimatedEnd(task.getEstimatedEnd())
                .durationMinutes(task.getDurationMinutes())
                .reservationId(task.getReservationId())
                .nextCheckinAt(task.getNextCheckinAt())
                .note(task.getNote())
                .createdAt(task.getCreatedAt())
                .build();
    }

    public HkTaskSheetResponse toResponse(HkTaskSheet sheet, String assignedToName) {
        return HkTaskSheetResponse.builder()
                .id(sheet.getId())
                .propertyId(sheet.getPropertyId())
                .sheetName(sheet.getSheetName())
                .sheetDate(sheet.getSheetDate())
                .assignedTo(sheet.getAssignedTo())
                .assignedToName(assignedToName)
                .totalRooms(sheet.getTotalRooms())
                .totalCredits(sheet.getTotalCredits())
                .completedRooms(sheet.getCompletedRooms())
                .build();
    }

    public HkConfigResponse toResponse(HkConfig config) {
        return HkConfigResponse.builder()
                .id(config.getId())
                .propertyId(config.getPropertyId())
                .inspectionRequired(config.getInspectionRequired())
                .autoCreateCheckout(config.getAutoCreateCheckout())
                .autoCreateStayover(config.getAutoCreateStayover())
                .defaultCheckoutCredit(config.getDefaultCheckoutCredit())
                .defaultStayoverCredit(config.getDefaultStayoverCredit())
                .defaultTurndownCredit(config.getDefaultTurndownCredit())
                .defaultDeepCleanCredit(config.getDefaultDeepCleanCredit())
                .defaultTouchUpCredit(config.getDefaultTouchUpCredit())
                .rushThresholdMinutes(config.getRushThresholdMinutes())
                .stayoverEnabled(config.getStayoverEnabled())
                .stayoverFrequency(config.getStayoverFrequency())
                .turndownEnabled(config.getTurndownEnabled())
                .dndPolicy(config.getDndPolicy())
                .dndMaxSkipDays(config.getDndMaxSkipDays())
                .dailyTaskGenTime(config.getDailyTaskGenTime())
                .odTransitionTime(config.getOdTransitionTime())
                .build();
    }

    public HkTaskIssueResponse toResponse(HkTaskIssue issue, String roomNumber) {
        return toResponse(issue, roomNumber, null);
    }

    public HkTaskIssueResponse toResponse(HkTaskIssue issue, String roomNumber, String createdByName) {
        return HkTaskIssueResponse.builder()
                .id(issue.getId())
                .taskId(issue.getTaskId())
                .propertyId(issue.getPropertyId())
                .roomNumberId(issue.getRoomNumberId())
                .roomNumber(roomNumber)
                .issueType(issue.getIssueType())
                .description(issue.getDescription())
                .imagePath(issue.getImagePath())
                .resolved(issue.getResolved())
                .resolvedAt(issue.getResolvedAt())
                .resolvedBy(issue.getResolvedBy())
                .createdAt(issue.getCreatedAt())
                .createdBy(issue.getCreatedBy())
                .createdByName(createdByName)
                .build();
    }
}
