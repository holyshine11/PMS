package com.hola.hotel.mapper;

import com.hola.hotel.dto.request.HotelCreateRequest;
import com.hola.hotel.dto.request.ReservationChannelRequest;
import com.hola.hotel.dto.response.*;
import com.hola.hotel.entity.*;
import org.springframework.stereotype.Component;

/**
 * Entity <-> DTO 변환 매퍼
 */
@Component
public class HotelMapper {

    public Hotel toEntity(HotelCreateRequest request, String hotelCode) {
        return Hotel.builder()
                .hotelCode(hotelCode)
                .hotelName(request.getHotelName())
                .representativeName(request.getRepresentativeName())
                .representativeNameEn(request.getRepresentativeNameEn())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "+82")
                .phone(request.getPhone())
                .email(request.getEmail())
                .zipCode(request.getZipCode())
                .address(request.getAddress())
                .addressDetail(request.getAddressDetail())
                .addressEn(request.getAddressEn())
                .addressDetailEn(request.getAddressDetailEn())
                .introduction(request.getIntroduction())
                .build();
    }

    public HotelResponse toResponse(Hotel hotel) {
        return HotelResponse.builder()
                .id(hotel.getId())
                .hotelCode(hotel.getHotelCode())
                .hotelName(hotel.getHotelName())
                .representativeName(hotel.getRepresentativeName())
                .representativeNameEn(hotel.getRepresentativeNameEn())
                .countryCode(hotel.getCountryCode())
                .phone(hotel.getPhone())
                .email(hotel.getEmail())
                .zipCode(hotel.getZipCode())
                .address(hotel.getAddress())
                .addressDetail(hotel.getAddressDetail())
                .addressEn(hotel.getAddressEn())
                .addressDetailEn(hotel.getAddressDetailEn())
                .introduction(hotel.getIntroduction())
                .description(hotel.getDescription())
                .sortOrder(hotel.getSortOrder())
                .useYn(hotel.getUseYn())
                .createdAt(hotel.getCreatedAt())
                .updatedAt(hotel.getUpdatedAt())
                .build();
    }

    public PropertyResponse toResponse(Property property, long floorCount, long roomCount, long marketCodeCount) {
        return PropertyResponse.builder()
                .id(property.getId())
                .hotelId(property.getHotel().getId())
                .hotelName(property.getHotel().getHotelName())
                .propertyCode(property.getPropertyCode())
                .propertyName(property.getPropertyName())
                .propertyType(property.getPropertyType())
                .starRating(property.getStarRating())
                .checkInTime(property.getCheckInTime())
                .checkOutTime(property.getCheckOutTime())
                .totalRooms(property.getTotalRooms())
                .timezone(property.getTimezone())
                .representativeName(property.getRepresentativeName())
                .representativeNameEn(property.getRepresentativeNameEn())
                .countryCode(property.getCountryCode())
                .phone(property.getPhone())
                .email(property.getEmail())
                .businessNumber(property.getBusinessNumber())
                .introduction(property.getIntroduction())
                .zipCode(property.getZipCode())
                .address(property.getAddress())
                .addressDetail(property.getAddressDetail())
                .addressEn(property.getAddressEn())
                .addressDetailEn(property.getAddressDetailEn())
                .description(property.getDescription())
                .bizLicensePath(property.getBizLicensePath())
                .logoPath(property.getLogoPath())
                .taxRate(property.getTaxRate())
                .taxDecimalPlaces(property.getTaxDecimalPlaces())
                .taxRoundingMethod(property.getTaxRoundingMethod())
                .serviceChargeRate(property.getServiceChargeRate())
                .serviceChargeDecimalPlaces(property.getServiceChargeDecimalPlaces())
                .serviceChargeRoundingMethod(property.getServiceChargeRoundingMethod())
                .sortOrder(property.getSortOrder())
                .useYn(property.getUseYn())
                .floorCount(floorCount)
                .roomCount(roomCount)
                .marketCodeCount(marketCodeCount)
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }

    public FloorResponse toResponse(Floor floor) {
        return FloorResponse.builder()
                .id(floor.getId())
                .propertyId(floor.getProperty().getId())
                .floorNumber(floor.getFloorNumber())
                .floorName(floor.getFloorName())
                .descriptionKo(floor.getDescriptionKo())
                .descriptionEn(floor.getDescriptionEn())
                .sortOrder(floor.getSortOrder())
                .useYn(floor.getUseYn())
                .updatedAt(floor.getUpdatedAt())
                .build();
    }

    public RoomNumberResponse toResponse(RoomNumber roomNumber) {
        return RoomNumberResponse.builder()
                .id(roomNumber.getId())
                .propertyId(roomNumber.getProperty().getId())
                .roomNumber(roomNumber.getRoomNumber())
                .descriptionKo(roomNumber.getDescriptionKo())
                .descriptionEn(roomNumber.getDescriptionEn())
                .sortOrder(roomNumber.getSortOrder())
                .useYn(roomNumber.getUseYn())
                .updatedAt(roomNumber.getUpdatedAt())
                .build();
    }

    public MarketCodeResponse toResponse(MarketCode marketCode) {
        return MarketCodeResponse.builder()
                .id(marketCode.getId())
                .propertyId(marketCode.getProperty().getId())
                .marketCode(marketCode.getMarketCode())
                .marketName(marketCode.getMarketName())
                .descriptionKo(marketCode.getDescriptionKo())
                .descriptionEn(marketCode.getDescriptionEn())
                .sortOrder(marketCode.getSortOrder())
                .useYn(marketCode.getUseYn())
                .updatedAt(marketCode.getUpdatedAt())
                .build();
    }

    public CancellationFeeResponse toResponse(CancellationFee fee) {
        return CancellationFeeResponse.builder()
                .id(fee.getId())
                .propertyId(fee.getProperty().getId())
                .checkinBasis(fee.getCheckinBasis())
                .daysBefore(fee.getDaysBefore())
                .feeAmount(fee.getFeeAmount())
                .feeType(fee.getFeeType())
                .sortOrder(fee.getSortOrder())
                .useYn(fee.getUseYn())
                .createdAt(fee.getCreatedAt())
                .updatedAt(fee.getUpdatedAt())
                .build();
    }

    public PropertySettlementResponse toResponse(PropertySettlement settlement) {
        return PropertySettlementResponse.builder()
                .id(settlement.getId())
                .propertyId(settlement.getProperty().getId())
                .countryType(settlement.getCountryType())
                .accountNumber(settlement.getAccountNumber())
                .bankName(settlement.getBankName())
                .bankCode(settlement.getBankCode())
                .accountHolder(settlement.getAccountHolder())
                .routingNumber(settlement.getRoutingNumber())
                .swiftCode(settlement.getSwiftCode())
                .settlementDay(settlement.getSettlementDay())
                .bankBookPath(settlement.getBankBookPath())
                .sortOrder(settlement.getSortOrder())
                .useYn(settlement.getUseYn())
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt())
                .build();
    }

    // 예약채널 변환
    public ReservationChannel toReservationChannelEntity(ReservationChannelRequest request, Property property) {
        return ReservationChannel.builder()
                .property(property)
                .channelCode(request.getChannelCode())
                .channelName(request.getChannelName())
                .channelType(request.getChannelType())
                .descriptionKo(request.getDescriptionKo())
                .descriptionEn(request.getDescriptionEn())
                .build();
    }

    public ReservationChannelResponse toReservationChannelResponse(ReservationChannel channel) {
        return ReservationChannelResponse.builder()
                .id(channel.getId())
                .propertyId(channel.getProperty().getId())
                .channelCode(channel.getChannelCode())
                .channelName(channel.getChannelName())
                .channelType(channel.getChannelType())
                .descriptionKo(channel.getDescriptionKo())
                .descriptionEn(channel.getDescriptionEn())
                .sortOrder(channel.getSortOrder())
                .useYn(channel.getUseYn())
                .createdAt(channel.getCreatedAt())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }

    // 얼리체크인/레이트체크아웃 요금 정책 변환
    public EarlyLateFeePolicyResponse toResponse(EarlyLateFeePolicy policy) {
        return EarlyLateFeePolicyResponse.builder()
                .id(policy.getId())
                .propertyId(policy.getProperty().getId())
                .policyType(policy.getPolicyType())
                .timeFrom(policy.getTimeFrom())
                .timeTo(policy.getTimeTo())
                .feeType(policy.getFeeType())
                .feeValue(policy.getFeeValue())
                .description(policy.getDescription())
                .sortOrder(policy.getSortOrder())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
