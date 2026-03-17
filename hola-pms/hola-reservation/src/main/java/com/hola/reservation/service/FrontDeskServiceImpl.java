package com.hola.reservation.service;

import com.hola.common.util.NameMaskingUtil;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.ReservationChannelRepository;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.reservation.dto.response.FrontDeskArrivalResponse;
import com.hola.reservation.dto.response.FrontDeskDepartureResponse;
import com.hola.reservation.dto.response.FrontDeskInHouseResponse;
import com.hola.reservation.entity.DailyCharge;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.PaymentTransaction;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.PaymentTransactionRepository;
import com.hola.reservation.repository.SubReservationRepository;
import com.hola.room.entity.RoomType;
import com.hola.room.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 프론트데스크 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontDeskServiceImpl implements FrontDeskService {

    private final SubReservationRepository subReservationRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final ReservationChannelRepository reservationChannelRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    public List<FrontDeskArrivalResponse> getArrivals(Long propertyId) {
        LocalDate today = LocalDate.now();
        List<SubReservation> subs = subReservationRepository.findArrivals(propertyId, today);

        // 벌크 이름 해석
        Map<Long, String> roomTypeNames = resolveRoomTypeNames(propertyId);
        Map<Long, RoomNumber> roomNumbers = resolveRoomNumbers(subs);
        Map<Long, String> channelNames = resolveChannelNames(propertyId);

        List<FrontDeskArrivalResponse> result = new ArrayList<>();
        for (SubReservation sub : subs) {
            MasterReservation master = sub.getMasterReservation();
            RoomNumber room = roomNumbers.get(sub.getRoomNumberId());
            BigDecimal totalAmount = calcTotalCharge(sub);

            result.add(FrontDeskArrivalResponse.builder()
                    .masterReservationId(master.getId())
                    .subReservationId(sub.getId())
                    .masterReservationNo(master.getMasterReservationNo())
                    .confirmationNo(master.getConfirmationNo())
                    .guestNameKo(NameMaskingUtil.maskKoreanName(master.getGuestNameKo()))
                    .phoneNumber(master.getPhoneNumber())
                    .roomTypeName(roomTypeNames.getOrDefault(sub.getRoomTypeId(), "-"))
                    .roomNumber(room != null ? room.getRoomNumber() : null)
                    .roomNumberId(sub.getRoomNumberId())
                    .roomTypeId(sub.getRoomTypeId())
                    .checkIn(sub.getCheckIn())
                    .checkOut(sub.getCheckOut())
                    .adults(sub.getAdults())
                    .children(sub.getChildren())
                    .reservationStatus(sub.getRoomReservationStatus())
                    .channelName(channelNames.getOrDefault(master.getReservationChannelId(), "-"))
                    .totalAmount(totalAmount)
                    .hkStatus(room != null ? room.getHkStatus() : null)
                    .build());
        }
        return result;
    }

    @Override
    public List<FrontDeskInHouseResponse> getInHouse(Long propertyId) {
        LocalDate today = LocalDate.now();
        List<SubReservation> subs = subReservationRepository.findInHouse(propertyId, today);

        Map<Long, String> roomTypeNames = resolveRoomTypeNames(propertyId);
        Map<Long, RoomNumber> roomNumbers = resolveRoomNumbers(subs);

        // 벌크 결제 금액 조회 (N+1 방지)
        List<Long> masterIds = subs.stream().map(s -> s.getMasterReservation().getId()).distinct().collect(Collectors.toList());
        Map<Long, BigDecimal> paidMap = calcPaidAmountBulk(masterIds);

        List<FrontDeskInHouseResponse> result = new ArrayList<>();
        for (SubReservation sub : subs) {
            MasterReservation master = sub.getMasterReservation();
            RoomNumber room = roomNumbers.get(sub.getRoomNumberId());
            BigDecimal totalAmount = calcTotalCharge(sub);
            BigDecimal paidAmount = paidMap.getOrDefault(master.getId(), BigDecimal.ZERO);
            BigDecimal balance = totalAmount.subtract(paidAmount);

            result.add(FrontDeskInHouseResponse.builder()
                    .masterReservationId(master.getId())
                    .subReservationId(sub.getId())
                    .masterReservationNo(master.getMasterReservationNo())
                    .confirmationNo(master.getConfirmationNo())
                    .guestNameKo(NameMaskingUtil.maskKoreanName(master.getGuestNameKo()))
                    .phoneNumber(master.getPhoneNumber())
                    .roomTypeName(roomTypeNames.getOrDefault(sub.getRoomTypeId(), "-"))
                    .roomNumber(room != null ? room.getRoomNumber() : null)
                    .checkIn(sub.getCheckIn())
                    .checkOut(sub.getCheckOut())
                    .actualCheckInTime(sub.getActualCheckInTime())
                    .adults(sub.getAdults())
                    .children(sub.getChildren())
                    .totalAmount(totalAmount)
                    .paidAmount(paidAmount)
                    .balance(balance)
                    .build());
        }
        return result;
    }

    @Override
    public List<FrontDeskDepartureResponse> getDepartures(Long propertyId) {
        LocalDate today = LocalDate.now();
        List<SubReservation> subs = subReservationRepository.findDepartures(propertyId, today);

        Map<Long, String> roomTypeNames = resolveRoomTypeNames(propertyId);
        Map<Long, RoomNumber> roomNumbers = resolveRoomNumbers(subs);

        // 벌크 결제 금액 조회 (N+1 방지)
        List<Long> masterIds = subs.stream().map(s -> s.getMasterReservation().getId()).distinct().collect(Collectors.toList());
        Map<Long, BigDecimal> paidMap = calcPaidAmountBulk(masterIds);

        List<FrontDeskDepartureResponse> result = new ArrayList<>();
        for (SubReservation sub : subs) {
            MasterReservation master = sub.getMasterReservation();
            RoomNumber room = roomNumbers.get(sub.getRoomNumberId());
            BigDecimal totalAmount = calcTotalCharge(sub);
            BigDecimal paidAmount = paidMap.getOrDefault(master.getId(), BigDecimal.ZERO);
            BigDecimal balance = totalAmount.subtract(paidAmount);

            result.add(FrontDeskDepartureResponse.builder()
                    .masterReservationId(master.getId())
                    .subReservationId(sub.getId())
                    .masterReservationNo(master.getMasterReservationNo())
                    .confirmationNo(master.getConfirmationNo())
                    .guestNameKo(NameMaskingUtil.maskKoreanName(master.getGuestNameKo()))
                    .phoneNumber(master.getPhoneNumber())
                    .roomTypeName(roomTypeNames.getOrDefault(sub.getRoomTypeId(), "-"))
                    .roomNumber(room != null ? room.getRoomNumber() : null)
                    .checkIn(sub.getCheckIn())
                    .checkOut(sub.getCheckOut())
                    .actualCheckInTime(sub.getActualCheckInTime())
                    .adults(sub.getAdults())
                    .children(sub.getChildren())
                    .totalAmount(totalAmount)
                    .paidAmount(paidAmount)
                    .balance(balance)
                    .lateCheckOut(sub.getLateCheckOut())
                    .build());
        }
        return result;
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

    // === 헬퍼 메서드 ===

    private Map<Long, String> resolveRoomTypeNames(Long propertyId) {
        return roomTypeRepository.findAllByPropertyIdOrderBySortOrderAscRoomTypeCodeAsc(propertyId)
                .stream().collect(Collectors.toMap(RoomType::getId, RoomType::getRoomTypeCode, (a, b) -> a));
    }

    private Map<Long, RoomNumber> resolveRoomNumbers(List<SubReservation> subs) {
        Set<Long> roomIds = subs.stream()
                .map(SubReservation::getRoomNumberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roomIds.isEmpty()) return Collections.emptyMap();
        return roomNumberRepository.findAllById(roomIds)
                .stream().collect(Collectors.toMap(RoomNumber::getId, Function.identity()));
    }

    private Map<Long, String> resolveChannelNames(Long propertyId) {
        return reservationChannelRepository.findByPropertyIdOrderBySortOrderAsc(propertyId)
                .stream().collect(Collectors.toMap(ch -> ch.getId(), ch -> ch.getChannelName(), (a, b) -> a));
    }

    private BigDecimal calcTotalCharge(SubReservation sub) {
        return sub.getDailyCharges().stream()
                .map(DailyCharge::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcPaidAmount(Long masterReservationId) {
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByMasterReservationIdOrderByTransactionSeqAsc(masterReservationId);
        return sumPaidFromTransactions(transactions);
    }

    /**
     * 벌크 결제 금액 조회: 여러 마스터 예약 ID에 대해 한 번에 쿼리
     */
    private Map<Long, BigDecimal> calcPaidAmountBulk(List<Long> masterIds) {
        if (masterIds.isEmpty()) return Collections.emptyMap();
        List<PaymentTransaction> allTx = paymentTransactionRepository.findByMasterReservationIdIn(masterIds);
        // masterReservationId별 그룹핑
        Map<Long, List<PaymentTransaction>> grouped = allTx.stream()
                .collect(Collectors.groupingBy(PaymentTransaction::getMasterReservationId));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Long id : masterIds) {
            result.put(id, sumPaidFromTransactions(grouped.getOrDefault(id, Collections.emptyList())));
        }
        return result;
    }

    private BigDecimal sumPaidFromTransactions(List<PaymentTransaction> transactions) {
        BigDecimal paid = BigDecimal.ZERO;
        for (PaymentTransaction tx : transactions) {
            if ("COMPLETED".equals(tx.getTransactionStatus())) {
                if ("PAYMENT".equals(tx.getTransactionType())) {
                    paid = paid.add(tx.getAmount());
                } else if ("REFUND".equals(tx.getTransactionType())) {
                    paid = paid.subtract(tx.getAmount());
                }
            }
        }
        return paid;
    }
}
