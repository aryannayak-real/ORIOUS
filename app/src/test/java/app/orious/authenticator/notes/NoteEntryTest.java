package app.orious.authenticator.notes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.UUID;

public class NoteEntryTest {
    @Test
    public void testJsonRoundtripPreservesMultilineContentAndMetadata() throws Exception {
        String multiline = "First line\n\nSecond paragraph with indent:\n  - bullet one\n  - bullet two\nFinal line";
        NoteEntry original = new NoteEntry(
                "Vault Recovery Notes",
                multiline,
                true,
                1700000000000L,
                1700001234567L
        );

        JSONObject json = original.toJson();
        NoteEntry restored = NoteEntry.fromJson(json);

        assertEquals(original.getUUID(), restored.getUUID());
        assertEquals("Vault Recovery Notes", restored.getTitle());
        assertEquals(multiline, restored.getContent());
        assertTrue(restored.isFavorite());
        assertEquals(1700000000000L, restored.getCreatedAt());
        assertEquals(1700001234567L, restored.getUpdatedAt());
        assertEquals(original, restored);
        assertEquals(original.hashCode(), restored.hashCode());
    }

    @Test
    public void testAllowsTitleOnlyOrContentOnlyNotes() throws Exception {
        NoteEntry titleOnly = new NoteEntry("Quick Reminder", "", false);
        titleOnly.validate();
        NoteEntry restoredTitleOnly = NoteEntry.fromJson(titleOnly.toJson());
        assertEquals("Quick Reminder", restoredTitleOnly.getTitle());
        assertEquals("", restoredTitleOnly.getContent());

        NoteEntry contentOnly = new NoteEntry("", "Line 1\nLine 2", false);
        contentOnly.validate();
        NoteEntry restoredContentOnly = NoteEntry.fromJson(contentOnly.toJson());
        assertEquals("", restoredContentOnly.getTitle());
        assertEquals("Line 1\nLine 2", restoredContentOnly.getContent());
    }

    @Test
    public void testRejectsBlankTitleAndContent() {
        NoteEntry blank = new NoteEntry("   ", "\n  \t  \n", false);
        assertThrows(NoteEntryException.class, blank::validate);

        JSONObject obj = new JSONObject();
        try {
            obj.put("uuid", UUID.randomUUID().toString());
            obj.put("title", "   ");
            obj.put("content", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThrows(NoteEntryException.class, () -> NoteEntry.fromJson(obj));
    }

    @Test
    public void testCopyConstructorAndCloneBehavior() {
        NoteEntry original = new NoteEntry("Title", "Body\nLine 2", true, 1000L, 2000L);
        NoteEntry copy = new NoteEntry(original);
        assertEquals(original, copy);
        assertEquals(original.getUUID(), copy.getUUID());

        copy.setTitle("Edited");
        copy.setFavorite(false);
        assertNotEquals(original, copy);
        assertEquals("Title", original.getTitle());
        assertTrue(original.isFavorite());
        assertFalse(copy.isFavorite());
    }
}
