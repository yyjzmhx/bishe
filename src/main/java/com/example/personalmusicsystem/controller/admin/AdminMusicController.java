package com.example.personalmusicsystem.controller.admin;

import com.example.personalmusicsystem.annotation.RequireAdmin;
import com.example.personalmusicsystem.dto.Result;
import com.example.personalmusicsystem.entity.Music;
import com.example.personalmusicsystem.service.FileService;
import com.example.personalmusicsystem.service.MusicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员音乐管理控制器
 */
@RestController
@RequestMapping("/api/admin/music")
@RequireAdmin
@Slf4j
public class AdminMusicController {
    
    @Autowired
    private MusicService musicService;
    
    @Autowired
    private FileService fileService;
    
    /**
     * 获取音乐列表
     */
    @GetMapping
    public Result<Map<String, Object>> getMusicList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Map<String, Object> result = musicService.getMusicList(
            keyword, artist, genre, status, pageNum, pageSize
        );
        return Result.success(result);
    }
    
    /**
     * 添加音乐（支持文件上传）
     * 音频文件为必填项
     */
    @PostMapping
    public Result<Map<String, Object>> addMusic(
            @RequestParam("title") String title,
            @RequestParam("artist") String artist,
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam(value = "album", required = false) String album,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "lyrics", required = false) String lyrics,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestParam(value = "coverUrl", required = false) String coverUrl) {
        try {
            // 验证必填字段
            if (title == null || title.trim().isEmpty()) {
                return Result.error("歌曲名不能为空");
            }
            if (artist == null || artist.trim().isEmpty()) {
                return Result.error("歌手不能为空");
            }
            if (audioFile == null || audioFile.isEmpty()) {
                return Result.error("音频文件不能为空");
            }
            
            // 验证音频文件格式
            String fileName = audioFile.getOriginalFilename();
            if (fileName == null || !fileName.contains(".")) {
                return Result.error("音频文件格式不正确");
            }
            
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            List<String> allowedTypes = Arrays.asList("mp3", "wav", "flac", "m4a", "aac", "ogg", "mgg");
            if (!allowedTypes.contains(extension)) {
                return Result.error("不支持的音频格式，支持: MP3、WAV、FLAC、M4A、AAC、OGG、MGG");
            }
            
            // 验证文件大小（50MB）
            if (audioFile.getSize() > 50 * 1024 * 1024) {
                return Result.error("音频文件大小不能超过50MB");
            }
            
            // 先上传音频文件（必填），使用临时文件名（musicId为null）
            Map<String, String> audioInfo = fileService.saveLibraryFile(audioFile, null);
            String audioFileUrl = audioInfo.get("fileUrl");
            
            // 上传封面图片（可选）
            String coverFileUrl = null;
            if (coverFile != null && !coverFile.isEmpty()) {
                log.info("开始上传封面图片: {}", coverFile.getOriginalFilename());
                try {
                    Map<String, String> coverInfo = fileService.saveCoverImage(coverFile, null);
                    coverFileUrl = coverInfo.get("fileUrl");
                    log.info("封面图片上传成功，URL: {}", coverFileUrl);
                } catch (Exception e) {
                    log.error("封面上传失败: {}", e.getMessage(), e);
                    // 封面上传失败不影响音频文件上传，继续执行
                    // 但记录错误日志
                }
            } else if (coverUrl != null && !coverUrl.isEmpty() && !coverUrl.trim().isEmpty()) {
                coverFileUrl = coverUrl.trim();
                log.info("使用封面URL: {}", coverFileUrl);
            }
            
            // 创建音乐对象，包含音频URL（必填）
            Music music = new Music();
            music.setTitle(title.trim());
            music.setArtist(artist.trim());
            music.setAlbum(album != null ? album.trim() : null);
            music.setGenre(genre);
            music.setLyrics(lyrics != null && !lyrics.trim().isEmpty() ? lyrics.trim() : null);
            music.setFileUrl(audioFileUrl); // 设置音频文件URL（必填）
            music.setCoverUrl(coverFileUrl); // 设置封面URL（可选，可能为null）
            music.setStatus(1); // 默认上架
            
            log.info("准备保存音乐信息: title={}, artist={}, fileUrl={}, coverUrl={}", 
                    music.getTitle(), music.getArtist(), music.getFileUrl(), music.getCoverUrl());
            
            // 保存音乐信息到数据库（此时fileUrl已设置）
            boolean success = musicService.addMusic(music);
            if (!success) {
                // 如果保存失败，尝试删除已上传的文件
                try {
                    fileService.deleteFile(audioFileUrl);
                    if (coverFileUrl != null) {
                        fileService.deleteFile(coverFileUrl);
                    }
                } catch (Exception e) {
                    log.warn("删除已上传文件失败: {}", e.getMessage());
                }
                return Result.error("添加音乐信息失败");
            }
            
            // 可选：提取音频时长（前端可以通过音频元素获取，这里暂时不实现）
            // 如果需要，可以调用AI服务或使用音频处理库
            
            Music refreshedMusic = musicService.refreshAudioFeatures(music.getId());
            if (refreshedMusic != null) {
                music = refreshedMusic;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("musicId", music.getId());
            result.put("fileUrl", music.getFileUrl());
            result.put("coverUrl", music.getCoverUrl());
            return Result.success("歌曲添加成功", result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("添加音乐失败", e);
            return Result.error("添加失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传封面图片
     */
    @PostMapping("/{id}/cover")
    public Result<Map<String, String>> uploadCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            Music music = musicService.getById(id);
            if (music == null) {
                return Result.error("音乐不存在");
            }
            
            String oldCoverUrl = music.getCoverUrl();
            
            // 上传新封面
            Map<String, String> coverInfo = fileService.saveCoverImage(file, id);
            String newCoverUrl = coverInfo.get("fileUrl");
            
            // 更新数据库
            music.setCoverUrl(newCoverUrl);
            boolean updateSuccess = musicService.updateMusic(music);
            
            if (!updateSuccess) {
                // 删除已上传的文件
                fileService.deleteFile(newCoverUrl);
                return Result.error("更新封面失败");
            }
            
            // 删除旧封面（如果存在）
            if (oldCoverUrl != null && !oldCoverUrl.isEmpty() && !oldCoverUrl.equals(newCoverUrl)) {
                try {
                    fileService.deleteFile(oldCoverUrl);
                } catch (Exception e) {
                    log.warn("删除旧封面失败: {}", oldCoverUrl, e);
                }
            }
            
            return Result.success("封面上传成功", coverInfo);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("上传封面失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取音乐文件信息
     */
    @GetMapping("/{id}/file-info")
    public Result<Map<String, Object>> getMusicFileInfo(@PathVariable Long id) {
        try {
            Music music = musicService.getById(id);
            if (music == null) {
                return Result.error("音乐不存在");
            }
            
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("fileUrl", music.getFileUrl());
            fileInfo.put("previewUrl", music.getPreviewUrl());
            fileInfo.put("duration", music.getDuration());
            
            // 从URL提取文件名
            if (music.getFileUrl() != null && !music.getFileUrl().isEmpty()) {
                String fileName = music.getFileUrl().substring(
                    music.getFileUrl().lastIndexOf("/") + 1
                );
                fileInfo.put("fileName", fileName);
            } else {
                fileInfo.put("fileName", "未上传");
            }
            
            return Result.success(fileInfo);
        } catch (Exception e) {
            log.error("获取音乐文件信息失败", e);
            return Result.error("获取文件信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传/替换音乐文件
     */
    @PostMapping("/{id}/upload")
    public Result<Map<String, String>> uploadMusicFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            // 验证文件格式
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("audio/") && 
                !file.getOriginalFilename().toLowerCase().endsWith(".mgg"))) {
                return Result.error("文件格式不正确，请上传音频文件");
            }
            
            // 验证文件大小（50MB）
            if (file.getSize() > 50 * 1024 * 1024) {
                return Result.error("文件大小不能超过50MB");
            }
            
            // 获取当前音乐信息
            Music currentMusic = musicService.getById(id);
            if (currentMusic == null) {
                return Result.error("音乐不存在");
            }
            
            // 保存旧文件URL（用于删除）
            String oldFileUrl = currentMusic.getFileUrl();
            log.info("开始替换音频文件，音乐ID: {}, 旧文件URL: {}", id, oldFileUrl);
            
            // 保存新文件到MinIO
            Map<String, String> fileInfo = fileService.saveLibraryFile(file, id);
            String newFileUrl = fileInfo.get("fileUrl");
            
            if (newFileUrl == null || newFileUrl.isEmpty()) {
                log.error("新文件URL为空，无法更新数据库");
                return Result.error("文件上传失败：未获取到文件URL");
            }
            
            log.info("新文件上传成功，新文件URL: {}", newFileUrl);
            
            // 更新音乐的fileUrl
            Music music = new Music();
            music.setId(id);
            music.setFileUrl(newFileUrl);
            
            boolean updateSuccess = musicService.updateMusic(music);
            if (!updateSuccess) {
                log.error("数据库更新失败，音乐ID: {}, 新文件URL: {}", id, newFileUrl);
                // 如果数据库更新失败，尝试删除已上传的新文件
                try {
                    fileService.deleteFile(newFileUrl);
                    log.info("数据库更新失败，已删除新上传的文件: {}", newFileUrl);
                } catch (Exception deleteEx) {
                    log.error("删除新上传的文件失败: {}", newFileUrl, deleteEx);
                }
                return Result.error("数据库更新失败，请重试");
            }
            
            // 验证更新是否成功
            Music updatedMusic = musicService.getById(id);
            if (updatedMusic == null) {
                log.error("更新后查询音乐失败，音乐ID: {}", id);
                return Result.error("更新后验证失败");
            }
            
            if (!newFileUrl.equals(updatedMusic.getFileUrl())) {
                log.error("数据库更新验证失败，期望URL: {}, 实际URL: {}", newFileUrl, updatedMusic.getFileUrl());
                return Result.error("数据库更新验证失败，请检查数据库");
            }
            
            log.info("数据库更新成功，音乐ID: {}, 新文件URL: {}", id, newFileUrl);
            
            // 删除旧文件（如果存在）
            if (oldFileUrl != null && !oldFileUrl.isEmpty() && !oldFileUrl.equals(newFileUrl)) {
                try {
                    boolean deleted = fileService.deleteFile(oldFileUrl);
                    if (deleted) {
                        log.info("删除旧文件成功: {}", oldFileUrl);
                    } else {
                        log.warn("删除旧文件返回false: {}", oldFileUrl);
                    }
                } catch (Exception e) {
                    log.warn("删除旧文件失败: {}", oldFileUrl, e);
                    // 不抛出异常，因为新文件已经上传成功且数据库已更新
                }
            }
            
            musicService.refreshAudioFeatures(id);
            return Result.success("音频文件替换成功", fileInfo);
        } catch (Exception e) {
            log.error("上传音频文件失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新音乐信息
     */
    @PutMapping("/{id}")
    public Result<String> updateMusic(@PathVariable Long id, @RequestBody Music music) {
        music.setId(id);
        boolean success = musicService.updateMusic(music);
        if (success) {
            musicService.refreshAudioFeatures(id);
            return Result.success("更新成功", null);
        }
        return Result.error("更新失败");
    }
    
    /**
     * 删除音乐
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteMusic(@PathVariable Long id) {
        boolean success = musicService.deleteMusic(id);
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }
}

