package com.example.personalmusicsystem.service.ai;

import com.example.personalmusicsystem.service.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Extract tag and container metadata from uploaded audio files.
 */
@Service
@Slf4j
public class AudioMetadataExtractor {

    @Autowired
    private StorageService storageService;

    public AudioMetadataSnapshot extract(AudioAnalysisContext context) {
        if (context == null || !StringUtils.hasText(context.getFileUrl())) {
            return AudioMetadataSnapshot.builder().build();
        }

        String suffix = resolveSuffix(context);
        Path tempFile = null;
        try (InputStream inputStream = storageService.getFileInputStream(context.getFileUrl())) {
            tempFile = Files.createTempFile("audio-analysis-", suffix);
            Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            AudioFile audioFile = AudioFileIO.read(tempFile.toFile());
            AudioHeader header = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();

            return AudioMetadataSnapshot.builder()
                    .durationSeconds(resolveDuration(header, context))
                    .sampleRate(parseInt(header == null ? null : header.getSampleRate()))
                    .bitRateKbps(parseInt(header == null ? null : header.getBitRate()))
                    .channels(parseChannels(header == null ? null : header.getChannels()))
                    .formatName(normalize(header == null ? null : header.getFormat()))
                    .codecName(normalize(header == null ? null : header.getEncodingType()))
                    .title(firstNonBlank(readTag(tag, FieldKey.TITLE), context.getTitle()))
                    .artist(firstNonBlank(readTag(tag, FieldKey.ARTIST), context.getArtist()))
                    .album(firstNonBlank(readTag(tag, FieldKey.ALBUM), context.getAlbum()))
                    .genre(firstNonBlank(readTag(tag, FieldKey.GENRE), context.getGenre()))
                    .comment(readTag(tag, FieldKey.COMMENT))
                    .fromTag(tag != null)
                    .build();
        } catch (Exception e) {
            log.debug("Audio metadata extraction failed for {}", context.getFileUrl(), e);
            return AudioMetadataSnapshot.builder()
                    .durationSeconds(context.getDurationSeconds())
                    .title(context.getTitle())
                    .artist(context.getArtist())
                    .album(context.getAlbum())
                    .genre(context.getGenre())
                    .build();
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private Integer resolveDuration(AudioHeader header, AudioAnalysisContext context) {
        if (header != null && header.getTrackLength() > 0) {
            return header.getTrackLength();
        }
        return context == null ? null : context.getDurationSeconds();
    }

    private String readTag(Tag tag, FieldKey key) {
        if (tag == null || key == null) {
            return null;
        }
        try {
            return normalize(tag.getFirst(key));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseChannels(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("mono")) {
            return 1;
        }
        if (normalized.contains("stereo")) {
            return 2;
        }
        return parseInt(normalized);
    }

    private String resolveSuffix(AudioAnalysisContext context) {
        if (context == null || !StringUtils.hasText(context.getFileName()) || !context.getFileName().contains(".")) {
            return ".tmp";
        }
        return context.getFileName().substring(context.getFileName().lastIndexOf('.'));
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.debug("Failed to delete temp audio file {}", tempFile, e);
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
