package com.hola.hotel.service;

import com.hola.hotel.dto.request.PropertyAdminCreateRequest;
import com.hola.hotel.dto.request.PropertyAdminUpdateRequest;
import com.hola.hotel.dto.response.PropertyAdminListResponse;
import com.hola.hotel.dto.response.PropertyAdminResponse;

import java.util.List;

public interface PropertyAdminService {

    List<PropertyAdminListResponse> getList(Long propertyId, String loginId, String userName, Boolean useYn);

    PropertyAdminResponse getDetail(Long propertyId, Long id);

    PropertyAdminResponse create(Long propertyId, PropertyAdminCreateRequest request);

    PropertyAdminResponse update(Long propertyId, Long id, PropertyAdminUpdateRequest request);

    void delete(Long propertyId, Long id);

    boolean checkLoginId(String loginId);

    void resetPassword(Long propertyId, Long id);
}
