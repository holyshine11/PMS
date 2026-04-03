package com.hola.hotel.service;

import com.hola.hotel.entity.Workstation;

import java.util.List;

public interface WorkstationService {

    List<Workstation> findActiveByProperty(Long propertyId);

    Workstation findById(Long id);
}
