package com.hola.reservation.service;

import com.hola.reservation.dto.response.FrontDeskOperationResponse;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.ReservationPayment;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.ReservationPaymentRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.RoomTypeRepository;
import com.hola.hotel.entity.ReservationChannel;
import com.hola.hotel.repository.ReservationChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 프론트데스크 운영현황 서비스 구현
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontDeskServiceImpl implements FrontDeskService {

    private final SubReservationRepository subReservationRepository;
    private final ReservationPaymentRepository paymentRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final ReservationChannelRepository reservationChannelRepository;

    @Override
    public List<FrontDeskOperationResponse> getArrivals(Long propertyId) {
        LocalDate today = LocalDate.now();
        List<SubReservation> subs = subReservationRepository.findArrivals(propertyId, today);
        return toResponseList(subs);
    }

    @Override
    public List<FrontDeskOperationResponse> getInHouse(Long propertyId) {
        LocalDate today = LocalDate.now();
        List<SubReservation> subs = subReservationRepository.findInHouse(propertyId, today);
        return toResponseList(subs);
    }

    @Override
    public List<FrontDeskOperationResponse> getDepartures(Long propertyId) {
        LocalDate today = LocalDate.now();
        List<SubReservation> subs = subReservationRepository.findDepartures(propertyId, today);
        return toResponseList(subs);
    }

    @Override
    public List<FrontDeskOperationResponse> getAllOperations(Long propertyId) {
        LocalDate today = LocalDate.now();
        List<SubReservation> subs = subReservationRepository.findAllOperations(propertyId, today);
        return toResponseList(subs);
    }

    @Override
    public Map<String, Long> getSummary(Long propertyId) {
        LocalDate today = LocalDate.now();
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("arrivals", subReservationRepository.countArrivals(propertyId, today));
        summary.put("inHouse", subReservationRepository.countInHouse(propertyId, today));
        summary.put("departures", subReservationRepository.countDepartures(propertyId, today));
        return summary;
    }

    /**
     * SubReservation 리스트를 DTO 리스트로 변환 (N+1 방지: 벌크 조회)
     */
    private List<FrontDeskOperationResponse> toResponseList(List<SubReservation> subs) {
        if (subs.isEmpty()) {
            return Collections.emptyList();
        }

        // 벌크 조회: 객실타입
        Set<Long> roomTypeIds = subs.stream()
                .map(SubReservation::getRoomTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, RoomType> roomTypeMap = roomTypeIds.isEmpty() ? Collections.emptyMap() :
                roomTypeRepository.findAllById(roomTypeIds).stream()
                        .collect(Collectors.toMap(RoomType::getId, Function.identity()));

        // 벌크 조회: 객실번호
        Set<Long> roomNumberIds = subs.stream()
                .map(SubReservation::getRoomNumberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, RoomNumber> roomNumberMap = roomNumberIds.isEmpty() ? Collections.emptyMap() :
                roomNumberRepository.findAllById(roomNumberIds).stream()
                        .collect(Collectors.toMap(RoomNumber::getId, Function.identity()));

        // 벌크 조회: 결제 정보 (N+1 방지)
        Set<Long> masterIds = subs.stream()
                .map(s -> s.getMasterReservation().getId())
                .collect(Collectors.toSet());
        Map<Long, ReservationPayment> paymentMap = masterIds.isEmpty() ? Collections.emptyMap() :
                paymentRepository.findAllByMasterReservationIdIn(masterIds).stream()
                        .collect(Collectors.toMap(
                                p -> p.getMasterReservation().getId(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        // 벌크 조회: 예약 채널
        Set<Long> channelIds = subs.stream()
                .map(s -> s.getMasterReservation().getReservationChannelId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ReservationChannel> channelMap = channelIds.isEmpty() ? Collections.emptyMap() :
                reservationChannelRepository.findAllById(channelIds).stream()
                        .collect(Collectors.toMap(ReservationChannel::getId, Function.identity()));

        return subs.stream().map(sub -> {
            MasterReservation master = sub.getMasterReservation();
            RoomType roomType = roomTypeMap.get(sub.getRoomTypeId());
            RoomNumber roomNumber = roomNumberMap.get(sub.getRoomNumberId());
            ReservationPayment payment = paymentMap.get(master.getId());
            ReservationChannel channel = master.getReservationChannelId() != null
                    ? channelMap.get(master.getReservationChannelId()) : null;

            return FrontDeskOperationResponse.builder()
                    .reservationId(master.getId())
                    .subReservationId(sub.getId())
                    .subReservationNo(sub.getSubReservationNo())
                    .masterReservationNo(master.getMasterReservationNo())
                    .confirmationNo(master.getConfirmationNo())
                    .reservationStatus(master.getReservationStatus())
                    .roomReservationStatus(sub.getRoomReservationStatus())
                    .guestNameKo(master.getGuestNameKo())
                    .guestLastNameEn(master.getGuestLastNameEn())
                    .phoneNumber(master.getPhoneNumber())
                    .email(master.getEmail())
                    .roomTypeName(roomType != null ? roomType.getRoomTypeCode() : null)
                    .roomNumber(roomNumber != null ? roomNumber.getRoomNumber() : null)
                    .roomNumberId(sub.getRoomNumberId())
                    .adults(sub.getAdults())
                    .children(sub.getChildren())
                    .checkIn(sub.getCheckIn())
                    .checkOut(sub.getCheckOut())
                    .nights((int) ChronoUnit.DAYS.between(sub.getCheckIn(), sub.getCheckOut()))
                    .eta(sub.getEta())
                    .etd(sub.getEtd())
                    .actualCheckInTime(sub.getActualCheckInTime())
                    .actualCheckOutTime(sub.getActualCheckOutTime())
                    .paymentStatus(payment != null ? payment.getPaymentStatus() : null)
                    .reservationChannelName(channel != null ? channel.getChannelName() : null)
                    .isOtaManaged(master.getIsOtaManaged())
                    .build();
        }).collect(Collectors.toList());
    }
}
