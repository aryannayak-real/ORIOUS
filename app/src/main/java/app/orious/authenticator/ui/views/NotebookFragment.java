package app.orious.authenticator.ui.views;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.orious.authenticator.R;
import app.orious.authenticator.notes.NoteEntry;
import app.orious.authenticator.notes.NoteSortCategory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class NotebookFragment extends Fragment implements NoteAdapter.Listener {
    public interface Listener {
        void onNoteClick(@NonNull NoteEntry note);
        void onNoteFavoriteToggle(@NonNull NoteEntry note);
        void onCreateNoteClick();
        void onNotebookScroll(int dx, int dy);
        void onNotebookTouch();
    }

    private NoteAdapter _adapter;
    private Listener _listener;
    private final List<NoteEntry> _allNotes = new ArrayList<>();
    private NoteSortCategory _sortCategory = NoteSortCategory.MODIFIED_DESC;
    private String _searchFilter;
    private boolean _favoritesOnly = false;

    private RecyclerView _recyclerView;
    private LinearLayout _emptyStateView;
    private ImageView _emptyIcon;
    private TextView _emptyTitle;
    private TextView _emptyMessage;
    private MaterialButton _emptyCta;
    private TextView _countLabel;
    private Chip _chipAll;
    private Chip _chipFavorites;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _adapter = new NoteAdapter();
        _adapter.setListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notebook, container, false);

        _recyclerView = view.findViewById(R.id.rv_notes);
        _recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        _recyclerView.setAdapter(_adapter);

        _recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (_listener != null) {
                    _listener.onNotebookScroll(dx, dy);
                }
            }
        });

        _recyclerView.setOnTouchListener((v, event) -> {
            if (_listener != null) {
                _listener.onNotebookTouch();
            }
            return false;
        });

        _emptyStateView = view.findViewById(R.id.notebook_empty_state);
        _emptyIcon = view.findViewById(R.id.notebook_empty_icon);
        _emptyTitle = view.findViewById(R.id.notebook_empty_title);
        _emptyMessage = view.findViewById(R.id.notebook_empty_message);
        _emptyCta = view.findViewById(R.id.notebook_empty_cta);
        _countLabel = view.findViewById(R.id.notebook_count_label);
        _chipAll = view.findViewById(R.id.notebook_chip_all);
        _chipFavorites = view.findViewById(R.id.notebook_chip_favorites);

        _emptyCta.setOnClickListener(v -> {
            if (_listener != null) {
                _listener.onCreateNoteClick();
            }
        });

        _chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && _favoritesOnly) {
                _favoritesOnly = false;
                refreshDisplayedNotes();
            }
        });

        _chipFavorites.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !_favoritesOnly) {
                _favoritesOnly = true;
                refreshDisplayedNotes();
            }
        });

        refreshDisplayedNotes();
        return view;
    }

    @Override
    public void onDestroy() {
        if (_adapter != null) {
            _adapter.setListener(null);
        }
        super.onDestroy();
    }

    public void setListener(Listener listener) {
        _listener = listener;
    }

    public void setNotes(@Nullable Collection<NoteEntry> notes) {
        _allNotes.clear();
        if (notes != null) {
            for (NoteEntry note : notes) {
                _allNotes.add(new NoteEntry(note));
            }
        }
        refreshDisplayedNotes();
    }

    public void clearNotes() {
        _allNotes.clear();
        refreshDisplayedNotes();
    }

    public void setSortCategory(@NonNull NoteSortCategory sortCategory) {
        if (_sortCategory != sortCategory) {
            _sortCategory = sortCategory;
            refreshDisplayedNotes();
        }
    }

    public void setSearchFilter(@Nullable String search) {
        String normalized = (search != null && !search.trim().isEmpty())
                ? search.trim().toLowerCase(Locale.getDefault())
                : null;
        if ((normalized == null && _searchFilter != null)
                || (normalized != null && !normalized.equals(_searchFilter))) {
            _searchFilter = normalized;
            refreshDisplayedNotes();
        }
    }

    private void refreshDisplayedNotes() {
        if (_adapter == null) {
            return;
        }

        List<NoteEntry> filtered = new ArrayList<>();
        String[] tokens = _searchFilter != null ? _searchFilter.split("\\s+") : null;

        for (NoteEntry note : _allNotes) {
            if (_favoritesOnly && !note.isFavorite()) {
                continue;
            }
            if (tokens != null && !matchesSearch(note, tokens)) {
                continue;
            }
            filtered.add(note);
        }

        filtered.sort(_sortCategory.getComparator());
        _adapter.submitList(filtered);

        if (!isAdded() || _recyclerView == null) {
            return;
        }

        _countLabel.setText(getResources().getQuantityString(
                R.plurals.notes_count, filtered.size(), filtered.size()));

        if (filtered.isEmpty()) {
            _recyclerView.setVisibility(View.GONE);
            _emptyStateView.setVisibility(View.VISIBLE);

            if (!Strings.isNullOrEmpty(_searchFilter)) {
                _emptyIcon.setImageResource(R.drawable.ic_outline_description_24);
                _emptyTitle.setText(R.string.note_empty_search_title);
                _emptyMessage.setText(R.string.note_empty_search_message);
                _emptyCta.setVisibility(View.GONE);
            } else if (_favoritesOnly) {
                _emptyIcon.setImageResource(R.drawable.ic_outline_star_24);
                _emptyTitle.setText(R.string.note_empty_favorites_title);
                _emptyMessage.setText(R.string.note_empty_favorites_message);
                _emptyCta.setVisibility(View.GONE);
            } else {
                _emptyIcon.setImageResource(R.drawable.ic_outline_description_24);
                _emptyTitle.setText(R.string.note_empty_title);
                _emptyMessage.setText(R.string.note_empty_message);
                _emptyCta.setVisibility(View.VISIBLE);
            }
        } else {
            _recyclerView.setVisibility(View.VISIBLE);
            _emptyStateView.setVisibility(View.GONE);
        }
    }

    private static boolean matchesSearch(NoteEntry note, String[] tokens) {
        String title = note.getTitle().toLowerCase(Locale.getDefault());
        String content = note.getContent().toLowerCase(Locale.getDefault());
        return Arrays.stream(tokens).allMatch(token ->
                title.contains(token) || content.contains(token));
    }

    @Override
    public void onNoteClick(@NonNull NoteEntry note) {
        if (_listener != null) {
            _listener.onNoteClick(note);
        }
    }

    @Override
    public void onToggleFavorite(@NonNull NoteEntry note) {
        if (_listener != null) {
            _listener.onNoteFavoriteToggle(note);
        }
    }
}
