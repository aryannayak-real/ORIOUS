package app.orious.authenticator.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public final class ClipboardUtils {
    public static String readText(Context context) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard == null) {
            return null;
        }

        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return null;
        }

        ClipData.Item item = clip.getItemAt(0);
        CharSequence cs = item.coerceToText(context);
        if (cs == null) {
            return null;
        }

        String text = cs.toString().trim();
        if (text.isEmpty()) {
            return null;
        }

        return text;
    }

    public static void copyText(Context context, String label, String text, boolean sensitive) {
        if (context == null || text == null) {
            return;
        }

        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }

        ClipData clip = ClipData.newPlainText(label != null ? label : "text/plain", text);
        if (sensitive && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.os.PersistableBundle extras = new android.os.PersistableBundle();
            extras.putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true);
            clip.getDescription().setExtras(extras);
        }
        clipboard.setPrimaryClip(clip);
    }
}
