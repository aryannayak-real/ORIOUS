package app.orious.authenticator.vault;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;

import app.orious.authenticator.R;
import app.orious.authenticator.encoding.Base32;
import app.orious.authenticator.encoding.Hex;
import app.orious.authenticator.helpers.QrCodeHelper;
import app.orious.authenticator.notes.NoteEntry;
import app.orious.authenticator.otp.GoogleAuthInfo;
import app.orious.authenticator.otp.HotpInfo;
import app.orious.authenticator.otp.MotpInfo;
import app.orious.authenticator.otp.OtpInfo;
import app.orious.authenticator.otp.YandexInfo;
import app.orious.authenticator.passwords.PasswordEntry;
import com.google.common.html.HtmlEscapers;
import com.google.zxing.WriterException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class VaultHtmlExporter {
    private VaultHtmlExporter() {

    }

    public static void export(Context context, PrintStream ps, Collection<VaultEntry> entries) throws WriterException, IOException {
        export(context, ps, entries, Collections.emptyList(), Collections.emptyList());
    }

    public static void export(Context context, PrintStream ps, Collection<VaultEntry> entries, Collection<PasswordEntry> passwordEntries) throws WriterException, IOException {
        export(context, ps, entries, passwordEntries, Collections.emptyList());
    }

    public static void export(Context context, PrintStream ps, Collection<VaultEntry> entries, Collection<PasswordEntry> passwordEntries, Collection<NoteEntry> notes) throws WriterException, IOException {
        ps.print("<html><head><title>");
        ps.print(context.getString(R.string.export_html_title));
        ps.print("</title></head><body>");
        ps.print("<h1>");
        ps.print(context.getString(R.string.export_html_title));
        ps.print("</h1>");
        ps.print("<table>");
        ps.print("<tr>");
        ps.print("<th>Issuer</th>");
        ps.print("<th>Name</th>");
        ps.print("<th>Type</th>");
        ps.print("<th>QR Code</th>");
        ps.print("<th>UUID</th>");
        ps.print("<th>Note</th>");
        ps.print("<th>Favorite</th>");
        ps.print("<th>Algo</th>");
        ps.print("<th>Digits</th>");
        ps.print("<th>Secret</th>");
        ps.print("<th>Counter</th>");
        ps.print("<th>PIN</th>");
        ps.print("</tr>");
        for (VaultEntry entry : entries) {
            ps.print("<tr>");
            OtpInfo info = entry.getInfo();
            GoogleAuthInfo gaInfo = new GoogleAuthInfo(info, entry.getName(), entry.getIssuer());
            appendRow(ps, entry.getIssuer());
            appendRow(ps, entry.getName());
            appendRow(ps, info.getType());
            appendQrRow(ps, gaInfo.getUri().toString());
            appendRow(ps, entry.getUUID().toString());
            appendRow(ps, entry.getNote());
            appendRow(ps, Boolean.toString(entry.isFavorite()));
            appendRow(ps, info.getAlgorithm(false));
            appendRow(ps, Integer.toString(info.getDigits()));
            if (info instanceof MotpInfo) {
                appendRow(ps, Hex.encode(info.getSecret()));
            } else {
                appendRow(ps, Base32.encode(info.getSecret()));
            }
            if (info instanceof HotpInfo) {
                appendRow(ps, Long.toString(((HotpInfo) info).getCounter()));
            } else {
                appendRow(ps, "-");
            }
            if (info instanceof YandexInfo) {
                appendRow(ps, ((YandexInfo) info).getPin());
            } else if (info instanceof MotpInfo) {
                appendRow(ps, ((MotpInfo) info).getPin());
            } else {
                appendRow(ps, "-");
            }
            ps.print("</tr>");
        }
        ps.print("</table>");
        if (passwordEntries != null && !passwordEntries.isEmpty()) {
            ps.print("<h2>");
            ps.print(escape(context.getString(R.string.export_html_passwords_title)));
            ps.print("</h2><table><tr>");
            ps.print("<th>Name</th>");
            ps.print("<th>Username</th>");
            ps.print("<th>Password</th>");
            ps.print("<th>Website</th>");
            ps.print("<th>Notes</th>");
            ps.print("<th>UUID</th>");
            ps.print("</tr>");
            for (PasswordEntry password : passwordEntries) {
                ps.print("<tr>");
                appendRow(ps, password.getName());
                appendRow(ps, password.getUsername());
                appendRow(ps, password.getPassword());
                appendRow(ps, password.getWebsite());
                appendRow(ps, password.getNotes());
                appendRow(ps, password.getUUID().toString());
                ps.print("</tr>");
            }
            ps.print("</table>");
        }
        if (notes != null && !notes.isEmpty()) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            ps.print("<h2>");
            ps.print(escape(context.getString(R.string.export_html_notes_title)));
            ps.print("</h2><table><tr>");
            ps.print("<th>Title</th>");
            ps.print("<th>Content</th>");
            ps.print("<th>Favorite</th>");
            ps.print("<th>Last Modified</th>");
            ps.print("<th>UUID</th>");
            ps.print("</tr>");
            for (NoteEntry note : notes) {
                ps.print("<tr>");
                appendRow(ps, note.getTitle());
                ps.print("<td>");
                ps.print(escape(note.getContent()).replace("\n", "<br/>"));
                ps.print("</td>");
                appendRow(ps, Boolean.toString(note.isFavorite()));
                appendRow(ps, dateFormat.format(new Date(note.getUpdatedAt())));
                appendRow(ps, note.getUUID().toString());
                ps.print("</tr>");
            }
            ps.print("</table>");
        }
        ps.print("</body>");
        ps.print("<style>table,td,th{border:1px solid #000;border-collapse:collapse;text-align:center}td:not(.qr),th{padding:1em}</style>");
        ps.print("</html>");
    }

    private static void appendRow(PrintStream ps, String s) {
        ps.print("<td>");
        ps.print(escape(s));
        ps.print("</td>");
    }

    private static void appendQrRow(PrintStream ps, String s) throws IOException, WriterException {
        ps.print("<td class='qr'><img src=\"data:image/png;base64,");
        Bitmap bm = QrCodeHelper.encodeToBitmap(s, 256, 256, Color.WHITE);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
            String encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            ps.print(encoded);
        }
        ps.print("\"/></td>");
    }

    private static String escape(String s) {
        return HtmlEscapers.htmlEscaper().escape(s);
    }
}
