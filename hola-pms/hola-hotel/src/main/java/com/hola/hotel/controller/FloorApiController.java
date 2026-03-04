package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.hotel.dto.request.FloorRequest;
import com.hola.hotel.dto.response.FloorResponse;
import com.hola.hotel.service.FloorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/floors")
@RequiredArgsConstructor
public class FloorApiController {

    private final FloorService floorService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<FloorResponse>>> getFloors(@PathVariable Long propertyId) {
        return ResponseEntity.ok(HolaResponse.success(floorService.getFloors(propertyId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<FloorResponse>> getFloor(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(floorService.getFloor(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<FloorResponse>> createFloor(
            @PathVariable Long propertyId, @Valid @RequestBody FloorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HolaResponse.success(floorService.createFloor(propertyId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<FloorResponse>> updateFloor(
            @PathVariable Long id, @Valid @RequestBody FloorRequest request) {
        return ResponseEntity.ok(HolaResponse.success(floorService.updateFloor(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteFloor(@PathVariable Long id) {
        floorService.deleteFloor(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 층코드 중복 확인 */
    @GetMapping("/check-code")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkFloorCode(
            @PathVariable Long propertyId,
            @RequestParam String floorNumber) {
        boolean duplicate = floorService.existsFloorNumber(propertyId, floorNumber);
        return ResponseEntity.ok(HolaResponse.success(Collections.singletonMap("duplicate", duplicate)));
    }
}
