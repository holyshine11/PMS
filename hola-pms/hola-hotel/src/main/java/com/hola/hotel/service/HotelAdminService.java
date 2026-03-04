package com.hola.hotel.service;

import com.hola.hotel.dto.request.HotelAdminCreateRequest;
import com.hola.hotel.dto.request.HotelAdminUpdateRequest;
import com.hola.hotel.dto.response.HotelAdminListResponse;
import com.hola.hotel.dto.response.HotelAdminResponse;

import java.util.List;

public interface HotelAdminService {

    List<HotelAdminListResponse> getList(Long hotelId, String loginId, String userName, Boolean useYn);

    HotelAdminResponse getDetail(Long hotelId, Long id);

    HotelAdminResponse create(Long hotelId, HotelAdminCreateRequest request);

    HotelAdminResponse update(Long hotelId, Long id, HotelAdminUpdateRequest request);

    void delete(Long hotelId, Long id);

    boolean checkLoginId(String loginId);

    void resetPassword(Long hotelId, Long id);
}
