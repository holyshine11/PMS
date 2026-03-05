package com.hola.common.auth.service;

import com.hola.common.auth.dto.BluewaveAdminCreateRequest;
import com.hola.common.auth.dto.BluewaveAdminListResponse;
import com.hola.common.auth.dto.BluewaveAdminResponse;
import com.hola.common.auth.dto.BluewaveAdminUpdateRequest;

import java.util.List;

public interface BluewaveAdminService {

    List<BluewaveAdminListResponse> getList(String loginId, String userName, Boolean useYn);

    BluewaveAdminResponse getDetail(Long id);

    BluewaveAdminResponse create(BluewaveAdminCreateRequest request);

    BluewaveAdminResponse update(Long id, BluewaveAdminUpdateRequest request);

    void delete(Long id);

    boolean checkLoginId(String loginId);

    void resetPassword(Long id);
}
