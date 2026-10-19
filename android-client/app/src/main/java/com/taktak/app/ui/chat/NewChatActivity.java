package com.taktak.app.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.taktak.app.R;
import com.taktak.app.data.model.FriendModels;
import com.taktak.app.databinding.ActivityNewChatBinding;
import com.taktak.app.databinding.ItemConversationBinding;
import com.taktak.app.ui.profile.AvatarLoader;
import com.taktak.app.viewmodel.ChatViewModel;
import java.util.List;

public final class NewChatActivity extends AppCompatActivity {
    private ActivityNewChatBinding binding;
    private ChatViewModel model;
    private AvatarLoader avatars;
    private final FriendsAdapter adapter=new FriendsAdapter();
    private boolean navigated;
    protected void onCreate(Bundle saved) {
        ChatUi.theme(this);super.onCreate(saved);
        binding=ActivityNewChatBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        ChatUi.insets(this,binding.getRoot(),binding.newChatContent);
        model=new ViewModelProvider(this).get(ChatViewModel.class);
        avatars=new AvatarLoader(Glide.with(this));
        binding.newChatList.setLayoutManager(new LinearLayoutManager(this));binding.newChatList.setAdapter(adapter);
        binding.newChatBack.setOnClickListener(v->finish());
        binding.newChatRefresh.setOnClickListener(v->model.loadFriends());
        binding.newChatMore.setOnClickListener(v->model.moreFriends());
        binding.newChatSearch.addTextChangedListener(ChatUi.watch(()->render(model.state().getValue())));
        model.state().observe(this,this::render);model.loadFriends();
    }
    private void render(ChatViewModel.State state) {
        if(state==null) return;
        if(state.expired) { finish();return; }
        if(state.openedId!=null && !navigated) { navigated=true;startActivity(ChatActivity.intent(this,state.openedId));finish();return; }
        String query=ChatUi.query(binding.newChatSearch.getText());
        adapter.items=state.friends.stream().filter(f->ChatUi.matches(query,f.displayName,f.username))
                .collect(java.util.stream.Collectors.toList());adapter.notifyDataSetChanged();
        binding.newChatProgress.setVisibility(state.busy?View.VISIBLE:View.INVISIBLE);
        binding.newChatStatus.setText(state.message!=0?state.message:adapter.items.isEmpty()?R.string.chat_no_results:R.string.chat_private);
        binding.newChatRefresh.setEnabled(!state.busy);binding.newChatMore.setEnabled(!state.busy);
        binding.newChatMore.setVisibility(state.hasNext?View.VISIBLE:View.GONE);
    }
    protected void onDestroy() { if(avatars!=null) avatars.close();binding.newChatList.setAdapter(null);super.onDestroy(); }
    private final class FriendsAdapter extends RecyclerView.Adapter<ConversationAdapter.Holder> {
        private List<FriendModels.User> items=List.of();
        public int getItemCount() { return items.size(); }
        @NonNull public ConversationAdapter.Holder onCreateViewHolder(@NonNull ViewGroup parent,int type) {
            return new ConversationAdapter.Holder(ItemConversationBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
        }
        public void onBindViewHolder(@NonNull ConversationAdapter.Holder holder,int position) {
            var friend=items.get(position);var b=holder.binding;b.conversationName.setText(friend.displayName);
            b.conversationSnippet.setText(getString(R.string.friends_username,friend.username));
            b.conversationTime.setVisibility(View.GONE);b.conversationUnread.setVisibility(View.GONE);
            avatars.load(friend.id,b.conversationAvatar);
            b.getRoot().setOnClickListener(v->{ var state=model.state().getValue();if(state!=null&&!state.busy)model.openFriend(friend.id); });
        }
        public void onViewRecycled(@NonNull ConversationAdapter.Holder holder) { avatars.clear(holder.binding.conversationAvatar); }
    }
}
