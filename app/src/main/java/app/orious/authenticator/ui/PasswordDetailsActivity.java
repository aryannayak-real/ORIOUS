package app.orious.authenticator.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.orious.authenticator.R;
import app.orious.authenticator.helpers.ViewHelper;
import app.orious.authenticator.passwords.PasswordEntry;
import app.orious.authenticator.vault.VaultRepository;

import java.util.UUID;

public class PasswordDetailsActivity extends AegisActivity {
    private String _uuid;
    private PasswordEntry _entry;
    private TextView _password;
    private TextView _passwordToggle;
    private boolean _passwordVisible;

    private final ActivityResultLauncher<Intent> _editLauncher =
            registerForActivityResult(new StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadEntry();
                    setResult(RESULT_OK);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) return;

        setContentView(R.layout.activity_password_details);
        setSupportActionBar(findViewById(R.id.toolbar));
        ViewHelper.setupAppBarInsets(findViewById(R.id.app_bar_layout));

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        _uuid = getIntent().getStringExtra(EditPasswordActivity.EXTRA_PASSWORD_UUID);
        _password = findViewById(R.id.password_detail_value);
        _passwordToggle = findViewById(R.id.password_detail_toggle);

        _passwordToggle.setOnClickListener(v -> {
            _passwordVisible = !_passwordVisible;
            updatePasswordVisibility();
        });

        loadEntry();
    }

    private void loadEntry() {
        try {
            _entry = _vaultManager.getVault().getPasswordEntryByUUID(UUID.fromString(_uuid));
        } catch (Exception e) {
            _entry = null;
        }

        if (_entry == null) {
            Toast.makeText(this, R.string.password_entry_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(_entry.getName());
        setText(R.id.password_detail_name, _entry.getName());
        setText(R.id.password_detail_username, _entry.getUsername());
        setText(R.id.password_detail_website, _entry.getWebsite());
        setText(R.id.password_detail_notes, _entry.getNotes());

        findViewById(R.id.password_detail_website_container)
                .setVisibility(_entry.getWebsite().isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.password_detail_notes_container)
                .setVisibility(_entry.getNotes().isEmpty() ? View.GONE : View.VISIBLE);

        _passwordVisible = false;
        updatePasswordVisibility();
    }

    private void setText(int id, String value) {
        ((TextView) findViewById(id)).setText(value);
    }

    private String getMaskedPassword() {
        int length = Math.max(8, _entry.getPassword().length());
        StringBuilder masked = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            masked.append('•');
        }
        return masked.toString();
    }

    private void updatePasswordVisibility() {
        if (_entry == null) return;

        _password.setText(_passwordVisible
                ? _entry.getPassword()
                : getMaskedPassword());
        _passwordToggle.setText(_passwordVisible
                ? R.string.password_details_hide
                : R.string.password_details_reveal);
    }

    private void editEntry() {
        if (_entry == null) return;
        Intent intent = new Intent(this, EditPasswordActivity.class);
        intent.putExtra(EditPasswordActivity.EXTRA_PASSWORD_UUID, _uuid);
        _editLauncher.launch(intent);
    }

    private void deleteEntry() {
        if (_entry == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.password_details_delete_title)
                .setMessage(R.string.password_details_delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    VaultRepository repository = _vaultManager.getVault();
                    PasswordEntry removedEntry = repository.removePasswordEntry(_entry);
                    if (saveAndBackupVault()) {
                        setResult(RESULT_OK);
                        finish();
                    } else if (removedEntry != null) {
                        repository.addPasswordEntry(removedEntry);
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_password_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_edit_password_details) {
            editEntry();
            return true;
        }
        if (item.getItemId() == R.id.action_delete_password_details) {
            deleteEntry();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
