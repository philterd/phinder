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
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StarterPolicyGeneratorTest {

    @Test
    public void enablesOnlyDetectedTypes() throws Exception {
        final StarterPolicyGenerator.Result result =
                StarterPolicyGenerator.generate(List.of("email-address", "ssn"));

        assertNotNull(result.getPolicy().getIdentifiers().getEmailAddress());
        assertNotNull(result.getPolicy().getIdentifiers().getSsn());
        assertNull(result.getPolicy().getIdentifiers().getCreditCard());

        assertTrue(result.getEnabledTypes().contains("email-address"));
        assertTrue(result.getEnabledTypes().contains("ssn"));
        assertTrue(result.getUnsupportedTypes().isEmpty());
    }

    @Test
    public void reportsUnsupportedTypes() throws Exception {
        final StarterPolicyGenerator.Result result =
                StarterPolicyGenerator.generate(List.of("email-address", "person", "id"));

        assertTrue(result.getEnabledTypes().contains("email-address"));
        assertTrue(result.getUnsupportedTypes().contains("person"));
        assertTrue(result.getUnsupportedTypes().contains("id"));
    }

    @Test
    public void generatedPolicyJsonLoadsAndRedacts() throws Exception {
        final StarterPolicyGenerator.Result result =
                StarterPolicyGenerator.generate(List.of("email-address", "ssn"));

        // The output must load unchanged into Phileas/Philter.
        final String json = result.toJson();
        final Policy loaded = new Gson().fromJson(json, Policy.class);
        assertNotNull(loaded.getIdentifiers().getEmailAddress());
        assertNotNull(loaded.getIdentifiers().getSsn());

        // And it must actually detect the enabled types.
        final List<Span> spans = new Phinder()
                .findPii("Contact jane@example.com or SSN 123-45-6789.", loaded);
        final List<String> types = spans.stream().map(s -> s.getFilterType().getType()).toList();
        assertTrue(types.contains("email-address"), "expected an email-address span, got " + types);
        assertTrue(types.contains("ssn"), "expected an ssn span, got " + types);
    }

    @Test
    public void everySupportedTypeBuildsExactlyOneIdentifierAndRoundTrips() throws Exception {
        // The 29-way mapping is where a wiring mistake is most likely, so exercise every supported
        // type: generating from a single type must enable just that type and set exactly one
        // identifier on a policy that round-trips through JSON.
        for (final String type : StarterPolicyGenerator.supportedTypes()) {
            final StarterPolicyGenerator.Result result = StarterPolicyGenerator.generate(List.of(type));

            assertEquals(Set.of(type), result.getEnabledTypes(), "enabled mismatch for " + type);
            assertTrue(result.getUnsupportedTypes().isEmpty(), "unexpected unsupported for " + type);

            final Policy loaded = new Gson().fromJson(result.toJson(), Policy.class);
            assertEquals(1, countSetIdentifiers(loaded.getIdentifiers()),
                    "expected exactly one identifier set for " + type);
        }
    }

    @Test
    public void supportedTypesMatchThePhinderDetectableSet() {
        // Guards against a forgotten or stray type relative to what Phinder's default policy detects.
        final Set<String> expected = Set.of(
                "age", "bank-routing-number", "bitcoin-address", "city", "county", "credit-card",
                "currency", "date", "drivers-license-number", "email-address", "first-name",
                "hospital", "iban-code", "ip-address", "mac-address", "medical-condition",
                "passport-number", "phone-number", "phone-number-extension", "physician-name",
                "ssn", "state", "state-abbreviation", "street-address", "surname",
                "tracking-number", "url", "vin", "zip-code");
        assertEquals(expected, StarterPolicyGenerator.supportedTypes());
    }

    // Counts the non-null identifier fields on an Identifiers instance, so a builder that sets the
    // wrong number of identifiers (none, or more than one) is caught.
    private static int countSetIdentifiers(final Identifiers identifiers) throws Exception {
        int count = 0;
        for (final Field field : Identifiers.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            if (field.get(identifiers) != null) {
                count++;
            }
        }
        return count;
    }

    @Test
    public void emptyScanProducesEmptyButValidPolicy() throws Exception {
        final StarterPolicyGenerator.Result result = StarterPolicyGenerator.generate(List.of());
        assertNotNull(result.getPolicy().getIdentifiers());
        assertTrue(result.getEnabledTypes().isEmpty());
        // Still serializes to valid JSON that loads.
        final Policy loaded = new Gson().fromJson(result.toJson(), Policy.class);
        assertNotNull(loaded.getIdentifiers());
        assertFalse(result.toJson().isBlank());
    }
}
