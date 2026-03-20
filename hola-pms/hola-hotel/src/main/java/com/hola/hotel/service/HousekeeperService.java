package com.hola.hotel.service;

import com.hola.hotel.dto.request.HousekeeperCreateRequest;
import com.hola.hotel.dto.request.HousekeeperUpdateRequest;
import com.hola.hotel.dto.response.HousekeeperResponse;

import java.util.List;

/**
 * 하우스키퍼 담당자 관리 서비스
 */
public interface HousekeeperService {

    List<HousekeeperResponse> getList(Long propertyId);

    HousekeeperResponse getDetail(Long propertyId, Long id);

    HousekeeperResponse create(Long propertyId, HousekeeperCreateRequest request);

    HousekeeperResponse update(Long propertyId, Long id, HousekeeperUpdateRequest request);

    void delete(Long propertyId, Long id);

    void resetPassword(Long propertyId, Long id);

    /** 비밀번호 변경 (현재 비밀번호 검증 후 변경) */
    void changePassword(Long propertyId, Long id, String newPassword);

    boolean checkLoginIdAvailable(String loginId);
}
