package com.hola.reservation.booking.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.reservation.booking.dto.response.EasyPayCardResponse;
import com.hola.reservation.booking.entity.EasyPayCard;
import com.hola.reservation.booking.pg.kicc.KiccApiClient;
import com.hola.reservation.booking.pg.kicc.KiccProperties;
import com.hola.reservation.booking.pg.kicc.dto.KiccRemoveKeyRequest;
import com.hola.reservation.booking.repository.EasyPayCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 간편결제 카드 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EasyPayCardServiceImpl implements EasyPayCardService {

    private static final int MAX_CARDS_PER_EMAIL = 5;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EasyPayCardRepository easyPayCardRepository;

    @Autowired(required = false)
    private KiccApiClient kiccApiClient;

    @Autowired(required = false)
    private KiccProperties kiccProperties;

    @Override
    public List<EasyPayCardResponse> getCardsByEmail(String email) {
        return easyPayCardRepository.findByEmailOrderByCreatedAtDesc(email).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EasyPayCardResponse registerCard(String email, String batchKey, String cardMaskNo,
                                             String issuerName, String cardType, String pgCno) {
        // 카드 수 제한 체크
        long count = easyPayCardRepository.countByEmail(email);
        if (count >= MAX_CARDS_PER_EMAIL) {
            throw new HolaException(ErrorCode.EASY_PAY_CARD_LIMIT_EXCEEDED);
        }

        EasyPayCard card = EasyPayCard.builder()
                .email(email)
                .batchKey(batchKey)
                .cardMaskNo(cardMaskNo)
                .issuerName(issuerName)
                .cardType(cardType)
                .pgCno(pgCno)
                .build();

        EasyPayCard saved = easyPayCardRepository.save(card);
        log.info("[간편결제] 카드 등록 완료 - email: {}, cardMaskNo: {}, issuer: {}", email, cardMaskNo, issuerName);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId, String email) {
        EasyPayCard card = easyPayCardRepository.findById(cardId)
                .orElseThrow(() -> new HolaException(ErrorCode.EASY_PAY_CARD_NOT_FOUND));

        // 이메일 소유권 확인
        if (!card.getEmail().equalsIgnoreCase(email)) {
            throw new HolaException(ErrorCode.EASY_PAY_CARD_EMAIL_MISMATCH);
        }

        // KICC 빌키 삭제 (실패해도 DB에서는 삭제 처리 — 데모 편의)
        if (kiccApiClient != null && kiccProperties != null) {
            try {
                KiccRemoveKeyRequest removeRequest = KiccRemoveKeyRequest.builder()
                        .mallId(kiccProperties.getMallId())
                        .shopTransactionId(UUID.randomUUID().toString())
                        .batchKey(card.getBatchKey())
                        .removeReqDate(LocalDate.now().format(DATE_FMT))
                        .build();
                kiccApiClient.removeBatchKey(removeRequest);
                log.info("[간편결제] KICC 빌키 삭제 성공 - cardId: {}", cardId);
            } catch (Exception e) {
                log.warn("[간편결제] KICC 빌키 삭제 실패 (DB에서는 삭제 진행) - cardId: {}, error: {}", cardId, e.getMessage());
            }
        } else {
            log.info("[간편결제] KICC 클라이언트 비활성화 — DB soft delete만 수행 - cardId: {}", cardId);
        }

        // DB soft delete
        card.softDelete();
        log.info("[간편결제] 카드 삭제 완료 - cardId: {}, email: {}", cardId, email);
    }

    @Override
    public boolean canRegisterMore(String email) {
        return easyPayCardRepository.countByEmail(email) < MAX_CARDS_PER_EMAIL;
    }

    private EasyPayCardResponse toResponse(EasyPayCard card) {
        return EasyPayCardResponse.builder()
                .id(card.getId())
                .email(card.getEmail())
                .cardMaskNo(card.getCardMaskNo())
                .issuerName(card.getIssuerName())
                .cardType(card.getCardType())
                .cardAlias(card.getCardAlias())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
