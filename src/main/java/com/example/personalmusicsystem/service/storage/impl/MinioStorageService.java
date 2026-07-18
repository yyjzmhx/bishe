package com.example.personalmusicsystem.service.storage.impl;

import com.example.personalmusicsystem.config.MinioConfig;
import com.example.personalmusicsystem.service.storage.StorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MinIO存储服务实现
 */
@Service
@Slf4j
public class MinioStorageService implements StorageService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 初始化存储桶（如果不存在则创建）
     */
    public void initBucket() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );

            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
                log.info("MinIO存储桶创建成功: {}", minioConfig.getBucketName());
            } else {
                log.debug("MinIO存储桶已存在: {}", minioConfig.getBucketName());
            }
        } catch (io.minio.errors.ErrorResponseException e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Access Key Id")) {
                log.error("MinIO Access Key 配置错误，请检查 application.yml 中的 minio.access-key 和 minio.secret-key");
                throw new RuntimeException("MinIO认证失败，Access Key 不存在。请检查 application.yml 中的 minio 配置是否正确，或参考 docs/MINIO_SETUP.md 进行配置。");
            }
            log.error("MinIO存储桶初始化失败", e);
            throw new RuntimeException("MinIO存储桶初始化失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("MinIO存储桶初始化失败，请检查 MinIO 服务是否运行，以及配置是否正确", e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Connection refused")) {
                throw new RuntimeException("无法连接到 MinIO 服务，请确保 MinIO 服务已启动（默认地址：http://localhost:9000）。参考 docs/MINIO_SETUP.md 了解如何启动 MinIO。");
            }
            throw new RuntimeException("MinIO存储桶初始化失败: " + e.getMessage() + "。请检查 MinIO 服务状态和配置。");
        }
    }

    @Override
    public String saveFile(MultipartFile file, String relativePath, String fileName) {
        try {
            initBucket();
            String objectName = buildObjectName(relativePath, fileName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(getContentType(fileName))
                            .build()
            );

            log.info("文件上传到 MinIO 成功: {}", objectName);
            return getFileUrl(relativePath, fileName);
        } catch (Exception e) {
            log.error("文件上传到 MinIO 失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String saveFile(InputStream inputStream, String relativePath, String fileName, long fileSize) {
        try {
            initBucket();
            String objectName = buildObjectName(relativePath, fileName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(inputStream, fileSize, -1)
                            .contentType(getContentType(fileName))
                            .build()
            );

            log.info("文件流上传到 MinIO 成功: {}", objectName);
            return getFileUrl(relativePath, fileName);
        } catch (Exception e) {
            log.error("文件流上传到 MinIO 失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件流上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            String objectName = extractObjectNameFromUrl(fileUrl);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("MinIO文件删除成功: {}", objectName);
            return true;
        } catch (Exception e) {
            log.error("MinIO文件删除失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFileUrl(String relativePath, String fileName) {
        String objectName = buildObjectName(relativePath, fileName);
        return "/api/files/" + objectName;
    }

    @Override
    public boolean fileExists(String fileUrl) {
        try {
            String objectName = extractObjectNameFromUrl(fileUrl);
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public InputStream getFileInputStream(String fileUrl) {
        try {
            String objectName = extractObjectNameFromUrl(fileUrl);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("从 MinIO 获取文件流失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取文件流失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream getFileInputStream(String fileUrl, long offset, long length) {
        try {
            String objectName = extractObjectNameFromUrl(fileUrl);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .offset(offset)
                            .length(length)
                            .build()
            );
        } catch (Exception e) {
            log.error("从 MinIO 按范围获取文件流失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取文件流失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getFileMetadata(String fileUrl) {
        try {
            String objectName = extractObjectNameFromUrl(fileUrl);
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("size", stat.size());
            metadata.put("contentType", stat.contentType());
            metadata.put("etag", stat.etag());
            metadata.put("lastModified", stat.lastModified());
            return metadata;
        } catch (Exception e) {
            log.error("获取 MinIO 文件元数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取文件元数据失败: " + e.getMessage());
        }
    }

    private String buildObjectName(String relativePath, String fileName) {
        String objectName = relativePath.replace("\\", "/");
        if (!objectName.isEmpty() && !objectName.endsWith("/")) {
            objectName += "/";
        }
        objectName += fileName;
        if (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }
        return objectName;
    }

    private String extractObjectNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("fileUrl cannot be empty");
        }

        String objectName = fileUrl.trim().replace("\\", "/");

        String proxyPrefix = "/api/files/";
        if (objectName.startsWith(proxyPrefix)) {
            objectName = objectName.substring(proxyPrefix.length());
        } else if (objectName.startsWith("api/files/")) {
            objectName = objectName.substring("api/files/".length());
        }

        String bucketMarker = "/" + minioConfig.getBucketName() + "/";
        int bucketIndex = objectName.indexOf(bucketMarker);
        if (bucketIndex >= 0) {
            objectName = objectName.substring(bucketIndex + bucketMarker.length());
        } else if (objectName.contains(minioConfig.getBucketName() + "/")) {
            objectName = objectName.substring(
                    objectName.indexOf(minioConfig.getBucketName() + "/") + minioConfig.getBucketName().length() + 1
            );
        }

        while (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }

        return objectName;
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
            case "bmp":
                return "image/bmp";
            case "svg":
                return "image/svg+xml";
            default:
                return "application/octet-stream";
        }
    }

    public String generateDatePath() {
        LocalDate now = LocalDate.now();
        return String.format("%s/%s/%s",
                now.getYear(),
                String.format("%02d", now.getMonthValue()),
                String.format("%02d", now.getDayOfMonth()));
    }

    public String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
