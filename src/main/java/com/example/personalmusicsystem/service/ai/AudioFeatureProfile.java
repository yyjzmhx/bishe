package com.example.personalmusicsystem.service.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Local audio profile extracted before any AI enrichment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioFeatureProfile {

    private String extractionMode;
    private Integer durationSeconds;
    private Integer sampleRate;
    private Integer bitRateKbps;
    private Integer channels;
    private String formatName;
    private String codecName;
    private String tagTitle;
    private String tagArtist;
    private String tagAlbum;
    private String tagGenre;
    private String tagComment;
    private List<Double> vector;
    private List<String> vectorLabels;
    private String technicalSummary;
}
