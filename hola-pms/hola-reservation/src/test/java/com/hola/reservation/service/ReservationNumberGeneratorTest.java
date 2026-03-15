package com.hola.reservation.service;

import com.hola.hotel.entity.Property;
import com.hola.reservation.entity.ReservationNoSeq;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.reservation.repository.ReservationNoSeqRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 예약번호 생성기 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationNumberGenerator")
class ReservationNumberGeneratorTest {

    @InjectMocks
    private ReservationNumberGenerator generator;

    @Mock
    private ReservationNoSeqRepository reservationNoSeqRepository;

    @Mock
    private MasterReservationRepository masterReservationRepository;

    private Property createProperty() {
        return Property.builder()
                .propertyCode("GMP")
                .propertyName("테스트")
                .checkInTime("15:00")
                .checkOutTime("11:00")
                .build();
    }

    // ===== 마스터 예약번호 =====

    @Test
    @DisplayName("마스터 예약번호 형식: {propertyCode}{YYMMDD}-{4자리 시퀀스}")
    void generateMasterReservationNo_format() {
        Property property = createProperty();
        ReservationNoSeq seq = ReservationNoSeq.builder()
                .propertyId(1L)
                .seqDate(LocalDate.now())
                .lastSeq(0)
                .build();

        when(reservationNoSeqRepository.findByPropertyIdAndSeqDate(any(), any()))
                .thenReturn(Optional.of(seq));
        when(reservationNoSeqRepository.save(any())).thenReturn(seq);

        String result = generator.generateMasterReservationNo(property);

        // GMP + YYMMDD + - + 4자리
        assertThat(result).matches("GMP\\d{6}-\\d{4}");
    }

    @Test
    @DisplayName("시퀀스 증가: 동일 날짜에 연속 호출 시 0001 → 0002")
    void generateMasterReservationNo_sequenceIncrements() {
        Property property = createProperty();
        ReservationNoSeq seq = ReservationNoSeq.builder()
                .propertyId(1L)
                .seqDate(LocalDate.now())
                .lastSeq(0)
                .build();

        when(reservationNoSeqRepository.findByPropertyIdAndSeqDate(any(), any()))
                .thenReturn(Optional.of(seq));
        when(reservationNoSeqRepository.save(any())).thenReturn(seq);

        String first = generator.generateMasterReservationNo(property);
        assertThat(first).endsWith("-0001");

        String second = generator.generateMasterReservationNo(property);
        assertThat(second).endsWith("-0002");
    }

    @Test
    @DisplayName("새 날짜에 시퀀스 없으면 자동 생성")
    void generateMasterReservationNo_newDate_createsSeq() {
        Property property = createProperty();
        when(reservationNoSeqRepository.findByPropertyIdAndSeqDate(any(), any()))
                .thenReturn(Optional.empty());
        when(reservationNoSeqRepository.save(any())).thenAnswer(inv -> {
            ReservationNoSeq saved = inv.getArgument(0);
            return saved;
        });

        String result = generator.generateMasterReservationNo(property);
        assertThat(result).endsWith("-0001");
        verify(reservationNoSeqRepository, times(2)).save(any()); // 생성 + 업데이트
    }

    // ===== 서브 예약번호 =====

    @Test
    @DisplayName("서브 예약번호 형식: {masterNo}-{2자리 시퀀스}")
    void generateSubReservationNo_format() {
        String result = generator.generateSubReservationNo("GMP260310-0001", 1);
        assertThat(result).isEqualTo("GMP260310-0001-01");
    }

    @Test
    @DisplayName("서브 예약번호 2자리 제로패딩")
    void generateSubReservationNo_twoDigitPadding() {
        assertThat(generator.generateSubReservationNo("GMP260310-0001", 3))
                .isEqualTo("GMP260310-0001-03");
        assertThat(generator.generateSubReservationNo("GMP260310-0001", 12))
                .isEqualTo("GMP260310-0001-12");
    }

    // ===== 확인번호 =====

    @Test
    @DisplayName("확인번호는 8자리 영숫자")
    void generateConfirmationNo_8chars() {
        when(masterReservationRepository.existsByConfirmationNo(anyString())).thenReturn(false);

        String result = generator.generateConfirmationNo();
        assertThat(result).hasSize(8);
        assertThat(result).matches("[A-Z0-9]{8}");
    }

    @Test
    @DisplayName("중복 발생 시 재시도")
    void generateConfirmationNo_retryOnDuplicate() {
        when(masterReservationRepository.existsByConfirmationNo(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String result = generator.generateConfirmationNo();
        assertThat(result).hasSize(8);
        verify(masterReservationRepository, times(2)).existsByConfirmationNo(anyString());
    }

    @Test
    @DisplayName("3회 모두 충돌 시 12자리 폴백")
    void generateConfirmationNo_fallbackTo12() {
        when(masterReservationRepository.existsByConfirmationNo(anyString()))
                .thenReturn(true);

        String result = generator.generateConfirmationNo();
        assertThat(result).hasSize(12);
        assertThat(result).matches("[A-Z0-9]{12}");
    }
}
