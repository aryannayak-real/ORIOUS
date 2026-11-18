package app.orious.authenticator.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;

import app.orious.authenticator.AegisApplication;
import app.orious.authenticator.R;
import app.orious.authenticator.notes.NoteEntry;
import app.orious.authenticator.vault.VaultFile;
import app.orious.authenticator.vault.VaultManager;
import app.orious.authenticator.vault.VaultRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class NoteEditorActivityTest {
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
        _vaultManager.initNew(null);
    }

    @Test
    public void testCreateEditFavoriteAndDeleteNoteFlow() throws Exception {
        // 1. Create a new multiline note
        try (ActivityController<NoteEditorActivity> controller =
                     Robolectric.buildActivity(NoteEditorActivity.class).setup()) {
            NoteEditorActivity activity = controller.get();
            EditText titleEdit = activity.findViewById(R.id.note_editor_title);
            EditText contentEdit = activity.findViewById(R.id.note_editor_content);

            titleEdit.setText("Emergency Backup Keys");
            contentEdit.setText("Key 1: ALPHA\n\nKey 2: BRAVO");

            activity.onOptionsItemSelected(
                    new org.robolectric.fakes.RoboMenuItem(R.id.action_note_favorite));
            activity.onOptionsItemSelected(
                    new org.robolectric.fakes.RoboMenuItem(R.id.action_note_save));

            awaitAsyncVaultSave(activity);
            assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).getResultCode());
            assertTrue(activity.isFinishing());
        }

        // Verify persisted on disk inside vault file
        VaultFile diskFile = VaultRepository.readVaultFile(_app);
        VaultRepository diskRepo = VaultRepository.fromFile(_app, diskFile, null);
        Collection<NoteEntry> savedNotes = diskRepo.getNotes();
        assertEquals(1, savedNotes.size());
        NoteEntry saved = savedNotes.iterator().next();
        assertEquals("Emergency Backup Keys", saved.getTitle());
        assertEquals("Key 1: ALPHA\n\nKey 2: BRAVO", saved.getContent());
        assertTrue(saved.isFavorite());

        // 2. Edit the existing note and verify multiline edits persist
        Intent editIntent = new Intent(_app, NoteEditorActivity.class);
        editIntent.putExtra(NoteEditorActivity.EXTRA_NOTE_UUID, saved.getUUID().toString());
        try (ActivityController<NoteEditorActivity> editController =
                     Robolectric.buildActivity(NoteEditorActivity.class, editIntent).setup()) {
            NoteEditorActivity activity = editController.get();
            EditText titleEdit = activity.findViewById(R.id.note_editor_title);
            EditText contentEdit = activity.findViewById(R.id.note_editor_content);

            assertEquals("Emergency Backup Keys", titleEdit.getText().toString());
            assertEquals("Key 1: ALPHA\n\nKey 2: BRAVO", contentEdit.getText().toString());

            titleEdit.setText("Emergency Backup Keys (Updated)");
            contentEdit.setText("Key 1: ALPHA\nKey 2: BRAVO\nKey 3: CHARLIE");
            activity.onOptionsItemSelected(
                    new org.robolectric.fakes.RoboMenuItem(R.id.action_note_save));

            awaitAsyncVaultSave(activity);
            assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).getResultCode());
        }

        NoteEntry updated = _vaultManager.getVault().getNoteByUUID(saved.getUUID());
        assertNotNull(updated);
        assertEquals("Emergency Backup Keys (Updated)", updated.getTitle());
        assertEquals("Key 1: ALPHA\nKey 2: BRAVO\nKey 3: CHARLIE", updated.getContent());

        // 3. Delete note requires confirmation dialog before removing from vault
        Intent deleteIntent = new Intent(_app, NoteEditorActivity.class);
        deleteIntent.putExtra(NoteEditorActivity.EXTRA_NOTE_UUID, saved.getUUID().toString());
        try (ActivityController<NoteEditorActivity> deleteController =
                     Robolectric.buildActivity(NoteEditorActivity.class, deleteIntent).setup()) {
            NoteEditorActivity activity = deleteController.get();
            activity.onOptionsItemSelected(
                    new org.robolectric.fakes.RoboMenuItem(R.id.action_note_delete));
            ShadowLooper.idleMainLooper();

            // Note must still exist until user confirms deletion
            assertNotNull(_vaultManager.getVault().getNoteByUUID(saved.getUUID()));

            AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
            assertNotNull(dialog);
            assertTrue(dialog.isShowing());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();

            awaitAsyncVaultSave(activity);
            assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).getResultCode());
            assertTrue(activity.isFinishing());
        }

        assertNull(_vaultManager.getVault().getNoteByUUID(saved.getUUID()));
        assertTrue(_vaultManager.getVault().getNotes().isEmpty());
    }

    private static void awaitAsyncVaultSave(Activity activity) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!activity.isFinishing() && System.nanoTime() < deadline) {
            ShadowLooper.idleMainLooper();
            Thread.sleep(15);
        }
        ShadowLooper.idleMainLooper();
    }
}
