package app.orious.authenticator.helpers;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import app.orious.authenticator.ui.views.EntryAdapter;
import app.orious.authenticator.vault.VaultEntry;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private VaultEntry _selectedEntry;
    private final EntryAdapter _adapter;
    private boolean _positionChanged = false;
    private boolean _isLongPressDragEnabled = true;
    private int _dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;

    public SimpleItemTouchHelperCallback(EntryAdapter adapter) {
        _adapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return _isLongPressDragEnabled;
    }

    public void setIsLongPressDragEnabled(boolean enabled) {
        _isLongPressDragEnabled = enabled;
    }

    public void setSelectedEntry(VaultEntry entry) {
        if (entry == null) {
            _selectedEntry = null;
            return;
        }

        if (!entry.isFavorite()) {
            _selectedEntry = entry;
        }
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    public void setDragFlags(int dragFlags) {
        _dragFlags = dragFlags;
    }

    @Override
    public int getMovementFlags(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder
    ) {
        if (viewHolder.getBindingAdapter() != _adapter) {
            return 0;
        }

        int position = viewHolder.getBindingAdapterPosition();
        if (position == NO_POSITION) {
            return 0;
        }

        boolean passwordRow = _adapter.isPasswordEntryAtPosition(position);
        boolean selectedOtpRow = !passwordRow
                && _adapter.getEntryAtPosition(position) == _selectedEntry;

        if (_adapter.isPositionFooter(position)
                || _adapter.isPositionErrorCard(position)
                || (!passwordRow && !selectedOtpRow)
                || !_adapter.isDragAndDropAllowed()
                || !isLongPressDragEnabled()) {
            return makeMovementFlags(0, 0);
        }

        return makeMovementFlags(_dragFlags, 0);
    }

    @Override
    public boolean onMove(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target
    ) {
        if (viewHolder.getBindingAdapter() != _adapter
                || target.getBindingAdapter() != _adapter) {
            return false;
        }

        int firstPosition = viewHolder.getBindingAdapterPosition();
        int secondPosition = target.getBindingAdapterPosition();

        if (firstPosition == NO_POSITION || secondPosition == NO_POSITION) {
            return false;
        }

        if (_adapter.isPositionInFavoriteSection(secondPosition)) {
            return false;
        }

        _adapter.onItemMove(firstPosition, secondPosition);
        _positionChanged = true;
        return true;
    }

    @Override
    public void onSwiped(
            @NonNull RecyclerView.ViewHolder viewHolder,
            int direction
    ) {
        if (viewHolder.getBindingAdapter() != _adapter) {
            return;
        }

        int position = viewHolder.getBindingAdapterPosition();
        if (position != NO_POSITION) {
            _adapter.onItemDismiss(position);
        }
    }

    @Override
    public void clearView(
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder
    ) {
        super.clearView(recyclerView, viewHolder);

        if (_positionChanged && viewHolder.getBindingAdapter() == _adapter) {
            int position = viewHolder.getBindingAdapterPosition();
            if (position != NO_POSITION) {
                _adapter.onItemDrop(position);
            }

            _positionChanged = false;
            _adapter.refresh(false);
        }
    }
}
