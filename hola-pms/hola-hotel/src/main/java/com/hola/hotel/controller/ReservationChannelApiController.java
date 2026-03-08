package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.security.AccessControlService;
import com.hola.hotel.dto.request.ReservationChannelRequest;
import com.hola.hotel.dto.response.ReservationChannelResponse;
import com.hola.hotel.service.ReservationChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/reservation-channels")
@RequiredArgsConstructor
public class ReservationChannelApiController {

    private final ReservationChannelService reservationChannelService;
    private final AccessControlService accessControlService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<ReservationChannelResponse>>> getList(@PathVariable Long propertyId) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.ok(HolaResponse.success(reservationChannelService.getList(propertyId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<ReservationChannelResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(reservationChannelService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<ReservationChannelResponse>> create(
            @PathVariable Long propertyId, @Valid @RequestBody ReservationChannelRequest request) {
        accessControlService.validatePropertyAccess(propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(reservationChannelService.create(propertyId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<ReservationChannelResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ReservationChannelRequest request) {
        return ResponseEntity.ok(HolaResponse.success(reservationChannelService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> delete(@PathVariable Long id) {
        reservationChannelService.delete(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 채널코드 중복 확인 */
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkCode(
            @PathVariable Long propertyId,
            @RequestParam String channelCode) {
        accessControlService.validatePropertyAccess(propertyId);
        boolean duplicate = reservationChannelService.checkCode(propertyId, channelCode);
        return ResponseEntity.ok(HolaResponse.success(Collections.singletonMap("duplicate", duplicate)));
    }
}
