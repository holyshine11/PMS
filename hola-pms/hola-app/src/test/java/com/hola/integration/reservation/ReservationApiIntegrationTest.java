package com.hola.integration.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.common.auth.entity.AdminUser;
import com.hola.common.security.AccessControlService;
import com.hola.reservation.dto.request.ReservationCreateRequest;
import com.hola.reservation.dto.request.ReservationStatusRequest;
import com.hola.reservation.dto.request.ReservationUpdateRequest;
import com.hola.reservation.dto.request.SubReservationRequest;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.ReservationMemo;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.ReservationMemoRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.reservation.service.ReservationService;
import com.hola.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 예약 API 통합 테스트
 * - ReservationApiController 전체 엔드포인트 검증
 * - AccessControlService Mock으로 인증/인가 바이패스
 * - ReservationService Mock으로 비즈니스 로직 제어
 */
@DisplayName("예약 API 통합 테스트")
class ReservationApiIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/properties/{propertyId}/reservations";
    private static final Long PROPERTY_ID = 1L;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccessControlService accessControlService;

    @MockBean
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        // SUPER_ADMIN Mock - AccessControlService 바이패스
        AdminUser mockAdmin = AdminUser.builder()
                .loginId("admin")
                .role("SUPER_ADMIN")
                .userName("관리자")
                .password("encoded")
                .build();
        when(accessControlService.getCurrentUser()).thenReturn(mockAdmin);
        // validatePropertyAccess는 SUPER_ADMIN이면 즉시 반환
        doNothing().when(accessControlService).validatePropertyAccess(anyLong());
    }

    // ========== 예약 리스트 조회 ==========

    @Nested
    @DisplayName("예약 리스트 조회 (GET /reservations)")
    class GetList {

        @Test
        @DisplayName("빈 리스트 조회 시 200 OK + 빈 배열 반환")
        void getList_empty_returns200() throws Exception {
            when(reservationService.getList(eq(PROPERTY_ID), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL, PROPERTY_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("상태 필터 파라미터 전달 시 서비스에 정상 전달")
        void getList_withStatusFilter_passesParameter() throws Exception {
            when(reservationService.getList(eq(PROPERTY_ID), eq("RESERVED"), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL, PROPERTY_ID)
                            .param("status", "RESERVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reservationService).getList(eq(PROPERTY_ID), eq("RESERVED"), any(), any(), any());
        }
    }

    // ========== 예약 상세 조회 ==========

    @Nested
    @DisplayName("예약 상세 조회 (GET /reservations/{id})")
    class GetById {

        @Test
        @DisplayName("존재하지 않는 예약 ID 조회 시 404 NOT FOUND 반환")
        void getById_notFound_returns404() throws Exception {
            when(reservationService.getById(eq(999L), eq(PROPERTY_ID)))
                    .thenThrow(new com.hola.common.exception.HolaException(
                            com.hola.common.exception.ErrorCode.RESERVATION_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/{id}", PROPERTY_ID, 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("HOLA-4000"));
        }
    }

    // ========== 예약 등록 ==========

    @Nested
    @DisplayName("예약 등록 (POST /reservations)")
    class Create {

        @Test
        @DisplayName("유효한 요청으로 예약 등록 시 201 CREATED 반환")
        void create_validRequest_returns201() throws Exception {
            ReservationCreateRequest request = buildCreateRequest(
                    LocalDate.now().plusDays(7), LocalDate.now().plusDays(8));

            when(reservationService.create(eq(PROPERTY_ID), any(ReservationCreateRequest.class)))
                    .thenReturn(com.hola.reservation.dto.response.ReservationDetailResponse.builder()
                            .id(1L)
                            .propertyId(PROPERTY_ID)
                            .masterReservationNo("RSV-20260314-0001")
                            .confirmationNo("C1234567")
                            .reservationStatus("RESERVED")
                            .masterCheckIn(LocalDate.now().plusDays(7))
                            .masterCheckOut(LocalDate.now().plusDays(8))
                            .guestNameKo("홍길동")
                            .build());

            mockMvc.perform(post(BASE_URL, PROPERTY_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.reservationStatus").value("RESERVED"))
                    .andExpect(jsonPath("$.data.guestNameKo").value("홍길동"));
        }

        @Test
        @DisplayName("과거 체크인 날짜로 예약 등록 시 400 BAD REQUEST 반환")
        void create_pastDate_returns400() throws Exception {
            ReservationCreateRequest request = buildCreateRequest(
                    LocalDate.now().minusDays(1), LocalDate.now());

            when(reservationService.create(eq(PROPERTY_ID), any(ReservationCreateRequest.class)))
                    .thenThrow(new com.hola.common.exception.HolaException(
                            com.hola.common.exception.ErrorCode.RESERVATION_CHECKIN_PAST_DATE));

            mockMvc.perform(post(BASE_URL, PROPERTY_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("HOLA-4014"));
        }

        @Test
        @DisplayName("레이트코드 없이 예약 등록 시 400 BAD REQUEST 반환")
        void create_noRateCode_returns400() throws Exception {
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(LocalDate.now().plusDays(7))
                    .masterCheckOut(LocalDate.now().plusDays(8))
                    .guestNameKo("홍길동")
                    .subReservations(List.of(SubReservationRequest.builder()
                            .roomTypeId(1L)
                            .checkIn(LocalDate.now().plusDays(7))
                            .checkOut(LocalDate.now().plusDays(8))
                            .adults(2)
                            .children(0)
                            .build()))
                    .build();

            when(reservationService.create(eq(PROPERTY_ID), any(ReservationCreateRequest.class)))
                    .thenThrow(new com.hola.common.exception.HolaException(
                            com.hola.common.exception.ErrorCode.RESERVATION_RATE_REQUIRED));

            mockMvc.perform(post(BASE_URL, PROPERTY_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("HOLA-4024"));
        }

        @Test
        @DisplayName("서브 예약 목록 누락 시 400 BAD REQUEST 반환 (Validation)")
        void create_noSubReservations_returns400() throws Exception {
            ReservationCreateRequest request = ReservationCreateRequest.builder()
                    .masterCheckIn(LocalDate.now().plusDays(7))
                    .masterCheckOut(LocalDate.now().plusDays(8))
                    .guestNameKo("홍길동")
                    .rateCodeId(1L)
                    // subReservations 누락 → @NotEmpty 위반
                    .build();

            mockMvc.perform(post(BASE_URL, PROPERTY_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========== 예약 수정 ==========

    @Nested
    @DisplayName("예약 수정 (PUT /reservations/{id})")
    class Update {

        @Test
        @DisplayName("정상 수정 시 200 OK 반환")
        void update_normal_returns200() throws Exception {
            ReservationUpdateRequest request = ReservationUpdateRequest.builder()
                    .masterCheckIn(LocalDate.now().plusDays(10))
                    .masterCheckOut(LocalDate.now().plusDays(12))
                    .guestNameKo("김철수")
                    .rateCodeId(1L)
                    .build();

            when(reservationService.update(eq(1L), eq(PROPERTY_ID), any(ReservationUpdateRequest.class)))
                    .thenReturn(com.hola.reservation.dto.response.ReservationDetailResponse.builder()
                            .id(1L)
                            .propertyId(PROPERTY_ID)
                            .reservationStatus("RESERVED")
                            .masterCheckIn(LocalDate.now().plusDays(10))
                            .masterCheckOut(LocalDate.now().plusDays(12))
                            .guestNameKo("김철수")
                            .build());

            mockMvc.perform(put(BASE_URL + "/{id}", PROPERTY_ID, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.guestNameKo").value("김철수"));
        }

        @Test
        @DisplayName("체크아웃 상태 예약 수정 시 400 BAD REQUEST 반환")
        void update_checkedOut_returns400() throws Exception {
            ReservationUpdateRequest request = ReservationUpdateRequest.builder()
                    .masterCheckIn(LocalDate.now().plusDays(1))
                    .masterCheckOut(LocalDate.now().plusDays(2))
                    .build();

            when(reservationService.update(eq(1L), eq(PROPERTY_ID), any(ReservationUpdateRequest.class)))
                    .thenThrow(new com.hola.common.exception.HolaException(
                            com.hola.common.exception.ErrorCode.RESERVATION_MODIFY_NOT_ALLOWED));

            mockMvc.perform(put(BASE_URL + "/{id}", PROPERTY_ID, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("HOLA-4041"));
        }
    }

    // ========== 예약 상태 변경 ==========

    @Nested
    @DisplayName("예약 상태 변경 (PUT /reservations/{id}/status)")
    class ChangeStatus {

        @Test
        @DisplayName("RESERVED → CHECK_IN 상태 변경 시 200 OK 반환")
        void changeStatus_reservedToCheckIn_returns200() throws Exception {
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECK_IN")
                    .build();

            doNothing().when(reservationService).changeStatus(eq(1L), eq(PROPERTY_ID), any(ReservationStatusRequest.class));

            mockMvc.perform(put(BASE_URL + "/{id}/status", PROPERTY_ID, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reservationService).changeStatus(eq(1L), eq(PROPERTY_ID), any(ReservationStatusRequest.class));
        }

        @Test
        @DisplayName("허용되지 않은 상태 전이 시 400 BAD REQUEST 반환")
        void changeStatus_invalidTransition_returns400() throws Exception {
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("CHECKED_OUT")
                    .build();

            doThrow(new com.hola.common.exception.HolaException(
                    com.hola.common.exception.ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED))
                    .when(reservationService).changeStatus(eq(1L), eq(PROPERTY_ID), any(ReservationStatusRequest.class));

            mockMvc.perform(put(BASE_URL + "/{id}/status", PROPERTY_ID, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("HOLA-4003"));
        }

        @Test
        @DisplayName("상태값 미입력 시 400 BAD REQUEST 반환 (Validation)")
        void changeStatus_emptyStatus_returns400() throws Exception {
            ReservationStatusRequest request = ReservationStatusRequest.builder()
                    .newStatus("")
                    .build();

            mockMvc.perform(put(BASE_URL + "/{id}/status", PROPERTY_ID, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========== 예약 취소 ==========

    @Nested
    @DisplayName("예약 취소 (DELETE /reservations/{id})")
    class Cancel {

        @Test
        @DisplayName("RESERVED 상태 예약 취소 시 200 OK 반환")
        void cancel_reserved_returns200() throws Exception {
            doNothing().when(reservationService).cancel(eq(1L), eq(PROPERTY_ID));

            mockMvc.perform(delete(BASE_URL + "/{id}", PROPERTY_ID, 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reservationService).cancel(1L, PROPERTY_ID);
        }

        @Test
        @DisplayName("INHOUSE 상태 예약 취소 시 400 BAD REQUEST 반환")
        void cancel_inhouse_returns400() throws Exception {
            doThrow(new com.hola.common.exception.HolaException(
                    com.hola.common.exception.ErrorCode.RESERVATION_STATUS_CHANGE_NOT_ALLOWED))
                    .when(reservationService).cancel(eq(1L), eq(PROPERTY_ID));

            mockMvc.perform(delete(BASE_URL + "/{id}", PROPERTY_ID, 1L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("HOLA-4003"));
        }
    }

    // ========== 서브 예약(객실 레그) ==========

    @Nested
    @DisplayName("서브 예약 관리 (POST/DELETE /reservations/{id}/legs)")
    class Legs {

        @Test
        @DisplayName("서브 예약(객실 레그) 추가 시 201 CREATED 반환")
        void addLeg_returns201() throws Exception {
            SubReservationRequest request = SubReservationRequest.builder()
                    .roomTypeId(1L)
                    .checkIn(LocalDate.now().plusDays(7))
                    .checkOut(LocalDate.now().plusDays(8))
                    .adults(2)
                    .children(0)
                    .build();

            when(reservationService.addLeg(eq(1L), eq(PROPERTY_ID), any(SubReservationRequest.class)))
                    .thenReturn(com.hola.reservation.dto.response.SubReservationResponse.builder()
                            .id(10L)
                            .subReservationNo("RSV-20260314-0001-02")
                            .roomReservationStatus("RESERVED")
                            .roomTypeId(1L)
                            .checkIn(LocalDate.now().plusDays(7))
                            .checkOut(LocalDate.now().plusDays(8))
                            .adults(2)
                            .children(0)
                            .build());

            mockMvc.perform(post(BASE_URL + "/{id}/legs", PROPERTY_ID, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.subReservationNo").isNotEmpty())
                    .andExpect(jsonPath("$.data.roomReservationStatus").value("RESERVED"));
        }

        @Test
        @DisplayName("서브 예약 삭제 시 200 OK 반환")
        void deleteLeg_returns200() throws Exception {
            doNothing().when(reservationService).deleteLeg(eq(1L), eq(PROPERTY_ID), eq(10L));

            mockMvc.perform(delete(BASE_URL + "/{id}/legs/{legId}", PROPERTY_ID, 1L, 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reservationService).deleteLeg(1L, PROPERTY_ID, 10L);
        }
    }

    // ========== 객실 가용성 조회 ==========

    @Nested
    @DisplayName("객실 가용성 조회 (GET /reservations/availability)")
    class Availability {

        @Test
        @DisplayName("가용 객실 조회 시 200 OK + 가용 수량 반환")
        void checkAvailability_returns200() throws Exception {
            LocalDate checkIn = LocalDate.now().plusDays(7);
            LocalDate checkOut = LocalDate.now().plusDays(8);

            when(reservationService.checkAvailability(eq(PROPERTY_ID), eq(1L), eq(checkIn), eq(checkOut)))
                    .thenReturn(5);

            mockMvc.perform(get(BASE_URL + "/availability", PROPERTY_ID)
                            .param("roomTypeId", "1")
                            .param("checkIn", checkIn.toString())
                            .param("checkOut", checkOut.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.availableCount").value(5))
                    .andExpect(jsonPath("$.data.overbooking").value(false));
        }
    }

    // ========== 예약 메모 ==========

    @Nested
    @DisplayName("예약 메모 (GET/POST /reservations/{id}/memos)")
    class Memos {

        @Test
        @DisplayName("메모 등록 시 201 CREATED 반환")
        void addMemo_returns201() throws Exception {
            when(reservationService.addMemo(eq(1L), eq(PROPERTY_ID), eq("VIP 고객")))
                    .thenReturn(com.hola.reservation.dto.response.ReservationMemoResponse.builder()
                            .id(100L)
                            .content("VIP 고객")
                            .createdBy("admin")
                            .build());

            mockMvc.perform(post(BASE_URL + "/{id}/memos", PROPERTY_ID, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", "VIP 고객"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").value("VIP 고객"))
                    .andExpect(jsonPath("$.data.createdBy").value("admin"));
        }
    }

    // ========== 캘린더 뷰 ==========

    @Nested
    @DisplayName("캘린더 뷰 (GET /reservations/calendar)")
    class Calendar {

        @Test
        @DisplayName("캘린더 데이터 조회 시 200 OK + 날짜별 그룹핑 데이터 반환")
        void getCalendar_returns200() throws Exception {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);

            when(reservationService.getCalendarData(
                    eq(PROPERTY_ID), eq(startDate), eq(endDate), any(), any()))
                    .thenReturn(Collections.emptyMap());

            mockMvc.perform(get(BASE_URL + "/calendar", PROPERTY_ID)
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isMap());
        }
    }

    // ========== 예약 삭제 (SUPER_ADMIN 전용) ==========

    @Nested
    @DisplayName("예약 삭제 (DELETE /reservations/{id}/delete)")
    class DeleteReservation {

        @Test
        @DisplayName("SUPER_ADMIN이 CHECKED_OUT 예약 삭제 시 200 OK 반환")
        void deleteReservation_superAdmin_returns200() throws Exception {
            doNothing().when(reservationService).deleteReservation(eq(1L), eq(PROPERTY_ID));

            mockMvc.perform(delete(BASE_URL + "/{id}/delete", PROPERTY_ID, 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(reservationService).deleteReservation(1L, PROPERTY_ID);
        }

        @Test
        @DisplayName("CHECKED_OUT 아닌 예약 삭제 시 400 BAD REQUEST 반환")
        void deleteReservation_notCheckedOut_returns400() throws Exception {
            doThrow(new com.hola.common.exception.HolaException(
                    com.hola.common.exception.ErrorCode.RESERVATION_DELETE_NOT_ALLOWED))
                    .when(reservationService).deleteReservation(eq(1L), eq(PROPERTY_ID));

            mockMvc.perform(delete(BASE_URL + "/{id}/delete", PROPERTY_ID, 1L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("HOLA-4044"));
        }
    }

    // ========== 취소 수수료 미리보기 ==========

    @Nested
    @DisplayName("취소 수수료 미리보기 (GET /reservations/{id}/cancel-preview)")
    class CancelPreview {

        @Test
        @DisplayName("취소 수수료 미리보기 조회 시 200 OK + 수수료 정보 반환")
        void getCancelPreview_returns200() throws Exception {
            when(reservationService.getCancelPreview(eq(1L), eq(PROPERTY_ID), eq(false)))
                    .thenReturn(com.hola.reservation.dto.response.AdminCancelPreviewResponse.builder()
                            .reservationId(1L)
                            .masterReservationNo("RSV-20260314-0001")
                            .guestNameKo("홍길동")
                            .cancelFeeAmount(java.math.BigDecimal.valueOf(50000))
                            .cancelFeePercent(java.math.BigDecimal.valueOf(50))
                            .totalPaidAmount(java.math.BigDecimal.valueOf(200000))
                            .refundAmount(java.math.BigDecimal.valueOf(150000))
                            .policyDescription("체크인 3일 전: 1박 요금의 50%")
                            .build());

            mockMvc.perform(get(BASE_URL + "/{id}/cancel-preview", PROPERTY_ID, 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.cancelFeeAmount").value(50000))
                    .andExpect(jsonPath("$.data.refundAmount").value(150000))
                    .andExpect(jsonPath("$.data.policyDescription").isNotEmpty());
        }
    }

    // ========== 헬퍼 메서드 ==========

    /**
     * 예약 생성 요청 빌더 (공통)
     */
    private ReservationCreateRequest buildCreateRequest(LocalDate checkIn, LocalDate checkOut) {
        return ReservationCreateRequest.builder()
                .masterCheckIn(checkIn)
                .masterCheckOut(checkOut)
                .guestNameKo("홍길동")
                .phoneCountryCode("+82")
                .phoneNumber("01012345678")
                .rateCodeId(1L)
                .subReservations(List.of(
                        SubReservationRequest.builder()
                                .roomTypeId(1L)
                                .checkIn(checkIn)
                                .checkOut(checkOut)
                                .adults(2)
                                .children(0)
                                .build()
                ))
                .build();
    }
}
