package com.example.personalmusicsystem.service.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Best-effort metadata extracted from uploaded audio files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioMetadataSnapshot {

    private Integer durationSeconds;
    private Integer sampleRate;
    private Integer bitRateKbps;
    private Integer channels;
    private String formatName;
    private String codecName;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private String comment;
    private boolean fromTag;
}
