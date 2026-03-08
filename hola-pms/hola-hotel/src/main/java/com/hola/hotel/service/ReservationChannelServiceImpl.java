package com.hola.hotel.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import com.hola.hotel.dto.request.ReservationChannelRequest;
import com.hola.hotel.dto.response.ReservationChannelResponse;
import com.hola.hotel.entity.Property;
import com.hola.hotel.entity.ReservationChannel;
import com.hola.hotel.mapper.HotelMapper;
import com.hola.hotel.repository.PropertyRepository;
import com.hola.hotel.repository.ReservationChannelRepository;
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
public class ReservationChannelServiceImpl implements ReservationChannelService {

    private final ReservationChannelRepository reservationChannelRepository;
    private final PropertyRepository propertyRepository;
    private final HotelMapper hotelMapper;

    @Override
    public List<ReservationChannelResponse> getList(Long propertyId) {
        return reservationChannelRepository.findByPropertyIdOrderBySortOrderAsc(propertyId).stream()
                .map(hotelMapper::toReservationChannelResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReservationChannelResponse getById(Long id) {
        return hotelMapper.toReservationChannelResponse(findById(id));
    }

    @Override
    @Transactional
    public ReservationChannelResponse create(Long propertyId, ReservationChannelRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new HolaException(ErrorCode.PROPERTY_NOT_FOUND));

        if (reservationChannelRepository.existsByPropertyIdAndChannelCode(propertyId, request.getChannelCode())) {
            throw new HolaException(ErrorCode.RESERVATION_CHANNEL_CODE_DUPLICATE);
        }

        ReservationChannel channel = ReservationChannel.builder()
                .property(property)
                .channelCode(request.getChannelCode())
                .channelName(request.getChannelName())
                .channelType(request.getChannelType())
                .descriptionKo(request.getDescriptionKo())
                .descriptionEn(request.getDescriptionEn())
                .build();

        if (request.getUseYn() != null && !request.getUseYn()) {
            channel.deactivate();
        }
        if (request.getSortOrder() != null) channel.changeSortOrder(request.getSortOrder());

        ReservationChannel saved = reservationChannelRepository.save(channel);
        log.info("예약채널 생성: {} ({}) - 프로퍼티: {}", saved.getChannelName(), saved.getChannelCode(), property.getPropertyName());
        return hotelMapper.toReservationChannelResponse(saved);
    }

    @Override
    @Transactional
    public ReservationChannelResponse update(Long id, ReservationChannelRequest request) {
        ReservationChannel channel = findById(id);
        channel.update(request.getChannelName(), request.getChannelType(),
                       request.getDescriptionKo(), request.getDescriptionEn());

        if (request.getUseYn() != null) {
            if (request.getUseYn()) {
                channel.activate();
            } else {
                channel.deactivate();
            }
        }
        if (request.getSortOrder() != null) channel.changeSortOrder(request.getSortOrder());
        return hotelMapper.toReservationChannelResponse(channel);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ReservationChannel channel = findById(id);
        channel.softDelete();
        log.info("예약채널 삭제: {} ({})", channel.getChannelName(), channel.getChannelCode());
    }

    @Override
    public boolean checkCode(Long propertyId, String channelCode) {
        return reservationChannelRepository.existsByPropertyIdAndChannelCode(propertyId, channelCode);
    }

    private ReservationChannel findById(Long id) {
        return reservationChannelRepository.findById(id)
                .orElseThrow(() -> new HolaException(ErrorCode.RESERVATION_CHANNEL_NOT_FOUND));
    }
}
