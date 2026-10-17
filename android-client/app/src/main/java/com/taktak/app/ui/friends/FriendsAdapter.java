package com.taktak.app.ui.friends;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.core.view.ViewCompat;
import com.bumptech.glide.RequestManager;
import com.taktak.app.R;
import com.taktak.app.data.model.FriendModels.*;
import com.taktak.app.databinding.ItemFriendBinding;
import com.taktak.app.databinding.ItemFriendsSectionBinding;
import com.taktak.app.viewmodel.FriendsViewModel.State;
import java.util.*;

final class FriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface Actions {
        void send(String id);
        void respond(String id, boolean accept);
        void confirm(User user, boolean block);
        void more(boolean incoming);
    }
    private static final int RESULT = 0, REQUEST = 1, FRIEND = 2;
    private static final class Row {
        int title, message, role;
        User user;
        Invitation invitation;
        boolean more, incoming;
    }
    private final Actions actions;
    private final RequestManager images;
    private final List<Row> rows = new ArrayList<>();
    private State state;
    FriendsAdapter(RequestManager images, Actions actions) { this.images = images; this.actions = actions; }
    void submit(State next) {
        var previous = new ArrayList<>(rows);
        state = next; rows.clear();
        if (next.searched) {
            section(R.string.friends_results, next.results.isEmpty() ? R.string.friends_empty_results : 0);
            for (User user : next.results) { person(user, null, RESULT); }
        }
        section(R.string.friends_incoming, !next.loaded ? R.string.friends_loading
                : next.incoming.isEmpty() ? R.string.friends_empty_incoming : 0);
        for (Invitation invitation : next.incoming) { person(invitation.sender, invitation, REQUEST); }
        if (next.moreIncoming) { more(true); }
        section(R.string.friends_list, !next.loaded ? R.string.friends_loading
                : next.friends.isEmpty() ? R.string.friends_empty_list : 0);
        for (User user : next.friends) { person(user, null, FRIEND); }
        if (next.moreFriends) { more(false); }
        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            public int getOldListSize() { return previous.size(); }
            public int getNewListSize() { return rows.size(); }
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                Row old = previous.get(oldPosition), current = rows.get(newPosition);
                if (old.user != null || current.user != null) {
                    return old.user != null && current.user != null && old.role == current.role
                            && (old.invitation != null ? old.invitation.id.equals(current.invitation.id) : old.user.id.equals(current.user.id));
                }
                return old.title == current.title && old.more == current.more && old.incoming == current.incoming;
            }
            // Trạng thái busy/quan hệ dùng chung ảnh hưởng action của mọi hàng.
            public boolean areContentsTheSame(int oldPosition, int newPosition) { return false; }
        }).dispatchUpdatesTo(this);
    }
    private void section(int title, int message) { Row row = new Row(); row.title = title; row.message = message; rows.add(row); }
    private void person(User user, Invitation invitation, int role) {
        Row row = new Row(); row.user = user; row.invitation = invitation; row.role = role; rows.add(row);
    }
    private void more(boolean incoming) { Row row = new Row(); row.more = true; row.incoming = incoming; rows.add(row); }
    @Override public int getItemCount() { return rows.size(); }
    @Override public int getItemViewType(int position) { return rows.get(position).user == null ? 0 : 1; }
    private static final class PersonHolder extends RecyclerView.ViewHolder {
        final ItemFriendBinding binding;
        PersonHolder(ItemFriendBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
    private static final class SectionHolder extends RecyclerView.ViewHolder {
        final ItemFriendsSectionBinding binding;
        SectionHolder(ItemFriendsSectionBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        var inflater = LayoutInflater.from(parent.getContext());
        return type == 0 ? new SectionHolder(ItemFriendsSectionBinding.inflate(inflater, parent, false))
                : new PersonHolder(ItemFriendBinding.inflate(inflater, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof SectionHolder section) {
            var b = section.binding;
            ViewCompat.setAccessibilityHeading(b.sectionTitle, true);
            b.sectionTitle.setVisibility(row.title == 0 ? View.GONE : View.VISIBLE);
            if (row.title != 0) { b.sectionTitle.setText(row.title); }
            b.sectionMessage.setVisibility(row.message == 0 ? View.GONE : View.VISIBLE);
            if (row.message != 0) { b.sectionMessage.setText(row.message == R.string.friends_loading && !state.busy
                    ? R.string.friends_server_error : row.message); }
            b.sectionMore.setVisibility(row.more ? View.VISIBLE : View.GONE);
            b.sectionMore.setContentDescription(b.getRoot().getContext().getString(R.string.friends_more) + " — "
                    + b.getRoot().getContext().getString(row.incoming ? R.string.friends_incoming : R.string.friends_list));
            b.sectionMore.setEnabled(!state.busy);
            b.sectionMore.setOnClickListener(view -> actions.more(row.incoming));
            return;
        }
        var b = ((PersonHolder) holder).binding;
        b.friendName.setText(row.user.displayName);
        b.friendUsername.setText(b.getRoot().getContext().getString(R.string.friends_username, row.user.username));
        images.load(R.drawable.friend_avatar).error(R.drawable.friend_avatar).into(b.friendAvatar);
        boolean alreadyFriend = state.friends.stream().anyMatch(user -> user.id.equals(row.user.id));
        boolean incoming = state.incoming.stream().anyMatch(item -> item.sender.id.equals(row.user.id));
        boolean sent = state.sent.contains(row.user.id);
        b.friendPrimary.setVisibility(row.role == FRIEND ? View.GONE : View.VISIBLE);
        b.friendPrimary.setText(row.role == REQUEST ? R.string.friends_accept : alreadyFriend ? R.string.friends_list
                : incoming ? R.string.friends_incoming : sent ? R.string.friends_sent : R.string.friends_send);
        b.friendPrimary.setEnabled(!state.busy && (row.role == REQUEST || (!alreadyFriend && !incoming && !sent)));
        b.friendPrimary.setOnClickListener(view -> {
            if (row.role == REQUEST) { actions.respond(row.invitation.id, true); }
            else { actions.send(row.user.id); }
        });
        b.friendSecondary.setVisibility(row.role == REQUEST ? View.VISIBLE : View.GONE);
        b.friendSecondary.setEnabled(!state.busy);
        b.friendSecondary.setOnClickListener(view -> actions.respond(row.invitation.id, false));
        b.friendOptions.setEnabled(!state.busy);
        b.friendOptions.setContentDescription(b.getRoot().getContext().getString(R.string.friends_options, row.user.username));
        b.friendOptions.setOnClickListener(view -> {
            PopupMenu menu = new PopupMenu(view.getContext(), view);
            if (row.role == FRIEND || alreadyFriend) { menu.getMenu().add(0, 1, 0, R.string.friends_unfriend); }
            menu.getMenu().add(0, 2, 1, R.string.friends_block);
            menu.setOnMenuItemClickListener(item -> { actions.confirm(row.user, item.getItemId() == 2); return true; });
            menu.show();
        });
    }
    @Override public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof PersonHolder person) { images.clear(person.binding.friendAvatar); }
        super.onViewRecycled(holder);
    }
}
