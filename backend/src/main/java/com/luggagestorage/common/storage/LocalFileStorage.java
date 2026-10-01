package com.luggagestorage.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 개발용: 서버 폴더에 저장하고 /uploads/** 로 제공한다 (StorageConfig에서 정적 리소스로 등록).
 * Render 같은 배포 서버는 재시작하면 디스크가 초기화되므로 배포에는 S3FileStorage를 쓴다.
 */
public class LocalFileStorage implements FileStorage {

    public static final String URL_PATH = "/uploads/";

    private final Path root;
    private final String urlPrefix;

    public LocalFileStorage(Path root, String publicBaseUrl) {
        this.root = root.toAbsolutePath().normalize();
        this.urlPrefix = (publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "")) + URL_PATH;
    }

    public Path root() {
        return root;
    }

    @Override
    public String store(String key, byte[] data, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return urlPrefix + key;
    }

    /** "../" 같은 키로 저장 폴더 밖에 쓰지 못하게 막는다 */
    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 저장 경로: " + key);
        }
        return target;
    }
}
