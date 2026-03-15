package com.hola.room.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.room.dto.request.RoomTypeCreateRequest;
import com.hola.room.dto.response.RoomTypeListResponse;
import com.hola.room.dto.response.RoomTypeResponse;
import com.hola.room.entity.RoomClass;
import com.hola.room.entity.RoomType;
import com.hola.room.mapper.RoomTypeMapper;
import com.hola.room.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 객실 타입 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoomTypeServiceImpl")
class RoomTypeServiceImplTest {

    @InjectMocks
    private RoomTypeServiceImpl roomTypeService;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomClassRepository roomClassRepository;

    @Mock
    private RoomTypeFloorRepository roomTypeFloorRepository;

    @Mock
    private RoomTypeFreeServiceRepository roomTypeFreeServiceRepository;

    @Mock
    private RoomTypePaidServiceRepository roomTypePaidServiceRepository;

    @Mock
    private FreeServiceOptionRepository freeServiceOptionRepository;

    @Mock
    private PaidServiceOptionRepository paidServiceOptionRepository;

    @Mock
    private RoomTypeMapper roomTypeMapper;

    private RoomType createRoomType() {
        return RoomType.builder()
                .propertyId(1L)
                .roomClassId(1L)
                .roomTypeCode("STD")
                .maxAdults(2)
                .maxChildren(1)
                .build();
    }

    @Nested
    @DisplayName("객실 타입 생성")
    class CreateRoomType {

        @Test
        @DisplayName("정상 생성")
        void createRoomType_success() {
            RoomTypeCreateRequest request = mock(RoomTypeCreateRequest.class);
            when(request.getRoomClassId()).thenReturn(1L);
            when(request.getRoomTypeCode()).thenReturn("DLX");
            when(request.getMaxAdults()).thenReturn(2);
            when(request.getMaxChildren()).thenReturn(1);
            when(request.getExtraBedYn()).thenReturn(null);
            when(request.getUseYn()).thenReturn(null);
            when(request.getSortOrder()).thenReturn(null);
            when(request.getFloors()).thenReturn(null);
            when(request.getFreeServiceOptions()).thenReturn(null);
            when(request.getPaidServiceOptions()).thenReturn(null);
            when(request.getDescription()).thenReturn("디럭스");
            when(request.getRoomSize()).thenReturn(new BigDecimal("35.5"));
            when(request.getFeatures()).thenReturn("오션뷰");

            when(roomTypeRepository.existsByPropertyIdAndRoomTypeCode(1L, "DLX")).thenReturn(false);
            when(roomClassRepository.findById(1L)).thenReturn(Optional.of(RoomClass.builder().build()));

            RoomType roomType = createRoomType();
            when(roomTypeRepository.save(any())).thenReturn(roomType);

            RoomTypeResponse response = RoomTypeResponse.builder()
                    .id(1L).roomTypeCode("DLX").maxAdults(2).maxChildren(1).build();
            when(roomTypeMapper.toResponse(any(), any(), anyList(), anyLong(), anyList(), anyList()))
                    .thenReturn(response);

            RoomTypeResponse result = roomTypeService.createRoomType(1L, request);
            assertThat(result.getRoomTypeCode()).isEqualTo("DLX");
            verify(roomTypeRepository).save(any());
        }

        @Test
        @DisplayName("코드 중복 → ROOM_TYPE_CODE_DUPLICATE")
        void createRoomType_duplicateCode_throws() {
            RoomTypeCreateRequest request = mock(RoomTypeCreateRequest.class);
            when(request.getRoomTypeCode()).thenReturn("STD");

            when(roomTypeRepository.existsByPropertyIdAndRoomTypeCode(1L, "STD")).thenReturn(true);

            assertThatThrownBy(() -> roomTypeService.createRoomType(1L, request))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ROOM_TYPE_CODE_DUPLICATE);
        }

        @Test
        @DisplayName("존재하지 않는 객실 클래스 → ROOM_CLASS_NOT_FOUND")
        void createRoomType_roomClassNotFound_throws() {
            RoomTypeCreateRequest request = mock(RoomTypeCreateRequest.class);
            when(request.getRoomTypeCode()).thenReturn("DLX");
            when(request.getRoomClassId()).thenReturn(999L);

            when(roomTypeRepository.existsByPropertyIdAndRoomTypeCode(1L, "DLX")).thenReturn(false);
            when(roomClassRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomTypeService.createRoomType(1L, request))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ROOM_CLASS_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("객실 타입 삭제")
    class DeleteRoomType {

        @Test
        @DisplayName("삭제 시 매핑 데이터 함께 삭제")
        void deleteRoomType_cascadeDelete() {
            RoomType roomType = createRoomType();
            when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));

            roomTypeService.deleteRoomType(1L);

            verify(roomTypeFloorRepository).deleteAllByRoomTypeId(1L);
            verify(roomTypeFreeServiceRepository).deleteAllByRoomTypeId(1L);
            verify(roomTypePaidServiceRepository).deleteAllByRoomTypeId(1L);
            assertThat(roomType.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 ID → ROOM_TYPE_NOT_FOUND")
        void deleteRoomType_notFound_throws() {
            when(roomTypeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> roomTypeService.deleteRoomType(999L))
                    .isInstanceOf(HolaException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ROOM_TYPE_NOT_FOUND);
        }
    }
}
