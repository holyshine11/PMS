package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.RoomNumberRequest;
import com.hola.hotel.dto.response.RoomNumberResponse;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomNumberServiceImpl implements RoomNumberService {

    private final RoomNumberRepository roomNumberRepository;
    private final PropertyRepository propertyRepository;
    private final HotelMapper hotelMapper;

    @Override
    public List<RoomNumberResponse> getRoomNumbers(Long propertyId) {
        return roomNumberRepository.findAllByPropertyIdOrderBySortOrderAscRoomNumberAsc(propertyId).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<RoomNumberResponse> getRoomNumbers(Long propertyId, Pageable pageable) {
        return roomNumberRepository.findAllByPropertyIdOrderBySortOrderAscRoomNumberAsc(propertyId, pageable)
                .map(hotelMapper::toResponse);
    }

    @Override
    public RoomNumberResponse getRoomNumber(Long id) {
        return hotelMapper.toResponse(findRoomNumberById(id));
    }

    @Override
    @Transactional
    public RoomNumberResponse createRoomNumber(Long propertyId, RoomNumberRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        if (roomNumberRepository.existsByPropertyIdAndRoomNumber(propertyId, request.getRoomNumber())) {
            throw new HolaException(ErrorCode.ROOM_NUMBER_DUPLICATE);
        }

        RoomNumber roomNumber = RoomNumber.builder()
                .property(property)
                .roomNumber(request.getRoomNumber())
                .descriptionKo(request.getDescriptionKo())
                .descriptionEn(request.getDescriptionEn())
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            roomNumber.deactivate();
        }
        if (request.getSortOrder() != null) roomNumber.changeSortOrder(request.getSortOrder());

        RoomNumber saved = roomNumberRepository.save(roomNumber);
        log.info("호수 생성: {} - 프로퍼티: {}", saved.getRoomNumber(), property.getPropertyName());
        return hotelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoomNumberResponse updateRoomNumber(Long id, RoomNumberRequest request) {
        RoomNumber roomNumber = findRoomNumberById(id);
        roomNumber.update(request.getRoomNumber(), request.getDescriptionKo(), request.getDescriptionEn());

        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                roomNumber.activate();
            } else {
                roomNumber.deactivate();
            }
        }
        if (request.getSortOrder() != null) roomNumber.changeSortOrder(request.getSortOrder());

        return hotelMapper.toResponse(roomNumber);
    }

    @Override
    @Transactional
    public void deleteRoomNumber(Long id) {
        RoomNumber roomNumber = findRoomNumberById(id);
        roomNumber.softDelete();
        log.info("호수 삭제: {}", roomNumber.getRoomNumber());
    }

    @Override
    public boolean existsRoomNumber(Long propertyId, String roomNumber) {
        return roomNumberRepository.existsByPropertyIdAndRoomNumber(propertyId, roomNumber);
    }

    private RoomNumber findRoomNumberById(Long id) {
        return roomNumberRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.ROOM_NUMBER_NOT_FOUND));
    }
}
