package com.hola.room.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.room.dto.request.RoomClassCreateRequest;
import com.hola.room.dto.request.RoomClassUpdateRequest;
import com.hola.room.dto.response.RoomClassResponse;
import com.hola.room.entity.RoomClass;
import com.hola.room.mapper.RoomClassMapper;
import com.hola.room.repository.RoomClassRepository;
import com.hola.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 객실 클래스 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomClassServiceImpl implements RoomClassService {

    private final RoomClassRepository roomClassRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomClassMapper roomClassMapper;

    @Override
    public List<RoomClassResponse> getRoomClasses(Long propertyId) {
        return roomClassRepository.findAllByPropertyIdOrderBySortOrderAscRoomClassNameAsc(propertyId)
                .stream()
                .map(roomClassMapper::toResponse)
                .toList();
    }

    @Override
    public RoomClassResponse getRoomClass(Long id) {
        RoomClass roomClass = findById(id);
        return roomClassMapper.toResponse(roomClass);
    }

    @Override
    @Transactional
    public RoomClassResponse createRoomClass(Long propertyId, RoomClassCreateRequest request) {
        // 코드 중복 확인
        if (roomClassRepository.existsByPropertyIdAndRoomClassCode(propertyId, request.getRoomClassCode())) {
            throw new HolaException(ErrorCode.ROOM_CLASS_CODE_DUPLICATE);
        }

        RoomClass roomClass = RoomClass.builder()
                .propertyId(propertyId)
                .roomClassCode(request.getRoomClassCode())
                .roomClassName(request.getRoomClassName())
                .description(request.getDescription())
                .build();

        // 사용여부 설정
        if (request.getUseYn() != null && !request.getUseYn()) {
            roomClass.deactivate();
        }

        // 정렬순서 설정
        if (request.getSortOrder() != null) {
            roomClass.changeSortOrder(request.getSortOrder());
        }

        RoomClass saved = roomClassRepository.save(roomClass);
        log.info("객실 클래스 생성: {} ({}) - 프로퍼티: {}", saved.getRoomClassName(), saved.getRoomClassCode(), propertyId);

        return roomClassMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoomClassResponse updateRoomClass(Long id, RoomClassUpdateRequest request) {
        RoomClass roomClass = findById(id);

        roomClass.update(request.getRoomClassName(), request.getDescription());

        // 사용여부 토글
        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                roomClass.activate();
            } else {
                roomClass.deactivate();
            }
        }

        // 정렬순서
        if (request.getSortOrder() != null) {
            roomClass.changeSortOrder(request.getSortOrder());
        }

        log.info("객실 클래스 수정: {} ({})", roomClass.getRoomClassName(), roomClass.getRoomClassCode());
        return roomClassMapper.toResponse(roomClass);
    }

    @Override
    @Transactional
    public void deleteRoomClass(Long id) {
        RoomClass roomClass = findById(id);
        // 하위 RoomType 존재 시 삭제 불가
        if (roomTypeRepository.existsByRoomClassId(id)) {
            throw new HolaException(ErrorCode.ROOM_CLASS_HAS_ROOM_TYPES);
        }
        roomClass.softDelete();
        log.info("객실 클래스 삭제: {} ({})", roomClass.getRoomClassName(), roomClass.getRoomClassCode());
    }

    @Override
    public boolean existsRoomClassCode(Long propertyId, String roomClassCode) {
        return roomClassRepository.existsByPropertyIdAndRoomClassCode(propertyId, roomClassCode);
    }

    private RoomClass findById(Long id) {
        return roomClassRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_CLASS_NOT_FOUND));
    }
}
