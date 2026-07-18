package com.example.personalmusicsystem.service.ai;
import com.example.personalmusicsystem.entity.Music;
import com.example.personalmusicsystem.entity.UploadRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified analysis context used for both user uploads and library tracks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioAnalysisContext {

    private String sourceKind;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer durationSeconds;

    private String title;
    private String artist;
    private String album;
    private String genre;
    private String lyrics;

    public static AudioAnalysisContext fromUploadRecord(UploadRecord record) {
        if (record == null) {
            return null;
        }
        return AudioAnalysisContext.builder()
                .sourceKind("upload")
                .fileUrl(record.getFileUrl())
                .fileName(record.getFileName())
                .fileType(record.getFileType())
                .fileSize(record.getFileSize())
                .build();
    }

    public static AudioAnalysisContext fromMusic(Music music) {
        if (music == null) {
            return null;
        }
        return AudioAnalysisContext.builder()
                .sourceKind("library")
                .fileUrl(music.getFileUrl())
                .fileName(extractFileName(music.getFileUrl()))
                .fileType(extractFileType(music.getFileUrl()))
                .durationSeconds(music.getDuration())
                .title(music.getTitle())
                .artist(music.getArtist())
                .album(music.getAlbum())
                .genre(music.getGenre())
                .lyrics(music.getLyrics())
                .build();
    }

    public String buildDisplayName() {
        if (hasText(title)) {
            return title;
        }
        if (hasText(fileName)) {
            return fileName;
        }
        return "audio";
    }

    private static String extractFileName(String fileUrl) {
        if (!hasText(fileUrl)) {
            return null;
        }
        int index = fileUrl.lastIndexOf('/');
        return index >= 0 ? fileUrl.substring(index + 1) : fileUrl;
    }

    private static String extractFileType(String fileUrl) {
        String fileName = extractFileName(fileUrl);
        if (!hasText(fileName) || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
