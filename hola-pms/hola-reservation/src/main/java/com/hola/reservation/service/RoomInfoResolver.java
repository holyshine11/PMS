package com.hola.reservation.service;

import com.hola.hotel.entity.Floor;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.FloorRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.PaidServiceOptionRepository;
import com.hola.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 객실 관련 ID -> 이름 벌크 해석 공통 유틸리티
 * - 여러 서비스에서 반복되는 collect IDs -> findAllById -> toMap 패턴 중앙화
 */
@Component
@RequiredArgsConstructor
public class RoomInfoResolver {

    private final FloorRepository floorRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;

    /**
     * 층 ID -> "12 | 12층" (floorNumber | floorName)
     */
    public Map<Long, String> resolveFloorNames(Set<Long> floorIds) {
        if (floorIds == null || floorIds.isEmpty()) return Collections.emptyMap();
        return floorRepository.findAllById(floorIds).stream()
                .collect(Collectors.toMap(Floor::getId,
                        f -> f.getFloorNumber() + (f.getFloorName() != null ? " | " + f.getFloorName() : "")));
    }

    /**
     * 층 ID -> "12" (floorNumber만, 캘린더/타임라인용)
     */
    public Map<Long, String> resolveFloorNumbers(Set<Long> floorIds) {
        if (floorIds == null || floorIds.isEmpty()) return Collections.emptyMap();
        return floorRepository.findAllById(floorIds).stream()
                .collect(Collectors.toMap(Floor::getId, Floor::getFloorNumber));
    }

    /**
     * 호수 ID -> "1201"
     */
    public Map<Long, String> resolveRoomNumbers(Set<Long> roomNumberIds) {
        if (roomNumberIds == null || roomNumberIds.isEmpty()) return Collections.emptyMap();
        return roomNumberRepository.findAllById(roomNumberIds).stream()
                .collect(Collectors.toMap(RoomNumber::getId, RoomNumber::getRoomNumber));
    }

    /**
     * 객실타입 ID -> "DLX" (roomTypeCode)
     */
    public Map<Long, String> resolveRoomTypeCodes(Set<Long> roomTypeIds) {
        if (roomTypeIds == null || roomTypeIds.isEmpty()) return Collections.emptyMap();
        return roomTypeRepository.findAllById(roomTypeIds).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getRoomTypeCode));
    }

    /**
     * 유료 서비스 옵션 ID -> "조식" (serviceNameKo)
     */
    public Map<Long, String> resolveServiceOptionNames(Set<Long> serviceOptionIds) {
        if (serviceOptionIds == null || serviceOptionIds.isEmpty()) return Collections.emptyMap();
        return paidServiceOptionRepository.findAllById(serviceOptionIds).stream()
                .collect(Collectors.toMap(PaidServiceOption::getId, PaidServiceOption::getServiceNameKo));
    }
}
