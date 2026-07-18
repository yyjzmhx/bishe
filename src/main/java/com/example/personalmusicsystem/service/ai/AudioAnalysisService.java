package com.example.personalmusicsystem.service.ai;

import com.alibaba.fastjson2.JSON;
import com.example.personalmusicsystem.entity.AIConfig;
import com.example.personalmusicsystem.mapper.AIConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Audio feature extraction and recommendation-oriented similarity analysis.
 */
@Service
@Slf4j
public class AudioAnalysisService {

    private static final List<Double> DEFAULT_VECTOR = List.of(0.5, 0.5, 0.5, 0.5, 0.5);
    private static final List<String> DEFAULT_VECTOR_LABELS = List.of(
            "能量强度",
            "节奏活跃度",
            "音色明亮度",
            "动态起伏",
            "纹理复杂度"
    );

    @Autowired
    private DynamicAIChatService dynamicAIChatService;

    @Autowired
    private AIConfigMapper aiConfigMapper;

    @Autowired
    private LocalAudioFeatureExtractor localAudioFeatureExtractor;

    @Autowired
    private AudioAnalysisBridgeService audioAnalysisBridgeService;

    @Value("${ai.service.analysis-strategy:description}")
    private String fallbackAnalysisStrategy;

    public String extractFeatures(String fileUrl) {
        return extractFeatures(AudioAnalysisContext.builder()
                .sourceKind("upload")
                .fileUrl(fileUrl)
                .build());
    }

    public String extractFeatures(AudioAnalysisContext context) {
        try {
            AIConfig activeConfig = aiConfigMapper.selectActive();
            String strategy = resolveAnalysisStrategy(activeConfig);

            try {
                Map<String, Object> pythonResult = audioAnalysisBridgeService.analyze(context);
                pythonResult.putIfAbsent("analysisStrategy", strategy);
                pythonResult.putIfAbsent("sourceKind", context == null ? null : context.getSourceKind());
                pythonResult.putIfAbsent("fileName", context == null ? null : context.getFileName());
                pythonResult.putIfAbsent("fileType", context == null ? null : context.getFileType());
                pythonResult.putIfAbsent("fileSize", context == null ? null : context.getFileSize());
                pythonResult.putIfAbsent("generatedAt", LocalDateTime.now().toString());
                if (activeConfig != null) {
                    pythonResult.putIfAbsent("provider", activeConfig.getProvider());
                    pythonResult.putIfAbsent("model", activeConfig.getModel());
                }
                return JSON.toJSONString(pythonResult);
            } catch (Exception pythonError) {
                log.warn("Python audio analysis failed, fallback to local extractor: {}", pythonError.getMessage());
            }

            AudioFeatureProfile localProfile = localAudioFeatureExtractor.extract(context);
            StructuredDescription structuredDescription = buildStructuredDescription(context, localProfile, strategy);

            Map<String, Object> featureData = new LinkedHashMap<>();
            featureData.put("style", structuredDescription.style());
            featureData.put("emotion", structuredDescription.emotion());
            featureData.put("rhythm", structuredDescription.rhythm());
            featureData.put("instruments", structuredDescription.instruments());
            featureData.put("atmosphere", structuredDescription.atmosphere());
            featureData.put("description", structuredDescription.description());
            featureData.put("keywords", structuredDescription.keywords());
            featureData.put("vector", normalizeVector(localProfile.getVector()));
            featureData.put("vectorLabels", localProfile.getVectorLabels() == null
                    ? DEFAULT_VECTOR_LABELS
                    : localProfile.getVectorLabels());
            featureData.put("technicalSummary", localProfile.getTechnicalSummary());
            featureData.put("extractionMode", localProfile.getExtractionMode());
            featureData.put("analysisStrategy", strategy);
            featureData.put("durationSeconds", resolveDuration(localProfile.getDurationSeconds(), context));
            featureData.put("sampleRate", localProfile.getSampleRate());
            featureData.put("bitRateKbps", localProfile.getBitRateKbps());
            featureData.put("channels", localProfile.getChannels());
            featureData.put("formatName", localProfile.getFormatName());
            featureData.put("codecName", localProfile.getCodecName());
            featureData.put("tagTitle", localProfile.getTagTitle());
            featureData.put("tagArtist", localProfile.getTagArtist());
            featureData.put("tagAlbum", localProfile.getTagAlbum());
            featureData.put("tagGenre", localProfile.getTagGenre());
            featureData.put("tagComment", localProfile.getTagComment());
            featureData.put("fileName", context == null ? null : context.getFileName());
            featureData.put("fileType", context == null ? null : context.getFileType());
            featureData.put("fileSize", context == null ? null : context.getFileSize());
            featureData.put("sourceKind", context == null ? null : context.getSourceKind());
            featureData.put("aiEnhanced", structuredDescription.aiEnhanced());
            featureData.put("generatedAt", LocalDateTime.now().toString());

            if (activeConfig != null) {
                featureData.put("provider", activeConfig.getProvider());
                featureData.put("model", activeConfig.getModel());
            }

            return JSON.toJSONString(featureData);
        } catch (Exception e) {
            log.error("Failed to extract audio features", e);
            throw new RuntimeException("音频特征提取失败: " + e.getMessage(), e);
        }
    }

