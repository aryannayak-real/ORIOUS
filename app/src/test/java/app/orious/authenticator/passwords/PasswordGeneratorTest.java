package app.orious.authenticator.passwords;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordGeneratorTest {
    @Test
    public void testGeneratesRequestedLengthsIncludingMaxLength() {
        String shortPass = PasswordGenerator.generate(4, true, true, true, true, false);
        assertEquals(4, shortPass.length());

        String maxPass = PasswordGenerator.generate(PasswordGenerator.MAX_LENGTH, true, true, true, true, false);
        assertEquals(64, maxPass.length());
    }

    @Test
    public void testGuaranteesAllEnabledCategories() {
        String password = PasswordGenerator.generate(24, true, true, true, true, false);
        assertTrue(password.chars().anyMatch(Character::isUpperCase));
        assertTrue(password.chars().anyMatch(Character::isLowerCase));
        assertTrue(password.chars().anyMatch(Character::isDigit));
        assertTrue(password.chars().anyMatch(ch -> "!@#$%^&*()-_=+[]{};:,.?/\\|~".indexOf(ch) >= 0));
    }

    @Test
    public void testExcludeAmbiguousCharacters() {
        for (int i = 0; i < 20; i++) {
            String password = PasswordGenerator.generate(48, true, true, true, false, true);
            assertFalse(password.contains("O"));
            assertFalse(password.contains("0"));
            assertFalse(password.contains("I"));
            assertFalse(password.contains("l"));
            assertFalse(password.contains("1"));
        }
    }

    @Test
    public void testInvalidOptionsThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGenerator.generate(12, false, false, false, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGenerator.generate(3, true, true, true, true, false));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGenerator.generate(PasswordGenerator.MAX_LENGTH + 1, true, true, true, true, false));
    }
}
