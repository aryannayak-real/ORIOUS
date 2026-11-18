package app.orious.authenticator.notes;

import static org.junit.Assert.assertEquals;

import app.orious.authenticator.ui.views.NoteAdapter;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class NoteSortCategoryTest {
    @Test
    public void testFavoritesAlwaysSortFirstAcrossAllSortModes() {
        NoteEntry favOld = new NoteEntry("Zulu", "fav old", true, 100L, 100L);
        NoteEntry nonFavNewest = new NoteEntry("Alpha", "non-fav newest", false, 300L, 500L);
        NoteEntry nonFavMiddle = new NoteEntry("Beta", "non-fav middle", false, 200L, 400L);
        NoteEntry favNew = new NoteEntry("Charlie", "fav new", true, 250L, 250L);

        List<NoteEntry> byModifiedDesc = Arrays.asList(favOld, nonFavNewest, nonFavMiddle, favNew);
        byModifiedDesc.sort(NoteSortCategory.MODIFIED_DESC.getComparator());
        assertEquals(Arrays.asList(favNew, favOld, nonFavNewest, nonFavMiddle), byModifiedDesc);

        List<NoteEntry> byModifiedAsc = Arrays.asList(favOld, nonFavNewest, nonFavMiddle, favNew);
        byModifiedAsc.sort(NoteSortCategory.MODIFIED_ASC.getComparator());
        assertEquals(Arrays.asList(favOld, favNew, nonFavMiddle, nonFavNewest), byModifiedAsc);

        List<NoteEntry> byTitleAsc = Arrays.asList(favOld, nonFavNewest, nonFavMiddle, favNew);
        byTitleAsc.sort(NoteSortCategory.TITLE_ASC.getComparator());
        assertEquals(Arrays.asList(favNew, favOld, nonFavNewest, nonFavMiddle), byTitleAsc);

        List<NoteEntry> byTitleDesc = Arrays.asList(favOld, nonFavNewest, nonFavMiddle, favNew);
        byTitleDesc.sort(NoteSortCategory.TITLE_DESC.getComparator());
        assertEquals(Arrays.asList(favOld, favNew, nonFavMiddle, nonFavNewest), byTitleDesc);

        List<NoteEntry> byCreatedDesc = Arrays.asList(favOld, nonFavNewest, nonFavMiddle, favNew);
        byCreatedDesc.sort(NoteSortCategory.CREATED_DESC.getComparator());
        assertEquals(Arrays.asList(favNew, favOld, nonFavNewest, nonFavMiddle), byCreatedDesc);
    }

    @Test
    public void testTitleAscPlacesUntitledNotesAfterTitledNotes() {
        NoteEntry untitled = new NoteEntry("", "Alpha content", false, 100L, 300L);
        NoteEntry titled = new NoteEntry("Zebra", "Zebra content", false, 100L, 200L);

        List<NoteEntry> notes = Arrays.asList(untitled, titled);
        notes.sort(NoteSortCategory.TITLE_ASC.getComparator());
        assertEquals(Arrays.asList(titled, untitled), notes);
    }

    @Test
    public void testFromIntegerDefaultsToModifiedDescOutOfBounds() {
        assertEquals(NoteSortCategory.MODIFIED_DESC, NoteSortCategory.fromInteger(-1));
        assertEquals(NoteSortCategory.MODIFIED_DESC, NoteSortCategory.fromInteger(999));
        assertEquals(NoteSortCategory.TITLE_ASC, NoteSortCategory.fromInteger(NoteSortCategory.TITLE_ASC.ordinal()));
    }

    @Test
    public void testNoteAdapterTextHelpers() {
        assertEquals(0, NoteAdapter.countWords(null));
        assertEquals(0, NoteAdapter.countWords("   \n\t  "));
        assertEquals(4, NoteAdapter.countWords("One\ntwo   three\tfour"));

        assertEquals("", NoteAdapter.trimEdgeNewlines(null));
        assertEquals("Line 1\n\nLine 2", NoteAdapter.trimEdgeNewlines("\n\rLine 1\n\nLine 2\r\n"));
    }
}
