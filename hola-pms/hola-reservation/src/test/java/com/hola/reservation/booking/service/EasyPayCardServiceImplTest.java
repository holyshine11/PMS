package com.hola.reservation.booking.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.booking.dto.response.EasyPayCardResponse;
import com.hola.reservation.booking.entity.EasyPayCard;
import com.hola.reservation.booking.pg.kicc.KiccApiClient;
import com.hola.reservation.booking.pg.kicc.KiccProperties;
import com.hola.reservation.booking.repository.EasyPayCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EasyPayCardServiceImpl 단위 테스트
 * - kiccApiClient, kiccProperties는 @Autowired(required=false) 필드 주입이므로
 *   ReflectionTestUtils.setField로 주입
 */
@DisplayName("EasyPayCardServiceImpl - 간편결제 카드 서비스")
@ExtendWith(MockitoExtension.class)
class EasyPayCardServiceImplTest {

    @InjectMocks
    private EasyPayCardServiceImpl easyPayCardServiceImpl;

    @Mock
    private EasyPayCardRepository easyPayCardRepository;

    @Mock
    private KiccApiClient kiccApiClient;

    @Mock
    private KiccProperties kiccProperties;

    @BeforeEach
    void setUp() {
        // @Autowired(required=false) 필드는 @InjectMocks가 주입하지 않으므로 직접 주입
        ReflectionTestUtils.setField(easyPayCardServiceImpl, "kiccApiClient", kiccApiClient);
        ReflectionTestUtils.setField(easyPayCardServiceImpl, "kiccProperties", kiccProperties);
    }

    // ========================================
    // 테스트 픽스처 빌더
    // ========================================

    private EasyPayCard buildCard(Long id, String email) {
        EasyPayCard card = EasyPayCard.builder()
                .email(email)
                .batchKey("BK_TEST_123")
                .cardMaskNo("1234****5678")
                .issuerName("신한카드")
                .cardType("신용")
                .pgCno("PG_TEST_001")
                .build();
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    // ========================================
    // getCardsByEmail
    // ========================================

    @Nested
    @DisplayName("getCardsByEmail")
    class GetCardsByEmail {

        @Test
        @DisplayName("등록된 카드 목록을 반환한다")
        void returnsCardList() {
            // given
            EasyPayCard card1 = buildCard(1L, "test@email.com");
            EasyPayCard card2 = buildCard(2L, "test@email.com");
            when(easyPayCardRepository.findByEmailOrderByCreatedAtDesc("test@email.com"))
                    .thenReturn(List.of(card1, card2));

            // when
            List<EasyPayCardResponse> result = easyPayCardServiceImpl.getCardsByEmail("test@email.com");

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEmail()).isEqualTo("test@email.com");
            assertThat(result.get(0).getCardMaskNo()).isEqualTo("1234****5678");
            assertThat(result.get(0).getIssuerName()).isEqualTo("신한카드");
        }

        @Test
        @DisplayName("등록된 카드가 없으면 빈 목록을 반환한다")
        void returnsEmptyListWhenNoCards() {
            // given
            when(easyPayCardRepository.findByEmailOrderByCreatedAtDesc("none@email.com"))
                    .thenReturn(Collections.emptyList());

            // when
            List<EasyPayCardResponse> result = easyPayCardServiceImpl.getCardsByEmail("none@email.com");

            // then
            assertThat(result).isEmpty();
        }
    }

    // ========================================
    // registerCard
    // ========================================

    @Nested
    @DisplayName("registerCard")
    class RegisterCard {

        @Test
        @DisplayName("카드를 정상 등록한다")
        void registersCardSuccessfully() {
            // given
            when(easyPayCardRepository.countByEmail("test@email.com")).thenReturn(0L);
            EasyPayCard saved = buildCard(1L, "test@email.com");
            when(easyPayCardRepository.save(any(EasyPayCard.class))).thenReturn(saved);

            // when
            EasyPayCardResponse response = easyPayCardServiceImpl.registerCard(
                    "test@email.com", "BK123", "1234****5678", "신한카드", "신용", "PG001");

            // then
            verify(easyPayCardRepository, times(1)).save(any(EasyPayCard.class));
            assertThat(response.getEmail()).isEqualTo("test@email.com");
            assertThat(response.getCardMaskNo()).isEqualTo("1234****5678");
            assertThat(response.getIssuerName()).isEqualTo("신한카드");
        }

