package com.example.personalmusicsystem.service.ai;

import com.example.personalmusicsystem.service.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Extract deterministic local features from audio content before AI enrichment.
 */
@Service
@Slf4j
public class LocalAudioFeatureExtractor {

    private static final int MAX_PCM_SAMPLES = 65536;
    private static final int MAX_BYTE_WINDOW = 262144;
    private static final int SPECTRUM_SIZE = 1024;
    private static final List<String> VECTOR_LABELS = List.of(
            "能量强度",
            "节奏活跃度",
            "音色明亮度",
            "动态起伏",
            "纹理复杂度"
    );
    private static final List<Double> DEFAULT_VECTOR = List.of(0.5, 0.5, 0.5, 0.5, 0.5);

    @Autowired
    private StorageService storageService;

    @Autowired
    private AudioMetadataExtractor audioMetadataExtractor;

    public AudioFeatureProfile extract(AudioAnalysisContext context) {
        AudioMetadataSnapshot metadata = audioMetadataExtractor.extract(context);
        AudioFeatureProfile pcmProfile = tryExtractFromPcm(context);
        if (pcmProfile != null) {
            return enrichWithMetadata(pcmProfile, metadata, context);
        }

        AudioFeatureProfile byteProfile = tryExtractFromBytes(context);
        if (byteProfile != null) {
            return enrichWithMetadata(byteProfile, metadata, context);
        }

        return enrichWithMetadata(AudioFeatureProfile.builder()
                .extractionMode("default")
                .durationSeconds(context == null ? null : context.getDurationSeconds())
                .vector(new ArrayList<>(DEFAULT_VECTOR))
                .vectorLabels(VECTOR_LABELS)
                .technicalSummary("mode=default, fallback profile used")
                .build(), metadata, context);
    }

