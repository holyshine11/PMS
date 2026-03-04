package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.dto.PageInfo;
import com.hola.hotel.dto.request.HotelCreateRequest;
import com.hola.hotel.dto.request.HotelUpdateRequest;
import com.hola.hotel.dto.response.HotelResponse;
import com.hola.hotel.service.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 호텔 REST API
 */
@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelApiController {

    private final HotelService hotelService;

    @GetMapping
    public ResponseEntity<HolaResponse<List<HotelResponse>>> getHotels(
            @RequestParam(required = false) String hotelName,
            @RequestParam(required = false) Boolean useYn,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<HotelResponse> page = hotelService.getHotels(hotelName, useYn, pageable);
        return ResponseEntity.ok(
                HolaResponse.success(page.getContent(), PageInfo.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<HotelResponse>> getHotel(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(hotelService.getHotel(id)));
    }

    @PostMapping
    public ResponseEntity<HolaResponse<HotelResponse>> createHotel(
            @Valid @RequestBody HotelCreateRequest request) {
        HotelResponse response = hotelService.createHotel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<HotelResponse>> updateHotel(
            @PathVariable Long id,
            @Valid @RequestBody HotelUpdateRequest request) {
        return ResponseEntity.ok(HolaResponse.success(hotelService.updateHotel(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 호텔명 중복 확인 */
    @GetMapping("/check-name")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkHotelName(
            @RequestParam String hotelName) {
        boolean duplicate = hotelService.checkHotelNameDuplicate(hotelName);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    /** 헤더 드롭다운용 호텔 목록 (간소화) */
    @GetMapping("/selector")
    public ResponseEntity<HolaResponse<List<HotelResponse>>> getHotelsForSelector() {
        return ResponseEntity.ok(HolaResponse.success(hotelService.getHotelsForSelector()));
    }
}
