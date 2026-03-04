package com.hola.hotel.service;

import com.hola.hotel.dto.request.CancellationFeeSaveRequest;
import com.hola.hotel.dto.response.CancellationFeeResponse;

import java.util.List;

public interface CancellationFeeService {

    List<CancellationFeeResponse> getCancellationFees(Long propertyId);

    List<CancellationFeeResponse> saveCancellationFees(Long propertyId, CancellationFeeSaveRequest request);
}
