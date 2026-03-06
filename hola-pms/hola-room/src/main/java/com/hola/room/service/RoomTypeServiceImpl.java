package com.hola.room.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.room.dto.request.RoomTypeCreateRequest;
import com.hola.room.dto.request.RoomTypeUpdateRequest;
import com.hola.room.dto.response.RoomTypeListResponse;
import com.hola.room.dto.response.RoomTypeResponse;
import com.hola.room.entity.*;
import com.hola.room.mapper.RoomTypeMapper;
import com.hola.room.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 객실 타입 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeFloorRepository roomTypeFloorRepository;
    private final RoomTypeFreeServiceRepository roomTypeFreeServiceRepository;
    private final RoomTypePaidServiceRepository roomTypePaidServiceRepository;
    private final RoomClassRepository roomClassRepository;
    private final FreeServiceOptionRepository freeServiceOptionRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final RoomTypeMapper roomTypeMapper;

    @Override
    public List<RoomTypeListResponse> getRoomTypes(Long propertyId) {
        List<RoomType> roomTypes = roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(propertyId);

        return roomTypes.stream().map(rt -> {
            RoomClass roomClass = roomClassRepository.findById(rt.getRoomClassId()).orElse(null);
            long roomCount = roomTypeFloorRepository.countByRoomTypeId(rt.getId());
            return roomTypeMapper.toListResponse(rt, roomClass, roomCount);
        }).toList();
    }

    @Override
    public RoomTypeResponse getRoomType(Long id) {
        RoomType roomType = findById(id);
        RoomClass roomClass = roomClassRepository.findById(roomType.getRoomClassId()).orElse(null);
        List<RoomTypeFloor> floorMappings = roomTypeFloorRepository.findAllByRoomTypeId(id);
        long roomCount = floorMappings.size();

        // 서비스 옵션 매핑 조회
        List<RoomTypeFreeService> freeMappings = roomTypeFreeServiceRepository.findAllByRoomTypeId(id);
        List<RoomTypePaidService> paidMappings = roomTypePaidServiceRepository.findAllByRoomTypeId(id);

        // 무료 서비스 옵션 상세
        List<RoomTypeResponse.ServiceOptionInfo> freeOptions = freeMappings.stream().map(m -> {
            FreeServiceOption opt = freeServiceOptionRepository.findById(m.getFreeServiceOptionId()).orElse(null);
            if (opt == null) return null;
            return RoomTypeResponse.ServiceOptionInfo.builder()
                    .id(opt.getId())
                    .serviceOptionCode(opt.getServiceOptionCode())
                    .serviceNameKo(opt.getServiceNameKo())
                    .serviceType(opt.getServiceType())
                    .quantity(m.getQuantity())
                    .build();
        }).filter(java.util.Objects::nonNull).toList();

        // 유료 서비스 옵션 상세
        List<RoomTypeResponse.ServiceOptionInfo> paidOptions = paidMappings.stream().map(m -> {
            PaidServiceOption opt = paidServiceOptionRepository.findById(m.getPaidServiceOptionId()).orElse(null);
            if (opt == null) return null;
            return RoomTypeResponse.ServiceOptionInfo.builder()
                    .id(opt.getId())
                    .serviceOptionCode(opt.getServiceOptionCode())
                    .serviceNameKo(opt.getServiceNameKo())
                    .serviceType(opt.getServiceType())
                    .quantity(m.getQuantity())
                    .build();
        }).filter(java.util.Objects::nonNull).toList();

        return roomTypeMapper.toResponse(roomType, roomClass, floorMappings, roomCount, freeOptions, paidOptions);
    }

    @Override
    @Transactional
    public RoomTypeResponse createRoomType(Long propertyId, RoomTypeCreateRequest request) {
        // 코드 중복 확인
        if (roomTypeRepository.existsByPropertyIdAndRoomTypeCode(propertyId, request.getRoomTypeCode())) {
            throw new HolaException(ErrorCode.ROOM_TYPE_CODE_DUPLICATE);
        }

        // 객실 클래스 존재 확인
        RoomClass roomClass = roomClassRepository.findById(request.getRoomClassId())
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_CLASS_NOT_FOUND));

        RoomType roomType = RoomType.builder()
                .propertyId(propertyId)
                .roomClassId(request.getRoomClassId())
                .roomTypeCode(request.getRoomTypeCode())
                .description(request.getDescription())
                .roomSize(request.getRoomSize())
                .features(request.getFeatures())
                .maxAdults(request.getMaxAdults())
                .maxChildren(request.getMaxChildren())
                .extraBedYn(request.getExtraBedYn() != null ? request.getExtraBedYn() : false)
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            roomType.deactivate();
        }
        if (request.getSortOrder() != null) {
            roomType.changeSortOrder(request.getSortOrder());
        }

        RoomType saved = roomTypeRepository.save(roomType);

        // 층/호수 매핑 저장
        List<RoomTypeFloor> floorMappings = saveFloorMappings(saved.getId(), request.getFloors());

        // 서비스 옵션 매핑 저장
        saveFreeServiceMappings(saved.getId(), request.getFreeServiceOptions());
        savePaidServiceMappings(saved.getId(), request.getPaidServiceOptions());

        log.info("객실 타입 생성: {} - 프로퍼티: {}", saved.getRoomTypeCode(), propertyId);
        return roomTypeMapper.toResponse(saved, roomClass, floorMappings, floorMappings.size(), List.of(), List.of());
    }

    @Override
    @Transactional
    public RoomTypeResponse updateRoomType(Long id, RoomTypeUpdateRequest request) {
        RoomType roomType = findById(id);

        roomType.update(
                request.getDescription(),
                request.getRoomSize(),
                request.getFeatures(),
                request.getMaxAdults(),
                request.getMaxChildren(),
                request.getExtraBedYn() != null ? request.getExtraBedYn() : roomType.getExtraBedYn()
        );

        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                roomType.activate();
            } else {
                roomType.deactivate();
            }
        }
        if (request.getSortOrder() != null) {
            roomType.changeSortOrder(request.getSortOrder());
        }

        // 층/호수 매핑 갱신 (전체 삭제 후 재등록)
        roomTypeFloorRepository.deleteAllByRoomTypeId(id);
        List<RoomTypeFloor> floorMappings = saveFloorMappings(id, request.getFloors());

        // 서비스 옵션 매핑 갱신 (전체 삭제 후 재등록)
        roomTypeFreeServiceRepository.deleteAllByRoomTypeId(id);
        roomTypePaidServiceRepository.deleteAllByRoomTypeId(id);
        saveFreeServiceMappings(id, request.getFreeServiceOptions());
        savePaidServiceMappings(id, request.getPaidServiceOptions());

        RoomClass roomClass = roomClassRepository.findById(roomType.getRoomClassId()).orElse(null);
        log.info("객실 타입 수정: {}", roomType.getRoomTypeCode());
        return roomTypeMapper.toResponse(roomType, roomClass, floorMappings, floorMappings.size(), List.of(), List.of());
    }

    @Override
    @Transactional
    public void deleteRoomType(Long id) {
        RoomType roomType = findById(id);
        // 매핑 데이터 삭제
        roomTypeFloorRepository.deleteAllByRoomTypeId(id);
        roomTypeFreeServiceRepository.deleteAllByRoomTypeId(id);
        roomTypePaidServiceRepository.deleteAllByRoomTypeId(id);
        roomType.softDelete();
        log.info("객실 타입 삭제: {}", roomType.getRoomTypeCode());
    }

    @Override
    public boolean existsRoomTypeCode(Long propertyId, String roomTypeCode) {
        return roomTypeRepository.existsByPropertyIdAndRoomTypeCode(propertyId, roomTypeCode);
    }

    private RoomType findById(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_TYPE_NOT_FOUND));
    }

    /**
     * 층/호수 매핑 일괄 저장
     */
    private List<RoomTypeFloor> saveFloorMappings(Long roomTypeId,
                                                   List<RoomTypeCreateRequest.FloorRoomData> floors) {
        if (floors == null || floors.isEmpty()) {
            return List.of();
        }

        List<RoomTypeFloor> mappings = new ArrayList<>();
        for (RoomTypeCreateRequest.FloorRoomData floor : floors) {
            if (floor.getFloorId() == null || floor.getRoomNumberIds() == null) continue;
            for (Long roomNumberId : floor.getRoomNumberIds()) {
                mappings.add(RoomTypeFloor.builder()
                        .roomTypeId(roomTypeId)
                        .floorId(floor.getFloorId())
                        .roomNumberId(roomNumberId)
                        .build());
            }
        }

        return roomTypeFloorRepository.saveAll(mappings);
    }

    /**
     * 무료 서비스 옵션 매핑 일괄 저장
     */
    private void saveFreeServiceMappings(Long roomTypeId,
                                          List<RoomTypeCreateRequest.ServiceOptionData> options) {
        if (options == null || options.isEmpty()) return;

        List<RoomTypeFreeService> mappings = options.stream()
                .filter(o -> o.getId() != null)
                .map(o -> RoomTypeFreeService.builder()
                        .roomTypeId(roomTypeId)
                        .freeServiceOptionId(o.getId())
                        .quantity(o.getQuantity() != null ? o.getQuantity() : 1)
                        .build())
                .toList();

        roomTypeFreeServiceRepository.saveAll(mappings);
    }

    /**
     * 유료 서비스 옵션 매핑 일괄 저장
     */
    private void savePaidServiceMappings(Long roomTypeId,
                                          List<RoomTypeCreateRequest.ServiceOptionData> options) {
        if (options == null || options.isEmpty()) return;

        List<RoomTypePaidService> mappings = options.stream()
                .filter(o -> o.getId() != null)
                .map(o -> RoomTypePaidService.builder()
                        .roomTypeId(roomTypeId)
                        .paidServiceOptionId(o.getId())
                        .quantity(o.getQuantity() != null ? o.getQuantity() : 1)
                        .build())
                .toList();

        roomTypePaidServiceRepository.saveAll(mappings);
    }
}
