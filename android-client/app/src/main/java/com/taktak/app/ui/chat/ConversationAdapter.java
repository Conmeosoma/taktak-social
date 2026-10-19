package com.taktak.app.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.taktak.app.R;
import com.taktak.app.data.model.ChatModels;
import com.taktak.app.databinding.ItemConversationBinding;
import com.taktak.app.ui.history.HistoryTimeFormatter;
import com.taktak.app.ui.profile.AvatarLoader;
import java.util.List;
import java.util.function.Consumer;

final class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.Holder> {
    private List<ChatModels.Conversation> items=List.of();
    private final AvatarLoader avatars;
    private final Consumer<ChatModels.Conversation> click;
    ConversationAdapter(AvatarLoader avatars,Consumer<ChatModels.Conversation> click) { this.avatars=avatars;this.click=click; }
    void submit(List<ChatModels.Conversation> values) { items=List.copyOf(values);notifyDataSetChanged(); }
    @NonNull public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type) {
        return new Holder(ItemConversationBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
    }
    public void onBindViewHolder(@NonNull Holder holder,int position) {
        var item=items.get(position);var b=holder.binding;
        b.conversationName.setText(item.peer.displayName);
        b.conversationSnippet.setText(item.lastMessage==null?b.getRoot().getContext().getString(R.string.chat_start):
                "MOMENT".equals(item.lastMessage.messageType)?b.getRoot().getContext().getString(R.string.chat_moment):item.lastMessage.content);
        b.conversationTime.setText(HistoryTimeFormatter.format(b.getRoot().getContext(),item.updatedAt));
        b.conversationUnread.setVisibility(item.unreadCount>0?View.VISIBLE:View.GONE);
        b.conversationUnread.setText(item.unreadCount>99?"99+":String.valueOf(item.unreadCount));
        b.conversationUnread.setContentDescription(b.getRoot().getContext().getString(R.string.chat_unread,item.unreadCount));
        avatars.load(item.peer.id,b.conversationAvatar);
        b.getRoot().setOnClickListener(v->click.accept(item));
    }
    public void onViewRecycled(@NonNull Holder holder) { avatars.clear(holder.binding.conversationAvatar); }
    public int getItemCount() { return items.size(); }
    static final class Holder extends RecyclerView.ViewHolder {
        final ItemConversationBinding binding;
        Holder(ItemConversationBinding binding) { super(binding.getRoot());this.binding=binding; }
    }
}
