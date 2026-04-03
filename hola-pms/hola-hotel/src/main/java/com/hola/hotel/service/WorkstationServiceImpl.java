package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.entity.Workstation;
import com.hola.hotel.repository.WorkstationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkstationServiceImpl implements WorkstationService {

    private final WorkstationRepository workstationRepository;

    @Override
    public List<Workstation> findActiveByProperty(Long propertyId) {
        return workstationRepository.findActiveByPropertyId(propertyId);
    }

    @Override
    public Workstation findById(Long id) {
        return workstationRepository.findByIdAndUseYnTrue(id)
                .orElseThrow(() -> new HolaException(ErrorCode.WORKSTATION_NOT_FOUND));
    }
}
