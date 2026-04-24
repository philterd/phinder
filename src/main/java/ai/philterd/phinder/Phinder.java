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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.*;
import ai.philterd.phileas.policy.filters.Currency;
import ai.philterd.phileas.policy.filters.Date;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.rules.*;
import ai.philterd.phileas.services.strategies.dynamic.*;
import ai.philterd.phileas.services.strategies.ai.*;
import ai.philterd.phinder.processors.CsvProcessor;
import ai.philterd.phinder.processors.DocumentProcessor;
import ai.philterd.phinder.processors.EmailProcessor;
import ai.philterd.phinder.processors.ExcelProcessor;
import ai.philterd.phinder.processors.ImageProcessor;
import ai.philterd.phinder.processors.LogProcessor;
import ai.philterd.phinder.processors.PdfProcessor;
import ai.philterd.phinder.processors.PlainTextProcessor;
import ai.philterd.phinder.processors.PowerPointProcessor;
import ai.philterd.phinder.processors.RtfProcessor;
import ai.philterd.phinder.processors.WordProcessor;
import com.google.gson.Gson;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.tika.Tika;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(name = "phinder", mixinStandardHelpOptions = true, version = "phinder 1.0.0",
        description = "Identifies PII in text using Phileas.")
public class Phinder implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, description = "The input file(s) or directories (text, Markdown, PDF, Word, Excel, PowerPoint, CSV, RTF, PNG, JPG/JPEG, EML, MSG, or LOG).", required = true)
    private List<File> inputFiles;

    @Option(names = {"-R", "--recursive"}, description = "Recursively traverse directories.")
    private boolean recursive;

    @Option(names = {"-p", "--policy"}, description = "The Phileas policy (JSON file).")
    private File policyFile;

    @Option(names = {"--csv-delimiter"}, description = "The CSV delimiter character (default: ,).", defaultValue = ",")
    private char csvDelimiter;

    @Option(names = {"--csv-quote"}, description = "The CSV quote character (default: \").", defaultValue = "\"")
    private char csvQuote;

    @Option(names = {"-w", "--weights"}, description = "The PII weights (JSON file).")
    private File weightsFile;

    @Option(names = {"-l", "--log"}, description = "Enable the scan log using a MongoDB database.")
    private boolean log;

    @Option(names = {"-s", "--skip-unchanged"}, description = "Skip scanning files that haven't changed since the last scan log.")
    private boolean skipUnchanged;

    @Option(names = {"--clean"}, description = "Truncate the scan log database.")
    private boolean clean;

    @Option(names = {"--mongodb"}, description = "The MongoDB URI.")
    private String mongoDbUri;

    private PlainTextFilterService filterService;

    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new Phinder()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        final Policy policy;

        if (policyFile != null) {

            if (!policyFile.exists()) {
                System.err.println("Policy file does not exist: " + policyFile.getAbsolutePath());
                return 1;
            }

        final String policyJson = FileUtils.readFileToString(policyFile, StandardCharsets.UTF_8);
        final Gson gson = new Gson();
        policy = gson.fromJson(policyJson, Policy.class);
        System.out.println("Using policy from file: " + policyFile.getAbsolutePath());

        } else {
            policy = createDefaultPolicy();
            System.out.println("No policy file specified. Using default policy.");
        }

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);
        this.filterService = new PlainTextFilterService(
                phileasConfiguration,
                new DefaultContextService(),
                new InMemoryVectorService(),
                HttpClients.createDefault()
        );

        final List<DocumentProcessor> processors = List.of(
                new LogProcessor(),
                new PlainTextProcessor(),
                new PdfProcessor(),
                new WordProcessor(),
                new ExcelProcessor(),
                new PowerPointProcessor(),
                new CsvProcessor(csvDelimiter, csvQuote),
                new RtfProcessor(),
                new ImageProcessor(),
                new EmailProcessor()
        );

        int exitCode = 0;
        int skippedCount = 0;
        final PhinderReport report = new PhinderReport();

        ScanLog scanLog = null;

        if (log || skipUnchanged || clean) {
            if (mongoDbUri == null || mongoDbUri.isEmpty()) {
                System.err.println("MongoDB URI is required when using --log, --skip-unchanged, or --clean. Use --mongodb to specify the URI.");
                return 1;
            }
            scanLog = new ScanLog(mongoDbUri);
        }

        try {

            if (clean) {
                scanLog.clean();
                System.out.println("Scan log cleaned.");
            }

            if (weightsFile != null) {

                if (!weightsFile.exists()) {
                    System.err.println("Weights file does not exist: " + weightsFile.getAbsolutePath());
                    return 1;
                }

                final String weightsJson = FileUtils.readFileToString(weightsFile, StandardCharsets.UTF_8);
                final Gson gson = new Gson();
                final Map<String, Object> weightsMap = gson.fromJson(weightsJson, Map.class);

                if (weightsMap != null) {
                    for (final Map.Entry<String, Object> entry : weightsMap.entrySet()) {
                        final String key = String.valueOf(entry.getKey());
                        final Double value = Double.valueOf(String.valueOf(entry.getValue()));
                        report.setWeight(key, value);
                    }
                }
            }

            for (final File inputFile : inputFiles) {
                if (!inputFile.exists()) {
                    System.err.println("Input file/directory does not exist: " + inputFile.getAbsolutePath());
                    exitCode = 1;
                    continue;
                }

                if (scanLog != null) {
                    scanLog.addScannedPath(inputFile.getAbsolutePath());
                }

                if (inputFile.isDirectory()) {
                    try (final Stream<Path> stream = recursive ? Files.walk(inputFile.toPath()) : Files.walk(inputFile.toPath(), 1)) {
                        // Process files directly from the stream to handle millions of files without memory issues
                        final Iterable<Path> iterable = stream::iterator;
                        for (final Path path : iterable) {
                            if (Files.isRegularFile(path)) {
                                final File file = path.toFile();
                                if (processFileWithCheck(file, processors, policy, report, scanLog)) {
                                    exitCode = 1;
                                } else if (skipUnchanged && scanLog != null) {
                                    final String hash = getFileHash(file);
                                    final String previousHash = scanLog.getFileHash(file.getAbsolutePath());
                                    if (hash.equals(previousHash)) {
                                        skippedCount++;
                                    }
                                }
                            }
                        }
                    } catch (final IOException e) {
                        System.err.println("Failed to walk directory: " + inputFile.getAbsolutePath() + " - " + e.getMessage());
                        exitCode = 1;
                    }
                } else {
                    if (processFileWithCheck(inputFile, processors, policy, report, scanLog)) {
                        exitCode = 1;
                    } else if (skipUnchanged && scanLog != null) {
                        final String hash = getFileHash(inputFile);
                        final String previousHash = scanLog.getFileHash(inputFile.getAbsolutePath());
                        if (hash.equals(previousHash)) {
                            skippedCount++;
                        }
                    }
                }
            }

            report.setSkippedFiles(skippedCount);
            generateReport(report);

            return exitCode;

        } finally {
            if (scanLog != null) {
                scanLog.close();
                System.out.println("Scan log updated.");
            }
        }

    }

    private boolean processFileWithCheck(final File inputFile, final List<DocumentProcessor> processors, final Policy policy, final PhinderReport report, final ScanLog scanLog) {

        final String hash = getFileHash(inputFile);

        if (skipUnchanged && scanLog != null) {
            final String previousHash = scanLog.getFileHash(inputFile.getAbsolutePath());
            if (hash.equals(previousHash)) {
                System.out.println("Skipping unchanged file: " + inputFile.getName());
                return false;
            }
        }

        if (scanLog != null) {
            scanLog.putFileHash(inputFile.getAbsolutePath(), hash);
        }

        return processFile(inputFile, processors, policy, report);

    }

    private String getFileHash(final File file) {

        try {

            final XXHashFactory factory = XXHashFactory.fastestInstance();
            final StreamingXXHash64 hasher = factory.newStreamingHash64(0); // Using seed 0
            try (final FileInputStream fis = new FileInputStream(file)) {
                final byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    hasher.update(buffer, 0, read);
                }
            }

            return String.format("%016x", hasher.getValue());

        } catch (final IOException e) {
            System.err.println("Error calculating xxHash64 hash for " + file.getAbsolutePath() + ": " + e.getMessage());
            return "";
        }

    }

    private boolean processFile(final File inputFile, final List<DocumentProcessor> processors, final Policy policy, final PhinderReport report) {

        final Tika tika = new Tika();
        final String mimeType;

        try {
            mimeType = tika.detect(inputFile);
        } catch (final IOException e) {
            System.err.println("Error detecting file type for " + inputFile.getAbsolutePath() + ": " + e.getMessage());
            return true;
        }

        final DocumentProcessor processor = processors.stream()
                .filter(p -> p.supports(mimeType, inputFile.getName()))
                .findFirst()
                .orElse(null);

        if (processor == null) {
            System.err.println("Unsupported file type: " + inputFile.getName());
            return true;
        }

        try {

            final List<Span> spans;
            final long wordCount;

            if (processor instanceof LogProcessor || processor instanceof CsvProcessor) {
                spans = processor.process(inputFile, policy, this);
                wordCount = processor.getWordCount(inputFile);
            } else {
                String text = processor.extractText(inputFile);
                if (text == null) text = "";

                final String combinedText = inputFile.getName() + "\n" + text;
                spans = findPii(combinedText, policy);
                wordCount = processor.countWords(text);
            }

            report.addFileResult(inputFile.getAbsolutePath(), spans, wordCount);

            System.out.println("PII found in " + inputFile.getName() + ":");
            if (spans.isEmpty()) {
                System.out.println(" - No PII found.");
            } else {
                for (final Span span : spans) {
                    System.out.printf(" - %s (type: %s, confidence: %.2f)\n",
                            span.getText(), span.getFilterType(), span.getConfidence());
                }
                System.out.printf("Magnitude Score: %.2f\n", report.getFileMagnitudeScore(inputFile.getAbsolutePath()));
                System.out.printf("Density Score: %.4f\n", report.getFileDensityScore(inputFile.getAbsolutePath()));
            }
            System.out.println();

        } catch (final Exception e) {
            System.err.println("Error processing file " + inputFile.getAbsolutePath() + ": " + e.getMessage());
            return true;
        }

        return false;

    }

    private void generateReport(final PhinderReport report) throws Exception {
        final ReportBuilder reportBuilder = new ReportBuilder();
        reportBuilder.build(report, mongoDbUri);
    }

    public Policy createDefaultPolicy() throws IOException {

        final Policy policy = new Policy();
        final Identifiers identifiers = new Identifiers();

        final Age age = new Age();
        final AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy();
        ageFilterStrategy.setStrategy("REDACT");
        age.setAgeFilterStrategies(Collections.singletonList(ageFilterStrategy));
        identifiers.setAge(age);

        final BankRoutingNumber bankRoutingNumber = new BankRoutingNumber();
        final BankRoutingNumberFilterStrategy bankRoutingNumberFilterStrategy = new BankRoutingNumberFilterStrategy();
        bankRoutingNumberFilterStrategy.setStrategy("REDACT");
        bankRoutingNumber.setBankRoutingNumberFilterStrategies(Collections.singletonList(bankRoutingNumberFilterStrategy));
        identifiers.setBankRoutingNumber(bankRoutingNumber);

        final BitcoinAddress bitcoinAddress = new BitcoinAddress();
        final BitcoinAddressFilterStrategy bitcoinAddressFilterStrategy = new BitcoinAddressFilterStrategy();
        bitcoinAddressFilterStrategy.setStrategy("REDACT");
        bitcoinAddress.setBitcoinFilterStrategies(Collections.singletonList(bitcoinAddressFilterStrategy));
        identifiers.setBitcoinAddress(bitcoinAddress);

        final City city = new City();
        final ai.philterd.phileas.services.strategies.dynamic.CityFilterStrategy cityFilterStrategy = new ai.philterd.phileas.services.strategies.dynamic.CityFilterStrategy();
        cityFilterStrategy.setStrategy("REDACT");
        city.setCityFilterStrategies(Collections.singletonList(cityFilterStrategy));
        identifiers.setCity(city);

        final County county = new County();
        final ai.philterd.phileas.services.strategies.dynamic.CountyFilterStrategy countyFilterStrategy = new ai.philterd.phileas.services.strategies.dynamic.CountyFilterStrategy();
        countyFilterStrategy.setStrategy("REDACT");
        county.setCountyFilterStrategies(Collections.singletonList(countyFilterStrategy));
        identifiers.setCounty(county);

        final CreditCard creditCard = new CreditCard();
        final CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();
        creditCardFilterStrategy.setStrategy("REDACT");
        creditCard.setCreditCardFilterStrategies(Collections.singletonList(creditCardFilterStrategy));
        identifiers.setCreditCard(creditCard);

        final Currency currency = new Currency();
        final CurrencyFilterStrategy currencyFilterStrategy = new CurrencyFilterStrategy();
        currencyFilterStrategy.setStrategy("REDACT");
        currency.setCurrencyFilterStrategies(Collections.singletonList(currencyFilterStrategy));
        identifiers.setCurrency(currency);

        final Date date = new Date();
        final DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy("REDACT");
        date.setDateFilterStrategies(Collections.singletonList(dateFilterStrategy));
        identifiers.setDate(date);

        final DriversLicense driversLicense = new DriversLicense();
        final DriversLicenseFilterStrategy driversLicenseFilterStrategy = new DriversLicenseFilterStrategy();
        driversLicenseFilterStrategy.setStrategy("REDACT");
        driversLicense.setDriversLicenseFilterStrategies(Collections.singletonList(driversLicenseFilterStrategy));
        identifiers.setDriversLicense(driversLicense);

        final EmailAddress emailAddress = new EmailAddress();
        final EmailAddressFilterStrategy emailAddressFilterStrategy = new EmailAddressFilterStrategy();
        emailAddressFilterStrategy.setStrategy("REDACT");
        emailAddress.setEmailAddressFilterStrategies(Collections.singletonList(emailAddressFilterStrategy));
        identifiers.setEmailAddress(emailAddress);

        final FirstName firstName = new FirstName();
        final ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy firstNameFilterStrategy = new ai.philterd.phileas.services.strategies.dynamic.FirstNameFilterStrategy();
        firstNameFilterStrategy.setStrategy("REDACT");
        firstName.setFirstNameFilterStrategies(Collections.singletonList(firstNameFilterStrategy));
        identifiers.setFirstName(firstName);

        final Hospital hospital = new Hospital();
        final ai.philterd.phileas.services.strategies.dynamic.HospitalFilterStrategy hospitalFilterStrategy = new ai.philterd.phileas.services.strategies.dynamic.HospitalFilterStrategy();
        hospitalFilterStrategy.setStrategy("REDACT");
        hospital.setHospitalFilterStrategies(Collections.singletonList(hospitalFilterStrategy));
        identifiers.setHospital(hospital);

        final IbanCode ibanCode = new IbanCode();
        final IbanCodeFilterStrategy ibanCodeFilterStrategy = new IbanCodeFilterStrategy();
        ibanCodeFilterStrategy.setStrategy("REDACT");
        ibanCode.setIbanCodeFilterStrategies(Collections.singletonList(ibanCodeFilterStrategy));
        identifiers.setIbanCode(ibanCode);

        final IpAddress ipAddress = new IpAddress();
        final IpAddressFilterStrategy ipAddressFilterStrategy = new IpAddressFilterStrategy();
        ipAddressFilterStrategy.setStrategy("REDACT");
        ipAddress.setIpAddressFilterStrategies(Collections.singletonList(ipAddressFilterStrategy));
        identifiers.setIpAddress(ipAddress);

        final MacAddress macAddress = new MacAddress();
        final MacAddressFilterStrategy macAddressFilterStrategy = new MacAddressFilterStrategy();
        macAddressFilterStrategy.setStrategy("REDACT");
        macAddress.setMacAddressFilterStrategies(Collections.singletonList(macAddressFilterStrategy));
        identifiers.setMacAddress(macAddress);

        final MedicalCondition medicalCondition = new MedicalCondition();
        final MedicalConditionFilterStrategy medicalConditionFilterStrategy = new MedicalConditionFilterStrategy();
        medicalConditionFilterStrategy.setStrategy("REDACT");
        medicalCondition.setMedicalConditionFilterStrategies(Collections.singletonList(medicalConditionFilterStrategy));
        identifiers.setMedicalCondition(medicalCondition);

        final PassportNumber passportNumber = new PassportNumber();
        final PassportNumberFilterStrategy passportNumberFilterStrategy = new PassportNumberFilterStrategy();
        passportNumberFilterStrategy.setStrategy("REDACT");
        passportNumber.setPassportNumberFilterStrategies(Collections.singletonList(passportNumberFilterStrategy));
        identifiers.setPassportNumber(passportNumber);

        /*final PhEye person = new PhEye();
        final PhEyeFilterStrategy phEyeFilterStrategy = new PhEyeFilterStrategy();
        phEyeFilterStrategy.setStrategy("REDACT");
        person.setPhEyeFilterStrategies(Collections.singletonList(phEyeFilterStrategy));
        identifiers.setPerson(person);*/

        final PhoneNumber phoneNumber = new PhoneNumber();
        final PhoneNumberFilterStrategy phoneNumberFilterStrategy = new PhoneNumberFilterStrategy();
        phoneNumberFilterStrategy.setStrategy("REDACT");
        phoneNumber.setPhoneNumberFilterStrategies(Collections.singletonList(phoneNumberFilterStrategy));
        identifiers.setPhoneNumber(phoneNumber);

        final PhoneNumberExtension phoneNumberExtension = new PhoneNumberExtension();
        final PhoneNumberExtensionFilterStrategy phoneNumberExtensionFilterStrategy = new PhoneNumberExtensionFilterStrategy();
        phoneNumberExtensionFilterStrategy.setStrategy("REDACT");
        phoneNumberExtension.setPhoneNumberExtensionFilterStrategies(Collections.singletonList(phoneNumberExtensionFilterStrategy));
        identifiers.setPhoneNumberExtension(phoneNumberExtension);

        final PhysicianName physicianName = new PhysicianName();
        final PhysicianNameFilterStrategy physicianNameFilterStrategy = new PhysicianNameFilterStrategy();
        physicianNameFilterStrategy.setStrategy("REDACT");
        physicianName.setPhysicianNameFilterStrategies(Collections.singletonList(physicianNameFilterStrategy));
        identifiers.setPhysicianName(physicianName);

        final Ssn ssn = new Ssn();
        final SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();
        ssnFilterStrategy.setStrategy("REDACT");
        ssn.setSsnFilterStrategies(Collections.singletonList(ssnFilterStrategy));
        identifiers.setSsn(ssn);

        final State state = new State();
        final ai.philterd.phileas.services.strategies.dynamic.StateFilterStrategy stateFilterStrategy = new ai.philterd.phileas.services.strategies.dynamic.StateFilterStrategy();
        stateFilterStrategy.setStrategy("REDACT");
        state.setStateFilterStrategies(Collections.singletonList(stateFilterStrategy));
        identifiers.setState(state);

        final StateAbbreviation stateAbbreviation = new StateAbbreviation();
        final StateAbbreviationFilterStrategy stateAbbreviationFilterStrategy = new StateAbbreviationFilterStrategy();
        stateAbbreviationFilterStrategy.setStrategy("REDACT");
        stateAbbreviation.setStateAbbreviationsFilterStrategies(Collections.singletonList(stateAbbreviationFilterStrategy));
        identifiers.setStateAbbreviation(stateAbbreviation);

        final StreetAddress streetAddress = new StreetAddress();
        final StreetAddressFilterStrategy streetAddressFilterStrategy = new StreetAddressFilterStrategy();
        streetAddressFilterStrategy.setStrategy("REDACT");
        streetAddress.setStreetAddressFilterStrategies(Collections.singletonList(streetAddressFilterStrategy));
        identifiers.setStreetAddress(streetAddress);

        final Surname surname = new Surname();
        final ai.philterd.phileas.services.strategies.dynamic.SurnameFilterStrategy surnameFilterStrategy = new ai.philterd.phileas.services.strategies.dynamic.SurnameFilterStrategy();
        surnameFilterStrategy.setStrategy("REDACT");
        surname.setSurnameFilterStrategies(Collections.singletonList(surnameFilterStrategy));
        identifiers.setSurname(surname);

        final TrackingNumber trackingNumber = new TrackingNumber();
        final TrackingNumberFilterStrategy trackingNumberFilterStrategy = new TrackingNumberFilterStrategy();
        trackingNumberFilterStrategy.setStrategy("REDACT");
        trackingNumber.setTrackingNumberFilterStrategies(Collections.singletonList(trackingNumberFilterStrategy));
        identifiers.setTrackingNumber(trackingNumber);

        final Url url = new Url();
        final UrlFilterStrategy urlFilterStrategy = new UrlFilterStrategy();
        urlFilterStrategy.setStrategy("REDACT");
        url.setUrlFilterStrategies(Collections.singletonList(urlFilterStrategy));
        identifiers.setUrl(url);

        final Vin vin = new Vin();
        final VinFilterStrategy vinFilterStrategy = new VinFilterStrategy();
        vinFilterStrategy.setStrategy("REDACT");
        vin.setVinFilterStrategies(Collections.singletonList(vinFilterStrategy));
        identifiers.setVin(vin);

        final ZipCode zipCode = new ZipCode();
        final ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setStrategy("REDACT");
        zipCode.setZipCodeFilterStrategies(Collections.singletonList(zipCodeFilterStrategy));
        identifiers.setZipCode(zipCode);

        policy.setIdentifiers(identifiers);

        return policy;

    }

    public List<Span> findPii(final String text, final Policy policy) throws Exception {
        if (filterService == null) {
            // Lazy initialization for cases like unit tests where call() isn't called
            final Properties properties = new Properties();
            final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);
            this.filterService = new PlainTextFilterService(
                    phileasConfiguration,
                    new DefaultContextService(),
                    new InMemoryVectorService(),
                    HttpClients.createDefault()
            );
        }

        final TextFilterResult response = filterService.filter(policy, "context", text);
        return response.getExplanation().appliedSpans();

    }

    public List<Span> findPii(final String text) throws Exception {
        return findPii(text, createDefaultPolicy());
    }

}
