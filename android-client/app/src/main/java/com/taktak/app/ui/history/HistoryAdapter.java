package com.taktak.app.ui.history;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.RequestManager;
import com.taktak.app.R;
import com.taktak.app.data.model.MomentModels.MomentItem;
import com.taktak.app.databinding.ItemHistoryFooterBinding;
import com.taktak.app.databinding.ItemHistoryMomentBinding;
import com.taktak.app.viewmodel.HistoryViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnMoreClickListener {
        void onMore();
    }

    private static final int TYPE_MOMENT = 0;
    private static final int TYPE_FOOTER = 1;

    private final RequestManager glide;
    private final HistoryViewModel viewModel;
    private final OnMoreClickListener moreListener;
    private final List<MomentItem> items = new ArrayList<>();
    private boolean hasNext = false;
    private boolean busy = false;

    public HistoryAdapter(RequestManager glide, HistoryViewModel viewModel, OnMoreClickListener moreListener) {
        this.glide = glide;
        this.viewModel = viewModel;
        this.moreListener = moreListener;
    }

    public void submit(List<MomentItem> newItems, boolean hasNext, boolean busy) {
        var oldItems = new ArrayList<>(this.items);
        boolean oldHasNext = this.hasNext;
        this.hasNext = hasNext;
        this.busy = busy;
        this.items.clear();
        this.items.addAll(newItems);

        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return oldItems.size() + (oldHasNext ? 1 : 0); }
            @Override public int getNewListSize() { return items.size() + (hasNext ? 1 : 0); }
            @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                boolean oldIsFooter = oldPos >= oldItems.size();
                boolean newIsFooter = newPos >= items.size();
                if (oldIsFooter && newIsFooter) { return true; }
                if (oldIsFooter || newIsFooter) { return false; }
                return oldItems.get(oldPos).id.equals(items.get(newPos).id);
            }
            @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                boolean oldIsFooter = oldPos >= oldItems.size();
                boolean newIsFooter = newPos >= items.size();
                if (oldIsFooter && newIsFooter) { return false; }
                if (oldIsFooter || newIsFooter) { return false; }
                var oldItem = oldItems.get(oldPos);
                var newItem = items.get(newPos);
                return oldItem.id.equals(newItem.id)
                        && Objects.equals(oldItem.caption, newItem.caption)
                        && Objects.equals(oldItem.createdAt, newItem.createdAt);
            }
        }).dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return items.size() + (hasNext ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        return position >= items.size() ? TYPE_FOOTER : TYPE_MOMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOOTER) {
            return new FooterHolder(ItemHistoryFooterBinding.inflate(inflater, parent, false));
        }
        return new MomentHolder(ItemHistoryMomentBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FooterHolder footer) {
            footer.binding.historyMoreButton.setEnabled(!busy);
            footer.binding.historyMoreProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
            footer.binding.historyMoreButton.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
            footer.binding.historyMoreButton.setOnClickListener(v -> {
                if (moreListener != null) { moreListener.onMore(); }
            });
            return;
        }

        MomentItem item = items.get(position);
        MomentHolder moment = (MomentHolder) holder;
        Context ctx = moment.itemView.getContext();

        moment.binding.momentSenderName.setText(item.sender.displayName);
        moment.binding.momentSenderUsername.setText(ctx.getString(R.string.friends_username, item.sender.username));
        moment.binding.momentTimestamp.setText(HistoryTimeFormatter.format(ctx, item.createdAt));

        glide.load(R.drawable.friend_avatar)
                .circleCrop()
                .into(moment.binding.momentSenderAvatar);

        if (item.caption != null && !item.caption.trim().isEmpty()) {
            moment.binding.momentCaption.setVisibility(View.VISIBLE);
            moment.binding.momentCaption.setText(item.caption);
        } else {
            moment.binding.momentCaption.setVisibility(View.GONE);
        }

        moment.currentMomentId = item.id;
        moment.binding.momentImageProgress.setVisibility(View.VISIBLE);
        moment.binding.momentImageError.setVisibility(View.GONE);
        moment.binding.momentImage.setImageDrawable(null);

        viewModel.loadImage(item.id, new HistoryViewModel.ImageCallback() {
            @Override
            public void onLoaded(Bitmap bitmap) {
                if (item.id.equals(moment.currentMomentId)) {
                    moment.binding.momentImageProgress.setVisibility(View.GONE);
                    moment.binding.momentImageError.setVisibility(View.GONE);
                    moment.binding.momentImage.setImageBitmap(bitmap);
                }
            }

            @Override
            public void onError() {
                if (item.id.equals(moment.currentMomentId)) {
                    moment.binding.momentImageProgress.setVisibility(View.GONE);
                    moment.binding.momentImageError.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof MomentHolder moment) {
            moment.currentMomentId = null;
            moment.binding.momentImage.setImageDrawable(null);
            glide.clear(moment.binding.momentSenderAvatar);
        }
        super.onViewRecycled(holder);
    }

    static final class MomentHolder extends RecyclerView.ViewHolder {
        final ItemHistoryMomentBinding binding;
        String currentMomentId;
        MomentHolder(ItemHistoryMomentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static final class FooterHolder extends RecyclerView.ViewHolder {
        final ItemHistoryFooterBinding binding;
        FooterHolder(ItemHistoryFooterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