    private AudioFeatureProfile tryExtractFromPcm(AudioAnalysisContext context) {
        if (context == null || !hasText(context.getFileUrl())) {
            return null;
        }

        try (InputStream rawInput = storageService.getFileInputStream(context.getFileUrl());
             BufferedInputStream bufferedInput = new BufferedInputStream(rawInput);
             AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bufferedInput)) {

            AudioFormat sourceFormat = sourceStream.getFormat();
            AudioFormat targetFormat = buildTargetFormat(sourceFormat);
            if (targetFormat == null) {
                return null;
            }

            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)) {
                return extractFromPcmStream(pcmStream, targetFormat, context);
            }
        } catch (UnsupportedAudioFileException e) {
            log.debug("PCM extraction unsupported for {}", context.getFileUrl(), e);
            return null;
        } catch (Exception e) {
            log.debug("PCM extraction failed for {}", context.getFileUrl(), e);
            return null;
        }
    }

    private AudioFeatureProfile extractFromPcmStream(AudioInputStream pcmStream,
                                                     AudioFormat format,
                                                     AudioAnalysisContext context) throws IOException {
        int channels = Math.max(format.getChannels(), 1);
        float sampleRate = format.getSampleRate() > 0 ? format.getSampleRate() : 44100f;
        int frameSize = Math.max(format.getFrameSize(), channels * 2);
        byte[] buffer = new byte[Math.max(frameSize * 1024, 4096)];
        double[] samples = new double[MAX_PCM_SAMPLES];
        int sampleCount = 0;

        int bytesRead;
        while ((bytesRead = pcmStream.read(buffer)) != -1 && sampleCount < MAX_PCM_SAMPLES) {
            int usableBytes = bytesRead - (bytesRead % frameSize);
            for (int offset = 0; offset < usableBytes && sampleCount < MAX_PCM_SAMPLES; offset += frameSize) {
                double mixedSample = 0.0;
                for (int channel = 0; channel < channels; channel++) {
                    int byteIndex = offset + channel * 2;
                    int low = buffer[byteIndex] & 0xFF;
                    int high = buffer[byteIndex + 1];
                    short sample = (short) ((high << 8) | low);
                    mixedSample += sample / 32768.0d;
                }
                samples[sampleCount++] = mixedSample / channels;
            }
        }

        if (sampleCount < 256) {
            return null;
        }

        double[] normalizedSamples = Arrays.copyOf(samples, sampleCount);
        AudioMetrics metrics = buildPcmMetrics(normalizedSamples, sampleRate);
        Integer durationSeconds = resolveDurationSeconds(pcmStream.getFrameLength(), sampleRate, sampleCount, context);

        return buildProfile("pcm", durationSeconds, Math.round(sampleRate), metrics);
    }

    private AudioFeatureProfile tryExtractFromBytes(AudioAnalysisContext context) {
        if (context == null || !hasText(context.getFileUrl())) {
            return null;
        }

        try (InputStream inputStream = storageService.getFileInputStream(context.getFileUrl())) {
            byte[] bytes = readWindow(inputStream, MAX_BYTE_WINDOW);
            if (bytes.length == 0) {
                return null;
            }

            AudioMetrics metrics = buildByteMetrics(bytes);
            return buildProfile("byte-fallback", context.getDurationSeconds(), null, metrics);
        } catch (Exception e) {
            log.warn("Byte-level extraction failed for {}", context.getFileUrl(), e);
            return null;
        }
    }

    private AudioFeatureProfile buildProfile(String mode,
                                             Integer durationSeconds,
                                             Integer sampleRate,
                                             AudioMetrics metrics) {
        List<Double> vector = List.of(
                round(metrics.energy),
                round(metrics.rhythm),
                round(metrics.brightness),
                round(metrics.dynamicRange),
                round(metrics.complexity)
        );

        String durationToken = durationSeconds == null ? "unknown" : String.valueOf(durationSeconds);
        String sampleRateToken = sampleRate == null ? "unknown" : String.valueOf(sampleRate);
        String summary = String.format(
                Locale.ROOT,
                "mode=%s, duration=%ss, sampleRate=%s, energy=%.2f, rhythm=%.2f, brightness=%.2f, dynamics=%.2f, complexity=%.2f",
                mode,
                durationToken,
                sampleRateToken,
                metrics.energy,
                metrics.rhythm,
                metrics.brightness,
                metrics.dynamicRange,
                metrics.complexity
        );

        return AudioFeatureProfile.builder()
                .extractionMode(mode)
                .durationSeconds(durationSeconds)
                .sampleRate(sampleRate)
                .vector(vector)
                .vectorLabels(VECTOR_LABELS)
                .technicalSummary(summary)
                .build();
    }

    private AudioFeatureProfile enrichWithMetadata(AudioFeatureProfile profile,
                                                   AudioMetadataSnapshot metadata,
                                                   AudioAnalysisContext context) {
        if (profile == null) {
            return null;
        }

        AudioFeatureProfile.AudioFeatureProfileBuilder builder = AudioFeatureProfile.builder()
                .extractionMode(profile.getExtractionMode())
                .durationSeconds(firstNonNull(
                        profile.getDurationSeconds(),
                        metadata == null ? null : metadata.getDurationSeconds(),
                        context == null ? null : context.getDurationSeconds()
                ))
                .sampleRate(firstNonNull(
                        profile.getSampleRate(),
                        metadata == null ? null : metadata.getSampleRate()
                ))
                .bitRateKbps(metadata == null ? null : metadata.getBitRateKbps())
                .channels(metadata == null ? null : metadata.getChannels())
                .formatName(metadata == null ? null : metadata.getFormatName())
                .codecName(metadata == null ? null : metadata.getCodecName())
                .tagTitle(metadata == null ? null : metadata.getTitle())
                .tagArtist(metadata == null ? null : metadata.getArtist())
                .tagAlbum(metadata == null ? null : metadata.getAlbum())
                .tagGenre(metadata == null ? null : metadata.getGenre())
                .tagComment(metadata == null ? null : metadata.getComment())
                .vector(profile.getVector())
                .vectorLabels(profile.getVectorLabels())
                .technicalSummary(mergeTechnicalSummary(profile.getTechnicalSummary(), metadata));

        return builder.build();
    }

    private String mergeTechnicalSummary(String baseSummary, AudioMetadataSnapshot metadata) {
        if (metadata == null) {
            return baseSummary;
        }

        List<String> parts = new ArrayList<>();
        if (hasText(baseSummary)) {
            parts.add(baseSummary);
        }
        if (metadata.getFormatName() != null) {
            parts.add("format=" + metadata.getFormatName());
        }
        if (metadata.getCodecName() != null) {
            parts.add("codec=" + metadata.getCodecName());
        }
        if (metadata.getBitRateKbps() != null) {
            parts.add("bitrate=" + metadata.getBitRateKbps() + "kbps");
        }
        if (metadata.getChannels() != null) {
            parts.add("channels=" + metadata.getChannels());
        }
        if (metadata.getTitle() != null) {
            parts.add("tagTitle=" + metadata.getTitle());
        }
        if (metadata.getArtist() != null) {
            parts.add("tagArtist=" + metadata.getArtist());
        }
        if (metadata.getGenre() != null) {
            parts.add("tagGenre=" + metadata.getGenre());
        }

        return String.join(", ", parts);
    }

    private AudioMetrics buildPcmMetrics(double[] samples, float sampleRate) {
        double sumSquares = 0.0;
        double sumAbs = 0.0;
        double diffAbs = 0.0;
        int zeroCrossings = 0;
        double[] absoluteSamples = new double[samples.length];

        for (int i = 0; i < samples.length; i++) {
            double current = samples[i];
            double absolute = Math.abs(current);
            absoluteSamples[i] = absolute;
            sumSquares += current * current;
            sumAbs += absolute;

            if (i > 0) {
                double previous = samples[i - 1];
                diffAbs += Math.abs(current - previous);
                if ((current >= 0 && previous < 0) || (current < 0 && previous >= 0)) {
                    zeroCrossings++;
                }
            }
        }

        double rms = Math.sqrt(sumSquares / samples.length);
        double avgAbs = sumAbs / samples.length;
        double transientRate = diffAbs / Math.max(samples.length - 1, 1);
        double zeroCrossRate = zeroCrossings / (double) Math.max(samples.length - 1, 1);
        double dynamicRange = percentile(absoluteSamples, 0.92) - percentile(absoluteSamples, 0.10);
        double entropy = normalizedEntropy(absoluteSamples, 32);
        double spectralCentroid = spectralCentroid(samples, sampleRate);

        return new AudioMetrics(
                clamp((rms * 1.75d) + (avgAbs * 0.55d)),
                clamp((transientRate * 2.4d) + (zeroCrossRate * 2.2d)),
                clamp(spectralCentroid * 1.15d),
                clamp(dynamicRange * 1.8d),
                clamp((entropy * 0.55d) + (transientRate * 0.95d) + (zeroCrossRate * 0.45d))
        );
    }

    private AudioMetrics buildByteMetrics(byte[] bytes) {
        int[] histogram = new int[256];
        double sumAbs = 0.0;
        double sumSquares = 0.0;
        double diffAbs = 0.0;
        int min = 255;
        int max = 0;
        int occupiedBuckets = 0;
        int chunkSize = 1024;
        double previousChunkEnergy = 0.0;
        double chunkFlux = 0.0;

        for (int i = 0; i < bytes.length; i++) {
            int unsignedValue = bytes[i] & 0xFF;
            histogram[unsignedValue]++;

            double centered = Math.abs((unsignedValue - 128) / 128.0d);
            sumAbs += centered;
            sumSquares += centered * centered;
            min = Math.min(min, unsignedValue);
            max = Math.max(max, unsignedValue);

            if (i > 0) {
                int previous = bytes[i - 1] & 0xFF;
                diffAbs += Math.abs(unsignedValue - previous) / 255.0d;
            }
        }

        for (int value : histogram) {
            if (value > 0) {
                occupiedBuckets++;
            }
        }

        for (int offset = 0; offset < bytes.length; offset += chunkSize) {
            int end = Math.min(offset + chunkSize, bytes.length);
            double chunkEnergy = 0.0;
            for (int i = offset; i < end; i++) {
                int unsignedValue = bytes[i] & 0xFF;
                chunkEnergy += Math.abs((unsignedValue - 128) / 128.0d);
            }
            chunkEnergy /= Math.max(end - offset, 1);
            if (offset > 0) {
                chunkFlux += Math.abs(chunkEnergy - previousChunkEnergy);
            }
            previousChunkEnergy = chunkEnergy;
        }

        double avgAbs = sumAbs / bytes.length;
        double variance = (sumSquares / bytes.length) - (avgAbs * avgAbs);
        double adjacentDiff = diffAbs / Math.max(bytes.length - 1, 1);
        double entropy = normalizedEntropy(histogram, bytes.length);
        double uniqueBucketRatio = occupiedBuckets / 256.0d;
        double normalizedRange = (max - min) / 255.0d;
        double normalizedFlux = chunkFlux / Math.max((bytes.length / chunkSize), 1);

        return new AudioMetrics(
                clamp(avgAbs * 1.8d),
                clamp((normalizedFlux * 3.0d) + (adjacentDiff * 1.4d)),
                clamp((variance * 2.1d) + (adjacentDiff * 0.7d)),
                clamp((normalizedRange * 0.7d) + (variance * 1.4d)),
                clamp((entropy * 0.8d) + (uniqueBucketRatio * 0.2d))
        );
    }

    private AudioFormat buildTargetFormat(AudioFormat sourceFormat) {
        if (sourceFormat == null || sourceFormat.getChannels() <= 0 || sourceFormat.getSampleRate() <= 0) {
            return null;
        }

        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(),
                false
        );

        return AudioSystem.isConversionSupported(targetFormat, sourceFormat) ? targetFormat : null;
    }

    private Integer resolveDurationSeconds(long frameLength,
                                           float sampleRate,
                                           int sampleCount,
                                           AudioAnalysisContext context) {
        if (frameLength > 0 && sampleRate > 0) {
            return Math.max(1, Math.round(frameLength / sampleRate));
        }
        if (context != null && context.getDurationSeconds() != null && context.getDurationSeconds() > 0) {
            return context.getDurationSeconds();
        }
        if (sampleRate > 0 && sampleCount > 0) {
            return Math.max(1, Math.round(sampleCount / sampleRate));
        }
        return null;
    }

    private byte[] readWindow(InputStream inputStream, int maxBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int totalBytes = 0;
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1 && totalBytes < maxBytes) {
            int writable = Math.min(bytesRead, maxBytes - totalBytes);
            outputStream.write(buffer, 0, writable);
            totalBytes += writable;
        }

        return outputStream.toByteArray();
    }

    private double percentile(double[] values, double percentile) {
        if (values.length == 0) {
            return 0.0;
        }
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int index = (int) Math.round((sorted.length - 1) * percentile);
        index = Math.min(Math.max(index, 0), sorted.length - 1);
        return sorted[index];
    }

    private double spectralCentroid(double[] samples, float sampleRate) {
        int size = Math.min(SPECTRUM_SIZE, samples.length);
        if (size < 64 || sampleRate <= 0) {
            return 0.5d;
        }

        double weightedFrequency = 0.0;
        double magnitudeSum = 0.0;
        double[] windowed = new double[size];
        for (int i = 0; i < size; i++) {
            double window = 0.5d - 0.5d * Math.cos((2.0d * Math.PI * i) / (size - 1));
            windowed[i] = samples[i] * window;
        }

        int frequencyBins = size / 2;
        for (int frequencyIndex = 1; frequencyIndex < frequencyBins; frequencyIndex++) {
            double real = 0.0;
            double imaginary = 0.0;
            double angularFrequency = (2.0d * Math.PI * frequencyIndex) / size;
            for (int sampleIndex = 0; sampleIndex < size; sampleIndex++) {
                double angle = angularFrequency * sampleIndex;
                real += windowed[sampleIndex] * Math.cos(angle);
                imaginary -= windowed[sampleIndex] * Math.sin(angle);
            }

            double magnitude = Math.sqrt((real * real) + (imaginary * imaginary));
            double normalizedFrequency = frequencyIndex / (double) frequencyBins;
            weightedFrequency += normalizedFrequency * magnitude;
            magnitudeSum += magnitude;
        }

        if (magnitudeSum == 0.0d) {
            return 0.5d;
        }
        return clamp(weightedFrequency / magnitudeSum);
    }

    private double normalizedEntropy(double[] values, int bins) {
        if (values.length == 0 || bins <= 1) {
            return 0.0d;
        }

        int[] histogram = new int[bins];
        for (double value : values) {
            int index = Math.min((int) Math.floor(clamp(value) * bins), bins - 1);
            histogram[index]++;
        }
        return normalizedEntropy(histogram, values.length);
    }

    private double normalizedEntropy(int[] histogram, int totalCount) {
        if (totalCount <= 0) {
            return 0.0d;
        }

        double entropy = 0.0d;
        int occupiedBuckets = 0;
        for (int count : histogram) {
            if (count <= 0) {
                continue;
            }
            occupiedBuckets++;
            double probability = count / (double) totalCount;
            entropy -= probability * (Math.log(probability) / Math.log(2.0d));
        }

        if (occupiedBuckets <= 1) {
            return 0.0d;
        }
        return clamp(entropy / (Math.log(occupiedBuckets) / Math.log(2.0d)));
    }

    private double round(double value) {
        return Math.round(value * 1000.0d) / 1000.0d;
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record AudioMetrics(double energy,
                                double rhythm,
                                double brightness,
                                double dynamicRange,
                                double complexity) {
    }
}
