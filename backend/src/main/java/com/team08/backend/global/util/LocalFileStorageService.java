package com.team08.backend.global.util;

import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static java.util.Comparator.reverseOrder;

/**
 * 로컬 개발용 S3 대체 구현. S3 key를 file.upload-dir 하위 경로로 그대로 매핑한다.
 * 저장된 파일은 WebConfig의 /videos-local/** 핸들러로 서빙된다.
 */
@Slf4j
@Component
@Profile("!prod")
public class LocalFileStorageService implements FileStorageService {

    private final Path rootPath;

    public LocalFileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String uploadFile(InputStream inputStream, String key) {
        try {
            Path target = resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (Exception e) {
            log.error("Failed to store file locally via InputStream. key: {}", key, e);
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    @Override
    public String uploadFile(File file, String key) {
        try {
            Path target = resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (Exception e) {
            log.error("Failed to store file locally. key: {}", key, e);
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    @Override
    public File downloadFile(String key, File destinationFile) {
        try {
            Files.copy(resolve(key), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return destinationFile;
        } catch (Exception e) {
            log.error("Failed to read local file. key: {}", key, e);
            throw new CustomException(ErrorCode.S3_DOWNLOAD_FAILED);
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (Exception e) {
            log.error("Failed to delete local file. key: {}", key, e);
            throw new CustomException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    @Override
    public void deleteDirectory(String prefix) {
        Path dir = resolve(prefix);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (Exception e) {
                    log.error("Failed to delete local path during directory clear. path: {}", path, e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to delete local directory. prefix: {}", prefix, e);
            throw new CustomException(ErrorCode.S3_DELETE_FAILED);
        }
    }

    private Path resolve(String key) {
        Path resolved = rootPath.resolve(key).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new IllegalArgumentException("Storage key escapes upload directory: " + key);
        }
        return resolved;
    }
}
