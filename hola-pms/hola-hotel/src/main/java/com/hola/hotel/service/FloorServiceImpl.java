package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.FloorRequest;
import com.hola.hotel.dto.response.FloorResponse;
import com.hola.hotel.entity.Floor;
import com.hola.hotel.entity.Property;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.FloorRepository;
import com.hola.hotel.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FloorServiceImpl implements FloorService {

    private final FloorRepository floorRepository;
    private final PropertyRepository propertyRepository;
    private final HotelMapper hotelMapper;

    @Override
    public List<FloorResponse> getFloors(Long propertyId) {
        return floorRepository.findAllByPropertyIdOrderBySortOrderAscFloorNumberAsc(propertyId).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FloorResponse getFloor(Long id) {
        Floor floor = findFloorById(id);
        return hotelMapper.toResponse(floor);
    }

    @Override
    @Transactional
    public FloorResponse createFloor(Long propertyId, FloorRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        if (floorRepository.existsByPropertyIdAndFloorNumber(propertyId, request.getFloorNumber())) {
            throw new HolaException(ErrorCode.FLOOR_NUMBER_DUPLICATE);
        }

        Floor floor = Floor.builder()
                .property(property)
                .floorNumber(request.getFloorNumber())
                .floorName(request.getFloorName())
                .descriptionKo(request.getDescriptionKo())
                .descriptionEn(request.getDescriptionEn())
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            floor.deactivate();
        }
        if (request.getSortOrder() != null) floor.changeSortOrder(request.getSortOrder());

        Floor saved = floorRepository.save(floor);
        log.info("층 생성: {} - 프로퍼티: {}", saved.getFloorNumber(), property.getPropertyName());
        return hotelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public FloorResponse updateFloor(Long id, FloorRequest request) {
        Floor floor = findFloorById(id);

        // 층 코드 변경 시 중복 확인
        if (!floor.getFloorNumber().equals(request.getFloorNumber())
                && floorRepository.existsByPropertyIdAndFloorNumberAndIdNot(
                    floor.getProperty().getId(), request.getFloorNumber(), id)) {
            throw new HolaException(ErrorCode.FLOOR_NUMBER_DUPLICATE);
        }

        floor.update(request.getFloorNumber(), request.getFloorName(),
                     request.getDescriptionKo(), request.getDescriptionEn());

        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                floor.activate();
            } else {
                floor.deactivate();
            }
        }
        if (request.getSortOrder() != null) floor.changeSortOrder(request.getSortOrder());

        return hotelMapper.toResponse(floor);
    }

    @Override
    @Transactional
    public void deleteFloor(Long id) {
        Floor floor = findFloorById(id);
        floor.softDelete();
        log.info("층 삭제: {}", floor.getFloorNumber());
    }

    @Override
    public boolean existsFloorNumber(Long propertyId, String floorNumber) {
        return floorRepository.existsByPropertyIdAndFloorNumber(propertyId, floorNumber);
    }

    private Floor findFloorById(Long id) {
        return floorRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.FLOOR_NOT_FOUND));
    }
}
