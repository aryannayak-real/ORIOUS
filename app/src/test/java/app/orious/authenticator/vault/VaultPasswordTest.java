package app.orious.authenticator.vault;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import app.orious.authenticator.importers.AegisImporter;
import app.orious.authenticator.importers.DatabaseImporter;
import app.orious.authenticator.otp.TotpInfo;
import app.orious.authenticator.passwords.PasswordEntry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VaultPasswordTest {
    @Test
    public void testVaultV4JsonRoundtripAndUnifiedOrder() throws Exception {
        Vault vault = new Vault();
        VaultEntry otp = new VaultEntry(new TotpInfo("ABCDEFGHIJKLMNOP".getBytes(StandardCharsets.US_ASCII)), "alice", "Example");
        PasswordEntry pass = new PasswordEntry("Email", "alice@example.com", "P@ssw0rd!", "https://mail.example.com", "Recovery");
        vault.getEntries().add(otp);
        vault.getPasswordEntries().add(pass);

        String otpId = "O:" + otp.getUUID();
        String passId = "P:" + pass.getUUID();
        vault.setUnifiedOrder(Arrays.asList(passId, otpId));

        Vault restored = Vault.fromJson(vault.toJson());
        assertEquals(1, restored.getEntries().getValues().size());
        assertEquals(1, restored.getPasswordEntries().getValues().size());
        assertEquals(Arrays.asList(passId, otpId), restored.getUnifiedOrder());

        restored.moveUnifiedEntry(otpId, passId);
        assertEquals(Arrays.asList(otpId, passId), restored.getUnifiedOrder());
    }

    @Test
    public void testWipeContentsClearsUnifiedOrder() {
        Context context = ApplicationProvider.getApplicationContext();
        Vault vault = new Vault();
        PasswordEntry pass = new PasswordEntry("SSH", "root", "top-secret", "", "");
        vault.getPasswordEntries().add(pass);
        vault.setUnifiedOrder(Collections.singletonList("P:" + pass.getUUID()));

        VaultRepository repo = new VaultRepository(context, vault, null);
        repo.wipeContents();

        assertTrue(repo.getEntries().isEmpty());
        assertTrue(repo.getPasswordEntries().isEmpty());
        assertTrue(repo.getUnifiedOrder().isEmpty());
    }

    @Test
    public void testAegisImporterPreservesPasswordEntriesAndUnifiedOrder() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Vault source = new Vault();
        VaultEntry otp = new VaultEntry(new TotpInfo("12345678901234567890".getBytes(StandardCharsets.US_ASCII)), "dev", "GitHub");
        PasswordEntry pass = new PasswordEntry("GitHub", "dev@example.com", "Secret!123", "https://github.com", "");
        source.getEntries().add(otp);
        source.getPasswordEntries().add(pass);
        String passId = "P:" + pass.getUUID();
        String otpId = "O:" + otp.getUUID();
        source.setUnifiedOrder(Arrays.asList(passId, otpId));

        VaultFile file = new VaultFile();
        file.setContent(source.toJson());

        AegisImporter importer = new AegisImporter(context);
        try (ByteArrayInputStream stream = new ByteArrayInputStream(file.toBytes())) {
            DatabaseImporter.State state = importer.read(stream, false);
            DatabaseImporter.Result result = state.convert();

            assertEquals(0, result.getErrors().size());
            assertEquals(1, result.getEntries().getValues().size());
            assertEquals(1, result.getPasswordEntries().getValues().size());
            assertEquals(Arrays.asList(passId, otpId), result.getUnifiedOrder());
        }
    }

    @Test
    public void testHtmlExporterIncludesEscapedPasswordEntries() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PasswordEntry pass = new PasswordEntry("Bank <Main>", "user&one", "p@ss\"word", "https://bank.example", "note <script>");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name())) {
            VaultHtmlExporter.export(context, ps, Collections.emptyList(), Collections.singletonList(pass));
        }

        String html = baos.toString(StandardCharsets.UTF_8.name());
        assertTrue(html.contains("Passwords"));
        assertTrue(html.contains("Bank &lt;Main&gt;"));
        assertTrue(html.contains("user&amp;one"));
        assertTrue(html.contains("note &lt;script&gt;"));
    }
}
