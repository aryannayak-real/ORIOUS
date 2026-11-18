package app.orious.authenticator.vault;

import androidx.annotation.Nullable;

import app.orious.authenticator.notes.NoteEntry;
import app.orious.authenticator.notes.NoteEntryException;
import app.orious.authenticator.util.UUIDMap;
import app.orious.authenticator.passwords.PasswordEntry;
import app.orious.authenticator.passwords.PasswordEntryException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;
import java.util.UUID;

public class Vault {
    private static final int VERSION = 5;
    private final UUIDMap<VaultEntry> _entries = new UUIDMap<>();
    private final UUIDMap<PasswordEntry> _passwordEntries = new UUIDMap<>();
    private final UUIDMap<NoteEntry> _notes = new UUIDMap<>();
    private final UUIDMap<VaultGroup> _groups = new UUIDMap<>();
    private final java.util.List<String> _unifiedOrder = new java.util.ArrayList<>();
    private boolean _iconsOptimized = true;

    // Whether we've migrated the group list to the new format while parsing the vault
    private boolean _isGroupsMigrationFresh = false;

    public JSONObject toJson() {
        return toJson(null);
    }

    public JSONObject toJson(@Nullable EntryFilter filter) {
        try {
            JSONArray entriesArray = new JSONArray();
            for (VaultEntry e : _entries) {
                if (filter == null || filter.includeEntry(e)) {
                    entriesArray.put(e.toJson());
                }
            }

            // Password and private note records are included in full vault exports only.
            // Existing filtered exports remain OTP-only.
            JSONArray passwordEntriesArray = new JSONArray();
            JSONArray notesArray = new JSONArray();
            if (filter == null) {
                for (PasswordEntry entry : _passwordEntries) {
                    passwordEntriesArray.put(entry.toJson());
                }
                for (NoteEntry note : _notes) {
                    notesArray.put(note.toJson());
                }
            }

            // Always include all groups, even if they're not assigned to any entry (before or after the entry filter)
            JSONArray groupsArray = new JSONArray();
            for (VaultGroup group : _groups) {
                groupsArray.put(group.toJson());
            }

            JSONObject obj = new JSONObject();
            obj.put("version", VERSION);
            obj.put("entries", entriesArray);
            obj.put("passwordEntries", passwordEntriesArray);
            obj.put("notes", notesArray);
                if (filter == null) {
                    JSONArray unifiedOrderArray = new JSONArray();
                    for (String item : getUnifiedOrder()) {
                        unifiedOrderArray.put(item);
                    }
                    obj.put("unifiedOrder", unifiedOrderArray);
                }
            obj.put("groups", groupsArray);
            obj.put("icons_optimized", _iconsOptimized);

            return obj;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static Vault fromJson(JSONObject obj) throws VaultException {
        Vault vault = new Vault();
        UUIDMap<VaultEntry> entries = vault.getEntries();
        UUIDMap<VaultGroup> groups = vault.getGroups();

        try {
            int ver = obj.getInt("version");
            if (ver > VERSION) {
                throw new VaultException("Unsupported version");
            }

            if (obj.has("groups")) {
                JSONArray groupsArray = obj.getJSONArray("groups");
                for (int i = 0; i < groupsArray.length(); i++) {
                    VaultGroup group = VaultGroup.fromJson(groupsArray.getJSONObject(i));
                    if (!groups.has(group)) {
                        groups.add(group);
                    }
                }
            }

            JSONArray array = obj.getJSONArray("entries");
            for (int i = 0; i < array.length(); i++) {
                VaultEntry entry = VaultEntry.fromJson(array.getJSONObject(i));
                if (vault.migrateOldGroup(entry)) {
                    vault.setGroupsMigrationFresh();
                }

                // check the vault has a group corresponding to each one the entry claims to be in
                for (UUID groupUuid: entry.getGroups()) {
                    if (!groups.has(groupUuid)) {
                        entry.removeGroup(groupUuid);
                    }
                }

                entries.add(entry);
            }

            JSONArray passwordArray = obj.optJSONArray("passwordEntries");
            if (passwordArray != null) {
                for (int i = 0; i < passwordArray.length(); i++) {
                    PasswordEntry entry = PasswordEntry.fromJson(
                            passwordArray.getJSONObject(i));
                    if (!vault.getPasswordEntries().has(entry.getUUID())) {
                        vault.getPasswordEntries().add(entry);
                    }
                }
            }


            JSONArray notesArray = obj.optJSONArray("notes");
            if (notesArray != null) {
                for (int i = 0; i < notesArray.length(); i++) {
                    NoteEntry note = NoteEntry.fromJson(notesArray.getJSONObject(i));
                    if (!vault.getNotes().has(note.getUUID())) {
                        vault.getNotes().add(note);
                    }
                }
            }


            JSONArray unifiedOrderArray = obj.optJSONArray("unifiedOrder");
            if (unifiedOrderArray != null) {
                for (int i = 0; i < unifiedOrderArray.length(); i++) {
                    String item = unifiedOrderArray.optString(i, "");
                    if (!item.isEmpty()) {
                        vault._unifiedOrder.add(item);
                    }
                }
            }

            if (!obj.optBoolean("icons_optimized")) {
                vault.setIconsOptimized(false);
            }
        } catch (VaultEntryException | PasswordEntryException | NoteEntryException | JSONException e) {
            throw new VaultException(e);
        }

        return vault;
    }

    private void setGroupsMigrationFresh() {
        _isGroupsMigrationFresh = true;
    }

    public boolean isGroupsMigrationFresh() {
        return _isGroupsMigrationFresh;
    }

    public void setIconsOptimized(boolean optimized) {
        _iconsOptimized = optimized;
    }

    public boolean areIconsOptimized() {
        return _iconsOptimized;
    }

    public boolean migrateOldGroup(VaultEntry entry) {
        if (entry.getOldGroup() != null) {
            Optional<VaultGroup> optGroup = getGroups().getValues()
                                                 .stream()
                                                 .filter(g -> g.getName().equals(entry.getOldGroup()))
                                                 .findFirst();

            if (optGroup.isPresent()) {
                entry.addGroup(optGroup.get().getUUID());
            } else {
                VaultGroup group = new VaultGroup(entry.getOldGroup());
                getGroups().add(group);
                entry.addGroup(group.getUUID());
            }

            entry.setOldGroup(null);
            return true;
        }

        return false;
    }


    public java.util.List<String> getUnifiedOrder() {
        java.util.Set<String> valid = new java.util.LinkedHashSet<>();

        for (VaultEntry entry : _entries) {
            valid.add("O:" + entry.getUUID());
        }
        for (PasswordEntry entry : _passwordEntries) {
            valid.add("P:" + entry.getUUID());
        }

        java.util.List<String> normalized = new java.util.ArrayList<>(valid.size());
        java.util.Set<String> seen = new java.util.HashSet<>(valid.size());
        for (String item : _unifiedOrder) {
            if (valid.contains(item) && seen.add(item)) {
                normalized.add(item);
            }
        }
        for (String item : valid) {
            if (seen.add(item)) {
                normalized.add(item);
            }
        }

        _unifiedOrder.clear();
        _unifiedOrder.addAll(normalized);
        return _unifiedOrder;
    }

    public void setUnifiedOrder(java.util.List<String> order) {
        _unifiedOrder.clear();
        if (order != null) {
            _unifiedOrder.addAll(order);
        }
        getUnifiedOrder();
    }

    public void moveUnifiedEntry(String firstId, String secondId) {
        java.util.List<String> order =
                new java.util.ArrayList<>(getUnifiedOrder());

        int first = order.indexOf(firstId);
        int second = order.indexOf(secondId);

        if (first < 0 || second < 0 || first == second) {
            return;
        }

        String item = order.remove(first);
        order.add(second, item);
        setUnifiedOrder(order);
    }

public UUIDMap<VaultEntry> getEntries() {
        return _entries;
    }

    public UUIDMap<VaultGroup> getGroups() {
        return _groups;
    }

    public UUIDMap<PasswordEntry> getPasswordEntries() {
        return _passwordEntries;
    }

    public UUIDMap<NoteEntry> getNotes() {
        return _notes;
    }

    public interface EntryFilter {
        boolean includeEntry(VaultEntry entry);
    }
}
