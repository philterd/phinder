/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phinder;

import ai.philterd.phileas.model.filtering.Span;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhinderReport {

    private final Map<String, Map<String, Integer>> perFileCounts = new HashMap<>();
    private final Map<String, Long> perFileWordCounts = new HashMap<>();
    private final Map<String, Integer> aggregateCounts = new HashMap<>();
    private final Map<String, Double> weights = new HashMap<>();
    private final Map<String, Double> severities = new HashMap<>();
    private final Map<String, ConfidenceStats> aggregateConfidence = new HashMap<>();
    private final Map<String, Map<String, ConfidenceStats>> perFileConfidence = new HashMap<>();
    private final long timestamp;
    private final String reportId;
    private int skippedFiles = 0;

    public static class ConfidenceStats {
        private double min = Double.MAX_VALUE;
        private double max = -Double.MAX_VALUE;
        private double sum = 0;
        private int count = 0;

        public void add(final double confidence) {
            min = Math.min(min, confidence);
            max = Math.max(max, confidence);
            sum += confidence;
            count++;
        }

        public double getMin() {
            return count == 0 ? 0 : min;
        }

        public double getMax() {
            return count == 0 ? 0 : max;
        }

        public double getAverage() {
            return count == 0 ? 0 : sum / count;
        }

        public int getCount() {
            return count;
        }
    }

    public PhinderReport() {
        this.timestamp = System.currentTimeMillis();
        this.reportId = java.util.UUID.randomUUID().toString();
        // Default weight is 1.0 for all types
    }

    public void addFileResult(final String filePath, final List<Span> spans, final long wordCount) {
        final Map<String, Integer> counts = new HashMap<>();
        final Map<String, ConfidenceStats> fileConfStats = new HashMap<>();

        for (final Span span : spans) {
            final String type = span.getFilterType().getType();
            final double confidence = span.getConfidence();

            counts.put(type, counts.getOrDefault(type, 0) + 1);
            aggregateCounts.put(type, aggregateCounts.getOrDefault(type, 0) + 1);

            // Update aggregate confidence stats
            aggregateConfidence.computeIfAbsent(type, k -> new ConfidenceStats()).add(confidence);

            // Update per-file confidence stats
            fileConfStats.computeIfAbsent(type, k -> new ConfidenceStats()).add(confidence);
        }

        perFileCounts.put(filePath, counts);
        perFileConfidence.put(filePath, fileConfStats);
        perFileWordCounts.put(filePath, wordCount);
    }

    /**
     * The Risk Score across every file scanned. Unlike the Magnitude Score, this accounts for how
     * sensitive each entity type is and how confident the detector was. See {@link RiskScorer}.
     */
    public double getAggregateRiskScore() {
        return RiskScorer.score(aggregateCounts, severities, aggregateConfidence);
    }

    /** The band {@link #getAggregateRiskScore()} falls into. */
    public RiskScorer.RiskLevel getAggregateRiskLevel() {
        return RiskScorer.RiskLevel.of(getAggregateRiskScore());
    }

    /** The Risk Score for a single file. See {@link RiskScorer}. */
    public double getFileRiskScore(final String filePath) {
        return RiskScorer.score(
                perFileCounts.getOrDefault(filePath, Collections.emptyMap()),
                severities,
                perFileConfidence.get(filePath));
    }

    /** The band {@link #getFileRiskScore(String)} falls into. */
    public RiskScorer.RiskLevel getFileRiskLevel(final String filePath) {
        return RiskScorer.RiskLevel.of(getFileRiskScore(filePath));
    }

    public double getAggregateMagnitudeScore() {
        return calculateMagnitudeScore(aggregateCounts);
    }

    public double getFileMagnitudeScore(final String filePath) {
        return calculateMagnitudeScore(perFileCounts.getOrDefault(filePath, new HashMap<>()));
    }

    public double getFileDensityScore(final String filePath) {
        final double magnitudeScore = getFileMagnitudeScore(filePath);
        final long wordCount = perFileWordCounts.getOrDefault(filePath, 0L);
        if (wordCount == 0) {
            return 0;
        }
        return magnitudeScore / wordCount;
    }

    public double getAggregateDensityScore() {
        final double aggregateMagnitudeScore = getAggregateMagnitudeScore();
        final long totalWordCount = perFileWordCounts.values().stream().mapToLong(Long::longValue).sum();
        if (totalWordCount == 0) {
            return 0;
        }
        return aggregateMagnitudeScore / totalWordCount;
    }

    private double calculateMagnitudeScore(final Map<String, Integer> counts) {
        double score = 0;
        for (final Map.Entry<String, Integer> entry : counts.entrySet()) {
            final double weight = weights.getOrDefault(entry.getKey(), 1.0);
            score += weight * entry.getValue();
        }
        return score;
    }

    public Map<String, Integer> getAggregateCounts() {
        return aggregateCounts;
    }

    public Map<String, Map<String, Integer>> getPerFileCounts() {
        return perFileCounts;
    }

    public Map<String, ConfidenceStats> getAggregateConfidence() {
        return aggregateConfidence;
    }

    public Map<String, Map<String, ConfidenceStats>> getPerFileConfidence() {
        return perFileConfidence;
    }

    public void setWeight(final String piiType, final double weight) {
        weights.put(piiType, weight);
    }

    public Map<String, Double> getWeights() {
        return weights;
    }

    /** Override the risk severity of an entity type, replacing the built-in default. */
    public void setSeverity(final String piiType, final double severity) {
        severities.put(piiType, severity);
    }

    /** The severity of an entity type, falling back to the built-in default. */
    public double getSeverity(final String piiType) {
        return severities.getOrDefault(piiType, RiskScorer.defaultSeverity(piiType));
    }

    /** Only the severities explicitly overridden for this report, not the built-in defaults. */
    public Map<String, Double> getSeverityOverrides() {
        return severities;
    }

    /** The severity actually in effect for each entity type the scan found. */
    public Map<String, Double> getEffectiveSeverities() {
        final Map<String, Double> effective = new HashMap<>();
        for (final String type : aggregateCounts.keySet()) {
            effective.put(type, getSeverity(type));
        }
        return effective;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getReportId() {
        return reportId;
    }

    public int getSkippedFiles() {
        return skippedFiles;
    }

    public void setSkippedFiles(final int skippedFiles) {
        this.skippedFiles = skippedFiles;
    }
}
