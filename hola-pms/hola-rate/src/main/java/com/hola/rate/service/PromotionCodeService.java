package com.hola.rate.service;

import com.hola.rate.dto.request.PromotionCodeCreateRequest;
import com.hola.rate.dto.request.PromotionCodeUpdateRequest;
import com.hola.rate.dto.response.PromotionCodeListResponse;
import com.hola.rate.dto.response.PromotionCodeResponse;
import java.util.List;

public interface PromotionCodeService {
    List<PromotionCodeListResponse> getPromotionCodes(Long propertyId);
    PromotionCodeResponse getPromotionCode(Long id);
    PromotionCodeResponse createPromotionCode(Long propertyId, PromotionCodeCreateRequest request);
    PromotionCodeResponse updatePromotionCode(Long id, PromotionCodeUpdateRequest request);
    void deletePromotionCode(Long id);
    boolean existsPromotionCode(Long propertyId, String promotionCode);
}
