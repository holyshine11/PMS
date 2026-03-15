package com.hola.common.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 파일 업로드 API
 */
@Tag(name = "파일 업로드", description = "파일 업로드 API (사업자등록증, 로고 등)")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "파일 업로드", description = "파일 업로드 (카테고리: biz-license, logo). 허용: pdf, jpg, png, gif, svg. 최대 10MB")
    @PostMapping("/upload")
    public HolaResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) {

        String filePath = fileUploadService.upload(file, category);
        String fileName = file.getOriginalFilename();

        return HolaResponse.success(Map.of(
                "filePath", filePath,
                "fileName", fileName != null ? fileName : ""
        ));
    }
}
