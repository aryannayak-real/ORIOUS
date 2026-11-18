package app.orious.authenticator.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import app.orious.authenticator.R;
import app.orious.authenticator.helpers.ViewHelper;
import app.orious.authenticator.notes.NoteEntry;
import app.orious.authenticator.ui.dialogs.Dialogs;
import app.orious.authenticator.ui.views.NoteAdapter;
import app.orious.authenticator.util.ClipboardUtils;
import app.orious.authenticator.vault.VaultRepository;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Date;
import java.util.UUID;

public class NoteEditorActivity extends AegisActivity {
    public static final String EXTRA_NOTE_UUID = "noteUUID";

    private static final String STATE_TITLE = "state_title";
    private static final String STATE_CONTENT = "state_content";
    private static final String STATE_FAVORITE = "state_favorite";

    private EditText _titleEdit;
    private EditText _contentEdit;
    private TextView _statusText;
    private Menu _menu;

    private NoteEntry _originalNote;
    private NoteEntry _workingNote;
    private boolean _isNew;
    private boolean _favorite;
    private boolean _isSaving;
    private boolean _isDeleting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }

        setContentView(R.layout.activity_note_editor);
        setSupportActionBar(findViewById(R.id.toolbar));
        ViewHelper.setupAppBarInsets(findViewById(R.id.app_bar_layout));

        NestedScrollView scrollView = findViewById(R.id.note_editor_scroll);
        final int padLeft = scrollView.getPaddingLeft();
        final int padTop = scrollView.getPaddingTop();
        final int padRight = scrollView.getPaddingRight();
        final int padBottom = scrollView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (targetView, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.ime()
                            | WindowInsetsCompat.Type.displayCutout());
            targetView.setPadding(padLeft, padTop, padRight, padBottom + insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowTitleEnabled(false);
        }

        _titleEdit = findViewById(R.id.note_editor_title);
        _contentEdit = findViewById(R.id.note_editor_content);
        _statusText = findViewById(R.id.note_editor_status);

        String uuidString = getIntent().getStringExtra(EXTRA_NOTE_UUID);
        _isNew = uuidString == null;
        setTitle(_isNew ? R.string.note_add_title : R.string.note_edit_title);

        if (_isNew) {
            _workingNote = new NoteEntry();
            _favorite = false;
            if (bar != null) {
                bar.setHomeAsUpIndicator(R.drawable.ic_outline_close_24);
            }
        } else {
            try {
                UUID uuid = UUID.fromString(uuidString);
                _originalNote = _vaultManager.getVault().getNoteByUUID(uuid);
            } catch (IllegalArgumentException e) {
                _originalNote = null;
            }

            if (_originalNote == null) {
                Toast.makeText(this, R.string.note_not_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            _workingNote = new NoteEntry(_originalNote);
            _favorite = _workingNote.isFavorite();
            _titleEdit.setText(_workingNote.getTitle());
            _contentEdit.setText(_workingNote.getContent());
            if (bar != null) {
                bar.setHomeAsUpIndicator(R.drawable.ic_outline_arrow_left_alt_24);
            }
        }

        if (savedInstanceState != null) {
            _titleEdit.setText(savedInstanceState.getString(STATE_TITLE, _titleEdit.getText().toString()));
            _contentEdit.setText(savedInstanceState.getString(STATE_CONTENT, _contentEdit.getText().toString()));
            _favorite = savedInstanceState.getBoolean(STATE_FAVORITE, _favorite);
        }

        _titleEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                _contentEdit.requestFocus();
                _contentEdit.setSelection(_contentEdit.getText().length());
                return true;
            }
            return false;
        });

        _contentEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                updateStatusLine();
            }
        });

        updateStatusLine();

        if (_isNew && savedInstanceState == null) {
            _titleEdit.requestFocus();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleClose();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (_titleEdit != null && _contentEdit != null) {
            outState.putString(STATE_TITLE, _titleEdit.getText().toString());
            outState.putString(STATE_CONTENT, _contentEdit.getText().toString());
            outState.putBoolean(STATE_FAVORITE, _favorite);
        }
    }

    private void updateStatusLine() {
        String content = _contentEdit.getText() == null ? "" : _contentEdit.getText().toString();
        int words = NoteAdapter.countWords(content);
        String wordsLabel = getResources().getQuantityString(R.plurals.note_words_count, words, words);

        if (_isNew) {
            if (words > 0) {
                _statusText.setText(getString(R.string.note_status_edited,
                        getString(R.string.note_add_title), wordsLabel));
            } else {
                _statusText.setText(R.string.note_status_new);
            }
        } else {
            Date updated = new Date(_workingNote.getUpdatedAt());
            String formattedDate = DateFormat.getMediumDateFormat(this).format(updated)
                    + ", " + DateFormat.getTimeFormat(this).format(updated);
            _statusText.setText(getString(R.string.note_status_edited, formattedDate, wordsLabel));
        }
    }

    private String currentTitle() {
        return _titleEdit.getText() == null ? "" : _titleEdit.getText().toString().trim();
    }

    private String currentContent() {
        return _contentEdit.getText() == null ? "" : _contentEdit.getText().toString();
    }

    private boolean hasUnsavedChanges() {
        String title = currentTitle();
        String content = currentContent();
        if (_isNew) {
            return !title.isEmpty() || !content.trim().isEmpty();
        }
        return !title.equals(_originalNote.getTitle().trim())
                || !content.equals(_originalNote.getContent())
                || _favorite != _originalNote.isFavorite();
    }

    private void handleClose() {
        if (!hasUnsavedChanges()) {
            finish();
            return;
        }

        Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.note_discard_title)
                .setMessage(R.string.note_discard_message)
                .setPositiveButton(R.string.note_save, (dialog, which) -> saveNote())
                .setNegativeButton(R.string.note_discard_action, (dialog, which) -> finish())
                .setNeutralButton(R.string.note_keep_editing, null)
                .create());
    }

    private void saveNote() {
        if (_isSaving || _isDeleting) {
            return;
        }

        String title = currentTitle();
        String content = currentContent();
        if (title.isEmpty() && content.trim().isEmpty()) {
            Toast.makeText(this, R.string.note_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }

        _workingNote.setTitle(title);
        _workingNote.setContent(content);
        _workingNote.setFavorite(_favorite);
        _workingNote.touchUpdatedAt();

        try {
            _workingNote.validate();
            VaultRepository repository = _vaultManager.getVault();

            NoteEntry previousNote = null;
            boolean addedNew = false;
            if (_isNew && !repository.hasNoteByUUID(_workingNote.getUUID())) {
                repository.addNote(_workingNote);
                addedNew = true;
            } else {
                previousNote = repository.replaceNote(_workingNote);
            }

            final NoteEntry rollbackPrevious = previousNote;
            final boolean rollbackAdded = addedNew;
            _isSaving = true;
            saveAndBackupVaultAsync(error -> {
                _isSaving = false;
                if (error == null) {
                    Intent result = new Intent();
                    result.putExtra(EXTRA_NOTE_UUID, _workingNote.getUUID().toString());
                    setResult(RESULT_OK, result);
                    Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    if (rollbackAdded) {
                        repository.removeNote(_workingNote);
                    } else if (rollbackPrevious != null) {
                        repository.replaceNote(rollbackPrevious);
                    }
                }
            });
        } catch (Exception e) {
            _isSaving = false;
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeleteNote() {
        if (_isNew || _originalNote == null || _isSaving || _isDeleting) {
            return;
        }

        Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.note_delete_title)
                .setMessage(R.string.note_delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteNote())
                .create());
    }

    private void deleteNote() {
        if (_originalNote == null || _isDeleting) {
            return;
        }

        _isDeleting = true;
        VaultRepository repository = _vaultManager.getVault();
        NoteEntry removed = repository.removeNote(_originalNote);
        saveAndBackupVaultAsync(error -> {
            _isDeleting = false;
            if (error == null) {
                setResult(RESULT_OK);
                Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
                finish();
            } else if (removed != null) {
                repository.addNote(removed);
            }
        });
    }

    private void copyNoteToClipboard() {
        String title = currentTitle();
        String content = currentContent();
        if (title.isEmpty() && content.trim().isEmpty()) {
            return;
        }

        String text;
        if (title.isEmpty()) {
            text = content;
        } else if (content.trim().isEmpty()) {
            text = title;
        } else {
            text = title + "\n\n" + content;
        }

        ClipboardUtils.copyText(this, "note", text, true);
        Toast.makeText(this, R.string.note_copied, Toast.LENGTH_SHORT).show();
    }

    private void updateFavoriteMenuItem() {
        if (_menu == null) {
            return;
        }
        MenuItem favItem = _menu.findItem(R.id.action_note_favorite);
        if (favItem == null) {
            return;
        }

        favItem.setIcon(_favorite ? R.drawable.ic_filled_star_24 : R.drawable.ic_outline_star_24);
        favItem.setTitle(_favorite ? R.string.note_favorite_remove : R.string.note_favorite_add);
        Drawable icon = favItem.getIcon();
        if (icon != null) {
            Drawable wrapped = DrawableCompat.wrap(icon.mutate());
            int tint = _favorite
                    ? ContextCompat.getColor(this, R.color.aegis_theme_all_favorite)
                    : MaterialColors.getColor(findViewById(R.id.toolbar), com.google.android.material.R.attr.colorOnSurface);
            DrawableCompat.setTintList(wrapped, ColorStateList.valueOf(tint));
            favItem.setIcon(wrapped);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_note_editor, menu);
        MenuItem deleteItem = menu.findItem(R.id.action_note_delete);
        if (deleteItem != null) {
            deleteItem.setVisible(!_isNew);
        }
        updateFavoriteMenuItem();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            handleClose();
            return true;
        } else if (id == R.id.action_note_favorite) {
            _favorite = !_favorite;
            updateFavoriteMenuItem();
            return true;
        } else if (id == R.id.action_note_save) {
            saveNote();
            return true;
        } else if (id == R.id.action_note_copy) {
            copyNoteToClipboard();
            return true;
        } else if (id == R.id.action_note_delete) {
            confirmDeleteNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