        @Test
        @DisplayName("5개 초과 시 EASY_PAY_CARD_LIMIT_EXCEEDED 예외 발생")
        void throwsExceptionWhenLimitExceeded() {
            // given
            when(easyPayCardRepository.countByEmail("test@email.com")).thenReturn(5L);

            // when/then
            assertThatThrownBy(() -> easyPayCardServiceImpl.registerCard(
                    "test@email.com", "BK123", "1234****5678", "신한카드", "신용", "PG001"))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.EASY_PAY_CARD_LIMIT_EXCEEDED));
        }
    }

    // ========================================
    // deleteCard
    // ========================================

    @Nested
    @DisplayName("deleteCard")
    class DeleteCard {

        @Test
        @DisplayName("카드를 정상 삭제한다 — KICC 삭제 + soft delete")
        void deletesCardSuccessfully() {
            // given
            EasyPayCard card = buildCard(1L, "test@email.com");
            when(easyPayCardRepository.findById(1L)).thenReturn(Optional.of(card));
            when(kiccProperties.getMallId()).thenReturn("T5102001");

            // when
            easyPayCardServiceImpl.deleteCard(1L, "test@email.com");

            // then
            verify(kiccApiClient, times(1)).removeBatchKey(any());
            assertThat(card.getDeletedAt()).isNotNull();
            assertThat(card.getUseYn()).isFalse();
        }

        @Test
        @DisplayName("이메일 불일치 시 EASY_PAY_CARD_EMAIL_MISMATCH 예외 발생")
        void throwsExceptionWhenEmailMismatch() {
            // given
            EasyPayCard card = buildCard(1L, "other@email.com");
            when(easyPayCardRepository.findById(1L)).thenReturn(Optional.of(card));

            // when/then
            assertThatThrownBy(() -> easyPayCardServiceImpl.deleteCard(1L, "test@email.com"))
                    .isInstanceOf(HolaException.class)
                    .satisfies(ex -> assertThat(((HolaException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.EASY_PAY_CARD_EMAIL_MISMATCH));
        }

        @Test
        @DisplayName("KICC 삭제 실패해도 DB soft delete는 수행한다")
        void softDeletesEvenWhenKiccFails() {
            // given
            EasyPayCard card = buildCard(1L, "test@email.com");
            when(easyPayCardRepository.findById(1L)).thenReturn(Optional.of(card));
            when(kiccProperties.getMallId()).thenReturn("T5102001");
            doThrow(new RuntimeException("KICC 연결 오류")).when(kiccApiClient).removeBatchKey(any());

            // when
            easyPayCardServiceImpl.deleteCard(1L, "test@email.com");

            // then — soft delete는 여전히 수행됨
            assertThat(card.getDeletedAt()).isNotNull();
        }
    }

    // ========================================
    // canRegisterMore
    // ========================================

    @Nested
    @DisplayName("canRegisterMore")
    class CanRegisterMore {

        @Test
        @DisplayName("4개 등록 시 true 반환")
        void returnsTrueWhenUnderLimit() {
            // given
            when(easyPayCardRepository.countByEmail("test@email.com")).thenReturn(4L);

            // when/then
            assertThat(easyPayCardServiceImpl.canRegisterMore("test@email.com")).isTrue();
        }

        @Test
        @DisplayName("5개 등록 시 false 반환")
        void returnsFalseWhenAtLimit() {
            // given
            when(easyPayCardRepository.countByEmail("test@email.com")).thenReturn(5L);

            // when/then
            assertThat(easyPayCardServiceImpl.canRegisterMore("test@email.com")).isFalse();
        }
    }
}
