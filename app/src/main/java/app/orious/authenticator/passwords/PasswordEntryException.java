package app.orious.authenticator.passwords;

public class PasswordEntryException extends Exception {
    public PasswordEntryException(Throwable cause) {
        super(cause);
    }

    public PasswordEntryException(String message) {
        super(message);
    }
}
