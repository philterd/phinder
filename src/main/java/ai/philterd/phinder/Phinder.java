package ai.philterd.phinder;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.EmailAddress;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(name = "phinder", mixinStandardHelpOptions = true, version = "phinder 1.0.0",
        description = "Identifies PII in text using Phileas.")
public class Phinder implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, description = "The input text file.", required = true)
    private File inputFile;

    @Option(names = {"-p", "--policy"}, description = "The Phileas policy (JSON file).")
    private File policyFile;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Phinder()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (!inputFile.exists()) {
            System.err.println("Input file does not exist: " + inputFile.getAbsolutePath());
            return 1;
        }

        String text = FileUtils.readFileToString(inputFile, StandardCharsets.UTF_8);

        Policy policy;
        if (policyFile != null) {
            if (!policyFile.exists()) {
                System.err.println("Policy file does not exist: " + policyFile.getAbsolutePath());
                return 1;
            }
            String policyJson = FileUtils.readFileToString(policyFile, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            policy = gson.fromJson(policyJson, Policy.class);
        } else {
            policy = createDefaultPolicy();
        }

        List<Span> spans = findPii(text, policy);

        System.out.println("PII found:");
        for (Span span : spans) {
            System.out.printf(" - %s (type: %s, confidence: %.2f)\n",
                    span.getText(), span.getFilterType(), span.getConfidence());
        }

        return 0;
    }

    private Policy createDefaultPolicy() {
        Policy policy = new Policy();
        Identifiers identifiers = new Identifiers();

        EmailAddress emailAddress = new EmailAddress();
        EmailAddressFilterStrategy strategy = new EmailAddressFilterStrategy();
        strategy.setStrategy("REDACT");
        emailAddress.setEmailAddressFilterStrategies(Collections.singletonList(strategy));
        identifiers.setEmailAddress(emailAddress);

        policy.setIdentifiers(identifiers);
        return policy;
    }

    public List<Span> findPii(String text, Policy policy) throws Exception {
        // Configure Phileas
        Properties properties = new Properties();
        PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        // The filter service does the work.
        PlainTextFilterService filterService = new PlainTextFilterService(
                phileasConfiguration,
                new DefaultContextService(),
                new InMemoryVectorService(),
                HttpClients.createDefault()
        );

        // Filter the text.
        TextFilterResult response = filterService.filter(policy, "context", text);

        return response.getExplanation().appliedSpans();
    }

    // Keep the old method for backward compatibility if needed, or remove it.
    // I'll update PhinderTest to use the new method.
    public List<Span> findPii(String text) throws Exception {
        return findPii(text, createDefaultPolicy());
    }

}
