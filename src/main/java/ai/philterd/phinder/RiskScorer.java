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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Scores how sensitive the PII in a file or a scan is, so that remediation can be prioritized.
 *
 * <p>The Magnitude Score treats every occurrence alike by default (a weight of 1.0), so a file with
 * a hundred first names outranks a file with ten Social Security numbers. The Risk Score is a
 * separate dimension that accounts for how sensitive each entity type is, how many of them there
 * are, and how confident the detector was:
 *
 * <pre>Risk Score = Sum over types of ( severity(type) * count(type) * confidenceFactor(type) )</pre>
 *
 * <p>where {@code confidenceFactor} maps an average confidence in [0, 1] onto
 * [{@value #CONFIDENCE_FLOOR}, 1.0]. Confidence discounts a finding, it never erases it: a detector
 * that is unsure still found something worth looking at, so a low-confidence hit contributes half
 * as much as a certain one rather than nothing at all.
 *
 * <p>The Risk Score is a prioritization aid built from the entity types found, not a measure of
 * regulatory exposure. It does not know how a file is stored, who can read it, or whether the
 * detections are correct. Review the findings against your own data before acting on the ranking.
 *
 * <p>Severities default to the table below and can be overridden per entity type; see
 * {@link PhinderReport#setSeverity(String, double)}.
 */
public final class RiskScorer {

    /** The multiplier applied to a finding the detector had no confidence in at all. */
    public static final double CONFIDENCE_FLOOR = 0.5;

    /** The severity used for an entity type that is not in the default table. */
    public static final double DEFAULT_SEVERITY = 1.0;

    /** Directly identifying or financially actionable on its own. */
    private static final double CRITICAL = 10.0;

    /** Sensitive enough to identify or harm a person with little other context. */
    private static final double HIGH = 8.0;

    /** Contact or quasi-identifying detail; identifying in combination. */
    private static final double ELEVATED = 5.0;

    /** Narrows a population but rarely identifies anyone alone. */
    private static final double MODERATE = 3.0;

    /** Weakly identifying, or identifying only alongside much more specific data. */
    private static final double LOW = 2.0;

    /** Not identifying on its own. */
    private static final double MINIMAL = 1.0;

    private static final Map<String, Double> DEFAULT_SEVERITIES = buildDefaultSeverities();

    private RiskScorer() {
    }

    /** The bands a Risk Score falls into, for reporting at a glance. */
    public enum RiskLevel {

        NONE(0.0),
        LOW(0.0),
        MEDIUM(10.0),
        HIGH(50.0),
        CRITICAL(250.0);

        private final double threshold;

        RiskLevel(final double threshold) {
            this.threshold = threshold;
        }

        /**
         * The lower bound of this band. The bound is inclusive except for {@code LOW}, which starts
         * just above its bound of zero: a score of exactly zero is {@code NONE}.
         */
        public double getThreshold() {
            return threshold;
        }

        /** The band a Risk Score falls into. */
        public static RiskLevel of(final double riskScore) {
            if (riskScore <= 0) {
                return NONE;
            } else if (riskScore < MEDIUM.threshold) {
                return LOW;
            } else if (riskScore < HIGH.threshold) {
                return MEDIUM;
            } else if (riskScore < CRITICAL.threshold) {
                return HIGH;
            }
            return CRITICAL;
        }
    }

    /** The built-in severity for each entity type, keyed by its Phileas filter-type token. */
    public static Map<String, Double> defaultSeverities() {
        return Collections.unmodifiableMap(DEFAULT_SEVERITIES);
    }

    /** The built-in severity for an entity type, or {@value #DEFAULT_SEVERITY} if it has none. */
    public static double defaultSeverity(final String type) {
        return DEFAULT_SEVERITIES.getOrDefault(type, DEFAULT_SEVERITY);
    }

    /**
     * How much an average detection confidence counts toward the Risk Score: a value in
     * [{@value #CONFIDENCE_FLOOR}, 1.0]. A confidence outside [0, 1] is clamped into it.
     */
    public static double confidenceFactor(final double averageConfidence) {
        final double clamped = Math.max(0.0, Math.min(1.0, averageConfidence));
        return CONFIDENCE_FLOOR + (1.0 - CONFIDENCE_FLOOR) * clamped;
    }

    /**
     * The Risk Score for a set of entity counts.
     *
     * @param counts     the number of occurrences of each entity type.
     * @param severities the severity to use per entity type; a type absent here falls back to
     *                   {@link #defaultSeverity(String)}.
     * @param confidence the confidence statistics per entity type. A type with no statistics is
     *                   scored as if the detector were certain.
     */
    public static double score(final Map<String, Integer> counts,
                               final Map<String, Double> severities,
                               final Map<String, PhinderReport.ConfidenceStats> confidence) {

        double score = 0;

        for (final Map.Entry<String, Integer> entry : counts.entrySet()) {
            final String type = entry.getKey();
            score += score(entry.getValue(),
                    severities.getOrDefault(type, defaultSeverity(type)),
                    confidence == null ? null : confidence.get(type));
        }

        return score;
    }

    /**
     * How much a single entity type contributes to a Risk Score.
     *
     * <p>Because the confidence factor is affine in the average confidence, the whole is the sum of
     * its parts: a file's Risk Score equals the sum of its per-type contributions, and a scan's
     * aggregate Risk Score equals the sum of its per-file scores. Reports rely on that to show
     * where a score came from without the columns failing to add up.
     *
     * @param count    the number of occurrences of the type.
     * @param severity the severity of the type.
     * @param stats    the confidence statistics for the type, or null to score it as certain.
     */
    public static double score(final int count, final double severity,
                               final PhinderReport.ConfidenceStats stats) {
        final double factor = stats == null || stats.getCount() == 0
                ? 1.0
                : confidenceFactor(stats.getAverage());
        return severity * count * factor;
    }

    // Severities are ordinal judgements about how much damage a single leaked value does, not
    // measurements. They are the starting point Phinder ships with so that a Social Security number
    // does not rank alongside a city name out of the box; tune them for your own data and threat
    // model with a severities file.
    private static Map<String, Double> buildDefaultSeverities() {

        final Map<String, Double> m = new TreeMap<>();

        // Directly identifying or financially actionable on its own.
        m.put(FilterType.SSN.getType(), CRITICAL);
        m.put(FilterType.CREDIT_CARD.getType(), CRITICAL);
        m.put(FilterType.BANK_ROUTING_NUMBER.getType(), CRITICAL);
        m.put(FilterType.IBAN_CODE.getType(), CRITICAL);
        m.put(FilterType.PASSPORT_NUMBER.getType(), CRITICAL);
        m.put(FilterType.DRIVERS_LICENSE_NUMBER.getType(), CRITICAL);

        // Sensitive enough to identify or harm a person with little other context.
        m.put(FilterType.MEDICAL_CONDITION.getType(), HIGH);
        m.put(FilterType.BITCOIN_ADDRESS.getType(), HIGH);

        // Contact or quasi-identifying detail; identifying in combination.
        m.put(FilterType.EMAIL_ADDRESS.getType(), ELEVATED);
        m.put(FilterType.PHONE_NUMBER.getType(), ELEVATED);
        m.put(FilterType.STREET_ADDRESS.getType(), ELEVATED);
        m.put(FilterType.IDENTIFIER.getType(), ELEVATED);
        m.put(FilterType.PERSON.getType(), ELEVATED);
        m.put(FilterType.PH_EYE.getType(), ELEVATED);
        m.put(FilterType.PHYSICIAN_NAME.getType(), ELEVATED);
        m.put(FilterType.VIN.getType(), ELEVATED);

        // Narrows a population but rarely identifies anyone alone.
        m.put(FilterType.FIRST_NAME.getType(), MODERATE);
        m.put(FilterType.SURNAME.getType(), MODERATE);
        m.put(FilterType.AGE.getType(), MODERATE);
        m.put(FilterType.IP_ADDRESS.getType(), MODERATE);
        m.put(FilterType.CUSTOM_DICTIONARY.getType(), MODERATE);

        // Weakly identifying, or identifying only alongside much more specific data.
        m.put(FilterType.DATE.getType(), LOW);
        m.put(FilterType.ZIP_CODE.getType(), LOW);
        m.put(FilterType.MAC_ADDRESS.getType(), LOW);
        m.put(FilterType.TRACKING_NUMBER.getType(), LOW);
        m.put(FilterType.PHONE_NUMBER_EXTENSION.getType(), LOW);
        m.put(FilterType.HOSPITAL.getType(), LOW);
        m.put(FilterType.HOSPITAL_ABBREVIATION.getType(), LOW);

        // Not identifying on its own.
        m.put(FilterType.LOCATION_CITY.getType(), MINIMAL);
        m.put(FilterType.LOCATION_COUNTY.getType(), MINIMAL);
        m.put(FilterType.LOCATION_STATE.getType(), MINIMAL);
        m.put(FilterType.STATE_ABBREVIATION.getType(), MINIMAL);
        m.put(FilterType.CURRENCY.getType(), MINIMAL);
        m.put(FilterType.URL.getType(), MINIMAL);
        m.put(FilterType.SECTION.getType(), MINIMAL);
        m.put(FilterType.OTHER.getType(), MINIMAL);

        return new HashMap<>(m);
    }

}
