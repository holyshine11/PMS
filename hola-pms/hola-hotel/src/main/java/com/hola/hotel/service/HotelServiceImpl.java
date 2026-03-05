package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.HotelCreateRequest;
import com.hola.hotel.dto.request.HotelUpdateRequest;
import com.hola.hotel.dto.response.HotelResponse;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.HotelRepository;
import com.hola.hotel.repository.PropertyRepository;
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
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final PropertyRepository propertyRepository;
    private final AdminUserRepository adminUserRepository;
    private final HotelMapper hotelMapper;

    @Override
    public Page<HotelResponse> getHotels(String hotelName, Boolean useYn, Pageable pageable) {
        Page<Hotel> hotels = hotelRepository.findAllByHotelNameAndUseYn(hotelName, useYn, pageable);
        return hotels.map(hotelMapper::toResponse);
    }

    @Override
    public HotelResponse getHotel(Long id) {
        Hotel hotel = findHotelById(id);
        return hotelMapper.toResponse(hotel);
    }

    @Override
    @Transactional
    public HotelResponse createHotel(HotelCreateRequest request) {
        // 호텔명 중복 체크
        if (hotelRepository.existsByHotelNameAndDeletedAtIsNull(request.getHotelName())) {
            throw new HolaException(ErrorCode.HOTEL_NAME_DUPLICATE);
        }

        // 호텔코드 자동생성 (HTL00001~)
        String hotelCode = generateHotelCode();
        Hotel hotel = hotelMapper.toEntity(request, hotelCode);

        if (request.getUseYn() != null && !request.getUseYn()) {
            hotel.deactivate();
        }
        if (request.getSortOrder() != null) {
            hotel.changeSortOrder(request.getSortOrder());
        }

        Hotel saved = hotelRepository.save(hotel);
        log.info("호텔 생성: {} ({})", saved.getHotelName(), saved.getHotelCode());
        return hotelMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HotelResponse updateHotel(Long id, HotelUpdateRequest request) {
        Hotel hotel = findHotelById(id);

        // 호텔명 변경 시 중복 체크 (자기 자신 제외)
        if (!hotel.getHotelName().equals(request.getHotelName())
                && hotelRepository.existsByHotelNameAndDeletedAtIsNullAndIdNot(request.getHotelName(), id)) {
            throw new HolaException(ErrorCode.HOTEL_NAME_DUPLICATE);
        }

        hotel.update(
                request.getHotelName(), request.getRepresentativeName(), request.getRepresentativeNameEn(),
                request.getCountryCode(), request.getPhone(), request.getEmail(),
                request.getZipCode(), request.getAddress(), request.getAddressDetail(),
                request.getAddressEn(), request.getAddressDetailEn(), request.getIntroduction());

        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                hotel.activate();
            } else {
                hotel.deactivate();
            }
        }
        if (request.getSortOrder() != null) {
            hotel.changeSortOrder(request.getSortOrder());
        }

        log.info("호텔 수정: {} ({})", hotel.getHotelName(), hotel.getHotelCode());
        return hotelMapper.toResponse(hotel);
    }

    @Override
    @Transactional
    public void deleteHotel(Long id) {
        Hotel hotel = findHotelById(id);

        // 하위 프로퍼티 존재 시 삭제 불가
        long propertyCount = propertyRepository.countByHotelId(hotel.getId());
        if (propertyCount > 0) {
            throw new HolaException(ErrorCode.HOTEL_HAS_PROPERTIES);
        }

        hotel.softDelete();
        log.info("호텔 삭제: {} ({})", hotel.getHotelName(), hotel.getHotelCode());
    }

    @Override
    public long getHotelCount() {
        return hotelRepository.countByDeletedAtIsNull();
    }

    @Override
    public boolean checkHotelNameDuplicate(String hotelName) {
        return hotelRepository.existsByHotelNameAndDeletedAtIsNull(hotelName);
    }

    @Override
    public List<HotelResponse> getHotelsForSelector(String loginId) {
        AdminUser currentUser = adminUserRepository.findByLoginIdAndDeletedAtIsNull(loginId)
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));

        // SUPER_ADMIN: 전체 호텔 목록
        if (currentUser.isSuperAdmin()) {
            return hotelRepository.findAllByUseYnTrueOrderBySortOrderAscHotelNameAsc()
                    .stream().map(hotelMapper::toResponse).collect(Collectors.toList());
        }

        // HOTEL_ADMIN, PROPERTY_ADMIN: 소속 호텔 단건 조회
        Long userHotelId = currentUser.getHotelId();
        if (userHotelId == null) {
            return List.of();
        }
        return hotelRepository.findById(userHotelId)
                .filter(Hotel::getUseYn)
                .map(h -> List.of(hotelMapper.toResponse(h)))
                .orElse(List.of());
    }

    private Hotel findHotelById(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.HOTEL_NOT_FOUND));
    }

    /**
     * 호텔코드 자동생성: HTL + 5자리 시퀀스 (HTL00001~)
     */
    private String generateHotelCode() {
        Long nextVal = hotelRepository.getNextHotelCodeSequence();
        return String.format("HTL%05d", nextVal);
    }
}
