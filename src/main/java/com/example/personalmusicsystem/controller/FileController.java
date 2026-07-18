package com.example.personalmusicsystem.controller;

import com.example.personalmusicsystem.config.MinioConfig;
import com.example.personalmusicsystem.service.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.Map;

/**
 * 文件访问控制器
 */
@RestController
@RequestMapping("/api/files")
@Slf4j
public class FileController {

    @Autowired
    private StorageService storageService;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 获取文件，支持音频分段播放
     */
    @GetMapping("/**")
    public ResponseEntity<InputStreamResource> getFile(HttpServletRequest request) {
        try {
            String requestPath = request.getRequestURI();
            String filePath = requestPath.replace("/api/files", "");
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }

            String fileUrl = buildFileUrl(filePath);
            log.debug("文件请求路径: {}, 构建URL: {}", filePath, fileUrl);

            String fullFileUrl = fileUrl;
            if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
                fullFileUrl = minioConfig.getEndpoint() + "/" + fileUrl;
            }

            if (!storageService.fileExists(fullFileUrl)) {
                log.warn("文件不存在: {}", fullFileUrl);
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> metadata = storageService.getFileMetadata(fullFileUrl);
            long fileSize = ((Number) metadata.getOrDefault("size", 0L)).longValue();
            String contentType = resolveContentType(filePath, metadata.get("contentType"));
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("inline", extractFileName(filePath));
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");

            if (rangeHeader != null && rangeHeader.startsWith("bytes=") && fileSize > 0) {
                long[] range = parseRange(rangeHeader, fileSize);
                long start = range[0];
                long end = range[1];
                long contentLength = end - start + 1;

                InputStream inputStream = storageService.getFileInputStream(fullFileUrl, start, contentLength);
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
                headers.setContentLength(contentLength);

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers)
                        .body(new InputStreamResource(inputStream));
            }

            InputStream inputStream = storageService.getFileInputStream(fullFileUrl);
            if (fileSize > 0) {
                headers.setContentLength(fileSize);
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("文件获取失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private long[] parseRange(String rangeHeader, long fileSize) {
        String rangeValue = rangeHeader.substring("bytes=".length()).trim();
        String[] parts = rangeValue.split("-", 2);

        long start;
        long end;

        if (parts[0].isEmpty()) {
            long suffixLength = Long.parseLong(parts[1]);
            start = Math.max(fileSize - suffixLength, 0);
            end = fileSize - 1;
        } else {
            start = Long.parseLong(parts[0]);
            end = parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : fileSize - 1;
        }

        if (start < 0) {
            start = 0;
        }
        if (end >= fileSize) {
            end = fileSize - 1;
        }
        if (end < start) {
            end = start;
        }

        return new long[]{start, end};
    }

    private String resolveContentType(String filePath, Object metadataContentType) {
        if (metadataContentType instanceof String contentType
                && !contentType.isBlank()
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }
        return getContentType(filePath);
    }

    private String buildFileUrl(String filePath) {
        String objectPath = filePath;
        if (!objectPath.startsWith(minioConfig.getBucketName() + "/")) {
            objectPath = minioConfig.getBucketName() + "/" + objectPath;
        }
        return minioConfig.getEndpoint() + "/" + objectPath;
    }

    private String extractFileName(String path) {
        if (path.contains("/")) {
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return path;
    }

    private String getContentType(String fileName) {
        String extension = "";
        if (fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }

        switch (extension) {
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "flac":
                return "audio/flac";
            case "m4a":
                return "audio/mp4";
            case "aac":
                return "audio/aac";
            case "ogg":
                return "audio/ogg";
            case "mgg":
                return "application/octet-stream";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }
}
