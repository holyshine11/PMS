package com.hola.hotel.service;

import com.hola.hotel.dto.request.HotelCreateRequest;
import com.hola.hotel.dto.request.HotelUpdateRequest;
import com.hola.hotel.dto.response.HotelResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HotelService {

    Page<HotelResponse> getHotels(String hotelName, Boolean useYn, Pageable pageable);

    HotelResponse getHotel(Long id);

    HotelResponse createHotel(HotelCreateRequest request);

    HotelResponse updateHotel(Long id, HotelUpdateRequest request);

    void deleteHotel(Long id);

    long getHotelCount();

    boolean checkHotelNameDuplicate(String hotelName);

    List<HotelResponse> getHotelsForSelector(String loginId);
}
