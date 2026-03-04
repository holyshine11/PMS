package com.hola.hotel.service;

import com.hola.hotel.dto.request.PropertySettlementRequest;
import com.hola.hotel.dto.response.PropertySettlementResponse;

import java.util.List;

public interface PropertySettlementService {

    List<PropertySettlementResponse> getSettlements(Long propertyId);

    PropertySettlementResponse saveSettlement(Long propertyId, PropertySettlementRequest request);

    void deleteSettlement(Long propertyId, String countryType);
}
