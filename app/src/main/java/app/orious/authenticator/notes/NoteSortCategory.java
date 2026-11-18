package app.orious.authenticator.notes;

import androidx.annotation.IdRes;

import app.orious.authenticator.R;

import java.util.Comparator;

public enum NoteSortCategory {
    MODIFIED_DESC,
    MODIFIED_ASC,
    TITLE_ASC,
    TITLE_DESC,
    CREATED_DESC;

    public static NoteSortCategory fromInteger(int x) {
        NoteSortCategory[] values = values();
        if (x < 0 || x >= values.length) {
            return MODIFIED_DESC;
        }
        return values[x];
    }

    public Comparator<NoteEntry> getComparator() {
        return (a, b) -> {
            if (a.isFavorite() != b.isFavorite()) {
                return a.isFavorite() ? -1 : 1;
            }

            int result;
            switch (this) {
                case MODIFIED_ASC:
                    result = Long.compare(a.getUpdatedAt(), b.getUpdatedAt());
                    break;
                case TITLE_ASC:
                    result = compareTitles(a, b);
                    break;
                case TITLE_DESC:
                    result = compareTitles(b, a);
                    break;
                case CREATED_DESC:
                    result = Long.compare(b.getCreatedAt(), a.getCreatedAt());
                    break;
                case MODIFIED_DESC:
                default:
                    result = Long.compare(b.getUpdatedAt(), a.getUpdatedAt());
                    break;
            }

            if (result != 0) {
                return result;
            }
            int updatedTieBreak = Long.compare(b.getUpdatedAt(), a.getUpdatedAt());
            if (updatedTieBreak != 0) {
                return updatedTieBreak;
            }
            return a.getUUID().compareTo(b.getUUID());
        };
    }

    private static int compareTitles(NoteEntry a, NoteEntry b) {
        String titleA = a.getTitle().trim();
        String titleB = b.getTitle().trim();
        if (titleA.isEmpty() && !titleB.isEmpty()) {
            return 1;
        }
        if (!titleA.isEmpty() && titleB.isEmpty()) {
            return -1;
        }
        int titleCmp = String.CASE_INSENSITIVE_ORDER.compare(titleA, titleB);
        if (titleCmp != 0) {
            return titleCmp;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(a.getContent().trim(), b.getContent().trim());
    }

    @IdRes
    public int getMenuItem() {
        switch (this) {
            case MODIFIED_ASC:
                return R.id.menu_sort_notes_modified_asc;
            case TITLE_ASC:
                return R.id.menu_sort_notes_title_asc;
            case TITLE_DESC:
                return R.id.menu_sort_notes_title_desc;
            case CREATED_DESC:
                return R.id.menu_sort_notes_created_desc;
            case MODIFIED_DESC:
            default:
                return R.id.menu_sort_notes_modified_desc;
        }
    }
}
