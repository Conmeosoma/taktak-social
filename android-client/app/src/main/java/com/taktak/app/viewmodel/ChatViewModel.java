package com.taktak.app.viewmodel;

import android.app.Application;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.ChatModels.*;
import com.taktak.app.data.model.FriendModels;
import com.taktak.app.data.model.MomentModels.MomentItem;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.ChatRepository;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChatViewModel extends AndroidViewModel {
    private static final String PROCESS = UUID.randomUUID().toString();
    public static final class State {
        public final List<Conversation> conversations;
        public final List<Message> messages;
        public final List<FriendModels.User> friends;
        public final List<MomentItem> attachments;
        public final Conversation conversation;
        public final boolean busy, hasNext, expired, loaded, hasPendingSend;
        public final int message, sentRevision;
        public final String openedId;
        public final long totalUnreadCount;
        private State(ChatViewModel model) {
            conversations = List.copyOf(model.conversations); messages = List.copyOf(model.messages);
            friends = List.copyOf(model.friends); attachments = List.copyOf(model.attachments);
            conversation = model.conversation; busy = model.busy; hasNext = model.hasNext;
            expired = model.expired; loaded = model.loaded; hasPendingSend = model.pending != null;
            message = model.error; sentRevision = model.sentRevision; openedId = model.openedId;
            totalUnreadCount = model.totalUnreadCount;
        }
    }
    private final SavedStateHandle saved;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final List<Conversation> conversations = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();
    private final List<FriendModels.User> friends = new ArrayList<>();
    private final List<MomentItem> attachments = new ArrayList<>();
    private ChatRepository repository;
    private Conversation conversation;
    private Send pending;
    private String conversationId, openedId, selfId;
    private int inboxPage, messagesPage, friendsPage, error, sentRevision;
    private long totalUnreadCount;
    private boolean busy, hasNext, expired, loaded, syncing, rerun;
    private volatile boolean cleared;

    public ChatViewModel(Application app, SavedStateHandle handle) {
        super(app); saved = handle;
        try {
            long generation = AuthGraph.repository(app).sessionGeneration();
            Long previousGeneration = saved.get("chat.ownerGeneration");
            if (!PROCESS.equals(saved.get("chat.ownerProcess")) || previousGeneration == null || previousGeneration != generation) {
                for (String name : new String[]{"chat.key", "chat.type", "chat.text", "chat.moment", "chat.draft", "chat.conversation"}) saved.remove(name);
            }
            saved.set("chat.ownerGeneration", generation); saved.set("chat.ownerProcess", PROCESS);
        } catch (IOException failure) {
            for (String name : new String[]{"chat.key", "chat.type", "chat.text", "chat.moment", "chat.draft", "chat.conversation"}) saved.remove(name);
        }
        conversationId = saved.get("chat.conversation");
        String key = saved.get("chat.key");
        if (key != null) pending = new Send(key, saved.get("chat.type"), saved.get("chat.text"), saved.get("chat.moment"));
        PushEventBus.register(listener); publish();
    }
    public LiveData<State> state() { return state; }
    public String currentUserId() { return selfId; }
    public String draft() { String value = saved.get("chat.draft"); return value == null ? "" : value; }
    public void setDraft(String value) { saved.set("chat.draft", value); }
    public boolean canSend() { return !busy && !expired && conversation != null && pending == null; }
    public void refreshInbox() {
        if (conversationId != null) return;
        run(repo -> {
            var page = repo.inbox(0);
            return () -> { conversations.clear(); conversations.addAll(page.items); inboxPage = 0; hasNext = page.hasNext;
                totalUnreadCount = page.totalUnreadCount; loaded = true; };
        });
    }
    public void moreInbox() {
        if (!hasNext || conversationId != null) return;
        int next = inboxPage + 1;
        run(repo -> { var page = repo.inbox(next); return () -> {
            Set<String> ids = new HashSet<>(); for (var item : conversations) ids.add(item.id);
            for (var item : page.items) if (ids.add(item.id)) conversations.add(item);
            inboxPage = next; hasNext = page.hasNext;
        }; });
    }
    public void loadFriends() { friendsPage = 0; loadFriendsPage(0); }
    public void moreFriends() { if (hasNext) loadFriendsPage(friendsPage + 1); }
    private void loadFriendsPage(int number) {
        run(repo -> { var page = AuthGraph.repository(getApplication()).friends().friends(number); return () -> {
            if (number == 0) friends.clear();
            Set<String> ids = new HashSet<>(); for (var item : friends) ids.add(item.id);
            for (var item : page.items) if (ids.add(item.id)) friends.add(item);
            friendsPage = number; hasNext = page.hasNext; loaded = true;
        }; });
    }
    public void openFriend(String friendId) {
        run(repo -> { Conversation found = repo.withFriend(friendId); return () -> openedId = found.id; });
    }
    public void openConversation(String id) {
        if (!FriendModels.uuid(id)) { error = R.string.chat_error; publish(); return; }
        if (conversationId != null && !conversationId.equals(id)) { pending = null; savePending(); messages.clear(); }
        conversationId = id; saved.set("chat.conversation", id); refreshMessages();
    }
    public void refreshMessages() {
        if (conversationId == null) return;
        run(repo -> {
            var found = repo.conversation(conversationId); var page = repo.messages(conversationId, 0);
            return () -> {
                conversation = found;
                if (messagesPage == 0) { messages.clear(); hasNext = page.hasNext; }
                else {
                    Set<String> fresh = new HashSet<>(); for (Message item : page.items) fresh.add(item.id);
                    messages.removeIf(item -> fresh.contains(item.id));
                }
                messages.addAll(page.items); sortMessages(); loaded = true;
            };
        });
    }
    public void moreMessages() {
        if (conversationId == null || !hasNext) return;
        int next = messagesPage + 1;
        run(repo -> { var page = repo.messages(conversationId, next); return () -> {
            Set<String> ids = new HashSet<>(); for (Message item : messages) ids.add(item.id);
            for (Message item : page.items) if (ids.add(item.id)) messages.add(item);
            sortMessages(); messagesPage = next; hasNext = page.hasNext;
        }; });
    }
    private void sortMessages() {
        messages.sort(Comparator.comparing((Message item) -> java.time.Instant.parse(item.createdAt)).thenComparing(item -> item.id));
    }
    public void sendText(String text) {
        if (!canSend()) return;
        if (!ChatRepository.validText(text)) { error = R.string.chat_invalid_text; publish(); return; }
        pending = new Send(UUID.randomUUID().toString(), "TEXT", text, null); savePending(); retrySend();
    }
    public void sendMoment(String momentId) {
        if (!canSend() || !FriendModels.uuid(momentId)) return;
        pending = new Send(UUID.randomUUID().toString(), "MOMENT", null, momentId); savePending(); retrySend();
    }
    public void retrySend() {
        if (pending == null || conversationId == null) return;
        Send frozen = pending;
        run(repo -> { Message sent = repo.send(conversationId, frozen); return () -> {
            messages.removeIf(item -> item.id.equals(sent.id)); messages.add(sent); sortMessages();
            if (Objects.equals(draft(), frozen.content)) setDraft("");
            pending = null; savePending(); sentRevision++; loaded = true;
            PushEventBus.emitChatChanged(conversationId, sent.id);
        }; });
    }
    public void discardPendingSend() {
        if (busy) return;
        pending = null; savePending(); error = 0; publish();
    }
    private void savePending() {
        saved.set("chat.key", pending == null ? null : pending.clientMessageId);
        saved.set("chat.type", pending == null ? null : pending.messageType);
        saved.set("chat.text", pending == null ? null : pending.content);
        saved.set("chat.moment", pending == null ? null : pending.momentId);
    }
    public void markRead() {
        if (busy || conversationId == null || selfId == null) return;
        String anchor = null;
        for (Message item : messages) if (selfId.equals(item.recipientId) && item.readAt == null) anchor = item.id;
        if (anchor == null) return;
        String last = anchor;
        run(repo -> { repo.read(conversationId, last); return () -> {
            boolean reached = false;
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message item = messages.get(i); if (item.id.equals(last)) reached = true;
                if (reached && selfId.equals(item.recipientId) && item.readAt == null) item.readAt = java.time.Instant.now().toString();
            }
            PushEventBus.emitChatChanged(conversationId, last);
        }; });
    }
    public void loadAttachableMoments() {
        run(repo -> {
            var media = AuthGraph.repository(getApplication()).moments();
            var sent = media.sent(0); var received = media.received(0);
            return () -> { attachments.clear(); attachments.addAll(sent.items);
                Set<String> ids = new HashSet<>(); for (var item : attachments) ids.add(item.id);
                for (var item : received.items) if (ids.add(item.id)) attachments.add(item);
            };
        });
    }
    public void loadImage(String id, HistoryViewModel.ImageCallback callback) {
        worker.execute(() -> {
            try {
                ChatRepository owner = repository;
                if (owner == null) throw new ApiFailure(ApiFailure.Kind.UNAUTHORIZED);
                owner.requireCurrent();
                byte[] bytes = AuthGraph.repository(getApplication()).moments().download(id);
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) throw new IOException("Invalid image");
                main.post(() -> {
                    if (!cleared && !expired && owner.publishIfCurrent(() -> callback.onLoaded(bitmap))) return;
                    callback.onError();
                });
            } catch (IOException | RuntimeException failure) { main.post(callback::onError); }
        });
    }
    public void startSync() {
        if (syncing) return;
        syncing = true; poll.run();
    }
    public void stopSync() { syncing = false; main.removeCallbacks(poll); }
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!syncing || cleared || expired) return;
            if (!busy) { if (conversationId == null) refreshInbox(); else refreshMessages(); }
            main.postDelayed(this, 5000);
        }
    };
    private final PushEventBus.Listener listener = new PushEventBus.Listener() {
        @Override public void onMomentCreated(String id) { }
        @Override public void onMomentDeleted(String id) {
            main.post(() -> {
                if (cleared) return;
                for (Message item : messages) if (Objects.equals(item.momentId, id)) { item.momentId = null; item.content = null; }
                publish();
            });
            changed();
        }
        @Override public void onChatChanged(String id, String messageId) {
            if (conversationId == null || conversationId.equals(id)) changed();
        }
        @Override public void onSessionCleared() { main.post(() -> {
            if (repository == null || !repository.publishIfCurrent(() -> { })) { expired = true; wipe(); publish(); }
        }); }
        private void changed() { main.post(() -> {
            if (!syncing || cleared || expired) return;
            if (busy) rerun = true;
            else if (conversationId == null) refreshInbox(); else refreshMessages();
        }); }
    };
    private interface Work { Runnable execute(ChatRepository repository) throws IOException; }
    private void run(Work action) {
        if (busy || expired || cleared) return;
        busy = true; error = 0; publish();
        worker.execute(() -> {
            Runnable result; ChatRepository owner = null;
            try {
                var auth = AuthGraph.repository(getApplication());
                if (repository == null) repository = auth.chat();
                owner = repository; owner.requireCurrent();
                if (selfId == null) selfId = auth.profile().id;
                result = action.execute(owner);
            } catch (IOException | RuntimeException failure) { result = () -> failed(failure); }
            Runnable complete = result; ChatRepository producer = owner;
            main.post(() -> {
                if (cleared) return;
                Runnable deliver = () -> { complete.run(); busy = false; publish();
                    if (rerun && syncing && !expired) { rerun = false; main.post(() -> {
                        if (conversationId == null) refreshInbox(); else refreshMessages();
                    }); }
                };
                if (producer == null) deliver.run();
                else if (!producer.publishIfCurrent(deliver)) { expired = true; wipe(); busy = false; error = R.string.chat_session_expired; publish(); }
            });
        });
    }
    private void failed(Exception failure) {
        error = failure instanceof IOException ? R.string.chat_network_error : R.string.chat_error;
        if (failure instanceof ApiFailure api) {
            error = R.string.chat_error;
            if (api.kind == ApiFailure.Kind.UNAUTHORIZED || api.kind == ApiFailure.Kind.CANCELLED) {
                expired = true; wipe(); error = R.string.chat_session_expired;
            } else if (api.status == 403 || api.status == 404) {
                conversation = null; messages.clear(); attachments.clear(); error = R.string.chat_forbidden;
            } else if (api.kind == ApiFailure.Kind.VALIDATION) error = R.string.chat_invalid_text;
        }
    }
    private void wipe() {
        conversations.clear(); messages.clear(); friends.clear(); attachments.clear(); conversation = null;
        pending = null; savePending(); setDraft(""); selfId = null; totalUnreadCount = 0; stopSync();
    }
    private void publish() { state.setValue(new State(this)); }
    @Override protected void onCleared() {
        cleared = true; stopSync(); main.removeCallbacksAndMessages(null); PushEventBus.unregister(listener); worker.shutdownNow();
    }
}
