package app.orious.authenticator.vault;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import app.orious.authenticator.BackupsVersioningStrategy;
import app.orious.authenticator.Preferences;
import app.orious.authenticator.R;
import app.orious.authenticator.crypto.KeyStoreHandle;
import app.orious.authenticator.crypto.KeyStoreHandleException;
import app.orious.authenticator.database.AuditLogRepository;
import app.orious.authenticator.ui.dialogs.Dialogs;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VaultManager {
    private static final String TAG = VaultManager.class.getSimpleName();

    private final Context _context;
    private final Preferences _prefs;

    private VaultRepository _repo;

    private final VaultBackupManager _backups;
    private final BackupManager _androidBackups;

    private final List<LockListener> _lockListeners;
    private boolean _blockAutoLock;

    private final AuditLogRepository _auditLogRepository;
    private final ExecutorService _saveExecutor = Executors.newSingleThreadExecutor();
    private final Handler _mainHandler = new Handler(Looper.getMainLooper());

    public VaultManager(@NonNull Context context, AuditLogRepository auditLogRepository) {
        _context = context;
        _prefs = new Preferences(_context);
        _backups = new VaultBackupManager(_context, auditLogRepository);
        _androidBackups = new BackupManager(context);
        _lockListeners = new ArrayList<>();
        _auditLogRepository = auditLogRepository;
    }

    /**
     * Initializes the vault repository with a new empty vault and the given creds. It can
     * only be called if isVaultLoaded() returns false.
     */
    @NonNull
    public VaultRepository initNew(@Nullable VaultFileCredentials creds) throws VaultRepositoryException {
        if (isVaultLoaded()) {
            throw new IllegalStateException("Vault manager is already initialized");
        }

        VaultRepository repo = new VaultRepository(_context, new Vault(), creds);
        repo.save();
        _repo = repo;

        return getVault();
    }

    /**
     * Initializes the vault repository by decrypting the given vaultFile with the given
     * creds. It can only be called if isVaultLoaded() returns false.
     */
    @NonNull
    public VaultRepository loadFrom(@NonNull VaultFile vaultFile, @Nullable VaultFileCredentials creds) throws VaultRepositoryException {
        if (isVaultLoaded()) {
            throw new IllegalStateException("Vault manager is already initialized");
        }

        _repo = VaultRepository.fromFile(_context, vaultFile, creds);

        return getVault();
    }

    @NonNull
    public VaultRepository loadFrom(@NonNull VaultFile vaultFile) throws VaultRepositoryException {
        return loadFrom(vaultFile, null);
    }

    /**
     * Locks the vault and the app.
     * @param userInitiated whether or not the user initiated the lock in MainActivity.
     */
    public void lock(boolean userInitiated) {
        _repo = null;

        for (LockListener listener : _lockListeners) {
            listener.onLocked(userInitiated);
        }
    }

    public void enableEncryption(VaultFileCredentials creds) throws VaultRepositoryException {
        getVault().setCredentials(creds);
        saveAndBackup();
    }

    public void disableEncryption() throws VaultRepositoryException {
        getVault().setCredentials(null);
        save();

        // remove any keys that are stored in the KeyStore
        try {
            KeyStoreHandle handle = new KeyStoreHandle();
            handle.clear();
        } catch (KeyStoreHandleException e) {
            // this cleanup operation is not strictly necessary, so we ignore any exceptions here
            Log.w(TAG, "Failed to clear KeyStore handle when disabling encryption", e);
        }
    }

    public void save() throws VaultRepositoryException {
        getVault().save();
    }

    public void saveAsync(@Nullable SaveCallback callback) {
        saveInternalAsync(false, callback);
    }

    public void saveAndBackupAsync(@Nullable SaveCallback callback) {
        saveInternalAsync(true, callback);
    }

    private void saveInternalAsync(boolean backup, @Nullable SaveCallback callback) {
        if (!isVaultLoaded()) {
            if (callback != null) {
                _mainHandler.post(() -> callback.onComplete(
                        new VaultRepositoryException("Vault manager is not initialized")
                ));
            }
            return;
        }

        VaultRepository repo = getVault();
        JSONObject snapshot = repo.snapshotJson();
        _saveExecutor.execute(() -> {
            try {
                repo.saveSnapshot(snapshot);
                if (backup) {
                    runBackups(repo, snapshot);
                }
                if (callback != null) {
                    _mainHandler.post(() -> callback.onComplete(null));
                }
            } catch (VaultRepositoryException e) {
                Log.e(TAG, "Async vault save failed", e);
                if (callback != null) {
                    _mainHandler.post(() -> callback.onComplete(e));
                }
            }
        });
    }

    public void saveAndBackup() throws VaultRepositoryException {
        save();
        runBackups(getVault(), null);
    }

    private void runBackups(VaultRepository repo, @Nullable JSONObject snapshot) {
        boolean backedUp = false;
        if (repo.isEncryptionEnabled()) {
            if (_prefs.isBackupsEnabled()) {
                backedUp = true;
                try {
                    scheduleBackup(repo, snapshot);
                } catch (VaultRepositoryException e) {
                    _prefs.setBuiltInBackupResult(new Preferences.BackupResult(e));
                }
            }

            if (_prefs.isAndroidBackupsEnabled()) {
                backedUp = true;
                scheduleAndroidBackup();
            }
        }

        if (!backedUp) {
            _prefs.setIsBackupReminderNeeded(true);
        }
    }

    public void scheduleBackup() throws VaultRepositoryException {
        scheduleBackup(getVault(), null);
    }

    private void scheduleBackup(VaultRepository repo, @Nullable JSONObject snapshot) throws VaultRepositoryException {
        _prefs.setIsBackupReminderNeeded(false);

        try {
            File dir = new File(_context.getCacheDir(), "backup");
            if (!dir.exists() && !dir.mkdir()) {
                throw new IOException(String.format("Unable to create directory %s", dir));
            }

            File tempFile = File.createTempFile(VaultBackupManager.FILENAME_PREFIX, ".json", dir);
            try (OutputStream outStream = new FileOutputStream(tempFile)) {
                if (snapshot != null) {
                    VaultFile vaultFile = new VaultFile();
                    VaultFileCredentials creds = repo.getCredentials();
                    if (creds != null) {
                        vaultFile.setContent(snapshot, creds.exportable());
                    } else {
                        vaultFile.setContent(snapshot);
                    }
                    outStream.write(vaultFile.toBytes());
                } else {
                    repo.export(outStream);
                }
            }
            BackupsVersioningStrategy strategy = _prefs.getBackupVersioningStrategy();
            Uri uri = _prefs.getBackupsLocation();
            int versionsToKeep = _prefs.getBackupsVersionCount();

            _backups.scheduleBackup(tempFile, strategy, uri, versionsToKeep);
        } catch (IOException | VaultFileException e) {
            throw new VaultRepositoryException(e);
        }
    }

    public void scheduleAndroidBackup() {
        _prefs.setIsBackupReminderNeeded(false);
        _androidBackups.dataChanged();
    }

    public boolean isAutoLockEnabled(int autoLockType) {
        return _prefs.isAutoLockTypeEnabled(autoLockType)
                && isVaultLoaded()
                && getVault().isEncryptionEnabled();
    }

    public void registerLockListener(LockListener listener) {
        _lockListeners.add(listener);
    }

    public void unregisterLockListener(LockListener listener) {
        _lockListeners.remove(listener);
    }

    /**
     * Sets whether to block automatic lock on minimization. This should only be called
     * by activities before invoking an intent that shows a DocumentsUI, because that
     * action leads AppLifecycleObserver to believe that the app has been minimized.
     */
    public void setBlockAutoLock(boolean block) {
        _blockAutoLock = block;
    }

    /**
     * Reports whether automatic lock on minimization is currently blocked.
     */
    public boolean isAutoLockBlocked() {
        return _blockAutoLock;
    }

    public boolean isVaultLoaded() {
        return _repo != null;
    }

    public boolean isVaultInitNeeded() {
        return !isVaultLoaded() && !VaultRepository.fileExists(_context);
    }

    @NonNull
    public VaultRepository getVault() {
        if (!isVaultLoaded()) {
            throw new IllegalStateException("Vault manager is not initialized");
        }

        return _repo;
    }

    /**
     * Starts an external activity, temporarily blocks automatic lock of Aegis and
     * shows an error dialog if the target activity is not found.
     */
    public void fireIntentLauncher(Activity activity, Intent intent, ActivityResultLauncher<Intent> resultLauncher) {
        setBlockAutoLock(true);

        try {
            resultLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to launch intent", e);

            if (isDocsAction(intent.getAction())) {
                Dialogs.showErrorDialog(activity, R.string.documentsui_error, e);
            } else {
                throw e;
            }
        }
    }

    /**
     * Starts an external activity, temporarily blocks automatic lock of Aegis and
     * shows an error dialog if the target activity is not found.
     */
    public void fireIntentLauncher(Fragment fragment, Intent intent, ActivityResultLauncher<Intent> resultLauncher) {
        setBlockAutoLock(true);

        try {
            resultLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to launch intent", e);

            if (isDocsAction(intent.getAction())) {
                Dialogs.showErrorDialog(fragment.requireContext(), R.string.documentsui_error, e);
            } else {
                throw e;
            }
        }
    }

    private static boolean isDocsAction(@Nullable String action) {
        return action != null && (action.equals(Intent.ACTION_GET_CONTENT)
                || action.equals(Intent.ACTION_CREATE_DOCUMENT)
                || action.equals(Intent.ACTION_OPEN_DOCUMENT)
                || action.equals(Intent.ACTION_OPEN_DOCUMENT_TREE));
    }

    public interface LockListener {
        /**
         * Called when the vault lock status changes
         * @param userInitiated whether or not the user initiated the lock in MainActivity.
         */
        void onLocked(boolean userInitiated);
    }

    public interface SaveCallback {
        void onComplete(@Nullable VaultRepositoryException error);
    }
}
