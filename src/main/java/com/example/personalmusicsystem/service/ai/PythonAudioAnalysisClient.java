package com.example.personalmusicsystem.service.ai;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP client for the external Python audio analysis service.
 */
@Service
@Slf4j
public class PythonAudioAnalysisClient {

    private final RestClient restClient;
    private final String analyzePath;
    private final boolean enabled;

    public PythonAudioAnalysisClient(
            @Value("${audio.analysis.python.base-url:http://127.0.0.1:9008}") String baseUrl,
            @Value("${audio.analysis.python.analyze-path:/analyze}") String analyzePath,
            @Value("${audio.analysis.python.enabled:true}") boolean enabled,
            @Value("${audio.analysis.python.timeout-ms:120000}") int timeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .requestFactory(requestFactory)
                .build();
        this.analyzePath = analyzePath;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> analyzeFile(String filePath, AudioAnalysisContext context) {
        if (!enabled) {
            throw new IllegalStateException("Python audio analysis service is disabled");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("file_path", filePath);
        payload.put("file_name", context == null ? null : context.getFileName());
        payload.put("title", context == null ? null : context.getTitle());
        payload.put("artist", context == null ? null : context.getArtist());
        payload.put("album", context == null ? null : context.getAlbum());
        payload.put("genre", context == null ? null : context.getGenre());
        payload.put("lyrics", context == null ? null : context.getLyrics());
        payload.put("source_kind", context == null ? null : context.getSourceKind());

        String response = restClient.post()
                .uri(analyzePath)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException("Python audio analysis service returned empty response");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = JSON.parseObject(response, Map.class);
        if (result == null || result.isEmpty()) {
            throw new IllegalStateException("Python audio analysis service returned invalid JSON");
        }
        return result;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "http://127.0.0.1:9008";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
