package com.hola.reservation.service;

import com.hola.reservation.dto.response.ReservationChangeLogResponse;
import com.hola.reservation.entity.ReservationChangeLog;
import com.hola.reservation.repository.ReservationChangeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReservationChangeLogServiceImpl implements ReservationChangeLogService {

    private final ReservationChangeLogRepository changeLogRepository;

    @Override
    @Transactional
    public void log(Long masterReservationId, Long subReservationId,
                    String category, String changeType, String fieldName,
                    String oldValue, String newValue, String description) {
        changeLogRepository.save(ReservationChangeLog.builder()
                .masterReservationId(masterReservationId)
                .subReservationId(subReservationId)
                .changeCategory(category)
                .changeType(changeType)
                .fieldName(fieldName)
                .oldValue(truncate(oldValue))
                .newValue(truncate(newValue))
                .description(truncate(description))
                .build());
    }

    @Override
    @Transactional
    public void logFieldChange(Long masterReservationId, Long subReservationId,
                               String category, String fieldName,
                               Object oldValue, Object newValue, String fieldLabel) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        String oldStr = oldValue != null ? String.valueOf(oldValue) : null;
        String newStr = newValue != null ? String.valueOf(newValue) : null;
        String desc = fieldLabel + " 변경: " + (oldStr != null ? oldStr : "(없음)")
                + " -> " + (newStr != null ? newStr : "(없음)");
        log(masterReservationId, subReservationId, category, "UPDATE", fieldName, oldStr, newStr, desc);
    }

    @Override
    @Transactional
    public void logStatusChange(Long masterReservationId, Long subReservationId,
                                String oldStatus, String newStatus) {
        String desc = "상태 변경: " + oldStatus + " -> " + newStatus;
        log(masterReservationId, subReservationId, "STATUS", "STATUS_CHANGE",
                "reservationStatus", oldStatus, newStatus, desc);
    }

    @Override
    @Transactional
    public void logUpgrade(Long masterReservationId, Long subReservationId,
                           String fromRoomType, String toRoomType,
                           String upgradeType, BigDecimal priceDiff) {
        String typeLabel = switch (upgradeType) {
            case "COMPLIMENTARY" -> "무료";
            case "UPSELL" -> "업셀";
            default -> "유료";
        };
        String desc = "객실 업그레이드: " + fromRoomType + " -> " + toRoomType
                + " (" + typeLabel + ")";
        if (priceDiff != null && priceDiff.compareTo(BigDecimal.ZERO) > 0) {
            desc += ", +" + priceDiff.toPlainString() + "원";
        }
        log(masterReservationId, subReservationId, "UPGRADE", "UPGRADE",
                null, fromRoomType, toRoomType, desc);
    }

    @Override
    public List<ReservationChangeLogResponse> getHistory(Long masterReservationId) {
        return changeLogRepository.findAllByMasterReservationIdOrderByCreatedAtDesc(masterReservationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ReservationChangeLogResponse toResponse(ReservationChangeLog entity) {
        return ReservationChangeLogResponse.builder()
                .id(entity.getId())
                .subReservationId(entity.getSubReservationId())
                .changeCategory(entity.getChangeCategory())
                .changeType(entity.getChangeType())
                .fieldName(entity.getFieldName())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .description(entity.getDescription())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() > 500 ? value.substring(0, 497) + "..." : value;
    }
}
