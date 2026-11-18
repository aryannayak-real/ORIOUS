package app.orious.authenticator.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import app.orious.authenticator.AegisApplication;
import app.orious.authenticator.R;
import app.orious.authenticator.notes.NoteEntry;
import app.orious.authenticator.otp.TotpInfo;
import app.orious.authenticator.passwords.PasswordEntry;
import app.orious.authenticator.ui.views.EntryAdapter;
import app.orious.authenticator.ui.views.NotebookFragment;
import app.orious.authenticator.vault.VaultEntry;
import app.orious.authenticator.vault.VaultManager;
import app.orious.authenticator.vault.VaultRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class MainNotebookIntegrationTest {
    private AegisApplication _app;
    private VaultManager _vaultManager;

    @Before
    public void setUp() throws Exception {
        _app = ApplicationProvider.getApplicationContext();
        _vaultManager = _app.getVaultManager();
        if (_vaultManager.isVaultLoaded()) {
            _vaultManager.lock(false);
        }
        File vaultFile = new File(_app.getFilesDir(), VaultRepository.FILENAME);
        if (vaultFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            vaultFile.delete();
        }
        VaultRepository repo = _vaultManager.initNew(null);
        repo.addEntry(new VaultEntry(
                new TotpInfo("ABCDEFGHIJKLMNOP".getBytes(StandardCharsets.US_ASCII)),
                "alice@example.com",
                "GitHub"
        ));
        repo.addPasswordEntry(new PasswordEntry(
                "Vault Mail",
                "alice@example.com",
                "P@ssw0rd!",
                "https://mail.example.com",
                ""
        ));
        repo.addNote(new NoteEntry("Recovery Codes", "Line 1\nLine 2", true, 100L, 200L));
        repo.addNote(new NoteEntry("Shopping List", "Milk\nCoffee", false, 150L, 250L));
        _vaultManager.save();
    }

    @Test
    public void testThreeSectionsSeparationAndNotebookFiltering() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            ShadowLooper.idleMainLooper();

            TextView headerTitle = activity.findViewById(R.id.header_title);
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_navigation);
            View notebookContainer = activity.findViewById(R.id.notebook_container);
            RecyclerView rvKeyProfiles = activity.findViewById(R.id.rvKeyProfiles);
            EntryAdapter entryAdapter = (EntryAdapter) rvKeyProfiles.getAdapter();
            assertNotNull(entryAdapter);

            // 1. Starts on Authenticator tab showing only OTP entries
            assertEquals(activity.getString(R.string.header_title_authenticator), headerTitle.getText().toString());
            assertEquals(View.GONE, notebookContainer.getVisibility());
            assertEquals(1, entryAdapter.getShownEntriesCount());
            int otpPos = entryAdapter.isErrorCardShown() ? 1 : 0;
            assertNotEquals(R.layout.card_password_entry, entryAdapter.getItemViewType(otpPos));

            // 2. Switch to Passwords tab showing only Password entries
            bottomNav.setSelectedItemId(R.id.nav_passwords);
            ShadowLooper.idleMainLooper();
            assertEquals(activity.getString(R.string.header_title_passwords), headerTitle.getText().toString());
            assertEquals(View.GONE, notebookContainer.getVisibility());
            assertEquals(1, entryAdapter.getShownEntriesCount());
            int pwPos = entryAdapter.isErrorCardShown() ? 1 : 0;
            assertEquals(R.layout.card_password_entry, entryAdapter.getItemViewType(pwPos));

            // 3. Switch to Private Notebook tab showing both notes
            bottomNav.setSelectedItemId(R.id.nav_notebook);
            ShadowLooper.idleMainLooper();
            assertEquals(activity.getString(R.string.header_title_notebook), headerTitle.getText().toString());
            assertEquals(View.VISIBLE, notebookContainer.getVisibility());

            RecyclerView rvNotes = activity.findViewById(R.id.rv_notes);
            assertNotNull(rvNotes);
            assertEquals(2, rvNotes.getAdapter().getItemCount());

            // Search filter matches multiline note content
            NotebookFragment notebookFragment = (NotebookFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.notebook_container);
            assertNotNull(notebookFragment);
            notebookFragment.setSearchFilter("coffee");
            ShadowLooper.idleMainLooper();
            assertEquals(1, rvNotes.getAdapter().getItemCount());
            notebookFragment.setSearchFilter(null);
            ShadowLooper.idleMainLooper();
            assertEquals(2, rvNotes.getAdapter().getItemCount());

            // Favorite filter chip narrows to the 1 favorited note
            Chip favoritesChip = activity.findViewById(R.id.notebook_chip_favorites);
            favoritesChip.setChecked(true);
            ShadowLooper.idleMainLooper();
            assertEquals(1, rvNotes.getAdapter().getItemCount());

            // Lock clears in-memory entries and notes
            _vaultManager.lock(true);
            ShadowLooper.idleMainLooper();
            assertEquals(0, rvNotes.getAdapter().getItemCount());
            assertFalse(_vaultManager.isVaultLoaded());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.notebook_empty_state).getVisibility());
        }
    }
}
