package com.hola.hotel.service;

import com.hola.hotel.dto.request.ReservationChannelRequest;
import com.hola.hotel.dto.response.ReservationChannelResponse;

import java.util.List;

public interface ReservationChannelService {
    List<ReservationChannelResponse> getList(Long propertyId);
    ReservationChannelResponse getById(Long id);
    ReservationChannelResponse create(Long propertyId, ReservationChannelRequest request);
    ReservationChannelResponse update(Long id, ReservationChannelRequest request);
    void delete(Long id);
    boolean checkCode(Long propertyId, String channelCode);
}
