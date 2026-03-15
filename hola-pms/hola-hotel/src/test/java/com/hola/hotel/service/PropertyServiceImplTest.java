package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserPropertyRepository;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.PropertyCreateRequest;
import com.hola.hotel.dto.response.PropertyResponse;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.entity.Property;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 프로퍼티 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyServiceImpl")
class PropertyServiceImplTest {

    @InjectMocks
    private PropertyServiceImpl propertyService;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private RoomNumberRepository roomNumberRepository;

    @Mock
    private MarketCodeRepository marketCodeRepository;

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private AdminUserPropertyRepository adminUserPropertyRepository;

    @Mock
    private HotelMapper hotelMapper;

    private Hotel createHotel() {
        return Hotel.builder()
                .hotelCode("HTL00001")
                .hotelName("테스트 호텔")
                .build();
    }

    private Property createProperty(Hotel hotel) {
        return Property.builder()
                .hotel(hotel)
                .propertyCode("HTL00001-P001")
                .propertyName("테스트 프로퍼티")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .taxRate(BigDecimal.TEN)
                .serviceChargeRate(new BigDecimal("5"))
                .taxDecimalPlaces(0)
                .serviceChargeDecimalPlaces(0)
                .build();
    }

    private PropertyResponse createPropertyResponse(Long id, String code, String name) {
        return PropertyResponse.builder()
                .id(id)
                .propertyCode(code)
                .propertyName(name)
                .useYn(true)
                .floorCount(0)
                .roomCount(0)
                .marketCodeCount(0)
                .build();
    }

    @Nested
    @DisplayName("프로퍼티 생성")
    class CreateProperty {

        @Test
        @DisplayName("프로퍼티코드 자동 생성 (호텔코드-P001)")
        void createProperty_autoCodeGeneration() {
            Hotel hotel = createHotel();
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(propertyRepository.existsByHotelIdAndPropertyName(any(), eq("새 프로퍼티"))).thenReturn(false);
            when(propertyRepository.countByHotelIdAndPropertyCodeStartingWith(any(), anyString())).thenReturn(0L);

            PropertyCreateRequest request = mock(PropertyCreateRequest.class);
            when(request.getPropertyName()).thenReturn("새 프로퍼티");
            when(request.getCheckInTime()).thenReturn(null);
            when(request.getCheckOutTime()).thenReturn(null);
            when(request.getTotalRooms()).thenReturn(null);
            when(request.getTimezone()).thenReturn(null);
            when(request.getCountryCode()).thenReturn(null);
            when(request.getUseYn()).thenReturn(null);
            when(request.getSortOrder()).thenReturn(null);

            Property property = createProperty(hotel);
            when(propertyRepository.save(any())).thenReturn(property);
            when(hotelMapper.toResponse(eq(property), eq(0L), eq(0L), eq(0L)))
                    .thenReturn(createPropertyResponse(1L, "HTL00001-P001", "새 프로퍼티"));

            PropertyResponse response = propertyService.createProperty(1L, request);
            assertThat(response.getPropertyCode()).isEqualTo("HTL00001-P001");
            verify(propertyRepository).save(any());
        }

        @Test
        @DisplayName("이름 중복 시 PROPERTY_NAME_DUPLICATE")
        void createProperty_duplicateName_throws() {
            Hotel hotel = createHotel();
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(propertyRepository.existsByHotelIdAndPropertyName(any(), eq("기존 프로퍼티"))).thenReturn(true);

            PropertyCreateRequest request = mock(PropertyCreateRequest.class);
            when(request.getPropertyName()).thenReturn("기존 프로퍼티");

            assertThatThrownBy(() -> propertyService.createProperty(1L, request))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PROPERTY_NAME_DUPLICATE);
        }

        @Test
        @DisplayName("존재하지 않는 호텔 → HOTEL_NOT_FOUND")
        void createProperty_hotelNotFound_throws() {
            when(hotelRepository.findById(999L)).thenReturn(Optional.empty());

            PropertyCreateRequest request = mock(PropertyCreateRequest.class);

            assertThatThrownBy(() -> propertyService.createProperty(999L, request))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.HOTEL_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("프로퍼티 조회")
    class GetProperty {

        @Test
        @DisplayName("존재하는 프로퍼티 → 정상 응답 (카운트 포함)")
        void getProperty_found_withCounts() {
            Hotel hotel = createHotel();
            Property property = createProperty(hotel);
            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
            when(floorRepository.countByPropertyId(any())).thenReturn(5L);
            when(roomNumberRepository.countByPropertyId(any())).thenReturn(50L);
            when(marketCodeRepository.countByPropertyId(any())).thenReturn(3L);
            when(hotelMapper.toResponse(property, 5L, 50L, 3L))
                    .thenReturn(PropertyResponse.builder()
                            .id(1L).propertyCode("HTL00001-P001").propertyName("테스트 프로퍼티")
                            .floorCount(5).roomCount(50).marketCodeCount(3)
                            .build());

            PropertyResponse response = propertyService.getProperty(1L);
            assertThat(response.getFloorCount()).isEqualTo(5);
            assertThat(response.getRoomCount()).isEqualTo(50);
            assertThat(response.getMarketCodeCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("프로퍼티 삭제")
    class DeleteProperty {

        @Test
        @DisplayName("하위 데이터 없는 프로퍼티 삭제 성공")
        void deleteProperty_noChildren_success() {
            Hotel hotel = createHotel();
            Property property = createProperty(hotel);
            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
            when(floorRepository.countByPropertyId(any())).thenReturn(0L);
            when(roomNumberRepository.countByPropertyId(any())).thenReturn(0L);
            when(marketCodeRepository.countByPropertyId(any())).thenReturn(0L);

            propertyService.deleteProperty(1L);
            assertThat(property.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("하위 데이터 존재 시 PROPERTY_HAS_CHILDREN")
        void deleteProperty_hasChildren_throws() {
            Hotel hotel = createHotel();
            Property property = createProperty(hotel);
            when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
            when(floorRepository.countByPropertyId(any())).thenReturn(3L);

            assertThatThrownBy(() -> propertyService.deleteProperty(1L))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PROPERTY_HAS_CHILDREN);
        }
    }

    @Test
    @DisplayName("프로퍼티 카운트 조회")
    void getPropertyCount() {
        when(propertyRepository.countByDeletedAtIsNull()).thenReturn(15L);
        assertThat(propertyService.getPropertyCount()).isEqualTo(15L);
    }
}
