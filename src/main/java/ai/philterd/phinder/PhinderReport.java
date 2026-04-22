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
    private int skippedFiles = 0;

    public PhinderReport() {
        // Default weight is 1.0 for all types
    }

    public void addFileResult(String filePath, List<Span> spans, long wordCount) {
        Map<String, Integer> counts = new HashMap<>();
        for (Span span : spans) {
            String type = span.getFilterType().getType();
            counts.put(type, counts.getOrDefault(type, 0) + 1);
            aggregateCounts.put(type, aggregateCounts.getOrDefault(type, 0) + 1);
        }
        perFileCounts.put(filePath, counts);
        perFileWordCounts.put(filePath, wordCount);
    }

    public double getAggregateMagnitudeScore() {
        return calculateMagnitudeScore(aggregateCounts);
    }

    public double getFileMagnitudeScore(String filePath) {
        return calculateMagnitudeScore(perFileCounts.getOrDefault(filePath, new HashMap<>()));
    }

    public double getFileDensityScore(String filePath) {
        double magnitudeScore = getFileMagnitudeScore(filePath);
        long wordCount = perFileWordCounts.getOrDefault(filePath, 0L);
        if (wordCount == 0) {
            return 0;
        }
        return magnitudeScore / wordCount;
    }

    public double getAggregateDensityScore() {
        double aggregateMagnitudeScore = getAggregateMagnitudeScore();
        long totalWordCount = perFileWordCounts.values().stream().mapToLong(Long::longValue).sum();
        if (totalWordCount == 0) {
            return 0;
        }
        return aggregateMagnitudeScore / totalWordCount;
    }

    private double calculateMagnitudeScore(Map<String, Integer> counts) {
        double score = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double weight = weights.getOrDefault(entry.getKey(), 1.0);
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

    public void setWeight(String piiType, double weight) {
        weights.put(piiType, weight);
    }

    public int getSkippedFiles() {
        return skippedFiles;
    }

    public void setSkippedFiles(int skippedFiles) {
        this.skippedFiles = skippedFiles;
    }
}
