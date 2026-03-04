package com.hola.common.controller;

import com.hola.common.dto.HolaResponse;
import com.hola.common.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 파일 업로드 API
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * 파일 업로드
     * @param file 업로드 파일
     * @param category 카테고리 (biz-license, logo)
     * @return 파일 경로, 원본 파일명
     */
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
