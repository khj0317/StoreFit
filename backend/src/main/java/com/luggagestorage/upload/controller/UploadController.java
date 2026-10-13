package com.luggagestorage.upload.controller;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.common.storage.FileStorage;
import com.luggagestorage.demo.DemoAccounts;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private static final int MAX_FILES = 10;

    /** 확장자는 사용자가 보낸 파일 이름이 아니라 검증한 Content-Type에서 정한다 */
    private static final Map<String, String> EXTENSIONS = Map.of(
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp",
        "image/gif", ".gif",
        "image/heic", ".heic"
    );

    private final FileStorage fileStorage;

    @PostMapping("/images")
    public ResponseEntity<List<String>> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        // 체험 계정은 휴대폰 인증 없이 누구나 받을 수 있으므로, 공개 저장소에 파일을 올리지 못하게 한다
        if (DemoAccounts.isDemoUsername(SecurityUtil.getCurrentUsername())) {
            throw new BusinessException(ErrorCode.DEMO_ACCOUNT_RESTRICTED);
        }
        if (files.size() > MAX_FILES) {
            throw new BusinessException(ErrorCode.TOO_MANY_FILES);
        }
        List<String> urls = files.stream()
            .map(this::saveImage)
            .toList();
        return ResponseEntity.ok(urls);
    }

    private String saveImage(MultipartFile file) {
        String contentType = file.getContentType();
        String extension = contentType == null ? null : EXTENSIONS.get(contentType.toLowerCase());
        if (extension == null) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }

        try {
            return fileStorage.store("stores/" + UUID.randomUUID() + extension, file.getBytes(), contentType);
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}
