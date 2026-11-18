package app.orious.authenticator.passwords;

import app.orious.authenticator.util.UUIDMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class PasswordEntry extends UUIDMap.Value {
    private String _name = "";
    private String _username = "";
    private String _password = "";
    private String _website = "";
    private String _notes = "";

    public PasswordEntry() {
        super();
    }

    public PasswordEntry(String name, String username, String password,
                         String website, String notes) {
        super();
        setName(name);
        setUsername(username);
        setPassword(password);
        setWebsite(website);
        setNotes(notes);
    }

    private PasswordEntry(UUID uuid) {
        super(uuid);
    }

    public PasswordEntry(PasswordEntry other) {
        super(other.getUUID());
        _name = other._name;
        _username = other._username;
        _password = other._password;
        _website = other._website;
        _notes = other._notes;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("uuid", getUUID().toString());
            obj.put("name", _name);
            obj.put("username", _username);
            obj.put("password", _password);
            obj.put("website", _website);
            obj.put("notes", _notes);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    public static PasswordEntry fromJson(JSONObject obj)
            throws PasswordEntryException {
        try {
            UUID uuid = obj.has("uuid")
                    ? UUID.fromString(obj.getString("uuid"))
                    : UUID.randomUUID();

            PasswordEntry entry = new PasswordEntry(uuid);
            entry.setName(obj.getString("name"));
            entry.setUsername(obj.optString("username", ""));
            entry.setPassword(obj.getString("password"));
            entry.setWebsite(obj.optString("website", ""));
            entry.setNotes(obj.optString("notes", ""));

            entry.validate();
            return entry;
        } catch (JSONException | IllegalArgumentException | PasswordEntryException e) {
            throw new PasswordEntryException(e);
        }
    }

    public void validate() throws PasswordEntryException {
        if (_name.trim().isEmpty()) {
            throw new PasswordEntryException("Password entry name is required");
        }
        if (_password.isEmpty()) {
            throw new PasswordEntryException("Password is required");
        }
    }

    public String getName() {
        return _name;
    }

    public String getUsername() {
        return _username;
    }

    public String getPassword() {
        return _password;
    }

    public String getWebsite() {
        return _website;
    }

    public String getNotes() {
        return _notes;
    }

    public void setName(String name) {
        _name = name == null ? "" : name;
    }

    public void setUsername(String username) {
        _username = username == null ? "" : username;
    }

    public void setPassword(String password) {
        _password = password == null ? "" : password;
    }

    public void setWebsite(String website) {
        _website = website == null ? "" : website;
    }

    public void setNotes(String notes) {
        _notes = notes == null ? "" : notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PasswordEntry)) {
            return false;
        }
        PasswordEntry that = (PasswordEntry) o;
        return super.equals(that)
                && _name.equals(that._name)
                && _username.equals(that._username)
                && _password.equals(that._password)
                && _website.equals(that._website)
                && _notes.equals(that._notes);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getUUID(), _name, _username, _password, _website, _notes);
    }
}
