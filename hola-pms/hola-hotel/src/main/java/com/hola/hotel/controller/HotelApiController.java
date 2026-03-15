package com.hola.hotel.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.dto.PageInfo;
import com.hola.hotel.dto.request.HotelCreateRequest;
import com.hola.hotel.dto.request.HotelUpdateRequest;
import com.hola.hotel.dto.response.HotelResponse;
import com.hola.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.hola.common.security.AccessControlService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 호텔 REST API
 */
@Tag(name = "호텔 관리", description = "호텔 CRUD 및 선택자 API")
@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelApiController {

    private final HotelService hotelService;
    private final AccessControlService accessControlService;

    @Operation(summary = "호텔 목록 조회", description = "호텔명/사용여부 필터 + 페이징")
    @GetMapping
    public ResponseEntity<HolaResponse<List<HotelResponse>>> getHotels(
            @RequestParam(required = false) String hotelName,
            @RequestParam(required = false) Boolean useYn,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<HotelResponse> page = hotelService.getHotels(hotelName, useYn, pageable);
        return ResponseEntity.ok(
                HolaResponse.success(page.getContent(), PageInfo.from(page)));
    }

    @Operation(summary = "호텔 상세 조회", description = "호텔 ID로 상세 정보 조회")
    @GetMapping("/{id}")
    public ResponseEntity<HolaResponse<HotelResponse>> getHotel(@PathVariable Long id) {
        return ResponseEntity.ok(HolaResponse.success(hotelService.getHotel(id)));
    }

    @Operation(summary = "호텔 등록", description = "새 호텔 생성")
    @PostMapping
    public ResponseEntity<HolaResponse<HotelResponse>> createHotel(
            @Valid @RequestBody HotelCreateRequest request) {
        HotelResponse response = hotelService.createHotel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HolaResponse.success(response));
    }

    @Operation(summary = "호텔 수정", description = "호텔 정보 수정")
    @PutMapping("/{id}")
    public ResponseEntity<HolaResponse<HotelResponse>> updateHotel(
            @PathVariable Long id,
            @Valid @RequestBody HotelUpdateRequest request) {
        return ResponseEntity.ok(HolaResponse.success(hotelService.updateHotel(id, request)));
    }

    @Operation(summary = "호텔 삭제", description = "호텔 소프트 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<HolaResponse<Void>> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);
        return ResponseEntity.ok(HolaResponse.success());
    }

    /** 호텔명 중복 확인 */
    @Operation(summary = "호텔명 중복 확인", description = "호텔명 중복 여부 조회")
    @GetMapping("/check-name")
    public ResponseEntity<HolaResponse<Map<String, Boolean>>> checkHotelName(
            @RequestParam String hotelName) {
        boolean duplicate = hotelService.checkHotelNameDuplicate(hotelName);
        return ResponseEntity.ok(HolaResponse.success(Map.of("duplicate", duplicate)));
    }

    /** 헤더 드롭다운용 호텔 목록 (로그인 사용자 권한 기반 필터링) */
    @Operation(summary = "호텔 선택 목록", description = "헤더 드롭다운용 호텔 목록 (권한 기반 필터링)")
    @GetMapping("/selector")
    public ResponseEntity<HolaResponse<List<HotelResponse>>> getHotelsForSelector() {
        String loginId = accessControlService.getCurrentLoginId();
        return ResponseEntity.ok(HolaResponse.success(hotelService.getHotelsForSelector(loginId)));
    }
}
