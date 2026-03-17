package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.response.RoomRackItemResponse;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.RoomNumberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 객실 상태 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomStatusServiceImpl implements RoomStatusService {

    private final RoomNumberRepository roomNumberRepository;

    @Override
    @Transactional
    public void updateRoomStatus(Long roomNumberId, Long propertyId, String hkStatus, String foStatus, String memo) {
        RoomNumber room = roomNumberRepository.findById(roomNumberId)
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));

        // propertyId 소속 검증 (IDOR 방지)
        if (!room.getProperty().getId().equals(propertyId)) {
            throw new HolaException(ErrorCode.FORBIDDEN);
        }

        if (hkStatus != null) {
            room.updateHkStatus(hkStatus, memo);
        }
        if (foStatus != null) {
            if ("VACANT".equals(foStatus)) {
                room.checkOut();
            } else if ("OCCUPIED".equals(foStatus)) {
                room.checkIn();
            }
        }
    }

    @Override
    public Map<String, Long> getStatusSummary(Long propertyId) {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("VC", roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "CLEAN", "VACANT"));
        summary.put("VD", roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "DIRTY", "VACANT"));
        summary.put("OC", roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "CLEAN", "OCCUPIED"));
        summary.put("OD", roomNumberRepository.countByPropertyIdAndHkStatusAndFoStatus(propertyId, "DIRTY", "OCCUPIED"));
        summary.put("OOO", roomNumberRepository.countByPropertyIdAndHkStatus(propertyId, "OOO"));
        summary.put("OOS", roomNumberRepository.countByPropertyIdAndHkStatus(propertyId, "OOS"));
        return summary;
    }

    @Override
    public List<RoomRackItemResponse> getRoomRackItems(Long propertyId) {
        List<RoomNumber> rooms = roomNumberRepository.findAllByPropertyIdOrderBySortOrderAscRoomNumberAsc(propertyId);
        List<RoomRackItemResponse> items = new ArrayList<>();

        for (RoomNumber room : rooms) {
            String statusCode = RoomStatusService.calcStatusCode(room.getHkStatus(), room.getFoStatus());
            items.add(RoomRackItemResponse.builder()
                    .roomNumberId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .hkStatus(room.getHkStatus())
                    .foStatus(room.getFoStatus())
                    .statusCode(statusCode)
                    .hkMemo(room.getHkMemo())
                    .build());
        }
        return items;
    }
}
