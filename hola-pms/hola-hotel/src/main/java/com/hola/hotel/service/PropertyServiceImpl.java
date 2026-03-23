package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.entity.AdminUserProperty;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.PropertyCreateRequest;
import com.hola.hotel.dto.request.PropertyTaxServiceChargeRequest;
import com.hola.hotel.dto.request.PropertyUpdateRequest;
import com.hola.hotel.dto.response.PropertyResponse;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final HotelRepository hotelRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminUserPropertyRepository adminUserPropertyRepository;
    private final FloorRepository floorRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final MarketCodeRepository marketCodeRepository;
    private final HotelMapper hotelMapper;

    @Override
    public List<PropertyResponse> getProperties(Long hotelId) {
        return propertyRepository.findAllByHotelId(hotelId).stream()
                .map(this::toResponseWithCounts)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PropertyResponse> getProperties(Long hotelId, Pageable pageable) {
        return propertyRepository.findAllByHotelId(hotelId, pageable)
                .map(this::toResponseWithCounts);
    }

    @Override
    public PropertyResponse getProperty(Long id) {
        Property property = findPropertyById(id);
        return toResponseWithCounts(property);
    }

    @Override
    @Transactional
    public PropertyResponse createProperty(Long hotelId, PropertyCreateRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HolaException(ErrorCode.HOTEL_NOT_FOUND));

        // 프로퍼티명 중복 확인
        if (propertyRepository.existsByHotelIdAndPropertyName(hotelId, request.getPropertyName())) {
            throw new HolaException(ErrorCode.PROPERTY_NAME_DUPLICATE);
        }

        // 프로퍼티코드 자동 생성 (호텔코드 + 순번)
        String propertyCode = generatePropertyCode(hotel);

        Property property = Property.builder()
                .hotel(hotel)
                .propertyCode(propertyCode)
                .propertyName(request.getPropertyName())
                .propertyType(request.getPropertyType())
                .starRating(request.getStarRating())
                .checkInTime(request.getCheckInTime() != null ? request.getCheckInTime() : "15:00")
                .checkOutTime(request.getCheckOutTime() != null ? request.getCheckOutTime() : "11:00")
                .totalRooms(request.getTotalRooms() != null ? request.getTotalRooms() : 0)
                .timezone(request.getTimezone() != null ? request.getTimezone() : "Asia/Seoul")
                .representativeName(request.getRepresentativeName())
                .representativeNameEn(request.getRepresentativeNameEn())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "+82")
                .phone(request.getPhone())
                .email(request.getEmail())
                .businessNumber(request.getBusinessNumber())
                .introduction(request.getIntroduction())
                .zipCode(request.getZipCode())
                .address(request.getAddress())
                .addressDetail(request.getAddressDetail())
                .addressEn(request.getAddressEn())
                .addressDetailEn(request.getAddressDetailEn())
                .description(request.getDescription())
                .bizLicensePath(request.getBizLicensePath())
                .logoPath(request.getLogoPath())
                .dayUseEnabled(request.getDayUseEnabled() != null ? request.getDayUseEnabled() : false)
                .dayUseStartTime(request.getDayUseStartTime() != null ? request.getDayUseStartTime() : "10:00")
                .dayUseEndTime(request.getDayUseEndTime() != null ? request.getDayUseEndTime() : "20:00")
                .dayUseDefaultHours(request.getDayUseDefaultHours() != null ? request.getDayUseDefaultHours() : 5)
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            property.deactivate();
        }
        if (request.getSortOrder() != null) {
            property.changeSortOrder(request.getSortOrder());
        }

        Property saved = propertyRepository.save(property);
        log.info("프로퍼티 생성: {} ({}) - 호텔: {}", saved.getPropertyName(), saved.getPropertyCode(), hotel.getHotelName());
        return hotelMapper.toResponse(saved, 0, 0, 0);
    }

    @Override
    @Transactional
    public PropertyResponse updateProperty(Long id, PropertyUpdateRequest request) {
        Property property = findPropertyById(id);

        property.update(
                request.getPropertyName(), request.getPropertyType(), request.getStarRating(),
                request.getCheckInTime(), request.getCheckOutTime(), request.getTotalRooms(),
                request.getTimezone(), request.getRepresentativeName(), request.getRepresentativeNameEn(),
                request.getCountryCode(), request.getPhone(), request.getEmail(),
                request.getBusinessNumber(), request.getIntroduction(),
                request.getZipCode(), request.getAddress(), request.getAddressDetail(),
                request.getAddressEn(), request.getAddressDetailEn(), request.getDescription(),
                request.getBizLicensePath(), request.getLogoPath());

        property.updateDayUse(
                request.getDayUseEnabled(), request.getDayUseStartTime(),
                request.getDayUseEndTime(), request.getDayUseDefaultHours());

        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                property.activate();
            } else {
                property.deactivate();
            }
        }
        if (request.getSortOrder() != null) {
            property.changeSortOrder(request.getSortOrder());
        }

        log.info("프로퍼티 수정: {} ({})", property.getPropertyName(), property.getPropertyCode());
        return toResponseWithCounts(property);
    }

    @Override
    @Transactional
    public void deleteProperty(Long id) {
        Property property = findPropertyById(id);

        long floorCount = floorRepository.countByPropertyId(id);
        long roomCount = roomNumberRepository.countByPropertyId(id);
        long marketCodeCount = marketCodeRepository.countByPropertyId(id);

        if (floorCount > 0 || roomCount > 0 || marketCodeCount > 0) {
            throw new HolaException(ErrorCode.PROPERTY_HAS_CHILDREN);
        }

        property.softDelete();
        log.info("프로퍼티 삭제: {} ({})", property.getPropertyName(), property.getPropertyCode());
    }

    @Override
    public long getPropertyCount() {
        return propertyRepository.countByDeletedAtIsNull();
    }

    @Override
    public List<PropertyResponse> getPropertiesForSelector(Long hotelId, String loginId) {
        List<Property> properties = propertyRepository
                .findAllByHotelIdAndUseYnTrueOrderBySortOrderAscPropertyNameAsc(hotelId);

        // 로그인 사용자의 권한에 따라 필터링
        AdminUser currentUser = adminUserRepository.findByLoginIdAndDeletedAtIsNull(loginId)
                .orElseThrow(() -> new HolaException(ErrorCode.ADMIN_NOT_FOUND));
        if (!currentUser.isSuperAdmin()) {
            // HOTEL_ADMIN, PROPERTY_ADMIN: 매핑된 프로퍼티만 표시
            Set<Long> allowedPropertyIds = adminUserPropertyRepository
                    .findByAdminUserId(currentUser.getId()).stream()
                    .map(AdminUserProperty::getPropertyId)
                    .collect(Collectors.toSet());
            properties = properties.stream()
                    .filter(p -> allowedPropertyIds.contains(p.getId()))
                    .collect(Collectors.toList());
        }

        return properties.stream()
                .map(p -> hotelMapper.toResponse(p, 0, 0, 0))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsPropertyName(Long hotelId, String propertyName) {
        return propertyRepository.existsByHotelIdAndPropertyName(hotelId, propertyName);
    }

    @Override
    @Transactional
    public PropertyResponse updateTaxServiceCharge(Long id, PropertyTaxServiceChargeRequest request) {
        Property property = findPropertyById(id);
        property.updateTaxServiceCharge(
                request.getTaxRate(), request.getTaxDecimalPlaces(), request.getTaxRoundingMethod(),
                request.getServiceChargeRate(), request.getServiceChargeDecimalPlaces(),
                request.getServiceChargeRoundingMethod());
        log.info("TAX/봉사료 수정: propertyId={}", id);
        return toResponseWithCounts(property);
    }

    /**
     * 프로퍼티코드 자동 생성 (호텔코드-P001 형태)
     */
    private String generatePropertyCode(Hotel hotel) {
        String prefix = hotel.getHotelCode() + "-P";
        long count = propertyRepository.countByHotelIdAndPropertyCodeStartingWith(hotel.getId(), prefix);
        return String.format("%s%03d", prefix, count + 1);
    }

    private Property findPropertyById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));
    }

    private PropertyResponse toResponseWithCounts(Property property) {
        long floorCount = floorRepository.countByPropertyId(property.getId());
        long roomCount = roomNumberRepository.countByPropertyId(property.getId());
        long marketCodeCount = marketCodeRepository.countByPropertyId(property.getId());
        return hotelMapper.toResponse(property, floorCount, roomCount, marketCodeCount);
    }
}
