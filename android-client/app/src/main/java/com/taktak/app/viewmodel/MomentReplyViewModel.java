package com.taktak.app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.ChatModels.Send;
import com.taktak.app.data.model.FriendModels;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.ChatRepository;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bản nháp riêng tư, retry cùng payload/key; không tự gửi lại sau recreation. */
public final class MomentReplyViewModel extends AndroidViewModel {
    private static final String PROCESS = UUID.randomUUID().toString();
    private static final String PREFIX = "momentReply.";
    private static final String[] PRIVATE_KEYS = {"draft", "key", "text", "moment", "sender", "room", "navigation", "revoked"};
    public static final class State {
        public final boolean busy, pending, revoked;
        public final int error;
        public final String conversationId;
        private State(MomentReplyViewModel model) {
            busy = model.busy; pending = model.pending != null; revoked = model.revoked;
            error = model.error; conversationId = model.navigation;
        }
    }
    private final SavedStateHandle saved;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final long generation;
    private ChatRepository repository;
    private Send pending;
    private String senderId, momentId, room, navigation;
    private boolean busy, revoked;
    private int error;
    private volatile int revision;
    private volatile boolean cleared;

    public MomentReplyViewModel(Application app, SavedStateHandle handle) {
        super(app); saved = handle;
        long owner = -1;
        try { owner = AuthGraph.repository(app).sessionGeneration(); }
        catch (IOException failure) { revoked = true; error = R.string.chat_session_expired; }
        generation = owner;
        Long previous = saved.get(PREFIX + "generation");
        if (owner < 0 || previous == null || previous != owner || !PROCESS.equals(saved.get(PREFIX + "process"))) clearSaved();
        saved.set(PREFIX + "generation", generation); saved.set(PREFIX + "process", PROCESS);
        if (Boolean.TRUE.equals(saved.get(PREFIX + "revoked"))) {
            revoked = true; error = R.string.chat_forbidden;
        }
        senderId = saved.get(PREFIX + "sender"); momentId = saved.get(PREFIX + "moment");
        room = saved.get(PREFIX + "room"); navigation = saved.get(PREFIX + "navigation");
        String key = saved.get(PREFIX + "key"), text = saved.get(PREFIX + "text");
        if (FriendModels.uuid(key) && FriendModels.uuid(senderId) && FriendModels.uuid(momentId) && ChatRepository.validText(text)) {
            pending = new Send(key, "MOMENT_REPLY", text, momentId);
        }
        PushEventBus.register(listener); publish();
    }
    public LiveData<State> state() { return state; }
    public String draft() { String value = saved.get(PREFIX + "draft"); return value == null ? "" : value; }
    public void setDraft(String text) {
        if (!busy && pending == null && !revoked && !cleared) saved.set(PREFIX + "draft", text == null ? "" : text);
    }
    public void send(String sender, String moment) {
        if (busy || pending != null || revoked || cleared) return;
        if (!FriendModels.uuid(sender) || !FriendModels.uuid(moment) || !ChatRepository.validText(draft())) {
            error = R.string.chat_invalid_text; publish(); return;
        }
        if (momentId != null && (!momentId.equalsIgnoreCase(moment) || !senderId.equalsIgnoreCase(sender))) return;
        senderId = sender; momentId = moment;
        pending = new Send(UUID.randomUUID().toString(), "MOMENT_REPLY", draft(), momentId);
        saved.set(PREFIX + "sender", senderId); saved.set(PREFIX + "moment", momentId);
        savePending(); retry();
    }
    public void retry() {
        if (busy || pending == null || revoked || cleared) return;
        busy = true; error = 0; publish();
        final Send frozen = pending;
        final String peer = senderId, existingRoom = room;
        final int attempt = revision;
        worker.execute(() -> {
            ChatRepository owner = null;
            String resolvedRoom = existingRoom;
            String messageId = null;
            Exception failure = null;
            try {
                var auth = AuthGraph.repository(getApplication());
                if (auth.sessionGeneration() != generation) throw new ApiFailure(ApiFailure.Kind.CANCELLED);
                if (repository == null) repository = auth.chat();
                owner = repository; owner.requireCurrent();
                if (peer.equalsIgnoreCase(auth.profile().id)) throw new ApiFailure(ApiFailure.Kind.VALIDATION);
                if (resolvedRoom == null) resolvedRoom = owner.withFriend(peer).id;
                if (cleared || attempt != revision || Thread.currentThread().isInterrupted()) return;
                var sent = owner.send(resolvedRoom, frozen); messageId = sent.id;
            } catch (IOException | RuntimeException problem) { failure = problem; }
            final ChatRepository producer = owner;
            final String resultRoom = resolvedRoom, resultMessage = messageId;
            final Exception problem = failure;
            main.post(() -> {
                if (cleared || attempt != revision || revoked) return;
                Runnable deliver = () -> {
                    busy = false;
                    room = resultRoom; saved.set(PREFIX + "room", room);
                    if (problem != null) failed(problem);
                    else {
                        pending = null; savePending(); saved.set(PREFIX + "draft", "");
                        navigation = room; saved.set(PREFIX + "navigation", navigation);
                        PushEventBus.emitChatChanged(room, resultMessage);
                    }
                    publish();
                };
                try {
                    if (AuthGraph.repository(getApplication()).sessionGeneration() != generation
                            || (producer != null && !producer.publishIfCurrent(deliver))) expire();
                    else if (producer == null) deliver.run();
                } catch (IOException ignored) { expire(); }
            });
        });
    }
    public void discardPending() {
        if (busy || revoked || cleared) return;
        pending = null; savePending(); error = 0; publish();
    }
    public void consumeNavigation() { navigation = null; saved.remove(PREFIX + "navigation"); publish(); }
    public void invalidate() {
        revision++; revoked = true; busy = false; pending = null; navigation = null; room = null;
        clearSaved(); saved.set(PREFIX + "revoked", true); error = R.string.chat_forbidden; publish();
    }
    private void expire() { invalidate(); error = R.string.chat_session_expired; publish(); }
    private void failed(Exception problem) {
        error = problem instanceof IOException ? R.string.chat_network_error : R.string.chat_error;
        if (problem instanceof ApiFailure api) {
            error = R.string.chat_error;
            if (api.kind == ApiFailure.Kind.UNAUTHORIZED || api.kind == ApiFailure.Kind.CANCELLED) expire();
            else if (api.status == 403 || api.status == 404) invalidate();
            else if (api.kind == ApiFailure.Kind.VALIDATION) error = R.string.chat_invalid_text;
        }
    }
    private void clearSaved() { for (String key : PRIVATE_KEYS) saved.remove(PREFIX + key); }
    private void savePending() {
        saved.set(PREFIX + "key", pending == null ? null : pending.clientMessageId);
        saved.set(PREFIX + "text", pending == null ? null : pending.content);
    }
    private final PushEventBus.Listener listener = new PushEventBus.Listener() {
        @Override public void onMomentCreated(String id) { }
        @Override public void onMomentDeleted(String id) {
            main.post(() -> { if (!cleared && Objects.equals(momentId, id)) invalidate(); });
        }
        @Override public void onSessionCleared() { main.post(() -> { if (!cleared) expire(); }); }
    };
    private void publish() { state.setValue(new State(this)); }
    @Override protected void onCleared() {
        cleared = true; PushEventBus.unregister(listener); main.removeCallbacksAndMessages(null); worker.shutdownNow();
    }
}
