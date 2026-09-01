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

import ai.philterd.phileas.model.filtering.FilterType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RiskScorerTest {

    @Test
    public void testSensitiveTypesOutrankWeakOnes() {
        // The ordering that motivates the score at all: an SSN is not a city name.
        assertTrue(RiskScorer.defaultSeverity("ssn") > RiskScorer.defaultSeverity("first-name"));
        assertTrue(RiskScorer.defaultSeverity("first-name") > RiskScorer.defaultSeverity("city"));
        assertTrue(RiskScorer.defaultSeverity("credit-card") > RiskScorer.defaultSeverity("email-address"));
        assertTrue(RiskScorer.defaultSeverity("email-address") > RiskScorer.defaultSeverity("zip-code"));
    }

    @Test
    public void testEveryFilterTypeHasASeverity() {
        // A type Phileas can emit but the table does not name still has to score, and it must not
        // silently score as the most sensitive thing in the scan.
        for (final FilterType filterType : FilterType.values()) {
            final double severity = RiskScorer.defaultSeverity(filterType.getType());
            assertTrue(severity > 0, filterType.getType() + " should have a positive severity");
            assertTrue(severity <= 10.0, filterType.getType() + " should not exceed the critical tier");
        }
    }

    @Test
    public void testUnknownTypeUsesTheDefaultSeverity() {
        assertEquals(RiskScorer.DEFAULT_SEVERITY, RiskScorer.defaultSeverity("not-a-real-type"));
    }

    @Test
    public void testDefaultSeveritiesAreImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> RiskScorer.defaultSeverities().put("ssn", 0.0));
    }

    @Test
    public void testConfidenceFactorDiscountsButNeverErases() {
        assertEquals(1.0, RiskScorer.confidenceFactor(1.0), 0.0001);
        assertEquals(0.75, RiskScorer.confidenceFactor(0.5), 0.0001);
        assertEquals(RiskScorer.CONFIDENCE_FLOOR, RiskScorer.confidenceFactor(0.0), 0.0001);
        // Out-of-range confidences are clamped rather than allowed to distort the score.
        assertEquals(RiskScorer.CONFIDENCE_FLOOR, RiskScorer.confidenceFactor(-5.0), 0.0001);
        assertEquals(1.0, RiskScorer.confidenceFactor(5.0), 0.0001);
    }

    @Test
    public void testScoreCombinesSeverityVolumeAndConfidence() {
        final PhinderReport.ConfidenceStats stats = new PhinderReport.ConfidenceStats();
        stats.add(0.5);

        // severity 10.0 * 3 occurrences * confidenceFactor(0.5) = 22.5
        final double score = RiskScorer.score(
                Map.of("ssn", 3),
                Map.of(),
                Map.of("ssn", stats));

        assertEquals(22.5, score, 0.0001);
    }

    @Test
    public void testScoreWithoutConfidenceStatsAssumesCertainty() {
        // severity 10.0 * 2 occurrences * 1.0 = 20.0
        assertEquals(20.0, RiskScorer.score(Map.of("ssn", 2), Map.of(), null), 0.0001);
        assertEquals(20.0, RiskScorer.score(Map.of("ssn", 2), Map.of(), Map.of()), 0.0001);
    }

    @Test
    public void testScoreHonorsSeverityOverrides() {
        assertEquals(4.0, RiskScorer.score(Map.of("ssn", 2), Map.of("ssn", 2.0), null), 0.0001);
    }

    @Test
    public void testEmptyCountsScoreZero() {
        assertEquals(0.0, RiskScorer.score(Map.of(), Map.of(), null), 0.0001);
    }

    @Test
    public void testSingleTypeScoreMatchesTheMapForm() {
        final PhinderReport.ConfidenceStats stats = new PhinderReport.ConfidenceStats();
        stats.add(0.5);

        assertEquals(RiskScorer.score(Map.of("ssn", 3), Map.of(), Map.of("ssn", stats)),
                RiskScorer.score(3, 10.0, stats), 0.0001);
    }

    @Test
    public void testScoreIsTheSumOfItsPerTypeParts() {
        // Report tables show a Risk column per entity type next to the file's total. The columns
        // have to add up to the total, which holds because the confidence factor is affine.
        final PhinderReport.ConfidenceStats ssnStats = new PhinderReport.ConfidenceStats();
        ssnStats.add(0.9);
        ssnStats.add(0.7);
        final PhinderReport.ConfidenceStats cityStats = new PhinderReport.ConfidenceStats();
        cityStats.add(1.0);

        final Map<String, Integer> counts = Map.of("ssn", 2, "city", 1);
        final Map<String, PhinderReport.ConfidenceStats> confidence = Map.of("ssn", ssnStats, "city", cityStats);

        final double total = RiskScorer.score(counts, Map.of(), confidence);
        final double parts = RiskScorer.score(2, RiskScorer.defaultSeverity("ssn"), ssnStats)
                + RiskScorer.score(1, RiskScorer.defaultSeverity("city"), cityStats);

        assertEquals(total, parts, 0.0001);
    }

    @Test
    public void testRiskLevelBands() {
        assertEquals(RiskScorer.RiskLevel.NONE, RiskScorer.RiskLevel.of(0));
        assertEquals(RiskScorer.RiskLevel.LOW, RiskScorer.RiskLevel.of(0.01));
        assertEquals(RiskScorer.RiskLevel.LOW, RiskScorer.RiskLevel.of(9.99));
        assertEquals(RiskScorer.RiskLevel.MEDIUM, RiskScorer.RiskLevel.of(10));
        assertEquals(RiskScorer.RiskLevel.MEDIUM, RiskScorer.RiskLevel.of(49.99));
        assertEquals(RiskScorer.RiskLevel.HIGH, RiskScorer.RiskLevel.of(50));
        assertEquals(RiskScorer.RiskLevel.HIGH, RiskScorer.RiskLevel.of(249.99));
        assertEquals(RiskScorer.RiskLevel.CRITICAL, RiskScorer.RiskLevel.of(250));
        assertEquals(RiskScorer.RiskLevel.CRITICAL, RiskScorer.RiskLevel.of(100000));
    }

    @Test
    public void testRiskLevelOfNegativeScoreIsNone() {
        assertEquals(RiskScorer.RiskLevel.NONE, RiskScorer.RiskLevel.of(-1));
    }

}
