package com.example.personalmusicsystem.dto.response;

import com.example.personalmusicsystem.entity.UploadRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 上传记录视图对象（包含用户信息）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UploadRecordVO extends UploadRecord {
    private String username;
    private String nickname;
}

