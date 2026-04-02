package com.hola.reservation.service;

import com.hola.common.util.NameMaskingUtil;
import com.hola.hotel.entity.RoomNumber;
import com.hola.hotel.repository.RoomNumberRepository;
import com.hola.reservation.dto.response.ReservationCalendarResponse;
import com.hola.reservation.dto.response.ReservationTimelineResponse;
import com.hola.reservation.entity.MasterReservation;
import com.hola.reservation.entity.SubReservation;
import com.hola.reservation.repository.MasterReservationRepository;
import com.hola.room.entity.RoomTypeFloor;
import com.hola.room.repository.RoomTypeFloorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 예약 뷰 서비스 구현 — 캘린더/타임라인 뷰 데이터
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationViewServiceImpl implements ReservationViewService {

    private final ReservationFinder finder;
    private final MasterReservationRepository masterReservationRepository;
    private final RoomNumberRepository roomNumberRepository;
    private final RoomTypeFloorRepository roomTypeFloorRepository;
    private final RoomInfoResolver roomInfoResolver;

    @Override
    public Map<String, List<ReservationCalendarResponse>> getCalendarData(
            Long propertyId, LocalDate startDate, LocalDate endDate,
            String status, String keyword) {

        List<MasterReservation> reservations = masterReservationRepository
                .findByPropertyIdAndDateRangeWithSubs(propertyId, startDate, endDate);

        List<MasterReservation> filtered = reservations.stream()
                .filter(r -> status == null || status.isEmpty() || status.equals(r.getReservationStatus()))
                .filter(r -> finder.filterByKeyword(r, keyword))
                .collect(Collectors.toList());

        // Floor/RoomNumber/RoomType ID 수집 → 벌크 조회 (N+1 방지)
        Set<Long> floorIds = new HashSet<>();
        Set<Long> roomNumberIds = new HashSet<>();
        Set<Long> roomTypeIds = new HashSet<>();

        for (MasterReservation m : filtered) {
            for (SubReservation sub : m.getSubReservations()) {
                if (sub.getFloorId() != null) floorIds.add(sub.getFloorId());
                if (sub.getRoomNumberId() != null) roomNumberIds.add(sub.getRoomNumberId());
                if (sub.getRoomTypeId() != null) roomTypeIds.add(sub.getRoomTypeId());
            }
        }

        Map<Long, String> floorMap = roomInfoResolver.resolveFloorNumbers(floorIds);
        Map<Long, String> roomNumberMap = roomInfoResolver.resolveRoomNumbers(roomNumberIds);
        Map<Long, String> roomTypeMap = roomInfoResolver.resolveRoomTypeCodes(roomTypeIds);

        // 날짜별 그룹핑
        Map<String, List<ReservationCalendarResponse>> result = new LinkedHashMap<>();

        for (MasterReservation m : filtered) {
            ReservationCalendarResponse dto = toCalendarResponse(m, floorMap, roomNumberMap, roomTypeMap);

            LocalDate effectiveCheckOut = "DAY_USE".equals(dto.getStayType())
                    ? m.getMasterCheckIn() : m.getMasterCheckOut();

            LocalDate cursor = m.getMasterCheckIn().isBefore(startDate) ? startDate : m.getMasterCheckIn();
            LocalDate until = effectiveCheckOut.isAfter(endDate) ? endDate : effectiveCheckOut;

            while (!cursor.isAfter(until)) {
                String dateKey = cursor.toString();
                result.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(dto);
                cursor = cursor.plusDays(1);
            }
        }

        return result;
    }

    @Override
    public ReservationTimelineResponse getTimelineData(
            Long propertyId, LocalDate startDate, LocalDate endDate,
            String status, String keyword) {

        List<MasterReservation> reservations = masterReservationRepository
                .findByPropertyIdAndDateRangeWithSubs(propertyId, startDate, endDate);

        List<MasterReservation> filtered = reservations.stream()
                .filter(r -> status == null || status.isEmpty() || status.equals(r.getReservationStatus()))
                .filter(r -> finder.filterByKeyword(r, keyword))
                .collect(Collectors.toList());

        // 프로퍼티 전체 객실 매핑 정보 조회
        List<RoomTypeFloor> allMappings = roomTypeFloorRepository.findAllByPropertyId(propertyId);

        Map<Long, RoomTypeFloor> roomMappingMap = new LinkedHashMap<>();
        for (RoomTypeFloor rtf : allMappings) {
            roomMappingMap.putIfAbsent(rtf.getRoomNumberId(), rtf);
        }

        // 벌크 ID 조회
        Set<Long> allFloorIds = allMappings.stream()
                .map(RoomTypeFloor::getFloorId).collect(Collectors.toSet());
        Set<Long> allRoomTypeIds = allMappings.stream()
                .map(RoomTypeFloor::getRoomTypeId).collect(Collectors.toSet());
        Set<Long> allRoomNumberIds = roomMappingMap.keySet();

        Map<Long, String> floorMap = new HashMap<>(roomInfoResolver.resolveFloorNumbers(allFloorIds));

        Map<Long, RoomNumber> roomNumberEntityMap = allRoomNumberIds.isEmpty() ? Map.of()
                : roomNumberRepository.findAllById(allRoomNumberIds).stream()
                    .collect(Collectors.toMap(RoomNumber::getId, Function.identity()));

        Map<Long, String> roomTypeMap = new HashMap<>(roomInfoResolver.resolveRoomTypeCodes(allRoomTypeIds));

        // 예약을 roomNumberId 기준 그룹핑
        Map<Long, List<MasterReservation>> roomReservationMap = new LinkedHashMap<>();
        List<MasterReservation> unassignedList = new ArrayList<>();

        for (MasterReservation m : filtered) {
            Long assignedRoomId = getAssignedRoomNumberId(m);
            if (assignedRoomId != null) {
                roomReservationMap.computeIfAbsent(assignedRoomId, k -> new ArrayList<>()).add(m);
            } else {
                unassignedList.add(m);
            }
        }

        // 서브예약 기반 추가 ID 보강
        Set<Long> subFloorIds = new HashSet<>(allFloorIds);
        Set<Long> subRoomNumberIds = new HashSet<>(allRoomNumberIds);
        Set<Long> subRoomTypeIds = new HashSet<>(allRoomTypeIds);
        for (MasterReservation m : filtered) {
            for (SubReservation sub : m.getSubReservations()) {
                if (sub.getFloorId() != null) subFloorIds.add(sub.getFloorId());
                if (sub.getRoomNumberId() != null) subRoomNumberIds.add(sub.getRoomNumberId());
                if (sub.getRoomTypeId() != null) subRoomTypeIds.add(sub.getRoomTypeId());
            }
        }
        if (subFloorIds.size() > allFloorIds.size()) {
            Set<Long> extraFloorIds = new HashSet<>(subFloorIds);
            extraFloorIds.removeAll(allFloorIds);
            floorMap.putAll(roomInfoResolver.resolveFloorNumbers(extraFloorIds));
        }
        Map<Long, String> subRoomNumberMap = roomInfoResolver.resolveRoomNumbers(subRoomNumberIds);
        if (subRoomTypeIds.size() > allRoomTypeIds.size()) {
            Set<Long> extraTypeIds = new HashSet<>(subRoomTypeIds);
            extraTypeIds.removeAll(allRoomTypeIds);
            roomTypeMap.putAll(roomInfoResolver.resolveRoomTypeCodes(extraTypeIds));
        }

        // TimelineRoom 목록 생성
        List<ReservationTimelineResponse.TimelineRoom> timelineRooms = new ArrayList<>();

        for (Map.Entry<Long, RoomTypeFloor> entry : roomMappingMap.entrySet()) {
            Long roomId = entry.getKey();
            RoomTypeFloor rtf = entry.getValue();

            RoomNumber rn = roomNumberEntityMap.get(roomId);
            if (rn == null || Boolean.FALSE.equals(rn.getUseYn())) continue;

            String floorName = floorMap.getOrDefault(rtf.getFloorId(), "");
            String roomTypeName = roomTypeMap.getOrDefault(rtf.getRoomTypeId(), "");

            List<MasterReservation> roomReservations = roomReservationMap.getOrDefault(roomId, List.of());
            List<ReservationCalendarResponse> calDtos = roomReservations.stream()
                    .map(m -> toCalendarResponse(m, floorMap, subRoomNumberMap, roomTypeMap))
                    .collect(Collectors.toList());

            timelineRooms.add(ReservationTimelineResponse.TimelineRoom.builder()
                    .roomId(roomId)
                    .roomNumber(rn.getRoomNumber())
                    .floorName(floorName)
                    .roomTypeName(roomTypeName)
                    .reservations(calDtos)
                    .build());
        }

        // 정렬: 층 자연 오름차순 → 호수 자연 오름차순
        timelineRooms.sort((a, b) -> {
            int floorCmp = naturalCompare(a.getFloorName(), b.getFloorName());
            if (floorCmp != 0) return floorCmp;
            return naturalCompare(a.getRoomNumber(), b.getRoomNumber());
        });

        List<ReservationCalendarResponse> unassignedDtos = unassignedList.stream()
                .map(m -> toCalendarResponse(m, floorMap, subRoomNumberMap, roomTypeMap))
                .collect(Collectors.toList());

        return ReservationTimelineResponse.builder()
                .rooms(timelineRooms)
                .unassigned(unassignedDtos)
                .build();
    }

    // ─── private helpers ──────────────────────────

    private ReservationCalendarResponse toCalendarResponse(
            MasterReservation master,
            Map<Long, String> floorMap,
            Map<Long, String> roomNumberMap,
            Map<Long, String> roomTypeMap) {

        String roomInfo = null;
        String roomTypeName = null;
        String stayType = null;
        List<SubReservation> subs = master.getSubReservations();
        if (subs != null && !subs.isEmpty()) {
            SubReservation firstSub = subs.stream()
                    .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus()))
                    .findFirst()
                    .orElse(subs.get(0));

            String floor = firstSub.getFloorId() != null ? floorMap.get(firstSub.getFloorId()) : null;
            String room = firstSub.getRoomNumberId() != null ? roomNumberMap.get(firstSub.getRoomNumberId()) : null;
            if (floor != null && room != null) {
                roomInfo = floor + "-" + room;
            } else if (room != null) {
                roomInfo = room;
            }

            roomTypeName = firstSub.getRoomTypeId() != null ? roomTypeMap.get(firstSub.getRoomTypeId()) : null;
            if (firstSub.getStayType() != null) {
                stayType = firstSub.getStayType().name();
            }
        }

        // 이름: 국문 우선, 없으면 영문 폴백
        String displayName = master.getGuestNameKo();
        if (displayName == null || displayName.isBlank()) {
            StringBuilder en = new StringBuilder();
            if (master.getGuestLastNameEn() != null) en.append(master.getGuestLastNameEn());
            if (master.getGuestFirstNameEn() != null) {
                if (en.length() > 0) en.append(" ");
                en.append(master.getGuestFirstNameEn());
            }
            if (en.length() > 0) displayName = en.toString();
        }
        String maskedName = (displayName != null && !displayName.isBlank())
                ? NameMaskingUtil.maskKoreanName(displayName) : null;

        return ReservationCalendarResponse.builder()
                .id(master.getId())
                .masterReservationNo(master.getMasterReservationNo())
                .reservationStatus(master.getReservationStatus())
                .masterCheckIn(master.getMasterCheckIn())
                .masterCheckOut(master.getMasterCheckOut())
                .guestNameMasked(maskedName)
                .roomInfo(roomInfo)
                .roomTypeName(roomTypeName)
                .stayType(stayType)
                .build();
    }

    private Long getAssignedRoomNumberId(MasterReservation master) {
        if (master.getSubReservations() == null) return null;
        return master.getSubReservations().stream()
                .filter(s -> !"CANCELED".equals(s.getRoomReservationStatus()))
                .map(SubReservation::getRoomNumberId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static int naturalCompare(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        int ia = 0, ib = 0;
        while (ia < a.length() && ib < b.length()) {
            char ca = a.charAt(ia), cb = b.charAt(ib);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                long na = 0, nb = 0;
                while (ia < a.length() && Character.isDigit(a.charAt(ia))) {
                    na = na * 10 + (a.charAt(ia++) - '0');
                }
                while (ib < b.length() && Character.isDigit(b.charAt(ib))) {
                    nb = nb * 10 + (b.charAt(ib++) - '0');
                }
                if (na != nb) return Long.compare(na, nb);
            } else {
                if (ca != cb) return Character.compare(ca, cb);
                ia++;
                ib++;
            }
        }
        return Integer.compare(a.length(), b.length());
    }
}
