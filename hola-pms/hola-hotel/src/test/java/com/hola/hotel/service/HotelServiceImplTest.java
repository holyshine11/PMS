package com.hola.hotel.service;

import com.hola.common.auth.entity.AdminUser;
import com.hola.common.auth.repository.AdminUserRepository;
import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.HotelCreateRequest;
import com.hola.hotel.dto.response.HotelResponse;
import com.hola.hotel.entity.Hotel;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.HotelRepository;
import com.hola.hotel.repository.PropertyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 호텔 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HotelServiceImpl")
class HotelServiceImplTest {

    @InjectMocks
    private HotelServiceImpl hotelService;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private HotelMapper hotelMapper;

    private Hotel createHotel(String code, String name) {
        return Hotel.builder()
                .hotelCode(code)
                .hotelName(name)
                .build();
    }

    private HotelResponse createHotelResponse(Long id, String code, String name) {
        return HotelResponse.builder()
                .id(id)
                .hotelCode(code)
                .hotelName(name)
                .useYn(true)
                .build();
    }

    @Nested
    @DisplayName("호텔 생성")
    class CreateHotel {

        @Test
        @DisplayName("자동 코드 생성 (HTL00001 형식)")
        void createHotel_autoCodeGeneration() {
            // given
            HotelCreateRequest request = mock(HotelCreateRequest.class);
            when(request.getHotelName()).thenReturn("테스트 호텔");
            when(request.getUseYn()).thenReturn(null);
            when(request.getSortOrder()).thenReturn(null);

            when(hotelRepository.existsByHotelNameAndDeletedAtIsNull("테스트 호텔")).thenReturn(false);
            when(hotelRepository.getNextHotelCodeSequence()).thenReturn(1L);

            Hotel hotel = createHotel("HTL00001", "테스트 호텔");
            when(hotelMapper.toEntity(any(), eq("HTL00001"))).thenReturn(hotel);
            when(hotelRepository.save(any())).thenReturn(hotel);
            when(hotelMapper.toResponse(hotel)).thenReturn(createHotelResponse(1L, "HTL00001", "테스트 호텔"));

            // when
            HotelResponse response = hotelService.createHotel(request);

            // then
            assertThat(response.getHotelCode()).isEqualTo("HTL00001");
            verify(hotelRepository).getNextHotelCodeSequence();
        }

        @Test
        @DisplayName("이름 중복 시 HOTEL_NAME_DUPLICATE")
        void createHotel_duplicateName_throws() {
            HotelCreateRequest request = mock(HotelCreateRequest.class);
            when(request.getHotelName()).thenReturn("기존 호텔");
            when(hotelRepository.existsByHotelNameAndDeletedAtIsNull("기존 호텔")).thenReturn(true);

            assertThatThrownBy(() -> hotelService.createHotel(request))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.HOTEL_NAME_DUPLICATE);
        }
    }

    @Nested
    @DisplayName("호텔 조회")
    class GetHotel {

        @Test
        @DisplayName("존재하는 호텔 → 정상 응답")
        void getHotel_found() {
            Hotel hotel = createHotel("HTL00001", "테스트 호텔");
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(hotelMapper.toResponse(hotel)).thenReturn(createHotelResponse(1L, "HTL00001", "테스트 호텔"));

            HotelResponse response = hotelService.getHotel(1L);
            assertThat(response.getHotelName()).isEqualTo("테스트 호텔");
        }

        @Test
        @DisplayName("존재하지 않는 호텔 → HOTEL_NOT_FOUND")
        void getHotel_notFound() {
            when(hotelRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hotelService.getHotel(999L))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.HOTEL_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("호텔 삭제")
    class DeleteHotel {

        @Test
        @DisplayName("프로퍼티 없는 호텔 삭제 성공")
        void deleteHotel_noProperties_success() {
            Hotel hotel = createHotel("HTL00001", "테스트 호텔");
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(propertyRepository.countByHotelId(any())).thenReturn(0L);

            hotelService.deleteHotel(1L);

            assertThat(hotel.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("프로퍼티 존재 시 HOTEL_HAS_PROPERTIES")
        void deleteHotel_hasProperties_throws() {
            Hotel hotel = createHotel("HTL00001", "테스트 호텔");
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(propertyRepository.countByHotelId(any())).thenReturn(3L);

            assertThatThrownBy(() -> hotelService.deleteHotel(1L))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.HOTEL_HAS_PROPERTIES);
        }
    }

    @Nested
    @DisplayName("셀렉터")
    class Selector {

        @Test
        @DisplayName("SUPER_ADMIN → 모든 활성 호텔 반환")
        void getHotelsForSelector_superAdmin_allHotels() {
            AdminUser superAdmin = AdminUser.builder()
                    .loginId("admin").role("SUPER_ADMIN").password("enc").userName("관리자").build();
            when(adminUserRepository.findByLoginIdAndDeletedAtIsNull("admin"))
                    .thenReturn(Optional.of(superAdmin));

            Hotel h1 = createHotel("HTL00001", "호텔A");
            Hotel h2 = createHotel("HTL00002", "호텔B");
            when(hotelRepository.findAllByUseYnTrueOrderBySortOrderAscHotelNameAsc())
                    .thenReturn(List.of(h1, h2));
            when(hotelMapper.toResponse(h1)).thenReturn(createHotelResponse(1L, "HTL00001", "호텔A"));
            when(hotelMapper.toResponse(h2)).thenReturn(createHotelResponse(2L, "HTL00002", "호텔B"));

            List<HotelResponse> result = hotelService.getHotelsForSelector("admin");
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("HOTEL_ADMIN → 자기 호텔만 반환")
        void getHotelsForSelector_hotelAdmin_ownHotelOnly() {
            AdminUser hotelAdmin = AdminUser.builder()
                    .loginId("hoteladmin").role("HOTEL_ADMIN").hotelId(1L).password("enc").userName("호텔관리자").build();
            when(adminUserRepository.findByLoginIdAndDeletedAtIsNull("hoteladmin"))
                    .thenReturn(Optional.of(hotelAdmin));

            Hotel hotel = createHotel("HTL00001", "내 호텔");
            when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
            when(hotelMapper.toResponse(hotel)).thenReturn(createHotelResponse(1L, "HTL00001", "내 호텔"));

            List<HotelResponse> result = hotelService.getHotelsForSelector("hoteladmin");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getHotelName()).isEqualTo("내 호텔");
        }
    }
}
