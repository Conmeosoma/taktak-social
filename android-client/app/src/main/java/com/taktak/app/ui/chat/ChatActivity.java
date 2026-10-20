package com.taktak.app.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.taktak.app.R;
import com.taktak.app.data.model.ChatModels;
import com.taktak.app.databinding.ActivityChatBinding;
import com.taktak.app.databinding.ItemChatMessageBinding;
import com.taktak.app.ui.history.HistoryTimeFormatter;
import com.taktak.app.viewmodel.ChatViewModel;
import com.taktak.app.viewmodel.HistoryViewModel;
import java.util.List;
import java.util.Objects;

public final class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_CONVERSATION_ID="conversationId";
    private ActivityChatBinding binding;
    private ChatViewModel model;
    private final MessagesAdapter adapter=new MessagesAdapter();
    private boolean foreground,awaitingAttachments,changingDraft;
    private int sentRevision,renderedCount;
    private androidx.appcompat.app.AlertDialog attachmentDialog;
    public static Intent intent(Context context,String id) {
        return new Intent(context,ChatActivity.class).putExtra(EXTRA_CONVERSATION_ID,id);
    }
    @Override protected void onCreate(Bundle saved) {
        ChatUi.theme(this);super.onCreate(saved);
        binding=ActivityChatBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        ChatUi.insets(this,binding.getRoot(),binding.chatContent);
        model=new ViewModelProvider(this).get(ChatViewModel.class);
        var layout=new LinearLayoutManager(this);layout.setStackFromEnd(true);
        binding.chatList.setLayoutManager(layout);binding.chatList.setAdapter(adapter);binding.chatList.setItemAnimator(null);
        binding.chatList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(@NonNull RecyclerView list,int state) {
                if(state==RecyclerView.SCROLL_STATE_IDLE)markVisibleRead();
            }
        });
        binding.chatBack.setOnClickListener(v->finish());
        binding.chatRefresh.setOnClickListener(v->model.refreshMessages());
        binding.chatMore.setOnClickListener(v->model.moreMessages());
        binding.chatRetry.setOnClickListener(v->model.retrySend());
        binding.chatEditRetry.setOnClickListener(v->new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.chat_discard_pending_warning)
                .setNegativeButton(android.R.string.cancel,null)
                .setPositiveButton(android.R.string.ok,(dialog,which)->model.discardPendingSend()).show());
        binding.chatSend.setOnClickListener(v->model.sendText(binding.chatInput.getText()==null?"":binding.chatInput.getText().toString()));
        binding.chatAttach.setOnClickListener(v->{awaitingAttachments=true;model.loadAttachableMoments();});
        binding.chatInput.setText(model.draft());
        binding.chatInput.addTextChangedListener(ChatUi.watch(()->{
            if(!changingDraft)model.setDraft(binding.chatInput.getText()==null?"":binding.chatInput.getText().toString());
        }));
        model.state().observe(this,this::render);
        model.openConversation(getIntent().getStringExtra(EXTRA_CONVERSATION_ID));
    }
    private void render(ChatViewModel.State state) {
        if(state==null || binding==null) return;
        if(state.expired) { adapter.submit(List.of());binding.chatInput.setText("");finish();return; }
        binding.chatTitle.setText(state.conversation!=null?state.conversation.peer.displayName:getString(R.string.chat_title));
        binding.chatProgress.setVisibility(state.busy?View.VISIBLE:View.INVISIBLE);
        binding.chatStatus.setVisibility(state.message==0?View.GONE:View.VISIBLE);
        if(state.message!=0)binding.chatStatus.setText(state.message);
        binding.chatRetry.setVisibility(state.hasPendingSend && !state.busy?View.VISIBLE:View.GONE);
        binding.chatEditRetry.setVisibility(state.hasPendingSend && !state.busy?View.VISIBLE:View.GONE);
        binding.chatRefresh.setEnabled(!state.busy);
        binding.chatMore.setVisibility(state.hasNext?View.VISIBLE:View.GONE);binding.chatMore.setEnabled(!state.busy);
        binding.chatSend.setEnabled(model.canSend());binding.chatAttach.setEnabled(model.canSend());
        binding.chatInput.setEnabled(!state.hasPendingSend && state.conversation!=null);
        if(state.sentRevision!=sentRevision) {
            sentRevision=state.sentRevision;changingDraft=true;binding.chatInput.setText(model.draft());changingDraft=false;
        }
        String oldLast=adapter.items.isEmpty()?null:adapter.items.get(adapter.items.size()-1).id;
        String newLast=state.messages.isEmpty()?null:state.messages.get(state.messages.size()-1).id;
        adapter.submit(state.messages);
        if(!Objects.equals(oldLast,newLast))binding.chatList.scrollToPosition(Math.max(0,state.messages.size()-1));
        renderedCount=state.messages.size();
        if(foreground && !state.messages.isEmpty() && !state.busy)binding.chatList.post(this::markVisibleRead);
        if(awaitingAttachments && !state.busy) {
            awaitingAttachments=false;
            if(state.message==0) showAttachments(state);
        }
    }
    private void markVisibleRead() {
        if(!foreground || binding==null || renderedCount==0)return;
        var layout=(LinearLayoutManager)binding.chatList.getLayoutManager();
        if(layout!=null && layout.findLastVisibleItemPosition()==renderedCount-1)model.markRead();
    }
    private void showAttachments(ChatViewModel.State state) {
        if(!foreground || isFinishing())return;
        if(state.attachments.isEmpty()) {
            attachmentDialog=new MaterialAlertDialogBuilder(this).setMessage(R.string.chat_no_moments)
                    .setPositiveButton(android.R.string.ok,null).show();return;
        }
        var items=List.copyOf(state.attachments);
        String[] labels=items.stream().map(m->m.sender.displayName+" · "+HistoryTimeFormatter.format(this,m.createdAt)
                +(m.caption==null||m.caption.isEmpty()?"":"\n"+m.caption)).toArray(String[]::new);
        attachmentDialog=new MaterialAlertDialogBuilder(this).setTitle(R.string.chat_choose_moment)
                .setItems(labels,(dialog,index)->model.sendMoment(items.get(index).id))
                .setNegativeButton(android.R.string.cancel,null).show();
    }
    @Override protected void onStart() { super.onStart();foreground=true;model.startSync(); }
    @Override protected void onResume() { super.onResume();if(binding!=null)adapter.notifyDataSetChanged(); }
    @Override protected void onStop() {
        foreground=false;model.stopSync();adapter.hideImages();
        if(attachmentDialog!=null){attachmentDialog.dismiss();attachmentDialog=null;}
        super.onStop();
    }
    @Override protected void onDestroy() { binding.chatList.setAdapter(null);binding=null;super.onDestroy(); }
    private final class MessagesAdapter extends RecyclerView.Adapter<MessageHolder> {
        private List<ChatModels.Message> items=List.of();
        void submit(List<ChatModels.Message> value) { items=List.copyOf(value);notifyDataSetChanged(); }
        void hideImages() {
            if(binding==null)return;
            for(int index=0;index<binding.chatList.getChildCount();index++) {
                var holder=binding.chatList.getChildViewHolder(binding.chatList.getChildAt(index));
                if(holder instanceof MessageHolder message) {message.ticket=null;message.binding.messageImage.setImageDrawable(null);}
            }
        }
        public int getItemCount() { return items.size(); }
        @NonNull public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent,int type) {
            return new MessageHolder(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
        }
        public void onBindViewHolder(@NonNull MessageHolder holder,int position) {
            var item=items.get(position);var b=holder.binding;
            boolean own=Objects.equals(model.currentUserId(),item.senderId);
            b.getRoot().setGravity(own?Gravity.END:Gravity.START);
            var params=(LinearLayout.LayoutParams)b.messageBubble.getLayoutParams();params.gravity=own?Gravity.END:Gravity.START;
            b.messageBubble.setLayoutParams(params);
            float density=getResources().getDisplayMetrics().density;
            int available=binding.chatList.getWidth();
            if(available<=0)available=getResources().getDisplayMetrics().widthPixels-(int)(32*density);
            b.messageText.setMaxWidth(Math.max((int)(80*density),Math.min((int)(260*density),available-(int)(48*density))));
            b.messageBubble.setCardBackgroundColor(MaterialColors.getColor(b.getRoot(),own?
                    com.google.android.material.R.attr.colorPrimaryContainer:com.google.android.material.R.attr.colorSurfaceContainerHigh));
            b.messageText.setTextColor(MaterialColors.getColor(b.getRoot(),own?
                    com.google.android.material.R.attr.colorOnPrimaryContainer:com.google.android.material.R.attr.colorOnSurface));
            boolean reply="MOMENT_REPLY".equals(item.messageType);
            boolean moment="MOMENT".equals(item.messageType) || reply;
            b.messageReference.setVisibility(reply?View.VISIBLE:View.GONE);
            b.messageReference.setText(item.momentId==null?R.string.chat_moment_unavailable:R.string.moment_reply_reference);
            b.messageReference.setTextColor(b.messageText.getCurrentTextColor());
            b.messageImage.setImageDrawable(null);b.messageImage.setVisibility(moment && item.momentId!=null?View.VISIBLE:View.GONE);
            b.messageText.setText(moment && !reply?(item.momentId==null?getString(R.string.chat_moment_unavailable):
                    item.content==null||item.content.isBlank()?getString(R.string.chat_moment):item.content):item.content);
            String receipt=HistoryTimeFormatter.format(ChatActivity.this,item.createdAt);
            int lastRead=-1,lastOwn=-1;
            for(int index=0;index<items.size();index++)if(Objects.equals(model.currentUserId(),items.get(index).senderId)) {
                lastOwn=index;if(items.get(index).readAt!=null)lastRead=index;
            }
            if(own && position==lastRead)receipt+=" · "+getString(R.string.chat_seen);
            else if(own && position==lastOwn && item.readAt==null)receipt+=" · "+getString(R.string.chat_sent);
            b.messageReceipt.setText(receipt);
            Object ticket=new Object();holder.ticket=ticket;
            b.getRoot().setOnClickListener(null);
            if(moment && item.momentId!=null) {
                b.getRoot().setOnClickListener(v->loadImage(holder,item.momentId,ticket,reply));
                loadImage(holder,item.momentId,ticket,reply);
            }
        }
        private void loadImage(MessageHolder holder,String id,Object ticket,boolean reply) {
            if(!foreground)return;
            model.loadImage(id,new HistoryViewModel.ImageCallback() {
                public void onLoaded(android.graphics.Bitmap bitmap) {
                    if(foreground && holder.ticket==ticket){
                        holder.binding.messageImage.setVisibility(View.VISIBLE);holder.binding.messageImage.setImageBitmap(bitmap);
                        if(reply)holder.binding.messageReference.setText(R.string.moment_reply_reference);
                    }
                }
                public void onError() {
                    if(foreground && holder.ticket==ticket){holder.binding.messageImage.setImageDrawable(null);
                        holder.binding.messageImage.setVisibility(View.GONE);
                        if(reply)holder.binding.messageReference.setText(R.string.chat_moment_unavailable);
                        else holder.binding.messageText.setText(R.string.chat_moment_unavailable);}
                }
            });
        }
        public void onViewRecycled(@NonNull MessageHolder holder) { holder.ticket=null;holder.binding.messageImage.setImageDrawable(null); }
    }
    private static final class MessageHolder extends RecyclerView.ViewHolder {
        final ItemChatMessageBinding binding;Object ticket;
        MessageHolder(ItemChatMessageBinding binding) { super(binding.getRoot());this.binding=binding; }
    }
}
