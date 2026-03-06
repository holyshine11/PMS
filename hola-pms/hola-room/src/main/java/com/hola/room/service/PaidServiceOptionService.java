package com.hola.room.service;

import com.hola.room.dto.request.PaidServiceOptionCreateRequest;
import com.hola.room.dto.request.PaidServiceOptionUpdateRequest;
import com.hola.room.dto.response.PaidServiceOptionResponse;

import java.util.List;

/**
 * 유료 서비스 옵션 서비스 인터페이스
 */
public interface PaidServiceOptionService {

    List<PaidServiceOptionResponse> getPaidServiceOptions(Long propertyId);

    PaidServiceOptionResponse getPaidServiceOption(Long id);

    PaidServiceOptionResponse createPaidServiceOption(Long propertyId, PaidServiceOptionCreateRequest request);

    PaidServiceOptionResponse updatePaidServiceOption(Long id, PaidServiceOptionUpdateRequest request);

    void deletePaidServiceOption(Long id);

    boolean existsServiceOptionCode(Long propertyId, String serviceOptionCode);
}
