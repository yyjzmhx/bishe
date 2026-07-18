package com.example.personalmusicsystem.service;

import com.example.personalmusicsystem.entity.UploadRecord;
import com.example.personalmusicsystem.mapper.UploadRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 上传服务类
 */
@Service
@Slf4j
public class UploadService {
    
    @Autowired
    private UploadRecordMapper uploadRecordMapper;
    
    @Autowired
    private FileService fileService;
    
    /**
     * 保存上传记录
     */
    public UploadRecord saveUploadRecord(Long userId, MultipartFile file) {
        // 保存文件到MinIO
        Map<String, String> fileInfo = fileService.saveUserUploadFile(file, userId);
        
        // 创建上传记录
        UploadRecord record = new UploadRecord();
        record.setUserId(userId);
        record.setFileUrl(fileInfo.get("fileUrl"));
        record.setFileName(fileInfo.get("originalFileName"));
        record.setFileSize(Long.parseLong(fileInfo.get("fileSize")));
        record.setFileType(getFileType(fileInfo.get("originalFileName")));
        record.setStatus("UPLOADING");
        
        uploadRecordMapper.insert(record);
        return record;
    }
    
    /**
     * 根据ID查询上传记录
     */
    public UploadRecord getById(Long id) {
        return uploadRecordMapper.selectById(id);
    }
    
    /**
     * 更新上传记录状态
     */
    public boolean updateStatus(Long id, String status, String features, String analysisResult) {
        UploadRecord record = new UploadRecord();
        record.setId(id);
        record.setStatus(status);
        record.setFeatures(features);
        record.setAnalysisResult(analysisResult);
        return uploadRecordMapper.update(record) > 0;
    }
    
    /**
     * 删除上传记录
     */
    public boolean deleteUploadRecord(Long id, Long userId) {
        UploadRecord record = uploadRecordMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            return false;
        }
        
        // 删除MinIO中的文件
        if (record.getFileUrl() != null) {
            fileService.deleteFile(record.getFileUrl());
        }
        
        return uploadRecordMapper.deleteById(id) > 0;
    }
    
    /**
     * 查询用户的上传记录列表
     */
    public List<UploadRecord> getUserUploadRecords(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return uploadRecordMapper.selectByUserId(userId, offset, pageSize);
    }
    
    /**
     * 查询用户的上传记录列表（支持搜索、筛选、分页）
     */
    public List<UploadRecord> getUserUploadRecords(Long userId, int pageNum, int pageSize, 
                                                   String keyword, String startDate, String endDate) {
        int offset = (pageNum - 1) * pageSize;
        return uploadRecordMapper.selectUserList(userId, keyword, startDate, endDate, offset, pageSize);
    }
    
    /**
     * 统计用户的上传记录总数
     */
    public long countUserUploadRecords(Long userId) {
        return uploadRecordMapper.countByUserId(userId);
    }
    
    /**
     * 统计用户的上传记录总数（支持搜索、筛选）
     */
    public long countUserUploadRecords(Long userId, String keyword, String startDate, String endDate) {
        return uploadRecordMapper.countUserList(userId, keyword, startDate, endDate);
    }
    
    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
}