    public double calculateSimilarity(String features1Json, String features2Json) {
        ParsedFeatures first = parseFeatures(features1Json);
        ParsedFeatures second = parseFeatures(features2Json);

        double vectorSimilarity = cosineSimilarity(first.vector(), second.vector());
        double textSimilarity = calculateTextSimilarity(first.searchText(), second.searchText());
        double labelSimilarity = exactLabelSimilarity(first, second);
        double durationSimilarity = calculateDurationSimilarity(first.durationSeconds(), second.durationSeconds());
        double metadataSimilarity = calculateMetadataSimilarity(first, second);

        boolean hasVectorData = hasMeaningfulVector(first.vector()) && hasMeaningfulVector(second.vector());
        boolean hasTextData = StringUtils.hasText(first.searchText()) && StringUtils.hasText(second.searchText());

        double similarity;
        if (hasVectorData && hasTextData) {
            similarity = (vectorSimilarity * 0.60d)
                    + (textSimilarity * 0.16d)
                    + (labelSimilarity * 0.10d)
                    + (durationSimilarity * 0.07d)
                    + (metadataSimilarity * 0.07d);
        } else if (hasVectorData) {
            similarity = (vectorSimilarity * 0.76d)
                    + (labelSimilarity * 0.12d)
                    + (durationSimilarity * 0.06d)
                    + (metadataSimilarity * 0.06d);
        } else if (hasTextData) {
            similarity = (textSimilarity * 0.68d)
                    + (labelSimilarity * 0.18d)
                    + (durationSimilarity * 0.07d)
                    + (metadataSimilarity * 0.07d);
        } else {
            similarity = 0.5d;
        }

        return clamp(similarity);
    }

    public double calculateSemanticSimilarity(String description1, String description2) {
        return calculateTextSimilarity(description1, description2);
    }

    public Integer extractDurationSeconds(String featuresJson) {
        ParsedFeatures parsedFeatures = parseFeatures(featuresJson);
        return parsedFeatures.durationSeconds();
    }

    public String extractStyleLabel(String featuresJson) {
        ParsedFeatures parsedFeatures = parseFeatures(featuresJson);
        return parsedFeatures.style();
    }

    private StructuredDescription buildStructuredDescription(AudioAnalysisContext context,
                                                             AudioFeatureProfile localProfile,
                                                             String strategy) {
        StructuredDescription fallbackDescription = buildFallbackDescription(context, localProfile, strategy);

        if (!dynamicAIChatService.hasUsableRuntimeConfig()) {
            return fallbackDescription;
        }

        try {
            String prompt = buildAiPrompt(context, localProfile, fallbackDescription, strategy);
            String response = dynamicAIChatService.prompt(prompt);
            String responseJson = extractJsonFromResponse(response);

            @SuppressWarnings("unchecked")
            Map<String, Object> aiResponse = JSON.parseObject(responseJson, Map.class);
            if (aiResponse == null || aiResponse.isEmpty()) {
                return fallbackDescription;
            }

            return mergeStructuredDescription(fallbackDescription, aiResponse, true);
        } catch (Exception e) {
            log.warn("AI enrichment failed, fallback to local description: {}", e.getMessage());
            return fallbackDescription;
        }
    }

