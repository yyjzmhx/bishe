package com.example.personalmusicsystem.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

/**
 * 文件存储服务接口
 */
public interface StorageService {

    /**
     * 保存文件
     */
    String saveFile(MultipartFile file, String relativePath, String fileName);

    /**
     * 保存文件流
     */
    String saveFile(InputStream inputStream, String relativePath, String fileName, long fileSize);

    /**
     * 删除文件
     */
    boolean deleteFile(String fileUrl);

    /**
     * 获取文件访问URL
     */
    String getFileUrl(String relativePath, String fileName);

    /**
     * 检查文件是否存在
     */
    boolean fileExists(String fileUrl);

    /**
     * 获取完整文件输入流
     */
    InputStream getFileInputStream(String fileUrl);

    /**
     * 按字节范围获取文件输入流
     */
    InputStream getFileInputStream(String fileUrl, long offset, long length);

    /**
     * 获取文件元数据
     */
    Map<String, Object> getFileMetadata(String fileUrl);
}
