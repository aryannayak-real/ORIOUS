package app.orious.authenticator.passwords;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import org.json.JSONObject;
import org.junit.Test;

public class PasswordEntryTest {
    @Test
    public void testJsonRoundtripWithOptionalUsername() throws Exception {
        PasswordEntry entry = new PasswordEntry("Router Admin", "", "s3cr3t-pass", "https://192.168.1.1", "Local gateway");
        entry.validate();

        JSONObject json = entry.toJson();
        PasswordEntry parsed = PasswordEntry.fromJson(json);

        assertEquals(entry, parsed);
        assertEquals(entry.hashCode(), parsed.hashCode());
        assertEquals("", parsed.getUsername());
        assertEquals("https://192.168.1.1", parsed.getWebsite());
    }

    @Test
    public void testCopyConstructorAndEquality() {
        PasswordEntry original = new PasswordEntry("GitHub", "octocat", "Pass!123", "https://github.com", "Personal");
        PasswordEntry copy = new PasswordEntry(original);

        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());

        copy.setPassword("Pass!456");
        assertNotEquals(original, copy);
    }

    @Test
    public void testValidationRejectsMissingNameOrPassword() {
        PasswordEntry missingName = new PasswordEntry("   ", "user", "pass", "", "");
        assertThrows(PasswordEntryException.class, missingName::validate);

        PasswordEntry missingPassword = new PasswordEntry("Service", "user", "", "", "");
        assertThrows(PasswordEntryException.class, missingPassword::validate);
    }
}
