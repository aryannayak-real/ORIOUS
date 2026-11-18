package app.orious.authenticator.ui.views;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import app.orious.authenticator.R;
import app.orious.authenticator.notes.NoteEntry;
import com.google.android.material.color.MaterialColors;

import java.util.Date;

public class NoteAdapter extends ListAdapter<NoteEntry, NoteAdapter.NoteViewHolder> {
    public interface Listener {
        void onNoteClick(@NonNull NoteEntry note);
        void onToggleFavorite(@NonNull NoteEntry note);
    }

    private Listener _listener;

    public NoteAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setListener(Listener listener) {
        _listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_note_entry, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteEntry note = getItem(position);
        Context context = holder.itemView.getContext();

        String trimmedTitle = note.getTitle().trim();
        if (trimmedTitle.isEmpty()) {
            holder.title.setText(R.string.note_untitled);
            holder.title.setTypeface(holder.title.getTypeface(), Typeface.ITALIC);
            holder.title.setTextColor(MaterialColors.getColor(
                    holder.title, com.google.android.material.R.attr.colorOnSurfaceVariant));
        } else {
            holder.title.setText(trimmedTitle);
            holder.title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            holder.title.setTextColor(MaterialColors.getColor(
                    holder.title, com.google.android.material.R.attr.colorOnSurface));
        }

        String previewText = trimEdgeNewlines(note.getContent());
        if (previewText.isEmpty()) {
            holder.preview.setVisibility(View.GONE);
        } else {
            holder.preview.setVisibility(View.VISIBLE);
            holder.preview.setText(previewText);
        }

        holder.date.setText(formatUpdatedDate(context, note.getUpdatedAt()));

        int words = countWords(note.getContent());
        if (words > 0) {
            holder.meta.setVisibility(View.VISIBLE);
            holder.meta.setText(context.getResources().getQuantityString(
                    R.plurals.note_words_count, words, words));
        } else {
            holder.meta.setVisibility(View.GONE);
        }

        if (note.isFavorite()) {
            holder.favoriteButton.setImageResource(R.drawable.ic_filled_star_24);
            holder.favoriteButton.setContentDescription(
                    context.getString(R.string.note_favorite_remove));
            ImageViewCompat.setImageTintList(
                    holder.favoriteButton,
                    android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.aegis_theme_all_favorite)));
            holder.favoriteButton.setAlpha(1.0f);
        } else {
            holder.favoriteButton.setImageResource(R.drawable.ic_outline_star_24);
            holder.favoriteButton.setContentDescription(
                    context.getString(R.string.note_favorite_add));
            ImageViewCompat.setImageTintList(
                    holder.favoriteButton,
                    android.content.res.ColorStateList.valueOf(
                            MaterialColors.getColor(
                                    holder.favoriteButton,
                                    com.google.android.material.R.attr.colorOnSurfaceVariant)));
            holder.favoriteButton.setAlpha(0.6f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (_listener != null) {
                _listener.onNoteClick(note);
            }
        });

        holder.favoriteButton.setOnClickListener(v -> {
            if (_listener != null) {
                _listener.onToggleFavorite(note);
            }
        });
    }

    public static String trimEdgeNewlines(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        int start = 0;
        int end = content.length();
        while (start < end && (content.charAt(start) == '\n' || content.charAt(start) == '\r')) {
            start++;
        }
        while (end > start && (content.charAt(end - 1) == '\n' || content.charAt(end - 1) == '\r')) {
            end--;
        }
        return content.substring(start, end);
    }

    public static int countWords(String content) {
        if (content == null) {
            return 0;
        }
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }

    private static CharSequence formatUpdatedDate(Context context, long updatedAt) {
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - updatedAt);
        if (diff < DateUtils.WEEK_IN_MILLIS) {
            return DateUtils.getRelativeTimeSpanString(
                    updatedAt,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
        }
        return DateFormat.getMediumDateFormat(context).format(new Date(updatedAt));
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView preview;
        final TextView date;
        final TextView meta;
        final ImageButton favoriteButton;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_card_title);
            preview = itemView.findViewById(R.id.note_card_preview);
            date = itemView.findViewById(R.id.note_card_date);
            meta = itemView.findViewById(R.id.note_card_meta);
            favoriteButton = itemView.findViewById(R.id.note_card_favorite);
        }
    }

    private static final DiffUtil.ItemCallback<NoteEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NoteEntry>() {
                @Override
                public boolean areItemsTheSame(@NonNull NoteEntry oldItem, @NonNull NoteEntry newItem) {
                    return oldItem.getUUID().equals(newItem.getUUID());
                }

                @Override
                public boolean areContentsTheSame(@NonNull NoteEntry oldItem, @NonNull NoteEntry newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
