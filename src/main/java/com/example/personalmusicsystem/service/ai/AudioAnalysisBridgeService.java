package com.example.personalmusicsystem.service.ai;

import com.example.personalmusicsystem.service.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Bridges Java storage with the external Python analysis service via temp files.
 */
@Service
@Slf4j
public class AudioAnalysisBridgeService {

    @Autowired
    private StorageService storageService;

    @Autowired
    private PythonAudioAnalysisClient pythonAudioAnalysisClient;

    public Map<String, Object> analyze(AudioAnalysisContext context) {
        if (context == null || !StringUtils.hasText(context.getFileUrl())) {
            throw new IllegalArgumentException("Audio analysis context or file URL is empty");
        }
        if (!pythonAudioAnalysisClient.isEnabled()) {
            throw new IllegalStateException("Python audio analysis service is disabled");
        }

        Path tempFile = null;
        try (InputStream inputStream = storageService.getFileInputStream(context.getFileUrl())) {
            tempFile = Files.createTempFile("music-audio-analysis-", resolveSuffix(context));
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return pythonAudioAnalysisClient.analyzeFile(tempFile.toAbsolutePath().toString(), context);
        } catch (Exception e) {
            throw new IllegalStateException("Python audio analysis bridge failed: " + e.getMessage(), e);
        } finally {
            deleteTempFile(tempFile);
        }
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
            log.debug("Failed to delete temp audio bridge file {}", tempFile, e);
        }
    }
}
