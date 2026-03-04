package com.hola.common.service;

import com.hola.common.exception.ErrorCode;
import com.hola.common.exception.HolaException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 파일 업로드 서비스 (로컬 디스크 저장)
 */
@Slf4j
@Service
public class FileUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "gif", "svg"
    );

    @Value("${hola.upload.path:./uploads}")
    private String uploadPath;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("파일 업로드 경로 초기화: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리 생성 실패: " + rootLocation, e);
        }
    }

    /**
     * 파일 저장
     * @param file 업로드 파일
     * @param subDir 하위 디렉토리 (예: biz-license, logo)
     * @return URL 경로 (예: /uploads/biz-license/uuid.pdf)
     */
    public String upload(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new HolaException(ErrorCode.INVALID_INPUT, "파일이 비어있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new HolaException(ErrorCode.INVALID_INPUT,
                    "허용되지 않는 파일 형식입니다. (허용: " + ALLOWED_EXTENSIONS + ")");
        }

        // UUID 기반 파일명 생성
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetDir = rootLocation.resolve(subDir);
        Path targetFile = targetDir.resolve(storedFilename);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 업로드 완료: {} → {}", originalFilename, targetFile);
        } catch (IOException e) {
            throw new HolaException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }

        return "/uploads/" + subDir + "/" + storedFilename;
    }

    /**
     * 파일 삭제
     * @param filePath URL 경로 (예: /uploads/biz-license/uuid.pdf)
     */
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) return;

        // /uploads/ 접두사 제거 후 실제 경로 조합
        String relativePath = filePath.replaceFirst("^/uploads/", "");
        Path target = rootLocation.resolve(relativePath).normalize();

        // 경로 탈출 방지
        if (!target.startsWith(rootLocation)) {
            log.warn("잘못된 파일 삭제 경로: {}", filePath);
            return;
        }

        try {
            if (Files.deleteIfExists(target)) {
                log.info("파일 삭제 완료: {}", target);
            }
        } catch (IOException e) {
            log.warn("파일 삭제 실패: {}", target, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
