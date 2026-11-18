package app.orious.authenticator.vault;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import app.orious.authenticator.crypto.CryptoUtils;
import app.orious.authenticator.crypto.MasterKey;
import app.orious.authenticator.crypto.SCryptParameters;
import app.orious.authenticator.importers.AegisImporter;
import app.orious.authenticator.importers.DatabaseImporter;
import app.orious.authenticator.notes.NoteEntry;
import app.orious.authenticator.otp.TotpInfo;
import app.orious.authenticator.passwords.PasswordEntry;
import app.orious.authenticator.vault.slots.PasswordSlot;
import app.orious.authenticator.vault.slots.SlotList;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

@RunWith(RobolectricTestRunner.class)
public class VaultNotesTest {
    @Test
    public void testEncryptedVaultBackupAndRestorePreservesNotes() throws Exception {
        Vault source = new Vault();
        VaultEntry otp = new VaultEntry(new TotpInfo("ABCDEFGHIJKLMNOP".getBytes(StandardCharsets.US_ASCII)), "alice", "Example");
        PasswordEntry pass = new PasswordEntry("Email", "alice@example.com", "P@ssw0rd!", "https://mail.example.com", "Recovery");
        NoteEntry note = new NoteEntry(
                "SSH Recovery Codes",
                "Code 1: 1111-2222\nCode 2: 3333-4444\n\nKeep offline.",
                true,
                1700000000000L,
                1700005000000L
        );

        source.getEntries().add(otp);
        source.getPasswordEntries().add(pass);
        source.getNotes().add(note);

        MasterKey masterKey = MasterKey.generate();
        char[] password = "strong-vault-password".toCharArray();
        PasswordSlot slot = new PasswordSlot();
        SCryptParameters scryptParams = new SCryptParameters(
                CryptoUtils.CRYPTO_SCRYPT_N,
                CryptoUtils.CRYPTO_SCRYPT_r,
                CryptoUtils.CRYPTO_SCRYPT_p,
                CryptoUtils.generateSalt()
        );
        SecretKey key = slot.deriveKey(password, scryptParams);
        Cipher encryptCipher = slot.createEncryptCipher(key);
        slot.setKey(masterKey, encryptCipher);

        SlotList slots = new SlotList();
        slots.add(slot);
        VaultFileCredentials creds = new VaultFileCredentials(masterKey, slots);

        VaultFile encryptedFile = new VaultFile();
        encryptedFile.setContent(source.toJson(), creds);
        byte[] rawVaultBytes = encryptedFile.toBytes();
        String rawVaultString = new String(rawVaultBytes, StandardCharsets.UTF_8);

        // Note title and body must never be stored in plaintext inside the encrypted vault file
        assertFalse(rawVaultString.contains("SSH Recovery Codes"));
        assertFalse(rawVaultString.contains("1111-2222"));

        // Verify decryption via AegisImporter (backup/restore flow)
        Context context = ApplicationProvider.getApplicationContext();
        AegisImporter importer = new AegisImporter(context);
        try (ByteArrayInputStream stream = new ByteArrayInputStream(rawVaultBytes)) {
            DatabaseImporter.State state = importer.read(stream, false);
            assertTrue(state.isEncrypted());
            state = ((AegisImporter.EncryptedState) state).decrypt(password);
            DatabaseImporter.Result result = state.convert();

            assertEquals(0, result.getErrors().size());
            assertEquals(1, result.getEntries().getValues().size());
            assertEquals(1, result.getPasswordEntries().getValues().size());
            assertEquals(1, result.getNotes().getValues().size());

            NoteEntry restored = result.getNotes().getByUUID(note.getUUID());
            assertNotNull(restored);
            assertEquals(note, restored);
            assertEquals("Code 1: 1111-2222\nCode 2: 3333-4444\n\nKeep offline.", restored.getContent());
        }

        // Also verify Vault.fromJson roundtrip preserves VERSION and notes
        VaultFile parsedFile = VaultFile.fromBytes(rawVaultBytes);
        JSONObject decryptedJson = parsedFile.getContent(creds);
        Vault restoredVault = Vault.fromJson(decryptedJson);
        assertEquals(note, restoredVault.getNotes().getByUUID(note.getUUID()));
    }

    @Test
    public void testVaultRepositoryNotesCrudAndWipe() {
        Context context = ApplicationProvider.getApplicationContext();
        Vault vault = new Vault();
        VaultRepository repo = new VaultRepository(context, vault, null);

        NoteEntry note = new NoteEntry("Seed Note", "Line A\nLine B", false);
        repo.addNote(note);

        assertTrue(repo.hasNoteByUUID(note.getUUID()));
        assertEquals(1, repo.getNotes().size());
        assertEquals(note, repo.getNoteByUUID(note.getUUID()));

        NoteEntry edited = new NoteEntry(note);
        edited.setTitle("Updated Note");
        edited.setContent("Line A\nLine B\nLine C");
        edited.setFavorite(true);
        NoteEntry previous = repo.replaceNote(edited);
        assertEquals(note, previous);
        assertEquals("Updated Note", repo.getNoteByUUID(note.getUUID()).getTitle());
        assertTrue(repo.getNoteByUUID(note.getUUID()).isFavorite());

        repo.wipeContents();
        assertTrue(repo.getNotes().isEmpty());
        assertNull(repo.getNoteByUUID(note.getUUID()));
    }

    @Test
    public void testHtmlExporterEscapesNoteContentAndPreservesLineBreaks() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        NoteEntry note = new NoteEntry("Tax <2026>", "Line 1 & more\nLine 2 <tag>", true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())) {
            VaultHtmlExporter.export(
                    context,
                    ps,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.singletonList(note)
            );
        }

        String html = baos.toString(StandardCharsets.UTF_8.name());
        assertTrue(html.contains("Private Notes"));
        assertTrue(html.contains("Tax &lt;2026&gt;"));
        assertTrue(html.contains("Line 1 &amp; more<br/>Line 2 &lt;tag&gt;"));
    }
}
