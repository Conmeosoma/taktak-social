package com.taktak.app.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import com.taktak.app.MainActivity;
import com.taktak.app.R;
import com.taktak.app.databinding.FragmentMessagesBinding;
import com.taktak.app.ui.profile.AvatarLoader;
import com.taktak.app.viewmodel.ChatViewModel;
import com.taktak.app.viewmodel.SessionViewModel;

public final class MessagesFragment extends Fragment {
    private FragmentMessagesBinding binding;
    private ChatViewModel model;
    private AvatarLoader avatars;
    private ConversationAdapter adapter;
    public View onCreateView(@NonNull LayoutInflater inflater,ViewGroup parent,Bundle saved) {
        binding=FragmentMessagesBinding.inflate(inflater,parent,false);return binding.getRoot();
    }
    public void onViewCreated(@NonNull View view,Bundle saved) {
        model=new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
        avatars=new AvatarLoader(Glide.with(this));
        adapter=new ConversationAdapter(avatars,item->startActivity(ChatActivity.intent(requireContext(),item.id)));
        binding.messagesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.messagesList.setAdapter(adapter);
        binding.messagesList.setItemAnimator(null);
        ViewCompat.setAccessibilityHeading(binding.messagesTitle,true);
        binding.messagesNew.setOnClickListener(v->startActivity(new Intent(requireContext(),NewChatActivity.class)));
        binding.messagesFindFriends.setOnClickListener(v->((MainActivity)requireActivity()).openFriendsTab());
        binding.messagesRefresh.setOnClickListener(v->model.refreshInbox());
        binding.messagesNotifications.setOnClickListener(v->((MainActivity)requireActivity()).requestChatNotifications());
        binding.messagesMore.setOnClickListener(v->model.moreInbox());
        binding.messagesSearch.addTextChangedListener(ChatUi.watch(()->render(model.state().getValue())));
        model.state().observe(getViewLifecycleOwner(),this::render);
        model.refreshInbox();
    }
    private void render(ChatViewModel.State state) {
        if(binding==null || state==null) return;
        binding.messagesNotifications.setVisibility(android.os.Build.VERSION.SDK_INT >= 33
                && requireContext().checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED ? View.VISIBLE : View.GONE);
        if(state.expired) { new ViewModelProvider(requireActivity()).get(SessionViewModel.class).check(true);return; }
        String query=ChatUi.query(binding.messagesSearch.getText());
        var visible=state.conversations.stream().filter(c->ChatUi.matches(query,c.peer.displayName,c.peer.username))
                .collect(java.util.stream.Collectors.toList());
        adapter.submit(visible);
        boolean empty=state.loaded && state.conversations.isEmpty() && !state.busy && state.message==0;
        binding.messagesEmpty.setVisibility(empty?View.VISIBLE:View.GONE);
        binding.messagesList.setVisibility(empty?View.GONE:View.VISIBLE);
        binding.messagesProgress.setVisibility(state.busy?View.VISIBLE:View.INVISIBLE);
        binding.messagesRefresh.setEnabled(!state.busy);
        int message=state.message!=0?state.message:!empty && !query.isEmpty() && visible.isEmpty()?R.string.chat_no_results:0;
        binding.messagesStatus.setVisibility(message==0?View.GONE:View.VISIBLE);
        if(message!=0) binding.messagesStatus.setText(message);
        binding.messagesMore.setVisibility(state.hasNext?View.VISIBLE:View.GONE);
        binding.messagesMore.setEnabled(!state.busy);
    }
    public void onDestroyView() {
        if(binding!=null) binding.messagesList.setAdapter(null);
        if(avatars!=null) avatars.close();binding=null;adapter=null;super.onDestroyView();
    }
}
