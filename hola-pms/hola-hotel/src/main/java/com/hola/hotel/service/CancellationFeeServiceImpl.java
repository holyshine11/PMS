package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.CancellationFeeRequest;
import com.hola.hotel.dto.request.CancellationFeeSaveRequest;
import com.hola.hotel.dto.response.CancellationFeeResponse;
import com.hola.hotel.entity.CancellationFee;
import com.hola.hotel.entity.Property;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.CancellationFeeRepository;
import com.hola.hotel.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CancellationFeeServiceImpl implements CancellationFeeService {

    private final CancellationFeeRepository cancellationFeeRepository;
    private final PropertyRepository propertyRepository;
    private final HotelMapper hotelMapper;

    @Override
    public List<CancellationFeeResponse> getCancellationFees(Long propertyId) {
        return cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(propertyId).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<CancellationFeeResponse> saveCancellationFees(Long propertyId, CancellationFeeSaveRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        // 기존 데이터 전체 soft delete
        List<CancellationFee> existing = cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(propertyId);
        existing.forEach(CancellationFee::softDelete);

        // 새로 insert
        List<CancellationFeeRequest> fees = request.getFees();
        for (int i = 0; i < fees.size(); i++) {
            CancellationFeeRequest fee = fees.get(i);
            CancellationFee entity = CancellationFee.builder()
                    .property(property)
                    .checkinBasis(fee.getCheckinBasis())
                    .daysBefore(fee.getDaysBefore())
                    .feeAmount(fee.getFeeAmount())
                    .feeType(fee.getFeeType())
                    .build();
            entity.changeSortOrder(i);
            cancellationFeeRepository.save(entity);
        }

        log.info("취소 수수료 저장: propertyId={}, count={}", propertyId, fees.size());

        return cancellationFeeRepository.findAllByPropertyIdOrderBySortOrder(propertyId).stream()
                .map(hotelMapper::toResponse)
                .collect(Collectors.toList());
    }
}
