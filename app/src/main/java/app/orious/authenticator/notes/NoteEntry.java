package app.orious.authenticator.notes;

import androidx.annotation.NonNull;

import app.orious.authenticator.util.UUIDMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.UUID;

public class NoteEntry extends UUIDMap.Value {
    private String _title = "";
    private String _content = "";
    private boolean _favorite = false;
    private long _createdAt;
    private long _updatedAt;

    public NoteEntry() {
        super();
        long now = System.currentTimeMillis();
        _createdAt = now;
        _updatedAt = now;
    }

    public NoteEntry(String title, String content, boolean favorite) {
        this();
        setTitle(title);
        setContent(content);
        setFavorite(favorite);
    }

    public NoteEntry(String title, String content, boolean favorite, long createdAt, long updatedAt) {
        super();
        setTitle(title);
        setContent(content);
        setFavorite(favorite);
        _createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        _updatedAt = updatedAt > 0 ? updatedAt : _createdAt;
    }

    private NoteEntry(UUID uuid) {
        super(uuid);
        long now = System.currentTimeMillis();
        _createdAt = now;
        _updatedAt = now;
    }

    public NoteEntry(@NonNull NoteEntry other) {
        super(other.getUUID());
        _title = other._title;
        _content = other._content;
        _favorite = other._favorite;
        _createdAt = other._createdAt;
        _updatedAt = other._updatedAt;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("uuid", getUUID().toString());
            obj.put("title", _title);
            obj.put("content", _content);
            obj.put("favorite", _favorite);
            obj.put("createdAt", _createdAt);
            obj.put("updatedAt", _updatedAt);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    public static NoteEntry fromJson(JSONObject obj) throws NoteEntryException {
        try {
            UUID uuid = obj.has("uuid")
                    ? UUID.fromString(obj.getString("uuid"))
                    : UUID.randomUUID();

            NoteEntry entry = new NoteEntry(uuid);
            entry.setTitle(obj.optString("title", ""));
            entry.setContent(obj.optString("content", ""));
            entry.setFavorite(obj.optBoolean("favorite", false));

            long now = System.currentTimeMillis();
            long createdAt = obj.optLong("createdAt", now);
            if (createdAt <= 0) {
                createdAt = now;
            }
            long updatedAt = obj.optLong("updatedAt", createdAt);
            if (updatedAt <= 0) {
                updatedAt = createdAt;
            }
            entry.setCreatedAt(createdAt);
            entry.setUpdatedAt(updatedAt);

            entry.validate();
            return entry;
        } catch (JSONException | IllegalArgumentException | NoteEntryException e) {
            throw new NoteEntryException(e);
        }
    }

    public void validate() throws NoteEntryException {
        if (_title.trim().isEmpty() && _content.trim().isEmpty()) {
            throw new NoteEntryException("Note must contain a title or content");
        }
    }

    @NonNull
    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title == null ? "" : title;
    }

    @NonNull
    public String getContent() {
        return _content;
    }

    public void setContent(String content) {
        _content = content == null ? "" : content;
    }

    public boolean isFavorite() {
        return _favorite;
    }

    public void setFavorite(boolean favorite) {
        _favorite = favorite;
    }

    public long getCreatedAt() {
        return _createdAt;
    }

    public void setCreatedAt(long createdAt) {
        _createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return _updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        _updatedAt = updatedAt;
    }

    public void touchUpdatedAt() {
        _updatedAt = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NoteEntry)) {
            return false;
        }
        NoteEntry that = (NoteEntry) o;
        return super.equals(that)
                && _favorite == that._favorite
                && _createdAt == that._createdAt
                && _updatedAt == that._updatedAt
                && _title.equals(that._title)
                && _content.equals(that._content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUUID(), _title, _content, _favorite, _createdAt, _updatedAt);
    }
}
