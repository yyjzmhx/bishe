package com.example.personalmusicsystem.service;

import com.example.personalmusicsystem.service.storage.StorageService;
import com.example.personalmusicsystem.service.storage.impl.MinioStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件服务类
 */
@Service
@Slf4j
public class FileService {
    
    @Autowired
    private StorageService storageService;
    
    @Autowired
    private MinioStorageService minioStorageService;
    
    @Value("${file.upload.max-size:52428800}")
    private long maxFileSize;
    
    @Value("${file.upload.allowed-types:mp3,wav,flac,m4a,aac,ogg,mgg}")
    private String allowedTypes;
    
    /**
     * 保存用户上传的音频文件到MinIO
     */
    public Map<String, String> saveUserUploadFile(MultipartFile file, Long userId) {
        validateFile(file);
        
        String relativePath = "uploads/" + minioStorageService.generateDatePath();
        String fileName = "user_" + userId + "_" + 
                         minioStorageService.generateUniqueFileName(file.getOriginalFilename());
        
        String fileUrl = storageService.saveFile(file, relativePath, fileName);
        
        Map<String, String> result = new HashMap<>();
        result.put("fileUrl", fileUrl);
        result.put("fileName", fileName);
        result.put("originalFileName", file.getOriginalFilename());
        result.put("fileSize", String.valueOf(file.getSize()));
        result.put("contentType", file.getContentType());
        
        return result;
    }
    
    /**
     * 保存音乐库文件到MinIO
     * @param file 音频文件
     * @param musicId 音乐ID，如果为null则使用临时文件名
     */
    public Map<String, String> saveLibraryFile(MultipartFile file, Long musicId) {
        validateFile(file);
        
        String relativePath = "library";
        String fileName;
        if (musicId != null) {
            fileName = "music_" + musicId + "_" + 
                      minioStorageService.generateUniqueFileName(file.getOriginalFilename());
        } else {
            // 临时文件名，用于先上传文件再保存音乐信息
            fileName = "temp_" + System.currentTimeMillis() + "_" + 
                      minioStorageService.generateUniqueFileName(file.getOriginalFilename());
        }
        
        String fileUrl = storageService.saveFile(file, relativePath, fileName);
        
        Map<String, String> result = new HashMap<>();
        result.put("fileUrl", fileUrl);
        result.put("fileName", fileName);
        result.put("originalFileName", file.getOriginalFilename());
        result.put("fileSize", String.valueOf(file.getSize()));
        
        return result;
    }
    
    /**
     * 保存试听片段到MinIO
     */
    public Map<String, String> savePreviewFile(MultipartFile file, Long musicId) {
        validateFile(file);
        
        String relativePath = "previews";
        String fileName = "preview_" + musicId + ".mp3";
        
        String fileUrl = storageService.saveFile(file, relativePath, fileName);
        
        Map<String, String> result = new HashMap<>();
        result.put("fileUrl", fileUrl);
        result.put("fileName", fileName);
        
        return result;
    }
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new IllegalArgumentException("文件格式不正确");
        }
        
        String extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowedTypeList = Arrays.asList(allowedTypes.split(","));
        
        if (!allowedTypeList.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型，支持的类型: " + allowedTypes);
        }
    }
    
    /**
     * 保存封面图片到MinIO
     */
    public Map<String, String> saveCoverImage(MultipartFile file, Long musicId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        // 验证文件大小（5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("封面图片大小不能超过5MB");
        }
        
        // 验证文件类型（图片）
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) {
            throw new IllegalArgumentException("文件格式不正确");
        }
        
        String extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        List<String> imageTypes = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        
        if (!imageTypes.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型，支持的图片类型: jpg, jpeg, png, gif, webp");
        }
        
        String relativePath = "covers";
        String fileName;
        if (musicId != null) {
            fileName = "cover_" + musicId + "_" + 
                      minioStorageService.generateUniqueFileName(file.getOriginalFilename());
        } else {
            // 临时文件名，用于先上传文件再保存音乐信息
            fileName = "temp_cover_" + System.currentTimeMillis() + "_" + 
                      minioStorageService.generateUniqueFileName(file.getOriginalFilename());
        }
        
        log.info("开始上传封面图片到MinIO: 相对路径={}, 文件名={}, 原始文件名={}, 文件大小={} bytes", 
                relativePath, fileName, file.getOriginalFilename(), file.getSize());
        
        String fileUrl = storageService.saveFile(file, relativePath, fileName);
        
        log.info("封面图片上传到MinIO成功: URL={}", fileUrl);
        
        Map<String, String> result = new HashMap<>();
        result.put("fileUrl", fileUrl);
        result.put("fileName", fileName);
        result.put("originalFileName", file.getOriginalFilename());
        result.put("fileSize", String.valueOf(file.getSize()));
        result.put("contentType", file.getContentType());
        
        return result;
    }
    
    /**
     * 删除文件
     */
    public boolean deleteFile(String fileUrl) {
        return storageService.deleteFile(fileUrl);
    }
}

