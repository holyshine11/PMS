package com.hola.rate.service;

import com.hola.rate.dto.request.RateCodeCreateRequest;
import com.hola.rate.dto.request.RateCodeUpdateRequest;
import com.hola.rate.dto.request.RatePricingRequest;
import com.hola.rate.dto.response.RateCodeListResponse;
import com.hola.rate.dto.response.RateCodeResponse;
import com.hola.rate.dto.response.RatePricingResponse;

import java.util.List;

/**
 * 레이트 코드 서비스 인터페이스
 */
public interface RateCodeService {

    List<RateCodeListResponse> getRateCodes(Long propertyId);

    RateCodeResponse getRateCode(Long id);

    RateCodeResponse createRateCode(Long propertyId, RateCodeCreateRequest request);

    RateCodeResponse updateRateCode(Long id, RateCodeUpdateRequest request);

    void deleteRateCode(Long id);

    boolean existsRateCode(Long propertyId, String rateCode);

    /** 요금정보 조회 */
    RatePricingResponse getRatePricing(Long rateCodeId);

    /** 요금정보 저장 (전체 교체) */
    RatePricingResponse saveRatePricing(Long rateCodeId, RatePricingRequest request);

    /** 요금 행 삭제 */
    void deleteRatePricingRow(Long rateCodeId, Long pricingId);

    /** 옵션요금 매핑 조회 */
    List<Long> getOptionPricing(Long rateCodeId);

    /** 옵션요금 매핑 저장 (전체 교체) */
    List<Long> saveOptionPricing(Long rateCodeId, List<Long> paidServiceOptionIds);
}
