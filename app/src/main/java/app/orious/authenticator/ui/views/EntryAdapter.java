package app.orious.authenticator.ui.views;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.graphics.Typeface;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import app.orious.authenticator.AccountNamePosition;
import app.orious.authenticator.CopyBehavior;
import app.orious.authenticator.Preferences;
import app.orious.authenticator.R;
import app.orious.authenticator.SortCategory;
import app.orious.authenticator.ViewMode;
import app.orious.authenticator.helpers.ItemTouchHelperAdapter;
import app.orious.authenticator.helpers.comparators.FavoriteComparator;
import app.orious.authenticator.passwords.PasswordEntry;
import app.orious.authenticator.otp.HotpInfo;
import app.orious.authenticator.otp.OtpInfo;
import app.orious.authenticator.otp.OtpInfoException;
import app.orious.authenticator.otp.TotpInfo;
import app.orious.authenticator.ui.models.ErrorCardInfo;
import app.orious.authenticator.util.CollectionUtils;
import app.orious.authenticator.vault.VaultEntry;
import app.orious.authenticator.vault.VaultGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class EntryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {
    private EntryListView _view;
    private EntryList _entryList;
    private List<PasswordEntry> _passwordEntries = new ArrayList<>();
    private List<String> _unifiedOrder = new ArrayList<>();
    private List<VaultEntry> _selectedEntries;
    private Collection<VaultGroup> _groups;
    private Map<UUID, Integer> _usageCounts;
    private Map<UUID, Long> _lastUsedTimestamps;
    private VaultEntry _focusedEntry;
    private VaultEntry _clickedEntry;
    private Preferences.CodeGrouping _codeGroupSize;
    private AccountNamePosition _accountNamePosition;
    private boolean _showIcon;
    private boolean _showNextCode;
    private boolean _showExpirationState;
    private boolean _onlyShowNecessaryAccountNames;
    private boolean _highlightEntry;
    private boolean _tempHighlightEntry;
    private boolean _tapToReveal;
    private int _tapToRevealTime;
    private CopyBehavior _copyBehavior;
    private int _searchBehaviorMask;
    private Set<UUID> _groupFilter;
    private SortCategory _sortCategory;
    private ViewMode _viewMode;
    private String _searchFilter;
    private EntryTypeFilter _entryTypeFilter = EntryTypeFilter.ALL;
    private boolean _isPeriodUniform = true;
    private int _uniformPeriod = -1;
    private Handler _dimHandler;
    private Handler _doubleTapHandler;
    private boolean _pauseFocused;

    // keeps track of the EntryHolders that are currently bound
    private List<EntryHolder> _holders;

    public EntryAdapter(EntryListView view) {
        _entryList = new EntryList();
        _selectedEntries = new ArrayList<>();
        _groupFilter = new TreeSet<>();
        _holders = new ArrayList<>();
        _dimHandler = new Handler();
        _doubleTapHandler = new Handler();
        _view = view;
    }

    public void destroy() {
        for (EntryHolder holder : _holders) {
            holder.destroy();
        }
        _view = null;
    }

    public void setCodeGroupSize(Preferences.CodeGrouping codeGroupSize) {
        _codeGroupSize = codeGroupSize;
    }

    public void setAccountNamePosition(AccountNamePosition accountNamePosition) {
        _accountNamePosition = accountNamePosition;
    }

    public void setOnlyShowNecessaryAccountNames(boolean onlyShowNecessaryAccountNames) {
        _onlyShowNecessaryAccountNames = onlyShowNecessaryAccountNames;
    }

    public void setShowIcon(boolean showIcon) {
        _showIcon = showIcon;
    }

    public void setShowNextCode(boolean showNextCode) {
        _showNextCode = showNextCode;
    }

    public void setShowExpirationState(boolean showExpirationState) {
        _showExpirationState = showExpirationState;
    }

    public void setTapToReveal(boolean tapToReveal) {
        _tapToReveal = tapToReveal;
    }

    public void setTapToRevealTime(int number) {
        _tapToRevealTime = number;
    }

    public void setHighlightEntry(boolean highlightEntry) {
        _highlightEntry = highlightEntry;
    }

    public void setTempHighlightEntry(boolean highlightEntry) {
        _tempHighlightEntry = highlightEntry;
    }

    public void setCopyBehavior(CopyBehavior copyBehavior) { _copyBehavior = copyBehavior; }

    public void setSearchBehaviorMask(int searchBehaviorMask) { _searchBehaviorMask = searchBehaviorMask; }

    public void setPauseFocused(boolean pauseFocused) {
        _pauseFocused = pauseFocused;
    }

    public void setErrorCardInfo(ErrorCardInfo info) {
        if (Objects.equals(info, _entryList.getErrorCardInfo())) {
            return;
        }

        replaceEntryList(new EntryList(
                _entryList.getEntries(),
                _entryList.getShownEntries(),
                _entryList.getUnifiedEntries(),
                info
        ));
    }

    public VaultEntry getEntryAtPosition(int position) {
        UnifiedEntry unified = getUnifiedEntryAtPosition(position);
        return unified != null && unified.isOtp() ? unified.getOtpEntry() : null;
    }

    public int getEntryPosition(VaultEntry entry) {
        if (entry == null) {
            return NO_POSITION;
        }
        List<UnifiedEntry> unifiedEntries = _entryList.getUnifiedEntries();
        for (int i = 0; i < unifiedEntries.size(); i++) {
            UnifiedEntry unified = unifiedEntries.get(i);
            if (unified.isOtp() && unified.getOtpEntry() == entry) {
                return _entryList.translateEntryIndexToPos(i);
            }
        }
        return NO_POSITION;
    }

    public void setEntries(List<VaultEntry> entries) {
        setAllEntries(entries, _passwordEntries, _unifiedOrder);
    }

    public void setAllEntries(
            List<VaultEntry> entries,
            Collection<PasswordEntry> passwordEntries,
            Collection<String> unifiedOrder
    ) {
        // TODO: Move these fields to separate dedicated model for the UI
        for (VaultEntry entry : entries) {
            entry.setUsageCount(_usageCounts.containsKey(entry.getUUID()) ? _usageCounts.get(entry.getUUID()) : 0);
            entry.setLastUsedTimestamp(_lastUsedTimestamps.containsKey(entry.getUUID()) ? _lastUsedTimestamps.get(entry.getUUID()) : 0);
        }

        _passwordEntries = passwordEntries != null ? new ArrayList<>(passwordEntries) : new ArrayList<>();
        _unifiedOrder = unifiedOrder != null ? new ArrayList<>(unifiedOrder) : new ArrayList<>();

        List<VaultEntry> shownOtp = calculateShownEntries(entries);
        List<UnifiedEntry> unified = buildUnifiedEntries(shownOtp);
        replaceEntryList(new EntryList(
                entries,
                shownOtp,
                unified,
                _entryList.getErrorCardInfo()
        ));
    }

    public void setUnifiedOrder(Collection<String> order) {
        _unifiedOrder = order != null ? new ArrayList<>(order) : new ArrayList<>();
        refreshEntryList();
    }

    public void setPasswordEntries(Collection<PasswordEntry> entries) {
        _passwordEntries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
        refreshEntryList();
    }

    public void clearEntries() {
        _passwordEntries.clear();
        _unifiedOrder.clear();
        replaceEntryList(new EntryList());
    }

    public int translateEntryPosToIndex(int position) {
        return _entryList.translateEntryPosToIndex(position);
    }

    private boolean isEntryFiltered(VaultEntry entry) {
        Set<UUID> groups = entry.getGroups();
        String issuer = entry.getIssuer().toLowerCase();
        String name = entry.getName().toLowerCase();
        String note = entry.getNote().toLowerCase();

        if (_searchFilter != null) {
            String[] tokens = _searchFilter.toLowerCase().split("\\s+");

            // Return true if not all tokens match at least one of the relevant fields
            return !Arrays.stream(tokens)
                    .allMatch(token ->
                            ((_searchBehaviorMask & Preferences.SEARCH_IN_ISSUER) != 0 && issuer.contains(token)) ||
                                    ((_searchBehaviorMask & Preferences.SEARCH_IN_NAME) != 0 && name.contains(token)) ||
                                    ((_searchBehaviorMask & Preferences.SEARCH_IN_NOTE) != 0 && note.contains(token)) ||
                                    ((_searchBehaviorMask & Preferences.SEARCH_IN_GROUPS) != 0 && doesAnyGroupMatchSearchFilter(groups, token))
                    );
        }

        if (!_groupFilter.isEmpty()) {
            if (groups.isEmpty() && !_groupFilter.contains(null)) {
                return true;
            }
            if (!groups.isEmpty() && _groupFilter.stream().filter(Objects::nonNull).noneMatch(groups::contains)) {
                return true;
            }
        }

        return false;
    }

    private boolean doesAnyGroupMatchSearchFilter(Set<UUID> entryGroupUUIDs, String searchFilter) {
        return _groups.stream()
                .filter(group -> entryGroupUUIDs.contains(group.getUUID()))
                .map(VaultGroup::getName)
                .anyMatch(groupName -> groupName.toLowerCase().contains(searchFilter.toLowerCase()));
    }

    public void refresh(boolean hard) {
        if (hard) {
            refreshEntryList();
        } else {
            for (EntryHolder holder : _holders) {
                holder.refresh();
                holder.showIcon(_showIcon);
            }
        }
    }

    public void setGroupFilter(@NonNull Set<UUID> groups) {
        if (_groupFilter.equals(groups)) {
            return;
        }

        _groupFilter = groups;
        refreshEntryList();
    }

    public void setSortCategory(SortCategory category, boolean apply) {
        if (_sortCategory == category) {
            return;
        }

        _sortCategory = category;
        if (apply) {
            refreshEntryList();
        }
    }

    public String getSearchFilter() {
        return _searchFilter;
    }

    public void setSearchFilter(String search) {
        String newSearchFilter = (search != null && !search.isEmpty())
                ? search.toLowerCase().trim() : null;

        if (!Objects.equals(_searchFilter, newSearchFilter)) {
            _searchFilter = newSearchFilter;
            refreshEntryList();
        }
    }

    public EntryTypeFilter getEntryTypeFilter() {
        return _entryTypeFilter;
    }

    public void setEntryTypeFilter(@NonNull EntryTypeFilter filter) {
        if (_entryTypeFilter != filter) {
            _entryTypeFilter = filter;
            refreshEntryList();
        }
    }

    private void refreshEntryList() {
        List<VaultEntry> shownOtp = calculateShownEntries(_entryList.getEntries());
        List<UnifiedEntry> unified = buildUnifiedEntries(shownOtp);
        replaceEntryList(new EntryList(
                _entryList.getEntries(),
                shownOtp,
                unified,
                _entryList.getErrorCardInfo()
        ));
    }

    private void replaceEntryList(EntryList newEntryList) {
        DiffUtil.DiffResult diffRes = DiffUtil.calculateDiff(new DiffCallback(_entryList, newEntryList));
        _entryList = newEntryList;
        updatePeriodUniformity();

        // This scroll position trick is required in order to not have the recycler view
        // jump to some random position after a large change (like resorting entries)
        // Related: https://issuetracker.google.com/issues/70149059
        int scrollPos = _view.getScrollPosition();
        diffRes.dispatchUpdatesTo(this);
        _view.scrollToPosition(scrollPos);
        _view.onListChange();
    }

    private List<VaultEntry> calculateShownEntries(List<VaultEntry> entries) {
        List<VaultEntry> res = new ArrayList<>();
        for (VaultEntry entry : entries) {
            if (!isEntryFiltered(entry)) {
                res.add(entry);
            }
        }

        sortEntries(res, _sortCategory);
        return res;
    }

    private static void sortEntries(List<VaultEntry> entries, SortCategory sortCategory) {
        if (sortCategory != null) {
            Comparator<VaultEntry> comparator = sortCategory.getComparator();
            if (comparator != null) {
                Collections.sort(entries, comparator);
            }
        }

        Comparator<VaultEntry> favoriteComparator = new FavoriteComparator();
        Collections.sort(entries, favoriteComparator);
    }


    private boolean isPasswordEntryFiltered(PasswordEntry entry) {
        if (_searchFilter != null) {
            String[] tokens = _searchFilter.toLowerCase().split("\\s+");
            String name = entry.getName().toLowerCase();
            String username = entry.getUsername().toLowerCase();
            String website = entry.getWebsite().toLowerCase();
            String notes = entry.getNotes().toLowerCase();

            return !Arrays.stream(tokens).allMatch(token ->
                    name.contains(token)
                            || username.contains(token)
                            || website.contains(token)
                            || notes.contains(token)
            );
        }

        if (_entryTypeFilter != EntryTypeFilter.PASSWORDS_ONLY && !_groupFilter.isEmpty()) {
            return !_groupFilter.contains(null);
        }

        return false;
    }

    private List<UnifiedEntry> buildUnifiedEntries(List<VaultEntry> shownOtpEntries) {
        List<UnifiedEntry> result = new ArrayList<>();

        if (_entryTypeFilter != EntryTypeFilter.PASSWORDS_ONLY) {
            for (VaultEntry entry : shownOtpEntries) {
                result.add(UnifiedEntry.otp(entry));
            }
        }

        if (_entryTypeFilter != EntryTypeFilter.OTP_ONLY) {
            for (PasswordEntry entry : _passwordEntries) {
                if (!isPasswordEntryFiltered(entry)) {
                    result.add(UnifiedEntry.password(entry));
                }
            }
        }

        if (_sortCategory == SortCategory.CUSTOM) {
            Map<String, Integer> orderIndex = new HashMap<>(_unifiedOrder.size());
            for (int i = 0; i < _unifiedOrder.size(); i++) {
                orderIndex.put(_unifiedOrder.get(i), i);
            }

            result.sort((first, second) -> {
                boolean firstFav = first.isOtp() && first.getOtpEntry().isFavorite();
                boolean secondFav = second.isOtp() && second.getOtpEntry().isFavorite();
                if (firstFav != secondFav) {
                    return firstFav ? -1 : 1;
                }

                String firstId = (first.isOtp() ? "O:" : "P:") + first.getUUID();
                String secondId = (second.isOtp() ? "O:" : "P:") + second.getUUID();
                int idx1 = orderIndex.getOrDefault(firstId, Integer.MAX_VALUE);
                int idx2 = orderIndex.getOrDefault(secondId, Integer.MAX_VALUE);
                return Integer.compare(idx1, idx2);
            });
            return result;
        }

        result.sort((first, second) -> {
            if (first.isOtp() && second.isOtp()) {
                boolean firstFav = first.getOtpEntry().isFavorite();
                boolean secondFav = second.getOtpEntry().isFavorite();
                if (firstFav != secondFav) {
                    return firstFav ? -1 : 1;
                }
                Comparator<VaultEntry> comparator = _sortCategory != null ? _sortCategory.getComparator() : null;
                if (comparator != null) {
                    return comparator.compare(first.getOtpEntry(), second.getOtpEntry());
                }
                return 0;
            }

            if (!first.isOtp() && !second.isOtp()) {
                int comparison = String.CASE_INSENSITIVE_ORDER.compare(
                        first.getName(), second.getName());
                return (_sortCategory == SortCategory.ACCOUNT_REVERSED
                        || _sortCategory == SortCategory.ISSUER_REVERSED)
                        ? -comparison : comparison;
            }

            return first.isOtp() ? -1 : 1;
        });

        return result;
    }

    private boolean isEntryDraggable(VaultEntry entry) {
        return entry != null
                && isDragAndDropAllowed()
                && _selectedEntries.size() == 1
                && !_selectedEntries.get(0).isFavorite()
                && _selectedEntries.get(0) == entry;
    }

    public void setViewMode(ViewMode viewMode) {
        _viewMode = viewMode;
    }

    public void setUsageCounts(Map<UUID, Integer> usageCounts) { _usageCounts = usageCounts; }

    public Map<UUID, Integer> getUsageCounts() { return _usageCounts; }

    public void setGroups(Collection<VaultGroup> groups) { _groups = groups; }

    public void setLastUsedTimestamps(Map<UUID, Long> lastUsedTimestamps) { _lastUsedTimestamps = lastUsedTimestamps; }

    public Map<UUID, Long> getLastUsedTimestamps() { return _lastUsedTimestamps; }

    public int getShownFavoritesCount() {
        return (int) _entryList.getShownEntries().stream().filter(VaultEntry::isFavorite).count();
    }

    @Override
    public void onItemDismiss(int position) {

    }

    @Override
    public void onItemDrop(int position) {
        if (!_groupFilter.isEmpty()) {
            return;
        }
        _view.onUnifiedEntryDrop();
    }

    @Override
    public void onItemMove(int firstPosition, int secondPosition) {
        if (!_groupFilter.isEmpty()) {
            return;
        }

        UnifiedEntry first = getUnifiedEntryAtPosition(firstPosition);
        UnifiedEntry second = getUnifiedEntryAtPosition(secondPosition);
        if (first == null || second == null) {
            return;
        }

        String firstId = (first.isOtp() ? "O:" : "P:") + first.getUUID();
        String secondId = (second.isOtp() ? "O:" : "P:") + second.getUUID();

        _unifiedOrder.remove(firstId);
        int target = _unifiedOrder.indexOf(secondId);
        if (target < 0) {
            _unifiedOrder.add(secondId);
            target = _unifiedOrder.size() - 1;
        }
        _unifiedOrder.add(target, firstId);

        int firstIdx = _entryList.translateEntryPosToIndex(firstPosition);
        int secondIdx = _entryList.translateEntryPosToIndex(secondPosition);
        List<UnifiedEntry> unified = _entryList.getUnifiedEntries();
        if (firstIdx >= 0 && firstIdx < unified.size() && secondIdx >= 0 && secondIdx < unified.size()) {
            UnifiedEntry moved = unified.remove(firstIdx);
            unified.add(secondIdx, moved);
        }

        _view.onUnifiedEntryMove(firstId, secondId);
        notifyItemMoved(firstPosition, secondPosition);
    }


    private UnifiedEntry getUnifiedEntryAtPosition(int position) {
        int index = _entryList.translateEntryPosToIndex(position);
        List<UnifiedEntry> entries = _entryList.getUnifiedEntries();
        return index < 0 || index >= entries.size() ? null : entries.get(index);
    }

    @Override
    public int getItemViewType(int position) {
        if (_entryList.isErrorCardShown() && position == 0) {
            return R.layout.card_error;
        }
        if (position == getItemCount() - 1) {
            return R.layout.card_footer;
        }
        UnifiedEntry entry = getUnifiedEntryAtPosition(position);
        if (entry != null && !entry.isOtp()) {
            return R.layout.card_password_entry;
        }
        return _viewMode.getLayoutId();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        RecyclerView.ViewHolder holder;
        View view = inflater.inflate(viewType, parent, false);

        if (viewType == R.layout.card_error) {
            holder = new ErrorCardHolder(view, Objects.requireNonNull(_entryList.getErrorCardInfo()));
        } else if (viewType == R.layout.card_password_entry) {
            holder = new PasswordRowHolder(view);
        } else if (viewType == R.layout.card_footer) {
            holder = new FooterView(view);
        } else {
            holder = new EntryHolder(view);
        }

        if (_showIcon && holder instanceof EntryHolder) {
            _view.setPreloadView(((EntryHolder) holder).getIconView());
        }

        return holder;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof EntryHolder) {
            ((EntryHolder) holder).stopRefreshLoop();
            _holders.remove(holder);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PasswordRowHolder) {
            UnifiedEntry unifiedEntry = getUnifiedEntryAtPosition(position);
            if (unifiedEntry != null && !unifiedEntry.isOtp()) {
                PasswordEntry password = unifiedEntry.getPasswordEntry();
                PasswordRowHolder passwordHolder = (PasswordRowHolder) holder;
                passwordHolder.name.setText(password.getName());
                if (password.getUsername().isEmpty()) {
                    passwordHolder.username.setVisibility(View.GONE);
                } else {
                    passwordHolder.username.setVisibility(View.VISIBLE);
                    passwordHolder.username.setText(password.getUsername());
                }
                passwordHolder.copyButton.setOnClickListener(v -> _view.onPasswordEntryCopy(password));
                passwordHolder.itemView.setOnClickListener(v -> _view.onPasswordEntryClick(password));
            }
            return;
        }

        if (holder instanceof EntryHolder) {
            EntryHolder entryHolder = (EntryHolder) holder;
            UnifiedEntry unifiedEntry = getUnifiedEntryAtPosition(position);
            if (unifiedEntry == null || !unifiedEntry.isOtp()) {
                return;
            }
            VaultEntry entry = unifiedEntry.getOtpEntry();

            boolean hidden = _tapToReveal && !entry.equals(_focusedEntry);
            boolean paused = _pauseFocused && entry.equals(_focusedEntry);
            boolean dimmed = (_highlightEntry || _tempHighlightEntry) && _focusedEntry != null && !_focusedEntry.equals(entry);
            boolean showProgress = entry.getInfo() instanceof TotpInfo && ((TotpInfo) entry.getInfo()).getPeriod() != getMostFrequentPeriod();
            boolean showAccountName = true;
            if (_onlyShowNecessaryAccountNames) {
                // Only show account name when there's multiple entries found with the same issuer.
                showAccountName = _entryList.getEntries().stream()
                        .filter(x -> x.getIssuer().equals(entry.getIssuer()))
                        .count() > 1;
            }

            AccountNamePosition accountNamePosition = showAccountName ? _accountNamePosition : AccountNamePosition.HIDDEN;
            entryHolder.setData(entry, _codeGroupSize, _viewMode, accountNamePosition, _showIcon, showProgress, hidden, paused, dimmed, _showExpirationState, _showNextCode);
            entryHolder.setFocused(_selectedEntries.contains(entry));
            entryHolder.setShowDragHandle(isEntryDraggable(entry));

            if (_showIcon) {
                entryHolder.loadIcon(_view);
            }

            entryHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean handled = false;

                    if (_selectedEntries.isEmpty()) {
                        if (_highlightEntry || _tempHighlightEntry || _tapToReveal) {
                            if (_focusedEntry != null && _focusedEntry.equals(entry)) {
                                resetFocus();
                            } else {
                                focusEntry(entry, _tapToRevealTime);

                                // Prevent copying when singletap is set and the entry is being revealed
                                handled = _copyBehavior == CopyBehavior.SINGLETAP && _tapToReveal;
                            }
                        }

                        switch (_copyBehavior) {
                            case SINGLETAP:
                                if (!handled) {
                                    _view.onEntryCopy(entry);
                                    entryHolder.animateCopyText();
                                    _clickedEntry = null;
                                }
                                break;
                            case DOUBLETAP:
                                _doubleTapHandler.postDelayed(() -> _clickedEntry = null, ViewConfiguration.getDoubleTapTimeout());

                                if(entry == _clickedEntry) {
                                    _view.onEntryCopy(entry);
                                    entryHolder.animateCopyText();
                                    _clickedEntry = null;
                                } else {
                                    _clickedEntry = entry;
                                }
                                break;
                        }

                        incrementUsageCount(entry);
                    } else {
                        if (_selectedEntries.contains(entry)) {
                            _view.onDeselect(entry);
                            removeSelectedEntry(entry);
                            entryHolder.setFocusedAndAnimate(false);
                        } else {
                            entryHolder.setFocusedAndAnimate(true);
                            addSelectedEntry(entry);
                            _view.onSelect(entry);
                        }
                    }

                    if (!handled) {
                        _view.onEntryClick(entry);
                    }
                }
            });
            entryHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = holder.getBindingAdapterPosition();
                    if (_selectedEntries.isEmpty()) {
                        entryHolder.setFocusedAndAnimate(true);
                    }

                    boolean returnVal = _view.onLongEntryClick(entry);
                    if (_selectedEntries.size() == 0 || isEntryDraggable(entry)) {
                        _view.startDrag(entryHolder);
                    }

                    return returnVal;
                }
            });
            entryHolder.itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Start drag if this is the only item selected
                    if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                            && isEntryDraggable(entryHolder.getEntry())) {
                        _view.startDrag(entryHolder);
                        return true;
                    }
                    return false;
                }
            });
            entryHolder.setOnRefreshClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // this will only be called if the entry is of type HotpInfo
                    try {
                        ((HotpInfo) entry.getInfo()).incrementCounter();
                        focusEntry(entry, _tapToRevealTime);
                    } catch (OtpInfoException e) {
                        throw new RuntimeException(e);
                    }

                    // notify the listener that the counter has been incremented
                    // this gives it a chance to save the vault
                    _view.onEntryChange(entry);

                    // finally, refresh the code in the UI
                    entryHolder.refreshCode();
                }
            });

            _holders.add(entryHolder);
        } else if (holder instanceof FooterView) {
            ((FooterView) holder).refresh();
        }
    }

    private void updatePeriodUniformity() {
        int mostFrequentPeriod = getMostFrequentPeriod();
        boolean uniform = isPeriodUniform();
        if (uniform == _isPeriodUniform && mostFrequentPeriod == _uniformPeriod) {
            return;
        }

        _isPeriodUniform = uniform;
        _uniformPeriod = mostFrequentPeriod;

        for (EntryHolder holder : _holders) {
            if ((holder.getEntry().getInfo() instanceof TotpInfo)) {
                holder.setShowProgress(((TotpInfo) holder.getEntry().getInfo()).getPeriod() != mostFrequentPeriod);
            }
        }

        _view.onPeriodUniformityChanged(_isPeriodUniform, _uniformPeriod);
    }

    public int getMostFrequentPeriod() {
        List<TotpInfo> infos = new ArrayList<>();
        for (VaultEntry entry : _entryList.getShownEntries()) {
            OtpInfo info = entry.getInfo();
            if (info instanceof TotpInfo) {
                infos.add((TotpInfo) info);
            }
        }

        if (infos.isEmpty()) {
            return -1;
        }

        if (infos.size() == 1) {
            return infos.get(0).getPeriod();
        }

        Map<Integer, Integer> occurrences = new HashMap<>();
        for (TotpInfo info : infos) {
            int period = info.getPeriod();
            if(occurrences.containsKey(period)) {
                occurrences.put(period, occurrences.get(period) + 1);
            } else {
                occurrences.put(period, 1);
            }
        }

        Integer maxValue = 0;
        Integer maxKey = 0;
        for (Map.Entry<Integer, Integer> entry : occurrences.entrySet()){
            if(entry.getValue() > maxValue){
                maxValue = entry.getValue();
                maxKey = entry.getKey();
            }
        }

        return maxValue > 1 ? maxKey : -1;
    }

    public void focusEntry(VaultEntry entry, int secondsToFocus) {
        _focusedEntry = entry;
        _dimHandler.removeCallbacksAndMessages(null);

        for (EntryHolder holder : _holders) {
            if (!holder.getEntry().equals(_focusedEntry)) {
                if (_highlightEntry || _tempHighlightEntry) {
                    holder.dim();
                }
                if (_tapToReveal) {
                    holder.hideCode();
                }
                if (_pauseFocused) {
                    holder.setPaused(false);
                }
            } else {
                if (_highlightEntry || _tempHighlightEntry) {
                    holder.highlight();
                }
                if (_tapToReveal) {
                    holder.revealCode();
                }
                if (_pauseFocused) {
                    holder.setPaused(true);
                }
            }
        }

        _dimHandler.postDelayed(this::resetFocus, secondsToFocus * 1000);
    }

    private void resetFocus() {
        for (EntryHolder holder : _holders) {
            if (_focusedEntry != null) {
                holder.highlight();
            }
            if (_tapToReveal) {
                holder.hideCode();
            }
            if (_pauseFocused) {
                holder.setPaused(false);
            }
        }

        _focusedEntry = null;
        _tempHighlightEntry = false;
    }

    private void updateDraggableStatus() {
        for (EntryHolder holder : _holders) {
            VaultEntry entry = holder.getEntry();
            if (isEntryDraggable(entry)) {
                holder.setShowDragHandle(true);
                _view.setSelectedEntry(entry);
                break;
            }

            holder.setShowDragHandle(false);
        }
    }

    public void removeSelectedEntry(VaultEntry entry) {
        _selectedEntries.remove(entry);
        updateDraggableStatus();
    }

    public void addSelectedEntry(VaultEntry entry) {
        if (_focusedEntry != null) {
            resetFocus();
        }

        _selectedEntries.add(entry);
        updateDraggableStatus();
    }

    public List<VaultEntry> selectAllEntries() {
        _selectedEntries.clear();

        for (VaultEntry entry: _entryList.getShownEntries()) {
            for (EntryHolder holder: _holders) {
                if (holder.getEntry().equals(entry)) {
                    holder.setFocused(true);
                }
            }

            _selectedEntries.add(entry);
            updateDraggableStatus();
        }

        return new ArrayList<>(_selectedEntries);
    }

    public void deselectAllEntries() {
        for (VaultEntry entry: _selectedEntries) {
            for (EntryHolder holder : _holders) {
                if (holder.getEntry().equals(entry)) {
                    holder.setFocusedAndAnimate(false);
                    break;
                }
            }
        }

        _selectedEntries.clear();

        updateDraggableStatus();
    }

    private void incrementUsageCount(VaultEntry entry) {
        if (!_usageCounts.containsKey(entry.getUUID())) {
            _usageCounts.put(entry.getUUID(), 1);
        } else {
            int usageCount = _usageCounts.get(entry.getUUID());
            _usageCounts.put(entry.getUUID(), ++usageCount);
        }

        _lastUsedTimestamps.put(entry.getUUID(), new Date().getTime());
    }

    public boolean isPasswordEntryAtPosition(int position) {
        UnifiedEntry entry = getUnifiedEntryAtPosition(position);
        return entry != null && !entry.isOtp();
    }

    public boolean isPositionInFavoriteSection(int position) {
        UnifiedEntry entry = getUnifiedEntryAtPosition(position);
        return entry != null && entry.isOtp() && entry.getOtpEntry().isFavorite();
    }

    public boolean isDragAndDropAllowed() {
        return _sortCategory == SortCategory.CUSTOM && _groupFilter.isEmpty() && _searchFilter == null;
    }

    public boolean isPeriodUniform() {
        return isPeriodUniform(getMostFrequentPeriod());
    }

    private static boolean isPeriodUniform(int period) {
        return period != -1;
    }

    @Override
    public int getItemCount() {
        return _entryList.getItemCount();
    }

    public int getShownEntriesCount() {
        return _entryList.getUnifiedEntries().size();
    }

    public boolean isPositionFooter(int position) {
        return _entryList.isPositionFooter(position);
    }

    public boolean isPositionErrorCard(int position) {
        return _entryList.isPositionErrorCard(position);
    }

    public boolean isErrorCardShown() {
        return _entryList.isErrorCardShown();
    }

    private static class PasswordRowHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView username;
        final ImageButton copyButton;

        PasswordRowHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.password_card_name);
            username = itemView.findViewById(R.id.password_card_username);
            copyButton = itemView.findViewById(R.id.password_card_copy);
        }
    }

    private class FooterView extends RecyclerView.ViewHolder {
        View _footerView;

        public FooterView(@NonNull View itemView) {
            super(itemView);
            _footerView = itemView;
        }

        public void refresh() {
            int entriesShown = getShownEntriesCount();
            SpannableString entriesShownSpannable = new SpannableString(_footerView.getResources().getQuantityString(R.plurals.entries_shown, entriesShown, entriesShown));

            String entriesShownString = String.format("%d", entriesShown);
            int spanStart = entriesShownSpannable.toString().indexOf(entriesShownString);
            if (spanStart >= 0) {
                int spanEnd = spanStart + entriesShownString.length();
                entriesShownSpannable.setSpan(new StyleSpan(Typeface.BOLD), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            TextView textView = _footerView.findViewById(R.id.entries_shown_count);
            textView.setText(entriesShownSpannable);
        }
    }

    private static class EntryList {
        private final List<VaultEntry> _entries;
        private final List<VaultEntry> _shownEntries;
        private final List<UnifiedEntry> _unifiedEntries;
        private final ErrorCardInfo _errorCardInfo;

        public EntryList() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);
        }

        public EntryList(
                @NonNull List<VaultEntry> entries,
                @NonNull List<VaultEntry> shownEntries,
                @NonNull List<UnifiedEntry> unifiedEntries,
                @Nullable ErrorCardInfo errorCardInfo
        ) {
            _entries = entries;
            _shownEntries = shownEntries;
            _unifiedEntries = unifiedEntries;
            _errorCardInfo = errorCardInfo;
        }

        public List<VaultEntry> getEntries() {
            return _entries;
        }

        public List<VaultEntry> getShownEntries() {
            return _shownEntries;
        }

        public List<UnifiedEntry> getUnifiedEntries() {
            return _unifiedEntries;
        }

        public int getItemCount() {
            // Always at least one item because of the footer
            // Two in case there's also an error card
            int baseCount = 1;
            if (isErrorCardShown()) {
                baseCount++;
            }

            return baseCount + _unifiedEntries.size();
        }

        @Nullable
        public ErrorCardInfo getErrorCardInfo() {
            return _errorCardInfo;
        }

        public boolean isErrorCardShown() {
            return _errorCardInfo != null;
        }

        public boolean isPositionErrorCard(int position) {
            return isErrorCardShown() && position == 0;
        }

        public boolean isPositionFooter(int position) {
            return position == (getItemCount() - 1);
        }

        /**
         * Translates the given entry position in the recycler view, to its index in the unified entries list.
         */
        public int translateEntryPosToIndex(int position) {
            if (position == NO_POSITION) {
                return NO_POSITION;
            }

            if (isErrorCardShown()) {
                position -= 1;
            }

            return position;
        }

        /**
         * Translates the given entry index in the unified entries list, to its position in the recycler view.
         */
        public int translateEntryIndexToPos(int index) {
            if (index == NO_POSITION) {
                return NO_POSITION;
            }

            if (isErrorCardShown()) {
                index += 1;
            }

            return index;
        }

    }

    private static class DiffCallback extends DiffUtil.Callback {
        private final EntryList _old;
        private final EntryList _new;

        public DiffCallback(EntryList oldList, EntryList newList) {
            _old = oldList;
            _new = newList;
        }

        @Override
        public int getOldListSize() {
            return _old.getItemCount();
        }

        @Override
        public int getNewListSize() {
            return _new.getItemCount();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            if (_old.isPositionErrorCard(oldItemPosition) != _new.isPositionErrorCard(newItemPosition)
                    || _old.isPositionFooter(oldItemPosition) != _new.isPositionFooter(newItemPosition)) {
                return false;
            }

            if ((_old.isPositionFooter(oldItemPosition) && _new.isPositionFooter(newItemPosition))
                    || (_old.isPositionErrorCard(oldItemPosition) && _new.isPositionErrorCard(newItemPosition))) {
                return true;
            }

            int oldEntryIndex = _old.translateEntryPosToIndex(oldItemPosition);
            int newEntryIndex = _new.translateEntryPosToIndex(newItemPosition);
            if (oldEntryIndex < 0 || newEntryIndex < 0
                    || oldEntryIndex >= _old.getUnifiedEntries().size()
                    || newEntryIndex >= _new.getUnifiedEntries().size()) {
                return false;
            }

            UnifiedEntry oldEntry = _old.getUnifiedEntries().get(oldEntryIndex);
            UnifiedEntry newEntry = _new.getUnifiedEntries().get(newEntryIndex);
            return oldEntry.getType() == newEntry.getType()
                    && oldEntry.getUUID().equals(newEntry.getUUID());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            if (_old.isPositionFooter(oldItemPosition) && _new.isPositionFooter(newItemPosition)) {
                return _old.getUnifiedEntries().size() == _new.getUnifiedEntries().size();
            }

            if (_old.isPositionErrorCard(oldItemPosition) && _new.isPositionErrorCard(newItemPosition)) {
                return Objects.equals(_old.getErrorCardInfo(), _new.getErrorCardInfo());
            }

            int oldEntryIndex = _old.translateEntryPosToIndex(oldItemPosition);
            int newEntryIndex = _new.translateEntryPosToIndex(newItemPosition);
            UnifiedEntry oldEntry = _old.getUnifiedEntries().get(oldEntryIndex);
            UnifiedEntry newEntry = _new.getUnifiedEntries().get(newEntryIndex);
            if (oldEntry.isOtp() && newEntry.isOtp()) {
                return oldEntry.getOtpEntry().equals(newEntry.getOtpEntry());
            }
            if (!oldEntry.isOtp() && !newEntry.isOtp()) {
                return oldEntry.getPasswordEntry().equals(newEntry.getPasswordEntry());
            }
            return false;
        }
    }

    public enum EntryTypeFilter {
        ALL,
        OTP_ONLY,
        PASSWORDS_ONLY
    }

    public interface Listener {
        void onEntryClick(VaultEntry entry);
        boolean onLongEntryClick(VaultEntry entry);
        void onEntryMove(VaultEntry entry1, VaultEntry entry2);
        void onUnifiedEntryMove(String firstId, String secondId);
        void onUnifiedEntryDrop();
        void onEntryDrop(VaultEntry entry);
        void onEntryChange(VaultEntry entry);
        void onEntryCopy(VaultEntry entry);
        void onPasswordEntryCopy(PasswordEntry entry);
        void onPeriodUniformityChanged(boolean uniform, int period);
        void onSelect(VaultEntry entry);
        void onDeselect(VaultEntry entry);
        void onListChange();
    }
}
