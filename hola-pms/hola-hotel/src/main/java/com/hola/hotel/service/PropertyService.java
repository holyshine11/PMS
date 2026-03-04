package com.hola.hotel.service;

import com.hola.hotel.dto.request.PropertyCreateRequest;
import com.hola.hotel.dto.request.PropertyTaxServiceChargeRequest;
import com.hola.hotel.dto.request.PropertyUpdateRequest;
import com.hola.hotel.dto.response.PropertyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PropertyService {

    List<PropertyResponse> getProperties(Long hotelId);

    Page<PropertyResponse> getProperties(Long hotelId, Pageable pageable);

    PropertyResponse getProperty(Long id);

    PropertyResponse createProperty(Long hotelId, PropertyCreateRequest request);

    PropertyResponse updateProperty(Long id, PropertyUpdateRequest request);

    void deleteProperty(Long id);

    long getPropertyCount();

    List<PropertyResponse> getPropertiesForSelector(Long hotelId);

    boolean existsPropertyName(Long hotelId, String propertyName);

    PropertyResponse updateTaxServiceCharge(Long id, PropertyTaxServiceChargeRequest request);
}
