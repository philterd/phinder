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
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.*;
import ai.philterd.phileas.services.strategies.dynamic.*;
import ai.philterd.phileas.services.strategies.rules.*;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds a starter redaction policy from the entity types a Phinder scan found.
 *
 * <p>Each detected type is enabled with a {@code REDACT} strategy. The result is a starting point
 * to review, tune, and then measure (for example with Philter Scope); it is not a guarantee that
 * every value of those types will be redacted, and the user is responsible for validating it
 * against their own data. The output is the redaction-policy JSON that Philter and Phileas load
 * (identity comes from the file name, so no policy name is written into the document).
 *
 * <p>The per-type construction mirrors {@link Phinder#createDefaultPolicy()}; the difference is that
 * only the detected types are enabled rather than all of them. Types Phinder can detect only with a
 * supplied custom policy (for example a custom identifier, a section, or a PhEye {@code person}
 * block) have no direct mapping here and are reported as skipped.
 */
public final class StarterPolicyGenerator {

    private StarterPolicyGenerator() {
    }

    // A filter builder can touch resource-backed filter constructors, which may throw IOException,
    // so this is a checked-exception-friendly alternative to Consumer.
    @FunctionalInterface
    private interface FilterBuilder {
        void apply(Identifiers identifiers) throws IOException;
    }

    /** The generated policy plus which detected types it covers and which it could not map. */
    public static final class Result {

        private final Policy policy;
        private final Set<String> enabledTypes;
        private final Set<String> unsupportedTypes;

        Result(final Policy policy, final Set<String> enabledTypes, final Set<String> unsupportedTypes) {
            this.policy = policy;
            this.enabledTypes = enabledTypes;
            this.unsupportedTypes = unsupportedTypes;
        }

        public Policy getPolicy() {
            return policy;
        }

        public Set<String> getEnabledTypes() {
            return enabledTypes;
        }

        public Set<String> getUnsupportedTypes() {
            return unsupportedTypes;
        }

        public String toJson() {
            return new GsonBuilder().setPrettyPrinting().create().toJson(policy);
        }
    }

    /**
     * Build a starter policy enabling each detected type that maps to a redaction-policy filter.
     *
     * @param detectedTypes the filter-type tokens found in a scan (the keys of
     *                       {@link PhinderReport#getAggregateCounts()}).
     * @return the policy and the enabled / unsupported type sets.
     */
    public static Result generate(final Collection<String> detectedTypes) throws IOException {

        final Map<String, FilterBuilder> builders = builders();
        final Identifiers identifiers = new Identifiers();
        final Set<String> enabled = new TreeSet<>();
        final Set<String> unsupported = new TreeSet<>();

        for (final String type : new TreeSet<>(detectedTypes)) {
            final FilterBuilder builder = builders.get(type);
            if (builder != null) {
                builder.apply(identifiers);
                enabled.add(type);
            } else {
                unsupported.add(type);
            }
        }

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return new Result(policy, enabled, unsupported);
    }

    /** The detected-type tokens this generator can map to a redaction-policy filter. */
    public static Set<String> supportedTypes() {
        return new TreeSet<>(builders().keySet());
    }

    // A REDACT strategy is the safe, schema-valid default for a starter policy; the user tunes from
    // there (mask, encrypt, replace, conditions, and so on).
    private static <T> List<T> redact(final T strategy) {
        return Collections.singletonList(strategy);
    }

    private static Map<String, FilterBuilder> builders() {

        final Map<String, FilterBuilder> m = new HashMap<>();

        m.put(FilterType.AGE.getType(), id -> {
            final Age f = new Age();
            final AgeFilterStrategy s = new AgeFilterStrategy();
            s.setStrategy("REDACT");
            f.setAgeFilterStrategies(redact(s));
            id.setAge(f);
        });
        m.put(FilterType.BANK_ROUTING_NUMBER.getType(), id -> {
            final BankRoutingNumber f = new BankRoutingNumber();
            final BankRoutingNumberFilterStrategy s = new BankRoutingNumberFilterStrategy();
            s.setStrategy("REDACT");
            f.setBankRoutingNumberFilterStrategies(redact(s));
            id.setBankRoutingNumber(f);
        });
        m.put(FilterType.BITCOIN_ADDRESS.getType(), id -> {
            final BitcoinAddress f = new BitcoinAddress();
            final BitcoinAddressFilterStrategy s = new BitcoinAddressFilterStrategy();
            s.setStrategy("REDACT");
            f.setBitcoinFilterStrategies(redact(s));
            id.setBitcoinAddress(f);
        });
        m.put(FilterType.LOCATION_CITY.getType(), id -> {
            final City f = new City();
            final ai.philterd.phileas.services.strategies.dynamic.CityFilterStrategy s =
                    new ai.philterd.phileas.services.strategies.dynamic.CityFilterStrategy();
            s.setStrategy("REDACT");
            f.setCityFilterStrategies(redact(s));
            id.setCity(f);
        });
        m.put(FilterType.LOCATION_COUNTY.getType(), id -> {
            final County f = new County();
            final ai.philterd.phileas.services.strategies.dynamic.CountyFilterStrategy s =
                    new ai.philterd.phileas.services.strategies.dynamic.CountyFilterStrategy();
            s.setStrategy("REDACT");
            f.setCountyFilterStrategies(redact(s));
            id.setCounty(f);
        });
        m.put(FilterType.CREDIT_CARD.getType(), id -> {
            final CreditCard f = new CreditCard();
            final CreditCardFilterStrategy s = new CreditCardFilterStrategy();
            s.setStrategy("REDACT");
            f.setCreditCardFilterStrategies(redact(s));
            id.setCreditCard(f);
        });
        m.put(FilterType.CURRENCY.getType(), id -> {
            final Currency f = new Currency();
            final CurrencyFilterStrategy s = new CurrencyFilterStrategy();
            s.setStrategy("REDACT");
            f.setCurrencyFilterStrategies(redact(s));
            id.setCurrency(f);
        });
        m.put(FilterType.DATE.getType(), id -> {
            final Date f = new Date();
            final DateFilterStrategy s = new DateFilterStrategy();
            s.setStrategy("REDACT");
            f.setDateFilterStrategies(redact(s));
            id.setDate(f);
        });
        m.put(FilterType.DRIVERS_LICENSE_NUMBER.getType(), id -> {
            final DriversLicense f = new DriversLicense();
            final DriversLicenseFilterStrategy s = new DriversLicenseFilterStrategy();
            s.setStrategy("REDACT");
            f.setDriversLicenseFilterStrategies(redact(s));
            id.setDriversLicense(f);
        });
        m.put(FilterType.EMAIL_ADDRESS.getType(), id -> {
            final EmailAddress f = new EmailAddress();
            final EmailAddressFilterStrategy s = new EmailAddressFilterStrategy();
            s.setStrategy("REDACT");
            f.setEmailAddressFilterStrategies(redact(s));
            id.setEmailAddress(f);
        });
        m.put(FilterType.FIRST_NAME.getType(), id -> {
            final FirstName f = new FirstName();
            final ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy s =
                    new ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy();
            s.setStrategy("REDACT");
            f.setFirstNameFilterStrategies(redact(s));
            id.setFirstName(f);
        });
        m.put(FilterType.HOSPITAL.getType(), id -> {
            final Hospital f = new Hospital();
            final ai.philterd.phileas.services.strategies.dynamic.HospitalFilterStrategy s =
                    new ai.philterd.phileas.services.strategies.dynamic.HospitalFilterStrategy();
            s.setStrategy("REDACT");
            f.setHospitalFilterStrategies(redact(s));
            id.setHospital(f);
        });
        m.put(FilterType.IBAN_CODE.getType(), id -> {
            final IbanCode f = new IbanCode();
            final IbanCodeFilterStrategy s = new IbanCodeFilterStrategy();
            s.setStrategy("REDACT");
            f.setIbanCodeFilterStrategies(redact(s));
            id.setIbanCode(f);
        });
        m.put(FilterType.IP_ADDRESS.getType(), id -> {
            final IpAddress f = new IpAddress();
            final IpAddressFilterStrategy s = new IpAddressFilterStrategy();
            s.setStrategy("REDACT");
            f.setIpAddressFilterStrategies(redact(s));
            id.setIpAddress(f);
        });
        m.put(FilterType.MAC_ADDRESS.getType(), id -> {
            final MacAddress f = new MacAddress();
            final MacAddressFilterStrategy s = new MacAddressFilterStrategy();
            s.setStrategy("REDACT");
            f.setMacAddressFilterStrategies(redact(s));
            id.setMacAddress(f);
        });
        m.put(FilterType.MEDICAL_CONDITION.getType(), id -> {
            final MedicalCondition f = new MedicalCondition();
            final MedicalConditionFilterStrategy s = new MedicalConditionFilterStrategy();
            s.setStrategy("REDACT");
            f.setMedicalConditionFilterStrategies(redact(s));
            id.setMedicalCondition(f);
        });
        m.put(FilterType.PASSPORT_NUMBER.getType(), id -> {
            final PassportNumber f = new PassportNumber();
            final PassportNumberFilterStrategy s = new PassportNumberFilterStrategy();
            s.setStrategy("REDACT");
            f.setPassportNumberFilterStrategies(redact(s));
            id.setPassportNumber(f);
        });
        m.put(FilterType.PHONE_NUMBER.getType(), id -> {
            final PhoneNumber f = new PhoneNumber();
            final PhoneNumberFilterStrategy s = new PhoneNumberFilterStrategy();
            s.setStrategy("REDACT");
            f.setPhoneNumberFilterStrategies(redact(s));
            id.setPhoneNumber(f);
        });
        m.put(FilterType.PHONE_NUMBER_EXTENSION.getType(), id -> {
            final PhoneNumberExtension f = new PhoneNumberExtension();
            final PhoneNumberExtensionFilterStrategy s = new PhoneNumberExtensionFilterStrategy();
            s.setStrategy("REDACT");
            f.setPhoneNumberExtensionFilterStrategies(redact(s));
            id.setPhoneNumberExtension(f);
        });
        m.put(FilterType.PHYSICIAN_NAME.getType(), id -> {
            final PhysicianName f = new PhysicianName();
            final PhysicianNameFilterStrategy s = new PhysicianNameFilterStrategy();
            s.setStrategy("REDACT");
            f.setPhysicianNameFilterStrategies(redact(s));
            id.setPhysicianName(f);
        });
        m.put(FilterType.SSN.getType(), id -> {
            final Ssn f = new Ssn();
            final SsnFilterStrategy s = new SsnFilterStrategy();
            s.setStrategy("REDACT");
            f.setSsnFilterStrategies(redact(s));
            id.setSsn(f);
        });
        m.put(FilterType.LOCATION_STATE.getType(), id -> {
            final State f = new State();
            final ai.philterd.phileas.services.strategies.dynamic.StateFilterStrategy s =
                    new ai.philterd.phileas.services.strategies.dynamic.StateFilterStrategy();
            s.setStrategy("REDACT");
            f.setStateFilterStrategies(redact(s));
            id.setState(f);
        });
        m.put(FilterType.STATE_ABBREVIATION.getType(), id -> {
            final StateAbbreviation f = new StateAbbreviation();
            final StateAbbreviationFilterStrategy s = new StateAbbreviationFilterStrategy();
            s.setStrategy("REDACT");
            f.setStateAbbreviationsFilterStrategies(redact(s));
            id.setStateAbbreviation(f);
        });
        m.put(FilterType.STREET_ADDRESS.getType(), id -> {
            final StreetAddress f = new StreetAddress();
            final StreetAddressFilterStrategy s = new StreetAddressFilterStrategy();
            s.setStrategy("REDACT");
            f.setStreetAddressFilterStrategies(redact(s));
            id.setStreetAddress(f);
        });
        m.put(FilterType.SURNAME.getType(), id -> {
            final Surname f = new Surname();
            final ai.philterd.phileas.services.strategies.dynamic.SurnameFilterStrategy s =
                    new ai.philterd.phileas.services.strategies.dynamic.SurnameFilterStrategy();
            s.setStrategy("REDACT");
            f.setSurnameFilterStrategies(redact(s));
            id.setSurname(f);
        });
        m.put(FilterType.TRACKING_NUMBER.getType(), id -> {
            final TrackingNumber f = new TrackingNumber();
            final TrackingNumberFilterStrategy s = new TrackingNumberFilterStrategy();
            s.setStrategy("REDACT");
            f.setTrackingNumberFilterStrategies(redact(s));
            id.setTrackingNumber(f);
        });
        m.put(FilterType.URL.getType(), id -> {
            final Url f = new Url();
            final UrlFilterStrategy s = new UrlFilterStrategy();
            s.setStrategy("REDACT");
            f.setUrlFilterStrategies(redact(s));
            id.setUrl(f);
        });
        m.put(FilterType.VIN.getType(), id -> {
            final Vin f = new Vin();
            final VinFilterStrategy s = new VinFilterStrategy();
            s.setStrategy("REDACT");
            f.setVinFilterStrategies(redact(s));
            id.setVin(f);
        });
        m.put(FilterType.ZIP_CODE.getType(), id -> {
            final ZipCode f = new ZipCode();
            final ZipCodeFilterStrategy s = new ZipCodeFilterStrategy();
            s.setStrategy("REDACT");
            f.setZipCodeFilterStrategies(redact(s));
            id.setZipCode(f);
        });

        return m;
    }
}
