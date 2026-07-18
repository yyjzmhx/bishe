package com.example.personalmusicsystem.service.ai;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.example.personalmusicsystem.entity.AIConfig;
import com.example.personalmusicsystem.mapper.AIConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Build AI chat clients from the active database config so admin changes
 * take effect without restarting the application.
 */
@Service
@Slf4j
public class DynamicAIChatService {

    private static final String PROVIDER_DASHSCOPE = "dashscope";
    private static final String DEFAULT_DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com";
    private static final String DEFAULT_DASHSCOPE_MODEL = "qwen-turbo";
    private static final double DEFAULT_TEMPERATURE = 0.7d;
    private static final int DEFAULT_TIMEOUT = 30000;

    @Autowired
    private AIConfigMapper aiConfigMapper;

    @Value("${spring.ai.dashscope.api-key:}")
    private String fallbackApiKey;

    @Value("${spring.ai.dashscope.base-url:" + DEFAULT_DASHSCOPE_BASE_URL + "}")
    private String fallbackBaseUrl;

    @Value("${spring.ai.dashscope.chat.options.model:" + DEFAULT_DASHSCOPE_MODEL + "}")
    private String fallbackModel;

    @Value("${spring.ai.dashscope.chat.options.temperature:" + DEFAULT_TEMPERATURE + "}")
    private Double fallbackTemperature;

    @Value("${ai.service.timeout:" + DEFAULT_TIMEOUT + "}")
    private Integer fallbackTimeout;

    public String prompt(String prompt) {
        AIConfig runtimeConfig = getRuntimeConfig();
        return prompt(runtimeConfig, prompt);
    }

    public String prompt(AIConfig config, String prompt) {
        validateConfig(config);
        ChatClient chatClient = createChatClient(config);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    public void testConnection(AIConfig config) {
        String response = prompt(config, "Reply with OK only.");
        if (!StringUtils.hasText(response)) {
            throw new IllegalStateException("AI service returned an empty response");
        }
    }

    public boolean hasUsableRuntimeConfig() {
        try {
            getRuntimeConfig();
            return true;
        } catch (RuntimeException e) {
            log.warn("AI runtime configuration is not usable: {}", e.getMessage());
            return false;
        }
    }

    public AIConfig getRuntimeConfig() {
        AIConfig activeConfig = aiConfigMapper.selectActive();
        AIConfig mergedConfig = mergeWithFallbacks(activeConfig);
        validateConfig(mergedConfig);
        return mergedConfig;
    }

    public boolean supports(String provider) {
        return PROVIDER_DASHSCOPE.equalsIgnoreCase(provider == null ? "" : provider.trim());
    }

    private AIConfig mergeWithFallbacks(AIConfig activeConfig) {
        AIConfig merged = new AIConfig();

        if (activeConfig != null && StringUtils.hasText(activeConfig.getProvider())) {
            merged.setProvider(activeConfig.getProvider().trim());
        } else {
            merged.setProvider(PROVIDER_DASHSCOPE);
        }

        merged.setApiKey(firstNonBlank(
                activeConfig == null ? null : activeConfig.getApiKey(),
                fallbackApiKey
        ));
        merged.setBaseUrl(firstNonBlank(
                activeConfig == null ? null : activeConfig.getBaseUrl(),
                fallbackBaseUrl,
                DEFAULT_DASHSCOPE_BASE_URL
        ));
        merged.setBaseUrl(normalizeDashScopeBaseUrl(merged.getBaseUrl()));
        merged.setModel(firstNonBlank(
                activeConfig == null ? null : activeConfig.getModel(),
                fallbackModel,
                DEFAULT_DASHSCOPE_MODEL
        ));
        merged.setTemperature(activeConfig != null && activeConfig.getTemperature() != null
                ? activeConfig.getTemperature()
                : BigDecimal.valueOf(fallbackTemperature == null ? DEFAULT_TEMPERATURE : fallbackTemperature));
        merged.setTimeout(activeConfig != null && activeConfig.getTimeout() != null
                ? activeConfig.getTimeout()
                : (fallbackTimeout == null ? DEFAULT_TIMEOUT : fallbackTimeout));
        merged.setAnalysisStrategy(activeConfig == null ? null : activeConfig.getAnalysisStrategy());
        merged.setIsActive(activeConfig != null && Boolean.TRUE.equals(activeConfig.getIsActive()));
        merged.setId(activeConfig == null ? null : activeConfig.getId());
        merged.setCreateTime(activeConfig == null ? null : activeConfig.getCreateTime());
        merged.setUpdateTime(activeConfig == null ? null : activeConfig.getUpdateTime());

        return merged;
    }

    private void validateConfig(AIConfig config) {
        if (config == null) {
            throw new IllegalStateException("No usable AI configuration was found");
        }
        if (!supports(config.getProvider())) {
            throw new IllegalStateException("Only DashScope is supported for dynamic runtime config: " + config.getProvider());
        }
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new IllegalStateException("AI API key is not configured");
        }
        if (!StringUtils.hasText(config.getModel())) {
            throw new IllegalStateException("AI model is not configured");
        }
    }

    private ChatClient createChatClient(AIConfig config) {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .baseUrl(normalizeDashScopeBaseUrl(config.getBaseUrl()))
                .apiKey(config.getApiKey())
                .restClientBuilder(buildRestClientBuilder(config.getTimeout()))
                .build();

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(config.getModel())
                .withTemperature(resolveTemperature(config.getTemperature()))
                .build();

        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(options)
                .build();

        return ChatClient.create(chatModel);
    }

    private RestClient.Builder buildRestClientBuilder(Integer timeoutMs) {
        int timeout = timeoutMs == null || timeoutMs <= 0 ? DEFAULT_TIMEOUT : timeoutMs;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(requestFactory);
    }

    private double resolveTemperature(BigDecimal temperature) {
        return temperature == null ? DEFAULT_TEMPERATURE : temperature.doubleValue();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    /**
     * The Spring AI Alibaba DashScope client appends the native
     * /api/v1/services/... path itself. Users often paste either
     * /api/v1 or /compatible-mode/v1 from other SDK examples, which would
     * otherwise create an invalid final URL.
     */
    private String normalizeDashScopeBaseUrl(String baseUrl) {
        String normalized = firstNonBlank(baseUrl, DEFAULT_DASHSCOPE_BASE_URL);
        if (!StringUtils.hasText(normalized)) {
            return DEFAULT_DASHSCOPE_BASE_URL;
        }

        normalized = normalized.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        normalized = normalized
                .replaceFirst("/compatible-mode/v1$", "")
                .replaceFirst("/api/v1$", "");

        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }

        return normalized;
    }
}