    private StructuredDescription buildFallbackDescription(AudioAnalysisContext context,
                                                           AudioFeatureProfile localProfile,
                                                           String strategy) {
        List<Double> vector = normalizeVector(localProfile.getVector());
        double energy = vector.get(0);
        double rhythmScore = vector.get(1);
        double brightness = vector.get(2);
        double dynamicRange = vector.get(3);
        double complexity = vector.get(4);

        String style = firstNonBlank(
                context == null ? null : context.getGenre(),
                inferStyle(energy, rhythmScore, brightness, dynamicRange)
        );
        String emotion = inferEmotion(energy, brightness, dynamicRange);
        String rhythm = inferRhythm(rhythmScore, energy);
        String atmosphere = inferAtmosphere(energy, brightness, dynamicRange, complexity);
        List<String> instruments = inferInstruments(context, brightness, dynamicRange, rhythmScore);
        List<String> keywords = buildKeywords(style, emotion, rhythm, atmosphere, context, localProfile, strategy);
        String description = buildDescription(context, localProfile, style, emotion, rhythm, atmosphere, strategy);

        return new StructuredDescription(
                style,
                emotion,
                rhythm,
                instruments,
                atmosphere,
                description,
                keywords,
                false
        );
    }

    private StructuredDescription mergeStructuredDescription(StructuredDescription fallback,
                                                             Map<String, Object> aiResponse,
                                                             boolean aiEnhanced) {
        String style = firstNonBlank(asString(aiResponse.get("style")), fallback.style());
        String emotion = firstNonBlank(asString(aiResponse.get("emotion")), fallback.emotion());
        String rhythm = firstNonBlank(asString(aiResponse.get("rhythm")), fallback.rhythm());
        String atmosphere = firstNonBlank(asString(aiResponse.get("atmosphere")), fallback.atmosphere());
        String description = firstNonBlank(asString(aiResponse.get("description")), fallback.description());
        List<String> instruments = mergeStringList(aiResponse.get("instruments"), fallback.instruments());
        List<String> keywords = mergeStringList(aiResponse.get("keywords"), fallback.keywords());

        return new StructuredDescription(
                style,
                emotion,
                rhythm,
                instruments,
                atmosphere,
                description,
                keywords,
                aiEnhanced
        );
    }

    private String buildAiPrompt(AudioAnalysisContext context,
                                 AudioFeatureProfile localProfile,
                                 StructuredDescription fallback,
                                 String strategy) {
        List<Double> vector = normalizeVector(localProfile.getVector());

        return String.format("""
                你是音乐分析助手。
                你不能直接播放音频，但可以基于系统本地提取的技术特征和元数据，输出保守、可解释的音乐语义分析。

                请返回严格 JSON 对象，字段如下：
                {
                  "style": "音乐风格",
                  "emotion": "情绪色彩",
                  "rhythm": "节奏特征",
                  "instruments": ["乐器或音色1", "乐器或音色2"],
                  "atmosphere": "整体氛围",
                  "description": "80字以内的摘要",
                  "keywords": ["关键词1", "关键词2", "关键词3"]
                }

                分析策略: %s
                来源类型: %s
                文件名: %s
                标题: %s
                艺术家: %s
                专辑: %s
                已知风格: %s
                时长(秒): %s
                文件类型: %s
                本地特征向量标签: %s
                本地特征向量: %s
                技术摘要: %s
                歌词/文本线索: %s

                本地初步判断:
                style=%s
                emotion=%s
                rhythm=%s
                atmosphere=%s
                description=%s
                keywords=%s

                要求：
                1. 不要输出 markdown 代码块。
                2. 信息不足时用宽泛标签，不要捏造具体背景。
                3. instruments 保持 2 到 4 个条目，keywords 保持 3 到 6 个条目。
                4. 描述要体现节奏、能量、音色或氛围中的至少两个维度。
                """,
                strategy,
                context == null ? "unknown" : safeText(context.getSourceKind()),
                context == null ? "" : safeText(context.getFileName()),
                context == null ? "" : safeText(context.getTitle()),
                context == null ? "" : safeText(context.getArtist()),
                context == null ? "" : safeText(context.getAlbum()),
                context == null ? "" : safeText(context.getGenre()),
                resolveDuration(localProfile.getDurationSeconds(), context),
                context == null ? "" : safeText(context.getFileType()),
                localProfile.getVectorLabels(),
                vector,
                safeText(localProfile.getTechnicalSummary()),
                sanitizeTextPreview(context == null ? null : context.getLyrics()),
                fallback.style(),
                fallback.emotion(),
                fallback.rhythm(),
                fallback.atmosphere(),
                fallback.description(),
                fallback.keywords()
        );
    }

