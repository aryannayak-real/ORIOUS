package app.orious.authenticator.notes;

public class NoteEntryException extends Exception {
    public NoteEntryException(String message) {
        super(message);
    }

    public NoteEntryException(Throwable cause) {
        super(cause);
    }
}
