package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.hotel.dto.request.RoomNumberRequest;
import com.hola.hotel.dto.response.RoomNumberResponse;
import com.hola.hotel.service.RoomNumberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/room-numbers")
@RequiredArgsConstructor
public class RoomNumberApiController {

    private final RoomNumberService roomNumberService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<RoomNumberResponse>>> getRoomNumbers(@PathVariable Long propertyId) {
        return ResponseEntity.ok(HolaResponse.success(roomNumberService.getRoomNumbers(propertyId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomNumberResponse>> getRoomNumber(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(roomNumberService.getRoomNumber(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<RoomNumberResponse>> createRoomNumber(
            @PathVariable Long propertyId, @Valid @RequestBody RoomNumberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(roomNumberService.createRoomNumber(propertyId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<RoomNumberResponse>> updateRoomNumber(
            @PathVariable Long id, @Valid @RequestBody RoomNumberRequest request) {
        return ResponseEntity.ok(HolaResponse.success(roomNumberService.updateRoomNumber(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteRoomNumber(@PathVariable Long id) {
        roomNumberService.deleteRoomNumber(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 호수코드 중복 확인 */
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkRoomNumberCode(
            @PathVariable Long propertyId,
            @RequestParam String roomNumber) {
        boolean duplicate = roomNumberService.existsRoomNumber(propertyId, roomNumber);
        return ResponseEntity.ok(HolaResponse.success(Collections.singletonMap("duplicate", duplicate)));
    }
}