    private String resolveAnalysisStrategy(AIConfig activeConfig) {
        if (activeConfig != null && StringUtils.hasText(activeConfig.getAnalysisStrategy())) {
            return activeConfig.getAnalysisStrategy().trim();
        }
        return StringUtils.hasText(fallbackAnalysisStrategy)
                ? fallbackAnalysisStrategy.trim()
                : "description";
    }

    private String inferStyle(double energy, double rhythm, double brightness, double dynamicRange) {
        if (rhythm >= 0.72d && brightness >= 0.60d) {
            return "电子/流行";
        }
        if (energy >= 0.70d && brightness < 0.45d) {
            return "摇滚/说唱";
        }
        if (energy < 0.42d && dynamicRange >= 0.55d) {
            return "抒情/氛围";
        }
        if (brightness < 0.38d && rhythm < 0.45d) {
            return "轻音乐/环境";
        }
        return "流行";
    }

    private String inferEmotion(double energy, double brightness, double dynamicRange) {
        if (energy >= 0.75d && brightness >= 0.60d) {
            return "兴奋/积极";
        }
        if (energy <= 0.38d && brightness <= 0.42d) {
            return "平静/沉浸";
        }
        if (dynamicRange >= 0.62d) {
            return "抒情/张力";
        }
        return "舒展/自然";
    }

    private String inferRhythm(double rhythm, double energy) {
        if (rhythm >= 0.72d) {
            return "节奏推进明显";
        }
        if (rhythm >= 0.50d) {
            return "中等律动";
        }
        if (energy >= 0.58d) {
            return "稳定脉冲";
        }
        return "节奏舒缓";
    }

    private String inferAtmosphere(double energy, double brightness, double dynamicRange, double complexity) {
        if (energy < 0.40d && brightness < 0.45d) {
            return "安静沉浸";
        }
        if (brightness >= 0.62d && energy >= 0.60d) {
            return "明亮外放";
        }
        if (dynamicRange >= 0.65d || complexity >= 0.65d) {
            return "层次递进";
        }
        return "轻盈流畅";
    }

    private List<String> inferInstruments(AudioAnalysisContext context,
                                          double brightness,
                                          double dynamicRange,
                                          double rhythm) {
        LinkedHashSet<String> instruments = new LinkedHashSet<>();
        if (context != null && StringUtils.hasText(context.getLyrics())) {
            instruments.add("人声线条");
        }
        if (brightness >= 0.62d) {
            instruments.add("明亮合成器");
        } else {
            instruments.add("柔和铺底");
        }
        if (rhythm >= 0.60d) {
            instruments.add("节奏打击");
        }
        if (dynamicRange >= 0.60d) {
            instruments.add("旋律性主奏");
        }
        if (instruments.size() < 2) {
            instruments.add("旋律纹理");
        }
        return new ArrayList<>(instruments);
    }

