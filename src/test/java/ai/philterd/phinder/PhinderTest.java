package ai.philterd.phinder;

import ai.philterd.phileas.model.filtering.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhinderTest {

    @Test
    public void testFindPii() throws Exception {
        Phinder phinder = new Phinder();
        String text = "Contact me at test@example.com";
        // The findPii(String) method now uses the default policy
        List<Span> spans = phinder.findPii(text);

        assertFalse(spans.isEmpty(), "Should have found at least one PII span");
        
        boolean foundEmail = false;
        for (Span span : spans) {
            if (span.getText().equals("test@example.com")) {
                foundEmail = true;
                break;
            }
        }
        
        assertTrue(foundEmail, "Should have identified test@example.com");
    }
}
