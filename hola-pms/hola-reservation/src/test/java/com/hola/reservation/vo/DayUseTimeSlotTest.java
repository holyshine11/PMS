package com.hola.reservation.vo;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Property;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DayUseTimeSlot - Dayuse 이용 시간 슬롯")
class DayUseTimeSlotTest {

    @Nested
    @DisplayName("from() - Property 운영설정 기반 생성")
    class FromPropertyTest {

        @Test
        @DisplayName("정상: 운영시간 내 이용시간 생성")
        void 운영시간_내_정상_생성() {
            // given: 운영 10:00~20:00, 기본 5시간
            Property property = Property.builder()
                    .dayUseEnabled(true)
                    .dayUseStartTime("10:00")
                    .dayUseEndTime("20:00")
                    .dayUseDefaultHours(5)
                    .build();

            // when
            DayUseTimeSlot slot = DayUseTimeSlot.from(property, null);

            // then: 10:00 ~ 15:00
            assertThat(slot.startTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(slot.endTime()).isEqualTo(LocalTime.of(15, 0));
            assertThat(slot.durationHours()).isEqualTo(5);
        }

        @Test
        @DisplayName("정상: 명시적 이용시간 지정")
        void 명시적_이용시간_지정() {
            // given: 운영 10:00~20:00, 요청 3시간
            Property property = Property.builder()
                    .dayUseEnabled(true)
                    .dayUseStartTime("10:00")
                    .dayUseEndTime("20:00")
                    .dayUseDefaultHours(5)
                    .build();

            // when
            DayUseTimeSlot slot = DayUseTimeSlot.from(property, 3);

            // then: 10:00 ~ 13:00
            assertThat(slot.startTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(slot.endTime()).isEqualTo(LocalTime.of(13, 0));
            assertThat(slot.durationHours()).isEqualTo(3);
        }

        @Test
        @DisplayName("정상: 운영종료시간과 정확히 일치")
        void 운영종료시간_경계_일치() {
            // given: 운영 10:00~13:00, 3시간 → 종료 13:00 (경계값)
            Property property = Property.builder()
                    .dayUseEnabled(true)
                    .dayUseStartTime("10:00")
                    .dayUseEndTime("13:00")
                    .dayUseDefaultHours(3)
                    .build();

            // when
            DayUseTimeSlot slot = DayUseTimeSlot.from(property, null);

            // then: 10:00 ~ 13:00 (종료시간과 정확히 일치 → 허용)
            assertThat(slot.endTime()).isEqualTo(LocalTime.of(13, 0));
        }

        @Test
        @DisplayName("예외: 이용시간이 운영 종료시간 초과")
        void 운영종료시간_초과_예외() {
            // given: 운영 10:00~13:00, 기본 5시간 → 종료 15:00 > 13:00
            Property property = Property.builder()
                    .dayUseEnabled(true)
                    .dayUseStartTime("10:00")
                    .dayUseEndTime("13:00")
                    .dayUseDefaultHours(5)
                    .build();

            // when/then
            assertThatThrownBy(() -> DayUseTimeSlot.from(property, null))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DAY_USE_EXCEEDS_OPERATION_HOURS);
        }

        @Test
        @DisplayName("예외: 요청 이용시간이 운영 종료시간 초과")
        void 요청_이용시간_운영종료_초과() {
            // given: 운영 10:00~13:00, 요청 4시간 → 14:00 > 13:00
            Property property = Property.builder()
                    .dayUseEnabled(true)
                    .dayUseStartTime("10:00")
                    .dayUseEndTime("13:00")
                    .dayUseDefaultHours(3)
                    .build();

            // when/then
            assertThatThrownBy(() -> DayUseTimeSlot.from(property, 4))
                    .isInstanceOf(HolaException.class)
                    .extracting(e -> ((HolaException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DAY_USE_EXCEEDS_OPERATION_HOURS);
        }
    }

    @Nested
    @DisplayName("ofNullable() - null 안전 생성")
    class OfNullableTest {

        @Test
        @DisplayName("startTime null이면 null 반환")
        void startTime_null() {
            assertThat(DayUseTimeSlot.ofNullable(null, LocalTime.of(15, 0))).isNull();
        }

        @Test
        @DisplayName("endTime null이면 null 반환")
        void endTime_null() {
            assertThat(DayUseTimeSlot.ofNullable(LocalTime.of(10, 0), null)).isNull();
        }

        @Test
        @DisplayName("둘 다 있으면 정상 생성")
        void 정상_생성() {
            DayUseTimeSlot slot = DayUseTimeSlot.ofNullable(LocalTime.of(10, 0), LocalTime.of(15, 0));
            assertThat(slot).isNotNull();
            assertThat(slot.startTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(slot.endTime()).isEqualTo(LocalTime.of(15, 0));
        }
    }

    @Nested
    @DisplayName("overlapsWith() - 시간 겹침 검사")
    class OverlapsWithTest {

        @Test
        @DisplayName("겹침: 10:00~13:00 vs 12:00~15:00")
        void 일부_겹침() {
            DayUseTimeSlot a = new DayUseTimeSlot(LocalTime.of(10, 0), LocalTime.of(13, 0));
            DayUseTimeSlot b = new DayUseTimeSlot(LocalTime.of(12, 0), LocalTime.of(15, 0));
            assertThat(a.overlapsWith(b)).isTrue();
        }

        @Test
        @DisplayName("안겹침: 10:00~13:00 vs 13:00~16:00 (경계 접촉)")
        void 경계_접촉_안겹침() {
            DayUseTimeSlot a = new DayUseTimeSlot(LocalTime.of(10, 0), LocalTime.of(13, 0));
            DayUseTimeSlot b = new DayUseTimeSlot(LocalTime.of(13, 0), LocalTime.of(16, 0));
            assertThat(a.overlapsWith(b)).isFalse();
        }

        @Test
        @DisplayName("상대가 null이면 충돌로 처리")
        void null_충돌() {
            DayUseTimeSlot a = new DayUseTimeSlot(LocalTime.of(10, 0), LocalTime.of(13, 0));
            assertThat(a.overlapsWith(null)).isTrue();
        }
    }
}
