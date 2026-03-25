package com.hola.reservation.booking.pg.kicc;

import com.hola.common.exception.HolaException;
import com.hola.common.exception.ErrorCode;
import com.hola.reservation.booking.pg.kicc.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * KICC 이지페이 API HTTP 클라이언트
 * - 모든 KICC API 호출을 담당
 * - 공통 에러 처리 및 로깅
 */
@Slf4j
@Component
@Profile("!test")
public class KiccApiClient {

    private final RestTemplate restTemplate;
    private final KiccProperties properties;

    public KiccApiClient(@Qualifier("kiccRestTemplate") RestTemplate restTemplate,
                          KiccProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 거래등록 (POST /api/ep9/trades/webpay)
     * 결제창 URL(authPageUrl)을 반환
     */
    public KiccRegisterResponse registerTransaction(KiccRegisterRequest request) {
        String url = properties.getApiDomain() + "/api/ep9/trades/webpay";
        log.info("[KICC] 거래등록 요청 - shopOrderNo: {}, amount: {}", request.getShopOrderNo(), request.getAmount());

        KiccRegisterResponse response = post(url, request, KiccRegisterResponse.class);

        if (!response.isSuccess()) {
            log.error("[KICC] 거래등록 실패 - resCd: {}, resMsg: {}", response.getResCd(), response.getResMsg());
            throw new HolaException(ErrorCode.PG_REGISTER_FAILED, response.getResMsg());
        }

        log.info("[KICC] 거래등록 성공 - authPageUrl 발급 완료");
        return response;
    }

    /**
     * 결제승인 / 빌키발급 (POST /api/ep9/trades/approval)
     */
    public KiccApprovalResponse approvePayment(KiccApprovalRequest request) {
        String url = properties.getApiDomain() + "/api/ep9/trades/approval";
        log.info("[KICC] 결제승인 요청 - shopOrderNo: {}, shopTransactionId: {}",
                request.getShopOrderNo(), request.getShopTransactionId());

        KiccApprovalResponse response = post(url, request, KiccApprovalResponse.class);

        if (!response.isSuccess()) {
            log.error("[KICC] 결제승인 실패 - resCd: {}, resMsg: {}", response.getResCd(), response.getResMsg());
            throw new HolaException(ErrorCode.PG_APPROVAL_FAILED, response.getResMsg());
        }

        log.info("[KICC] 결제승인 성공 - pgCno: {}, amount: {}", response.getPgCno(), response.getAmount());
        return response;
    }

    /**
     * 빌키 결제 승인 (POST /api/trades/approval/batch)
     */
    public KiccApprovalResponse approveBatchPayment(KiccBatchApprovalRequest request) {
        String url = properties.getApiDomain() + "/api/trades/approval/batch";
        log.info("[KICC] 빌키결제 요청 - shopOrderNo: {}, amount: {}",
                request.getShopOrderNo(), request.getAmount());

        KiccApprovalResponse response = post(url, request, KiccApprovalResponse.class);

        if (!response.isSuccess()) {
            log.error("[KICC] 빌키결제 실패 - resCd: {}, resMsg: {}", response.getResCd(), response.getResMsg());
            throw new HolaException(ErrorCode.PG_APPROVAL_FAILED, response.getResMsg());
        }

        log.info("[KICC] 빌키결제 성공 - pgCno: {}, amount: {}", response.getPgCno(), response.getAmount());
        return response;
    }

    /**
     * 빌키 삭제 (POST /api/trades/removeBatchKey)
     */
    public KiccRemoveKeyResponse removeBatchKey(KiccRemoveKeyRequest request) {
        String url = properties.getApiDomain() + "/api/trades/removeBatchKey";
        log.info("[KICC] 빌키삭제 요청 - shopTransactionId: {}", request.getShopTransactionId());

        KiccRemoveKeyResponse response = post(url, request, KiccRemoveKeyResponse.class);

        if (!response.isSuccess()) {
            log.error("[KICC] 빌키삭제 실패 - resCd: {}, resMsg: {}", response.getResCd(), response.getResMsg());
            throw new HolaException(ErrorCode.PG_CANCEL_FAILED, response.getResMsg());
        }

        log.info("[KICC] 빌키삭제 성공");
        return response;
    }

    /**
     * 결제 취소/환불 (POST /api/trades/revise)
     */
    public KiccReviseResponse revisePayment(KiccReviseRequest request) {
        String url = properties.getApiDomain() + "/api/trades/revise";
        log.info("[KICC] 취소/환불 요청 - pgCno: {}, reviseTypeCode: {}",
                request.getPgCno(), request.getReviseTypeCode());

        KiccReviseResponse response = post(url, request, KiccReviseResponse.class);

        if (!response.isSuccess()) {
            log.error("[KICC] 취소/환불 실패 - resCd: {}, resMsg: {}", response.getResCd(), response.getResMsg());
            throw new HolaException(ErrorCode.PG_CANCEL_FAILED, response.getResMsg());
        }

        log.info("[KICC] 취소/환불 성공 - cancelPgCno: {}, cancelAmount: {}",
                response.getCancelPgCno(), response.getCancelAmount());
        return response;
    }

    /**
     * 거래상태 조회 (POST /api/trades/retrieveTransaction)
     */
    public KiccApprovalResponse retrieveTransaction(KiccQueryRequest request) {
        String url = properties.getApiDomain() + "/api/trades/retrieveTransaction";
        log.info("[KICC] 거래조회 요청 - shopTransactionId: {}", request.getShopTransactionId());

        KiccApprovalResponse response = post(url, request, KiccApprovalResponse.class);

        if (!response.isSuccess()) {
            log.warn("[KICC] 거래조회 실패 - resCd: {}, resMsg: {}", response.getResCd(), response.getResMsg());
        }

        return response;
    }

    /**
     * 공통 POST 요청
     */
    private <T> T post(String url, Object request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("[KICC] API 통신 오류 - url: {}, error: {}", url, e.getMessage());
            throw new HolaException(ErrorCode.PG_COMMUNICATION_ERROR, e.getMessage());
        }
    }
}
