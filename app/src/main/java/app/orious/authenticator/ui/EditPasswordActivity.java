package app.orious.authenticator.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import app.orious.authenticator.R;
import app.orious.authenticator.passwords.PasswordEntry;
import app.orious.authenticator.ui.dialogs.PasswordGeneratorDialog;
import app.orious.authenticator.helpers.ViewHelper;
import app.orious.authenticator.vault.VaultRepository;

import java.util.UUID;

public class EditPasswordActivity extends AegisActivity {
    public static final String EXTRA_PASSWORD_UUID = "passwordEntryUUID";

    private TextInputEditText _name;
    private TextInputEditText _username;
    private TextInputEditText _password;
    private TextInputEditText _website;
    private TextInputEditText _notes;

    private PasswordEntry _originalEntry;
    private PasswordEntry _workingEntry;
    private boolean _isNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }

        setContentView(R.layout.activity_edit_password);
        setSupportActionBar(findViewById(R.id.toolbar));
        ViewHelper.setupAppBarInsets(findViewById(R.id.app_bar_layout));

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setHomeAsUpIndicator(R.drawable.ic_outline_close_24);
            bar.setDisplayHomeAsUpEnabled(true);
        }

        _name = findViewById(R.id.password_name);
        _username = findViewById(R.id.password_username);
        _password = findViewById(R.id.password_value);
        _website = findViewById(R.id.password_website);
        _notes = findViewById(R.id.password_notes);

        findViewById(R.id.password_generate).setOnClickListener(v ->
                PasswordGeneratorDialog.show(this, generatedPassword -> {
                    _password.setText(generatedPassword);
                    _password.setSelection(generatedPassword.length());
                }));

        String uuidString = getIntent().getStringExtra(EXTRA_PASSWORD_UUID);
        _isNew = uuidString == null;

        if (_isNew) {
            _workingEntry = new PasswordEntry();
            setTitle(R.string.password_entry_add_title);
        } else {
            try {
                UUID uuid = UUID.fromString(uuidString);
                _originalEntry = _vaultManager.getVault().getPasswordEntryByUUID(uuid);
            } catch (IllegalArgumentException e) {
                _originalEntry = null;
            }

            if (_originalEntry == null) {
                Toast.makeText(this, R.string.password_entry_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            _workingEntry = new PasswordEntry(_originalEntry);
            setTitle(R.string.password_entry_edit_title);
            populateFields(_workingEntry);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                discardAndFinish();
            }
        });
    }

    private void populateFields(PasswordEntry entry) {
        _name.setText(entry.getName());
        _username.setText(entry.getUsername());
        _password.setText(entry.getPassword());
        _website.setText(entry.getWebsite());
        _notes.setText(entry.getNotes());
    }

    private String value(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    private boolean hasUnsavedChanges() {
        String name = value(_name);
        String username = value(_username);
        String password = value(_password);
        String website = value(_website);
        String notes = value(_notes);

        if (_isNew) {
            return !name.isEmpty() || !username.isEmpty() || !password.isEmpty()
                    || !website.isEmpty() || !notes.isEmpty();
        }

        return !name.equals(_originalEntry.getName())
                || !username.equals(_originalEntry.getUsername())
                || !password.equals(_originalEntry.getPassword())
                || !website.equals(_originalEntry.getWebsite())
                || !notes.equals(_originalEntry.getNotes());
    }

    private void discardAndFinish() {
        if (!hasUnsavedChanges()) {
            finish();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.discard_changes)
                .setMessage(R.string.discard_changes_description)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.discard, (dialog, which) -> finish())
                .show();
    }

    private void saveEntry() {
        String name = value(_name).trim();
        String username = value(_username).trim();
        String password = value(_password);
        String website = value(_website).trim();
        String notes = value(_notes);

        if (name.isEmpty()) {
            _name.setError(getString(R.string.password_entry_required));
            _name.requestFocus();
            return;
        }
        if (username.isEmpty()) {
            _username.setError(getString(R.string.password_entry_required));
            _username.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            _password.setError(getString(R.string.password_entry_required));
            _password.requestFocus();
            return;
        }

        _workingEntry.setName(name);
        _workingEntry.setUsername(username);
        _workingEntry.setPassword(password);
        _workingEntry.setWebsite(website);
        _workingEntry.setNotes(notes);

        try {
            _workingEntry.validate();
            VaultRepository repository = _vaultManager.getVault();

            PasswordEntry previousEntry = null;
            boolean addedNewEntry = false;

            if (_isNew && !repository.hasPasswordEntryByUUID(_workingEntry.getUUID())) {
                repository.addPasswordEntry(_workingEntry);
                addedNewEntry = true;
            } else {
                previousEntry = repository.replacePasswordEntry(_workingEntry);
            }

            if (saveAndBackupVault()) {
                Intent result = new Intent();
                result.putExtra(EXTRA_PASSWORD_UUID, _workingEntry.getUUID().toString());
                setResult(RESULT_OK, result);
                finish();
            } else {
                if (addedNewEntry) {
                    repository.removePasswordEntry(_workingEntry);
                } else if (previousEntry != null) {
                    repository.replacePasswordEntry(previousEntry);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_password, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            discardAndFinish();
            return true;
        }
        if (item.getItemId() == R.id.action_save_password) {
            saveEntry();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
