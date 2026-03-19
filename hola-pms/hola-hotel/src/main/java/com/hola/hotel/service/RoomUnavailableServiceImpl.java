package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.RoomUnavailableRequest;
import com.hola.hotel.dto.response.RoomUnavailableResponse;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.entity.RoomUnavailable;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.hotel.repository.RoomUnavailableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OOO/OOS 객실 관리 서비스 구현
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomUnavailableServiceImpl implements RoomUnavailableService {

    private final RoomUnavailableRepository roomUnavailableRepository;
    private final RoomNumberRepository roomNumberRepository;

    @Override
    public List<RoomUnavailableResponse> getList(Long propertyId, String type) {
        List<RoomUnavailable> list = (type != null && !type.isEmpty())
                ? roomUnavailableRepository.findByPropertyIdAndUnavailableTypeOrderByFromDateDesc(propertyId, type)
                : roomUnavailableRepository.findByPropertyIdOrderByFromDateDesc(propertyId);

        // 벌크 조회: 객실번호
        Set<Long> roomIds = list.stream().map(RoomUnavailable::getRoomNumberId).collect(Collectors.toSet());
        Map<Long, RoomNumber> roomMap = roomIds.isEmpty() ? Map.of() :
                roomNumberRepository.findAllById(roomIds).stream()
                        .collect(Collectors.toMap(RoomNumber::getId, Function.identity()));

        return list.stream().map(r -> toResponse(r, roomMap.get(r.getRoomNumberId()))).collect(Collectors.toList());
    }

    @Override
    public RoomUnavailableResponse getById(Long id, Long propertyId) {
        RoomUnavailable entity = findAndValidate(id, propertyId);
        RoomNumber room = roomNumberRepository.findById(entity.getRoomNumberId()).orElse(null);
        return toResponse(entity, room);
    }

    @Override
    @Transactional
    public RoomUnavailableResponse create(Long propertyId, RoomUnavailableRequest request) {
        // 객실 존재 및 소속 검증
        RoomNumber room = roomNumberRepository.findById(request.getRoomNumberId())
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));
        if (!room.getProperty().getId().equals(propertyId)) {
            throw new HolaException(ErrorCode.FORBIDDEN);
        }

        // 동일 객실 기간 중복 체크
        List<RoomUnavailable> overlapping = roomUnavailableRepository.findOverlapping(
                request.getRoomNumberId(), request.getFromDate(), request.getThroughDate());
        if (!overlapping.isEmpty()) {
            throw new HolaException(ErrorCode.ROOM_UNAVAILABLE_OVERLAP);
        }

        RoomUnavailable entity = RoomUnavailable.builder()
                .propertyId(propertyId)
                .roomNumberId(request.getRoomNumberId())
                .unavailableType(request.getUnavailableType())
                .reasonCode(request.getReasonCode())
                .reasonDetail(request.getReasonDetail())
                .fromDate(request.getFromDate())
                .throughDate(request.getThroughDate())
                .returnStatus(request.getReturnStatus() != null ? request.getReturnStatus() : "DIRTY")
                .build();

        RoomUnavailable saved = roomUnavailableRepository.save(entity);

        // 현재 유효 기간이면 객실 HK 상태도 즉시 변경
        LocalDate today = LocalDate.now();
        if (!today.isBefore(request.getFromDate()) && !today.isAfter(request.getThroughDate())) {
            room.updateHkStatus(request.getUnavailableType(), request.getReasonDetail());
        }

        return toResponse(saved, room);
    }

    @Override
    @Transactional
    public RoomUnavailableResponse update(Long id, Long propertyId, RoomUnavailableRequest request) {
        RoomUnavailable entity = findAndValidate(id, propertyId);

        // 동일 객실 기간 중복 체크 (자기 자신 제외)
        List<RoomUnavailable> overlapping = roomUnavailableRepository.findOverlappingExclude(
                entity.getRoomNumberId(), request.getFromDate(), request.getThroughDate(), id);
        if (!overlapping.isEmpty()) {
            throw new HolaException(ErrorCode.ROOM_UNAVAILABLE_OVERLAP);
        }

        entity.update(request.getReasonCode(), request.getReasonDetail(),
                      request.getFromDate(), request.getThroughDate(),
                      request.getReturnStatus());

        RoomNumber room = roomNumberRepository.findById(entity.getRoomNumberId()).orElse(null);
        return toResponse(entity, room);
    }

    @Override
    @Transactional
    public void delete(Long id, Long propertyId) {
        RoomUnavailable entity = findAndValidate(id, propertyId);
        entity.softDelete();

        // flush하여 @SQLRestriction이 softDelete된 레코드를 제외하도록 함
        roomUnavailableRepository.flush();

        // 같은 객실에 다른 활성 OOO/OOS가 남아있는지 확인
        LocalDate today = LocalDate.now();
        List<RoomUnavailable> remaining = roomUnavailableRepository.findOverlapping(
                entity.getRoomNumberId(), today, today);

        RoomNumber room = roomNumberRepository.findById(entity.getRoomNumberId()).orElse(null);
        if (room != null) {
            if (remaining.isEmpty()) {
                // 남은 OOO/OOS가 없으면 returnStatus로 복구
                String returnStatus = entity.getReturnStatus() != null ? entity.getReturnStatus() : "DIRTY";
                room.updateHkStatus(returnStatus, null);
            }
            // remaining이 있으면 기존 OOO/OOS 상태 유지 (변경 안 함)
        }
    }

    @Override
    @Transactional
    public int releaseExpired(Long propertyId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<RoomUnavailable> activeList = roomUnavailableRepository.findActiveByPropertyId(propertyId, yesterday);
        // through_date가 어제 이전인 것들 → 만료 처리
        int count = 0;
        for (RoomUnavailable r : activeList) {
            if (r.getThroughDate().isBefore(LocalDate.now())) {
                RoomNumber room = roomNumberRepository.findById(r.getRoomNumberId()).orElse(null);
                if (room != null) {
                    String returnStatus = r.getReturnStatus() != null ? r.getReturnStatus() : "DIRTY";
                    room.updateHkStatus(returnStatus, null);
                }
                r.softDelete();
                count++;
            }
        }
        return count;
    }

    private RoomUnavailable findAndValidate(Long id, Long propertyId) {
        RoomUnavailable entity = roomUnavailableRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!entity.getPropertyId().equals(propertyId)) {
            throw new HolaException(ErrorCode.FORBIDDEN);
        }
        return entity;
    }

    private RoomUnavailableResponse toResponse(RoomUnavailable entity, RoomNumber room) {
        return RoomUnavailableResponse.builder()
                .id(entity.getId())
                .roomNumberId(entity.getRoomNumberId())
                .roomNumber(room != null ? room.getRoomNumber() : null)
                .unavailableType(entity.getUnavailableType())
                .reasonCode(entity.getReasonCode())
                .reasonDetail(entity.getReasonDetail())
                .fromDate(entity.getFromDate())
                .throughDate(entity.getThroughDate())
                .returnStatus(entity.getReturnStatus())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }
}
