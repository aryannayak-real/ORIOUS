package app.orious.authenticator.passwords;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class PasswordGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final int MAX_LENGTH = 64;

    private PasswordGenerator() {}

    public static String generate(
            int length,
            boolean uppercase,
            boolean lowercase,
            boolean numbers,
            boolean symbols,
            boolean excludeAmbiguous
    ) {
        List<String> enabled = new ArrayList<>();

        if (uppercase) enabled.add("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (lowercase) enabled.add("abcdefghijklmnopqrstuvwxyz");
        if (numbers) enabled.add("0123456789");
        if (symbols) enabled.add("!@#$%^&*()-_=+[]{};:,.?/\\|~");

        if (enabled.isEmpty()) {
            throw new IllegalArgumentException("Select at least one character type");
        }
        if (length < enabled.size() || length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Password length must be between the number of selected character types and " + MAX_LENGTH
            );
        }

        if (excludeAmbiguous) {
            List<String> filtered = new ArrayList<>();
            for (String chars : enabled) {
                filtered.add(chars.replace("O", "")
                        .replace("0", "")
                        .replace("I", "")
                        .replace("l", "")
                        .replace("1", ""));
            }
            enabled = filtered;
        }

        StringBuilder all = new StringBuilder();
        for (String chars : enabled) {
            if (chars.isEmpty()) {
                throw new IllegalArgumentException("No characters available");
            }
            all.append(chars);
        }

        char[] result = new char[length];

        // Guarantee at least one character from every enabled category.
        int index = 0;
        for (String chars : enabled) {
            result[index++] = chars.charAt(RANDOM.nextInt(chars.length()));
        }

        while (index < length) {
            result[index++] = all.charAt(RANDOM.nextInt(all.length()));
        }

        // Secure Fisher-Yates shuffle.
        for (int i = result.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = result[i];
            result[i] = result[j];
            result[j] = temp;
        }

        return new String(result);
    }
}
