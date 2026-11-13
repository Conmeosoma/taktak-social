package com.taktak.app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AiModels.*;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.AiRepository;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** Nội dung AI chỉ trong RAM, không ghi saved state, disk cache hoặc log. */
public final class AiViewModel extends AndroidViewModel {
    public static final class State {
        public final List<Conversation> conversations;
        public final List<Message> messages;
        public final boolean busy, chat, hasNext, expired, pending, configured;
        public final int error, sentRevision;
        public final String draft, conversationId;
        State(AiViewModel m) {
            conversations = List.copyOf(m.conversations); messages = List.copyOf(m.messages);
            busy = m.busy; chat = m.chat; hasNext = m.hasNext; expired = m.expired;
            pending = m.pending != null; configured = m.status != null && m.status.canSend();
            error = m.error; sentRevision = m.sentRevision; draft = m.draft; conversationId = m.conversationId;
        }
    }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final List<Conversation> conversations = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();
    private AiRepository repo;
    private Status status;
    private Send pending;
    private String conversationId, draft = "";
    private int page, error, sentRevision;
    private boolean busy, chat, hasNext, expired, consent;
    private volatile boolean cleared;
    private long retryAt;
    private final PushEventBus.Listener listener = new PushEventBus.Listener() {
        public void onMomentCreated(String id) { }
        public void onMomentDeleted(String id) { }
        public void onSessionCleared() { main.post(() -> { if (!cleared) expire(); }); }
    };
    public AiViewModel(Application app) { super(app); PushEventBus.register(listener); publish(); }
    public LiveData<State> state() { return state; }
    public void checkSession() {
        try { if (repo != null) repo.requireCurrent(); }
        catch (IOException failure) { expire(); }
    }
    public void draft(String value) { draft = value; }
    public void refresh() {
        if (chat) { if (conversationId != null) loadMessages(false); else checkStatus(); return; }
        run(r -> {
            Status found = r.status(); var result = r.conversations(0);
            return () -> { status = found; conversations.clear(); conversations.addAll(result.items);
                page = 0; hasNext = result.hasNext; availability(); };
        });
    }
    private void checkStatus() { run(r -> { Status found = r.status(); return () -> { status = found; availability(); }; }); }
    public void newChat() {
        if (busy || pending != null || expired) return;
        chat = true; conversationId = null; messages.clear(); draft = ""; page = 0; hasNext = false;
        error = 0; publish(); checkStatus();
    }
    public void open(String id) {
        if (busy || pending != null || expired) return;
        chat = true; conversationId = id; draft = ""; messages.clear(); page = 0; loadMessages(false);
    }
    public boolean backToList() {
        if (busy || pending != null) return false;
        chat = false; conversationId = null; draft = ""; messages.clear(); page = 0; refresh(); return true;
    }
    public void more() { if (!hasNext || busy || pending != null) return;
        if (chat) loadMessages(true);
        else { int next = page + 1; run(r -> { var result = r.conversations(next); return () -> {
            Set<String> ids = new HashSet<>(); for (var c : conversations) ids.add(c.id);
            for (var c : result.items) if (ids.add(c.id)) conversations.add(c);
            page = next; hasNext = result.hasNext;
        }; }); }
    }
    private void loadMessages(boolean more) {
        if (conversationId == null) return;
        String id = conversationId; int next = more ? page + 1 : 0;
        run(r -> { Status found = r.status(); var result = r.messages(id, next); return () -> {
            status = found; if (!more) messages.clear();
            Set<String> ids = new HashSet<>(); for (var m : messages) ids.add(m.id);
            for (var m : result.items) if (ids.add(m.id)) messages.add(m);
            messages.sort(Comparator.comparing((Message m) -> java.time.Instant.parse(m.createdAt)).thenComparing(m -> m.id));
            page = next; hasNext = result.hasNext; availability();
        }; });
    }
    public boolean needsConsent() { return !consent; }
    public void send(boolean agreed) {
        if (busy || expired || !chat || pending != null) return;
        if (!AiRepository.validText(draft)) { error = R.string.ai_invalid; publish(); return; }
        if (!agreed || status == null || !status.canSend()) return;
        consent = true; pending = new Send(conversationId, UUID.randomUUID().toString(), draft, true); retry();
    }
    public void retry() {
        if (pending == null || busy || expired) return;
        if (System.nanoTime() < retryAt) { error = R.string.ai_rate; publish(); return; }
        Send request = pending;
        run(r -> { Message answer = r.send(request); return () -> {
            Message question = new Message(); question.id = request.clientRequestId;
            question.conversationId = answer.conversationId; question.role = "user";
            question.content = request.message; question.createdAt = answer.createdAt;
            messages.add(question); messages.add(answer); conversationId = answer.conversationId;
            pending = null; draft = ""; sentRevision++; retryAt = 0;
        }; });
    }
    public void discard() { if (!busy) { pending = null; error = 0; publish(); } }
    public void delete(String id) {
        if (pending != null) return;
        run(r -> { r.delete(id); return () -> conversations.removeIf(c -> c.id.equals(id)); });
    }
    private void availability() { if (!status.canSend()) error = R.string.ai_unavailable; }
    private interface Work { Runnable execute(AiRepository r) throws IOException; }
    private void run(Work work) {
        if (busy || expired || cleared) return;
        busy = true; error = 0; publish();
        worker.execute(() -> {
            AiRepository owner = repo; Runnable result;
            try {
                if (owner == null) owner = AuthGraph.repository(getApplication()).ai();
                owner.requireCurrent(); result = work.execute(owner);
            } catch (IOException | RuntimeException failure) { result = () -> failed(failure); }
            AiRepository producer = owner; Runnable complete = result;
            main.post(() -> {
                if (cleared || expired) return;
                Runnable deliver = () -> { repo = producer; complete.run(); busy = false; publish(); };
                if (producer == null) deliver.run();
                else if (!producer.publishIfCurrent(deliver)) expire();
            });
        });
    }
    private void failed(Exception failure) {
        error = failure instanceof IOException ? R.string.ai_offline : R.string.ai_error;
        if (!(failure instanceof ApiFailure api)) return;
        error = R.string.ai_error;
        if (api.kind == ApiFailure.Kind.CANCELLED || api.kind == ApiFailure.Kind.UNAUTHORIZED) { expire(); return; }
        if (api.kind == ApiFailure.Kind.RATE_LIMIT) {
            retryAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(api.retryAfterSeconds); error = R.string.ai_rate;
        } else if ("AI_BUSY".equals(api.code)) error = R.string.ai_busy;
        else if (api.status == 503) error = R.string.ai_unavailable;
        else if (api.status == 404) { pending = null; messages.clear(); error = R.string.ai_missing; }
        else if (api.kind == ApiFailure.Kind.VALIDATION) error = R.string.ai_invalid;
    }
    private void expire() {
        expired = true; busy = false; consent = false; conversations.clear(); messages.clear();
        pending = null; draft = ""; conversationId = null; status = null; publish();
    }
    private void publish() { state.setValue(new State(this)); }
    @Override protected void onCleared() {
        cleared = true; PushEventBus.unregister(listener); main.removeCallbacksAndMessages(null); worker.shutdownNow();
    }
}
