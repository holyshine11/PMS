package com.hola.reservation.service;

import com.hola.rate.entity.RateCodePaidService;
import com.hola.rate.repository.RateCodePaidServiceRepository;
import com.hola.reservation.entity.ReservationServiceItem;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.ReservationServiceItemRepository;
import com.hola.room.entity.PaidServiceOption;
import com.hola.room.repository.PaidServiceOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 레이트코드 포함 서비스 자동 추가 헬퍼
 * - 레이트코드에 매핑된 유료서비스를 예약 서비스 항목(RATE_INCLUDED)으로 자동 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateIncludedServiceHelper {

    private final RateCodePaidServiceRepository rateCodePaidServiceRepository;
    private final PaidServiceOptionRepository paidServiceOptionRepository;
    private final ReservationServiceItemRepository serviceItemRepository;

    /**
     * 레이트코드에 매핑된 서비스를 서브예약에 자동 추가
     *
     * @param sub        서브예약 (이미 persist된 상태)
     * @param rateCodeId 레이트코드 ID
     */
    public void addRateIncludedServices(SubReservation sub, Long rateCodeId) {
        if (rateCodeId == null) return;

        // 1. 레이트코드에 매핑된 서비스 ID 목록 조회
        List<RateCodePaidService> mappings = rateCodePaidServiceRepository.findAllByRateCodeId(rateCodeId);
        if (mappings.isEmpty()) return;

        List<Long> serviceIds = mappings.stream()
                .map(RateCodePaidService::getPaidServiceOptionId)
                .collect(Collectors.toList());

        // 2. 서비스 상세 벌크 조회
        Map<Long, PaidServiceOption> serviceMap = paidServiceOptionRepository.findAllById(serviceIds).stream()
                .filter(s -> Boolean.TRUE.equals(s.getUseYn()))  // 활성 서비스만
                .collect(Collectors.toMap(PaidServiceOption::getId, Function.identity()));

        if (serviceMap.isEmpty()) return;

        // 3. applicableNights에 따라 ReservationServiceItem 생성
        for (Long serviceId : serviceIds) {
            PaidServiceOption option = serviceMap.get(serviceId);
            if (option == null) continue;

            String applicableNights = option.getApplicableNights();

            if ("ALL_NIGHTS".equals(applicableNights)) {
                // 매 숙박일마다 1행 (체크인일 ~ 체크아웃 전날)
                LocalDate date = sub.getCheckIn();
                while (date.isBefore(sub.getCheckOut())) {
                    createServiceItem(sub, option, date);
                    date = date.plusDays(1);
                }
            } else if ("FIRST_NIGHT_ONLY".equals(applicableNights)) {
                // 체크인일에만 1행
                createServiceItem(sub, option, sub.getCheckIn());
            } else {
                // NOT_APPLICABLE: serviceDate null로 1행
                createServiceItem(sub, option, null);
            }
        }

        log.info("레이트코드 포함 서비스 자동 추가: subReservationId={}, rateCodeId={}, 서비스 {}건",
                sub.getId(), rateCodeId, serviceMap.size());
    }

    /**
     * 서브예약의 기존 RATE_INCLUDED 서비스 삭제 후 새 레이트코드 기준 재생성
     *
     * @param sub           서브예약
     * @param newRateCodeId 새 레이트코드 ID
     */
    public void refreshRateIncludedServices(SubReservation sub, Long newRateCodeId) {
        // 기존 RATE_INCLUDED 삭제
        serviceItemRepository.deleteBySubReservationIdAndServiceType(sub.getId(), "RATE_INCLUDED");
        serviceItemRepository.flush();

        // 새 레이트코드 기준 재생성
        addRateIncludedServices(sub, newRateCodeId);
    }

    /**
     * 개별 서비스 항목 생성 (RATE_INCLUDED: 0원)
     */
    private void createServiceItem(SubReservation sub, PaidServiceOption option, LocalDate serviceDate) {
        ReservationServiceItem item = ReservationServiceItem.builder()
                .subReservation(sub)
                .serviceType("RATE_INCLUDED")
                .serviceOptionId(option.getId())
                .serviceDate(serviceDate)
                .quantity(1)
                .unitPrice(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .build();
        serviceItemRepository.save(item);
    }
}
