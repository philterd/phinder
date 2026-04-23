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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhinderReport {

    private final Map<String, Map<String, Integer>> perFileCounts = new HashMap<>();
    private final Map<String, Long> perFileWordCounts = new HashMap<>();
    private final Map<String, Integer> aggregateCounts = new HashMap<>();
    private final Map<String, Double> weights = new HashMap<>();
    private final Map<String, ConfidenceStats> aggregateConfidence = new HashMap<>();
    private final Map<String, Map<String, ConfidenceStats>> perFileConfidence = new HashMap<>();
    private final long timestamp;
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

    public long getTimestamp() {
        return timestamp;
    }

    public int getSkippedFiles() {
        return skippedFiles;
    }

    public void setSkippedFiles(final int skippedFiles) {
        this.skippedFiles = skippedFiles;
    }
}
