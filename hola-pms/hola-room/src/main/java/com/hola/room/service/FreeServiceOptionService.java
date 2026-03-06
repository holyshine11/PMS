package com.hola.room.service;

import com.hola.room.dto.request.FreeServiceOptionCreateRequest;
import com.hola.room.dto.request.FreeServiceOptionUpdateRequest;
import com.hola.room.dto.response.FreeServiceOptionResponse;

import java.util.List;

/**
 * 무료 서비스 옵션 서비스 인터페이스
 */
public interface FreeServiceOptionService {

    List<FreeServiceOptionResponse> getFreeServiceOptions(Long propertyId);

    FreeServiceOptionResponse getFreeServiceOption(Long id);

    FreeServiceOptionResponse createFreeServiceOption(Long propertyId, FreeServiceOptionCreateRequest request);

    FreeServiceOptionResponse updateFreeServiceOption(Long id, FreeServiceOptionUpdateRequest request);

    void deleteFreeServiceOption(Long id);

    boolean existsServiceOptionCode(Long propertyId, String serviceOptionCode);
}
