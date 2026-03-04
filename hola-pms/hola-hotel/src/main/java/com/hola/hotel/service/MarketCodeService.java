package com.hola.hotel.service;

import com.hola.hotel.dto.request.MarketCodeRequest;
import com.hola.hotel.dto.response.MarketCodeResponse;

import java.util.List;

public interface MarketCodeService {

    List<MarketCodeResponse> getMarketCodes(Long propertyId);

    MarketCodeResponse getMarketCode(Long id);

    MarketCodeResponse createMarketCode(Long propertyId, MarketCodeRequest request);

    MarketCodeResponse updateMarketCode(Long id, MarketCodeRequest request);

    void deleteMarketCode(Long id);

    /** 마켓코드명 중복 확인 */
    boolean existsMarketCode(Long propertyId, String marketCode);
}
