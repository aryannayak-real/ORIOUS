package app.orious.authenticator.ui.views;

import app.orious.authenticator.passwords.PasswordEntry;
import app.orious.authenticator.vault.VaultEntry;

import java.util.UUID;

public final class UnifiedEntry {
    public enum Type {
        OTP,
        PASSWORD
    }

    private final VaultEntry otpEntry;
    private final PasswordEntry passwordEntry;

    private UnifiedEntry(VaultEntry otpEntry, PasswordEntry passwordEntry) {
        this.otpEntry = otpEntry;
        this.passwordEntry = passwordEntry;
    }

    public static UnifiedEntry otp(VaultEntry entry) {
        return new UnifiedEntry(entry, null);
    }

    public static UnifiedEntry password(PasswordEntry entry) {
        return new UnifiedEntry(null, entry);
    }

    public Type getType() {
        return otpEntry != null ? Type.OTP : Type.PASSWORD;
    }

    public boolean isOtp() {
        return otpEntry != null;
    }

    public VaultEntry getOtpEntry() {
        return otpEntry;
    }

    public PasswordEntry getPasswordEntry() {
        return passwordEntry;
    }

    public UUID getUUID() {
        return isOtp() ? otpEntry.getUUID() : passwordEntry.getUUID();
    }

    public String getName() {
        return isOtp() ? otpEntry.getName() : passwordEntry.getName();
    }
}
