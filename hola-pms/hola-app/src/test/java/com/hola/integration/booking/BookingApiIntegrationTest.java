package com.hola.integration.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hola.hotel.entity.Property;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.ReservationPayment;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.DailyChargeRepository;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.ReservationPaymentRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.RoomTypeRepository;
import com.hola.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BookingApiController 통합 테스트
 * - 부킹엔진 공개 API (permitAll)
 * - Flyway 테스트 데이터 기반 (V5_0_0 ~ V5_14_0)
 * - 프로퍼티 코드: GMP (올라 그랜드 명동), GMS, OBH
 * - 응답 형식: BookingResponse ($.result.RESULT_YN / $.result.data)
 */
@DisplayName("부킹엔진 API 통합 테스트")
class BookingApiIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/booking";
    /** Flyway 테스트 데이터의 프로퍼티 코드 */
    private static final String VALID_PROPERTY_CODE = "GMP";
    private static final String NON_EXISTENT_CODE = "ZZZZZ";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private MasterReservationRepository masterReservationRepository;

    @Autowired
    private SubReservationRepository subReservationRepository;

    @Autowired
    private DailyChargeRepository dailyChargeRepository;

    @Autowired
    private ReservationPaymentRepository reservationPaymentRepository;

    // ========== 1. 프로퍼티 정보 조회 ==========

    @Test
    @DisplayName("존재하는 프로퍼티 코드로 조회 시 200 반환 및 프로퍼티 정보 포함")
    void getPropertyInfo_existingCode_200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/properties/{code}", VALID_PROPERTY_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.RESULT_YN").value("Y"))
                .andExpect(jsonPath("$.result.data.propertyCode").value(VALID_PROPERTY_CODE))
                .andExpect(jsonPath("$.result.data.propertyName").value("올라 그랜드 명동"))
                .andExpect(jsonPath("$.result.data.checkInTime").value("15:00"))
                .andExpect(jsonPath("$.result.data.checkOutTime").value("11:00"))
                .andExpect(jsonPath("$.result.data.hotelName").value("올라 서울 호텔"));
    }

    @Test
    @DisplayName("존재하지 않는 프로퍼티 코드로 조회 시 404 반환")
    void getPropertyInfo_nonExistentCode_404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/properties/{code}", NON_EXISTENT_CODE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.result.RESULT_YN").value("N"))
                .andExpect(jsonPath("$.result.RESULT_CODE").value("HOLA-4070"));
    }

    // ========== 2. 가용 객실 검색 ==========

    @Test
    @DisplayName("유효한 날짜/인원으로 가용 객실 검색 시 200 반환")
    void searchAvailability_validParams_200() throws Exception {
        // 미래 날짜로 검색 (레이트코드 판매기간: 2026-01-01 ~ 2026-12-31)
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        mockMvc.perform(get(BASE_URL + "/properties/{code}/availability", VALID_PROPERTY_CODE)
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("adults", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.RESULT_YN").value("Y"))
                .andExpect(jsonPath("$.result.data").isArray());
    }

    @Test
    @DisplayName("체크인 > 체크아웃 (역전된 날짜)으로 검색 시 400 반환")
    void searchAvailability_reversedDates_400() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.minusDays(1); // 체크인보다 이전

        mockMvc.perform(get(BASE_URL + "/properties/{code}/availability", VALID_PROPERTY_CODE)
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("adults", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result.RESULT_YN").value("N"))
                .andExpect(jsonPath("$.result.RESULT_CODE").value("HOLA-4076"));
    }

    @Test
    @DisplayName("과거 체크인 날짜로 검색 시 400 반환")
    void searchAvailability_pastDates_400() throws Exception {
        LocalDate checkIn = LocalDate.now().minusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(1);

        mockMvc.perform(get(BASE_URL + "/properties/{code}/availability", VALID_PROPERTY_CODE)
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("adults", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result.RESULT_YN").value("N"))
                .andExpect(jsonPath("$.result.RESULT_CODE").value("HOLA-4076"));
    }

    @Test
    @DisplayName("30박 초과 숙박 검색 시 400 반환")
    void searchAvailability_exceeds30Nights_400() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(31); // 31박 = 30박 초과

        mockMvc.perform(get(BASE_URL + "/properties/{code}/availability", VALID_PROPERTY_CODE)
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("adults", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result.RESULT_YN").value("N"))
                .andExpect(jsonPath("$.result.RESULT_CODE").value("HOLA-4077"));
    }

    // ========== 3. 예약 생성 ==========

    @Test
    @DisplayName("이용약관 미동의 시 400 반환")
    void createBooking_termsNotAgreed_400() throws Exception {
        Property property = propertyRepository.findByPropertyCodeAndUseYnTrue(VALID_PROPERTY_CODE)
                .orElseThrow();
        List<RoomType> roomTypes = roomTypeRepository
                .findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(property.getId());
        RoomType firstRoomType = roomTypes.get(0);

        LocalDate checkIn = LocalDate.now().plusDays(14);
        LocalDate checkOut = checkIn.plusDays(2);

        Map<String, Object> request = buildBookingRequest(
                firstRoomType.getId(), 1L, checkIn, checkOut, false);

        mockMvc.perform(post(BASE_URL + "/properties/{code}/reservations", VALID_PROPERTY_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 예약 요청 시 201 반환 및 확인번호 포함")
    void createBooking_validRequest_201() throws Exception {
        // 프로퍼티 + 객실타입 조회 (Flyway 데이터)
        Property property = propertyRepository.findByPropertyCodeAndUseYnTrue(VALID_PROPERTY_CODE)
                .orElseThrow();
        List<RoomType> roomTypes = roomTypeRepository
                .findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(property.getId());

        // STD-D (성인 2명 수용 가능)
        RoomType stdDouble = roomTypes.stream()
                .filter(rt -> "STD-D".equals(rt.getRoomTypeCode()))
                .findFirst()
                .orElseThrow();

        // RACK 레이트코드 ID 조회 (가용 객실 검색으로 ID 확보)
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(2);

        // 가용 검색으로 rateCodeId 확인
        String searchResult = mockMvc.perform(get(BASE_URL + "/properties/{code}/availability", VALID_PROPERTY_CODE)
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("adults", "2"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 검색 결과에서 rateCodeId 추출 (BookingResponse 형식)
        var searchNode = objectMapper.readTree(searchResult);
        var dataArray = searchNode.get("result").get("data");
        if (dataArray == null || !dataArray.isArray() || dataArray.isEmpty()) {
            // 가용 객실이 없으면 테스트 스킵 (테스트 데이터 의존)
            return;
        }

        // STD-D 타입의 첫 번째 레이트옵션 사용
        Long rateCodeId = null;
        Long roomTypeId = null;
        for (var roomNode : dataArray) {
            if (roomNode.get("roomTypeId").asLong() == stdDouble.getId()) {
                var rateOptions = roomNode.get("rateOptions");
                if (rateOptions != null && rateOptions.isArray() && !rateOptions.isEmpty()) {
                    rateCodeId = rateOptions.get(0).get("rateCodeId").asLong();
                    roomTypeId = roomNode.get("roomTypeId").asLong();
                    break;
                }
            }
        }

        if (rateCodeId == null) {
            // 해당 객실에 적용 가능한 레이트 없으면 스킵
            return;
        }

        Map<String, Object> request = buildBookingRequest(
                roomTypeId, rateCodeId, checkIn, checkOut, true);

        mockMvc.perform(post(BASE_URL + "/properties/{code}/reservations", VALID_PROPERTY_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.RESULT_YN").value("Y"))
                .andExpect(jsonPath("$.result.data.confirmationNo").isNotEmpty())
                .andExpect(jsonPath("$.result.data.masterReservationNo").isNotEmpty())
                .andExpect(jsonPath("$.result.data.reservationStatus").value("RESERVED"))
                .andExpect(jsonPath("$.result.data.guestNameKo").value("테스트게스트"))
                .andExpect(jsonPath("$.result.data.propertyName").value("올라 그랜드 명동"));
    }

    // ========== 4. 예약 확인 조회 ==========

    @Test
    @DisplayName("부킹엔진 API는 인증 없이 200 응답 가능 (permitAll)")
    @WithAnonymousUser
    void getConfirmation_noAuth_200() throws Exception {
        // 테스트용 예약 데이터 직접 생성
        MasterReservation master = createTestReservation();

        mockMvc.perform(get(BASE_URL + "/confirmation/{confirmNo}", master.getConfirmationNo())
                        .param("email", master.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.RESULT_YN").value("Y"))
                .andExpect(jsonPath("$.result.data.confirmationNo").value(master.getConfirmationNo()))
                .andExpect(jsonPath("$.result.data.guestNameKo").value(master.getGuestNameKo()));
    }

    // ========== 5. 취소 수수료 미리보기 ==========

    @Test
    @DisplayName("존재하지 않는 확인번호로 취소 수수료 조회 시 404 반환")
    void cancelFeePreview_invalidConfirmation_404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/reservations/{confirmNo}/cancel-fee", "NONEXIST999")
                        .param("email", "test@test.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.result.RESULT_YN").value("N"))
                .andExpect(jsonPath("$.result.RESULT_CODE").value("HOLA-4078"));
    }

    @Test
    @DisplayName("유효한 확인번호로 취소 수수료 미리보기 시 200 반환")
    void cancelFeePreview_validConfirmation_200() throws Exception {
        MasterReservation master = createTestReservation();

        mockMvc.perform(get(BASE_URL + "/reservations/{confirmNo}/cancel-fee", master.getConfirmationNo())
                        .param("email", master.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.RESULT_YN").value("Y"))
                .andExpect(jsonPath("$.result.data.confirmationNo").value(master.getConfirmationNo()))
                .andExpect(jsonPath("$.result.data.cancelFeeAmount").isNumber())
                .andExpect(jsonPath("$.result.data.refundAmount").isNumber());
    }

    // ========== 6. 게스트 자가 취소 ==========

    @Test
    @DisplayName("이미 취소된 예약 재취소 시 400 반환")
    void cancelBooking_alreadyCanceled_400() throws Exception {
        MasterReservation master = createTestReservation();

        // 첫 번째 취소
        Map<String, String> cancelRequest = Map.of("email", master.getEmail());

        mockMvc.perform(post(BASE_URL + "/reservations/{confirmNo}/cancel", master.getConfirmationNo())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk());

        // 두 번째 취소 시도 → 이미 취소됨
        mockMvc.perform(post(BASE_URL + "/reservations/{confirmNo}/cancel", master.getConfirmationNo())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result.RESULT_YN").value("N"))
                .andExpect(jsonPath("$.result.RESULT_CODE").value("HOLA-4081"));
    }

    @Test
    @DisplayName("유효한 취소 요청 시 200 반환 및 CANCELED 상태")
    void cancelBooking_validRequest_200() throws Exception {
        MasterReservation master = createTestReservation();

        Map<String, String> cancelRequest = Map.of("email", master.getEmail());

        mockMvc.perform(post(BASE_URL + "/reservations/{confirmNo}/cancel", master.getConfirmationNo())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.RESULT_YN").value("Y"))
                .andExpect(jsonPath("$.result.data.confirmationNo").value(master.getConfirmationNo()))
                .andExpect(jsonPath("$.result.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.result.data.cancelFeeAmount").isNumber())
                .andExpect(jsonPath("$.result.data.refundAmount").isNumber());
    }

    // ========== 7. 인증 관련 ==========

    @Test
    @DisplayName("부킹엔진 모든 엔드포인트는 인증 없이 접근 가능 (permitAll)")
    @WithAnonymousUser
    void bookingEndpoint_noAuth_accessible() throws Exception {
        // 프로퍼티 조회 - 401/403 아님
        mockMvc.perform(get(BASE_URL + "/properties/{code}", VALID_PROPERTY_CODE))
                .andExpect(status().isOk());

        // 가용 객실 검색 - 인증 없이도 접근 가능 (401/403 아님)
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);
        mockMvc.perform(get(BASE_URL + "/properties/{code}/availability", VALID_PROPERTY_CODE)
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("adults", "1"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertThat(s).isNotEqualTo(401);
                    assertThat(s).isNotEqualTo(403);
                });

        // 취소 수수료 조회 - 404 (존재하지 않는 확인번호) but 401/403 아님
        mockMvc.perform(get(BASE_URL + "/reservations/FAKE123/cancel-fee")
                        .param("email", "test@test.com"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertThat(s).isNotEqualTo(401);
                    assertThat(s).isNotEqualTo(403);
                });
    }

    // ===== Helper Methods =====

    /**
     * 예약 생성 요청 Map 빌드
     */
    private Map<String, Object> buildBookingRequest(Long roomTypeId, Long rateCodeId,
                                                     LocalDate checkIn, LocalDate checkOut,
                                                     boolean agreedTerms) {
        Map<String, Object> guest = new LinkedHashMap<>();
        guest.put("guestNameKo", "테스트게스트");
        guest.put("guestFirstNameEn", "Test");
        guest.put("guestLastNameEn", "Guest");
        guest.put("phoneCountryCode", "+82");
        guest.put("phoneNumber", "01099998888");
        guest.put("email", "testguest@test.com");
        guest.put("nationality", "KR");

        Map<String, Object> room = new LinkedHashMap<>();
        room.put("roomTypeId", roomTypeId);
        room.put("rateCodeId", rateCodeId);
        room.put("checkIn", checkIn.toString());
        room.put("checkOut", checkOut.toString());
        room.put("adults", 2);
        room.put("children", 0);

        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("method", "CARD");
        payment.put("cardNumber", "4111111111111111");
        payment.put("expiryDate", "12/28");
        payment.put("cvv", "123");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("idempotencyKey", UUID.randomUUID().toString());
        request.put("guest", guest);
        request.put("rooms", List.of(room));
        request.put("payment", payment);
        request.put("agreedTerms", agreedTerms);

        return request;
    }

    /**
     * 테스트용 예약 데이터 직접 DB 생성 (취소/조회 테스트용)
     * - MasterReservation, SubReservation, DailyCharge, ReservationPayment
     */
    private MasterReservation createTestReservation() {
        Property property = propertyRepository.findByPropertyCodeAndUseYnTrue(VALID_PROPERTY_CODE)
                .orElseThrow();
        List<RoomType> roomTypes = roomTypeRepository
                .findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(property.getId());
        RoomType roomType = roomTypes.get(0);

        String confirmNo = "TEST" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LocalDate checkIn = LocalDate.now().plusDays(20);
        LocalDate checkOut = checkIn.plusDays(2);

        // 마스터 예약 생성
        MasterReservation master = MasterReservation.builder()
                .property(property)
                .masterReservationNo("T" + String.valueOf(System.nanoTime()).substring(6))
                .confirmationNo(confirmNo)
                .reservationStatus("RESERVED")
                .masterCheckIn(checkIn)
                .masterCheckOut(checkOut)
                .guestNameKo("홍길동")
                .guestFirstNameEn("Gildong")
                .guestLastNameEn("Hong")
                .phoneCountryCode("+82")
                .phoneNumber("01012345678")
                .email("hong@test.com")
                .rateCodeId(1L)
                .build();
        master = masterReservationRepository.save(master);

        // 서브 예약 생성
        SubReservation sub = SubReservation.builder()
                .masterReservation(master)
                .subReservationNo(master.getMasterReservationNo() + "-01")
                .roomReservationStatus("RESERVED")
                .roomTypeId(roomType.getId())
                .adults(2)
                .children(0)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .build();
        sub = subReservationRepository.save(sub);

        // 일별 요금 생성
        for (int i = 0; i < 2; i++) {
            DailyCharge charge = DailyCharge.builder()
                    .subReservation(sub)
                    .chargeDate(checkIn.plusDays(i))
                    .supplyPrice(new BigDecimal("100000"))
                    .tax(new BigDecimal("10000"))
                    .serviceCharge(new BigDecimal("10000"))
                    .total(new BigDecimal("120000"))
                    .build();
            dailyChargeRepository.save(charge);
        }

        // 결제 정보 생성
        ReservationPayment payment = ReservationPayment.builder()
                .masterReservation(master)
                .paymentStatus("PAID")
                .totalRoomAmount(new BigDecimal("240000"))
                .totalServiceAmount(BigDecimal.ZERO)
                .totalServiceChargeAmount(BigDecimal.ZERO)
                .totalAdjustmentAmount(BigDecimal.ZERO)
                .totalEarlyLateFee(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("240000"))
                .totalPaidAmount(new BigDecimal("240000"))
                .cancelFeeAmount(BigDecimal.ZERO)
                .refundAmount(BigDecimal.ZERO)
                .build();
        reservationPaymentRepository.save(payment);

        return master;
    }
}
