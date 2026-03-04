package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.PropertySettlementRequest;
import com.hola.hotel.dto.response.PropertySettlementResponse;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.PropertySettlement;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.PropertySettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertySettlementServiceImpl implements PropertySettlementService {

    private final PropertySettlementRepository settlementRepository;
    private final PropertyRepository propertyRepository;
    private final HotelMapper hotelMapper;

    @Override
    public List<PropertySettlementResponse> getSettlements(Long propertyId) {
        return settlementRepository.findAllByPropertyId(propertyId).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PropertySettlementResponse saveSettlement(Long propertyId, PropertySettlementRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        // Upsert: 기존 데이터 있으면 update, 없으면 create
        Optional<PropertySettlement> existing =
                settlementRepository.findByPropertyIdAndCountryType(propertyId, request.getCountryType());

        PropertySettlement settlement;
        if (existing.isPresent()) {
            settlement = existing.get();
            settlement.update(
                    request.getAccountNumber(), request.getBankName(), request.getBankCode(),
                    request.getAccountHolder(), request.getRoutingNumber(), request.getSwiftCode(),
                    request.getSettlementDay(), request.getBankBookPath()
            );
            log.info("정산정보 수정: propertyId={}, countryType={}", propertyId, request.getCountryType());
        } else {
            settlement = PropertySettlement.builder()
                    .property(property)
                    .countryType(request.getCountryType())
                    .accountNumber(request.getAccountNumber())
                    .bankName(request.getBankName())
                    .bankCode(request.getBankCode())
                    .accountHolder(request.getAccountHolder())
                    .routingNumber(request.getRoutingNumber())
                    .swiftCode(request.getSwiftCode())
                    .settlementDay(request.getSettlementDay())
                    .bankBookPath(request.getBankBookPath())
                    .build();
            settlement = settlementRepository.save(settlement);
            log.info("정산정보 생성: propertyId={}, countryType={}", propertyId, request.getCountryType());
        }

        return hotelMapper.toResponse(settlement);
    }

    @Override
    @Transactional
    public void deleteSettlement(Long propertyId, String countryType) {
        PropertySettlement settlement = settlementRepository
                .findByPropertyIdAndCountryType(propertyId, countryType)
                .orElseThrow(() -> new HolaException(ErrorCode.SETTLEMENT_NOT_FOUND));

        settlement.softDelete();
        log.info("정산정보 삭제: propertyId={}, countryType={}", propertyId, countryType);
    }
}