    private List<String> buildKeywords(String style,
                                       String emotion,
                                       String rhythm,
                                       String atmosphere,
                                       AudioAnalysisContext context,
                                       AudioFeatureProfile localProfile,
                                       String strategy) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        addIfPresent(keywords, style);
        addIfPresent(keywords, emotion);
        addIfPresent(keywords, rhythm);
        addIfPresent(keywords, atmosphere);
        addIfPresent(keywords, context == null ? null : context.getGenre());
        addIfPresent(keywords, context == null ? null : context.getFileType());
        addIfPresent(keywords, localProfile == null ? null : localProfile.getExtractionMode());
        addIfPresent(keywords, "transcribe".equalsIgnoreCase(strategy) ? "文本引导" : "音频特征");
        return new ArrayList<>(keywords);
    }

    private String buildDescription(AudioAnalysisContext context,
                                    AudioFeatureProfile localProfile,
                                    String style,
                                    String emotion,
                                    String rhythm,
                                    String atmosphere,
                                    String strategy) {
        List<Double> vector = normalizeVector(localProfile.getVector());
        String duration = resolveDuration(localProfile.getDurationSeconds(), context) == null
                ? "时长未知"
                : "时长约 " + resolveDuration(localProfile.getDurationSeconds(), context) + " 秒";
        String metadataHint = firstNonBlank(
                context == null ? null : context.getTitle(),
                context == null ? null : context.getFileName(),
                "该音频"
        );

        String lyricHint = StringUtils.hasText(context == null ? null : context.getLyrics())
                ? "，并结合了现有歌词文本线索"
                : "";
        String strategyHint = "transcribe".equalsIgnoreCase(strategy)
                ? "文本引导分析"
                : "音频特征分析";

        return String.format(
                Locale.ROOT,
                "%s呈现出%s取向，整体情绪偏%s，节奏表现为%s，氛围上更接近%s。%s，系统以%s为主%s。",
                metadataHint,
                style,
                emotion,
                rhythm,
                atmosphere,
                duration,
                strategyHint,
                lyricHint
        );
    }

    private ParsedFeatures parseFeatures(String featuresJson) {
        if (!StringUtils.hasText(featuresJson)) {
            return new ParsedFeatures(DEFAULT_VECTOR, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> featureMap = JSON.parseObject(featuresJson, Map.class);
            if (featureMap != null && !featureMap.isEmpty()) {
                List<Double> vector = normalizeVector(convertToDoubleList(featureMap.get("vector")));
                String style = asString(featureMap.get("style"));
                String emotion = asString(featureMap.get("emotion"));
                String rhythm = asString(featureMap.get("rhythm"));
                String atmosphere = asString(featureMap.get("atmosphere"));
                String description = asString(featureMap.get("description"));
                String genre = asString(featureMap.get("genre"));
                Integer duration = parseInteger(featureMap.get("durationSeconds"));
                String tagTitle = asString(featureMap.get("tagTitle"));
                String tagArtist = asString(featureMap.get("tagArtist"));
                String tagAlbum = asString(featureMap.get("tagAlbum"));
                String tagGenre = asString(featureMap.get("tagGenre"));
                String codecName = asString(featureMap.get("codecName"));
                String formatName = asString(featureMap.get("formatName"));

                String searchText = joinText(
                        style,
                        emotion,
                        rhythm,
                        atmosphere,
                        description,
                        genre,
                        tagTitle,
                        tagArtist,
                        tagAlbum,
                        tagGenre,
                        codecName,
                        formatName,
                        flattenToString(featureMap.get("keywords")),
                        flattenToString(featureMap.get("instruments")),
                        asString(featureMap.get("technicalSummary"))
                );

                return new ParsedFeatures(
                        vector,
                        style,
                        emotion,
                        rhythm,
                        atmosphere,
                        description,
                        searchText,
                        duration,
                        tagTitle,
                        tagArtist,
                        tagAlbum,
                        tagGenre,
                        codecName,
                        formatName
                );
            }
        } catch (Exception e) {
            log.debug("Feature JSON is not an object", e);
        }

        try {
            List<Object> rawList = JSON.parseArray(featuresJson);
            if (rawList != null && !rawList.isEmpty()) {
                return new ParsedFeatures(
                        normalizeVector(convertToDoubleList(rawList)),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
            }
        } catch (Exception e) {
            log.debug("Feature JSON is not an array", e);
        }

        return new ParsedFeatures(DEFAULT_VECTOR, null, null, null, null, null, featuresJson, null, null, null, null, null, null, null);
    }

    private List<Double> normalizeVector(List<Double> vector) {
        List<Double> result = new ArrayList<>();
        if (vector != null) {
            for (Double item : vector) {
                result.add(item == null ? 0.5d : clamp(item));
            }
        }
        while (result.size() < DEFAULT_VECTOR.size()) {
            result.add(0.5d);
        }
        if (result.size() > DEFAULT_VECTOR.size()) {
            result = new ArrayList<>(result.subList(0, DEFAULT_VECTOR.size()));
        }
        return result;
    }

    private List<Double> convertToDoubleList(Object value) {
        List<Double> vector = new ArrayList<>();
        if (value instanceof List<?> rawList) {
            for (Object item : rawList) {
                if (item instanceof Number number) {
                    vector.add(number.doubleValue());
                } else if (item instanceof String stringValue) {
                    try {
                        vector.add(Double.parseDouble(stringValue));
                    } catch (NumberFormatException ignored) {
                        vector.add(0.5d);
                    }
                }
            }
        }
        return vector.isEmpty() ? new ArrayList<>(DEFAULT_VECTOR) : vector;
    }

    private double cosineSimilarity(List<Double> firstVector, List<Double> secondVector) {
        List<Double> first = normalizeVector(firstVector);
        List<Double> second = normalizeVector(secondVector);

        double dotProduct = 0.0d;
        double firstNorm = 0.0d;
        double secondNorm = 0.0d;

        for (int index = 0; index < first.size(); index++) {
            dotProduct += first.get(index) * second.get(index);
            firstNorm += first.get(index) * first.get(index);
            secondNorm += second.get(index) * second.get(index);
        }

        if (firstNorm == 0.0d || secondNorm == 0.0d) {
            return 0.0d;
        }

        return clamp(dotProduct / (Math.sqrt(firstNorm) * Math.sqrt(secondNorm)));
    }

    private double calculateTextSimilarity(String firstText, String secondText) {
        if (!StringUtils.hasText(firstText) || !StringUtils.hasText(secondText)) {
            return 0.0d;
        }

        Set<String> firstTokens = tokenize(firstText);
        Set<String> secondTokens = tokenize(secondText);
        if (firstTokens.isEmpty() || secondTokens.isEmpty()) {
            return 0.0d;
        }

        Set<String> intersection = new LinkedHashSet<>(firstTokens);
        intersection.retainAll(secondTokens);

        Set<String> union = new LinkedHashSet<>(firstTokens);
        union.addAll(secondTokens);

        return union.isEmpty() ? 0.0d : clamp(intersection.size() / (double) union.size());
    }

    private double exactLabelSimilarity(ParsedFeatures first, ParsedFeatures second) {
        int matched = 0;
        int compared = 0;

        compared += matchField(first.style(), second.style()) ? 1 : 1;
        if (matchField(first.style(), second.style())) {
            matched++;
        }

        compared += matchField(first.emotion(), second.emotion()) ? 1 : 1;
        if (matchField(first.emotion(), second.emotion())) {
            matched++;
        }

        compared += matchField(first.rhythm(), second.rhythm()) ? 1 : 1;
        if (matchField(first.rhythm(), second.rhythm())) {
            matched++;
        }

        compared += matchField(first.atmosphere(), second.atmosphere()) ? 1 : 1;
        if (matchField(first.atmosphere(), second.atmosphere())) {
            matched++;
        }

        if (compared == 0) {
            return 0.0d;
        }
        return matched / (double) compared;
    }

    private double calculateDurationSimilarity(Integer firstDuration, Integer secondDuration) {
        if (firstDuration == null || secondDuration == null || firstDuration <= 0 || secondDuration <= 0) {
            return 0.5d;
        }
        double diff = Math.abs(firstDuration - secondDuration);
        double max = Math.max(firstDuration, secondDuration);
        return clamp(1.0d - (diff / max));
    }

    private double calculateMetadataSimilarity(ParsedFeatures first, ParsedFeatures second) {
        int matched = 0;
        int compared = 0;

        String[] left = {first.tagArtist(), first.tagAlbum(), first.tagGenre(), first.codecName(), first.formatName()};
        String[] right = {second.tagArtist(), second.tagAlbum(), second.tagGenre(), second.codecName(), second.formatName()};

        for (int i = 0; i < left.length; i++) {
            if (StringUtils.hasText(left[i]) && StringUtils.hasText(right[i])) {
                compared++;
                if (matchField(left[i], right[i])) {
                    matched++;
                }
            }
        }

        if (compared == 0) {
            return 0.5d;
        }
        return matched / (double) compared;
    }

    private boolean matchField(String first, String second) {
        if (!StringUtils.hasText(first) || !StringUtils.hasText(second)) {
            return false;
        }
        String normalizedFirst = first.trim().toLowerCase(Locale.ROOT);
        String normalizedSecond = second.trim().toLowerCase(Locale.ROOT);
        return normalizedFirst.equals(normalizedSecond)
                || normalizedFirst.contains(normalizedSecond)
                || normalizedSecond.contains(normalizedFirst);
    }

    private Set<String> tokenize(String text) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        if (!StringUtils.hasText(text)) {
            return tokens;
        }

        String normalized = text.toLowerCase(Locale.ROOT)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('，', ' ')
                .replace('。', ' ')
                .replace('、', ' ')
                .replace('/', ' ')
                .replace('|', ' ');

        for (String chunk : normalized.split("\\s+")) {
            if (!StringUtils.hasText(chunk)) {
                continue;
            }
            String trimmed = chunk.trim();
            if (trimmed.length() > 1) {
                tokens.add(trimmed);
            }

            StringBuilder latinToken = new StringBuilder();
            for (int index = 0; index < trimmed.length(); index++) {
                char currentChar = trimmed.charAt(index);
                if (Character.UnicodeScript.of(currentChar) == Character.UnicodeScript.HAN) {
                    tokens.add(String.valueOf(currentChar));
                    continue;
                }
                if (Character.isLetterOrDigit(currentChar)) {
                    latinToken.append(currentChar);
                } else if (!latinToken.isEmpty()) {
                    if (latinToken.length() > 1) {
                        tokens.add(latinToken.toString());
                    }
                    latinToken.setLength(0);
                }
            }
            if (!latinToken.isEmpty() && latinToken.length() > 1) {
                tokens.add(latinToken.toString());
            }
        }

        return tokens;
    }

    private String extractJsonFromResponse(String response) {
        if (!StringUtils.hasText(response)) {
            return "{}";
        }

        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }

        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                String extracted = response.substring(start, end).trim();
                if (extracted.startsWith("{")) {
                    return extracted;
                }
            }
        }

        Matcher matcher = Pattern.compile("\\{.*}", Pattern.DOTALL).matcher(response);
        if (matcher.find()) {
            return matcher.group().trim();
        }

        return response.trim();
    }

    private List<String> mergeStringList(Object candidate, List<String> fallback) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (fallback != null) {
            for (String item : fallback) {
                addIfPresent(merged, item);
            }
        }
        if (candidate instanceof List<?> rawList) {
            for (Object item : rawList) {
                addIfPresent(merged, asString(item));
            }
        } else {
            addIfPresent(merged, asString(candidate));
        }
        return new ArrayList<>(merged);
    }

    private String flattenToString(Object value) {
        if (value instanceof List<?> rawList) {
            List<String> values = new ArrayList<>();
            for (Object item : rawList) {
                String text = asString(item);
                if (StringUtils.hasText(text)) {
                    values.add(text);
                }
            }
            return String.join(" ", values);
        }
        return asString(value);
    }

    private String joinText(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer resolveDuration(Integer profileDuration, AudioAnalysisContext context) {
        if (profileDuration != null && profileDuration > 0) {
            return profileDuration;
        }
        if (context != null && context.getDurationSeconds() != null && context.getDurationSeconds() > 0) {
            return context.getDurationSeconds();
        }
        return null;
    }

    private boolean hasMeaningfulVector(List<Double> vector) {
        List<Double> normalized = normalizeVector(vector);
        for (Double item : normalized) {
            if (Math.abs(item - 0.5d) > 0.0001d) {
                return true;
            }
        }
        return false;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String sanitizeTextPreview(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }

    private void addIfPresent(Set<String> target, String value) {
        if (StringUtils.hasText(value)) {
            target.add(value.trim());
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private record StructuredDescription(String style,
                                         String emotion,
                                         String rhythm,
                                         List<String> instruments,
                                         String atmosphere,
                                         String description,
                                         List<String> keywords,
                                         boolean aiEnhanced) {
    }

    private record ParsedFeatures(List<Double> vector,
                                  String style,
                                  String emotion,
                                  String rhythm,
                                  String atmosphere,
                                  String description,
                                  String searchText,
                                  Integer durationSeconds,
                                  String tagTitle,
                                  String tagArtist,
                                  String tagAlbum,
                                  String tagGenre,
                                  String codecName,
                                  String formatName) {
    }
}
