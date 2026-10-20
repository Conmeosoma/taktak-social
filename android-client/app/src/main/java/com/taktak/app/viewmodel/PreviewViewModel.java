package com.taktak.app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.FriendModels.User;
import com.taktak.app.data.repository.*;
import com.taktak.app.data.remote.ApiFailure;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class PreviewViewModel extends AndroidViewModel {
    public record State(File file, byte[] downloaded, List<User> friends, boolean loading,
            boolean loadingFriends, boolean hasNext, boolean friendsError, boolean sending, boolean retry,
            int message, boolean all, Set<String> selected, int progress) { }
    private final SavedStateHandle saved;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<User> friends = new ArrayList<>();
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();
    private CaptureDraftRepository drafts;
    private FriendRepository friendRepository;
    private MomentRepository moments;
    private String id, momentId;
    private File file;
    private byte[] downloaded;
    private boolean loading, loadingFriends, hasNext, friendsError, sending, retry, all;
    private int page, progress, message = R.string.preview_local;
    private volatile boolean cleared;
    private boolean friendTransfer, frozen;
    private List<String> pendingRecipients;
    private String pendingCaption;
    public boolean friendTransfer() { return friendTransfer; }
    public boolean selectionLocked() { return frozen || downloaded != null; }
    public PreviewViewModel(Application application, SavedStateHandle saved) {
        super(application); this.saved = saved;
        all = !Boolean.FALSE.equals(saved.get("all"));
        ArrayList<String> ids = saved.get("selected"); if (ids != null) { selected.addAll(ids); }
        publish();
    }
    public LiveData<State> state() { return state; }
    public String caption() { String caption = saved.get("caption"); return caption == null ? "" : caption; }
    public void caption(String text) {
        if (!selectionLocked() && text.codePointCount(0, text.length()) <= 50) {
            saved.set("caption", text);
            saved.remove("idempotencyKey");
        }
    }
    public void open(String id, long generation) {
        if (loading || cleared) { return; }
        if (this.id != null && (!this.id.equals(id) || drafts.generation != generation)) { invalid(); return; }
        if (sending) { if (current()) { publish(); } return; }
        try {
            drafts = new CaptureDraftRepository(getApplication(), generation); this.id = id;
            friendRepository = AuthGraph.repository(getApplication()).friends();
            moments = AuthGraph.repository(getApplication()).moments();
        } catch (IOException failure) { invalid(); return; }
        if (downloaded != null) { publish(); return; }
        loading = true; publish();
        worker.execute(() -> {
            try {
                File result = drafts.file(id);
                main.post(() -> {
                    if (!cleared) { file = result; loading = false; publish(); if (friends.isEmpty()) { loadFriends(); } }
                });
            } catch (IOException failure) { main.post(this::invalid); }
        });
    }
    public void loadFriends() {
        if (cleared || drafts == null || loadingFriends || (page > 0 && !hasNext)) { return; }
        loadingFriends = true; friendsError = false; publish(); int next = page;
        worker.execute(() -> {
            try {
                var result = friendRepository.friends(next);
                main.post(() -> {
                    if (!current()) { return; }
                    for (User user : result.items) {
                        if (friends.stream().noneMatch(existing -> existing.id.equals(user.id))) { friends.add(user); }
                    }
                    page = next + 1; hasNext = result.hasNext; loadingFriends = false;
                    if (!hasNext) { selected.removeIf(value -> friends.stream().noneMatch(user -> user.id.equals(value))); }
                    publish();
                });
            } catch (IOException | RuntimeException failure) {
                main.post(() -> { if (current()) { loadingFriends = false; friendsError = true; publish(); } });
            }
        });
    }
    public void selectAll(boolean value) {
        if (!sending && !selectionLocked()) {
            all = value; selected.clear();
            saved.remove("idempotencyKey");
            publish();
        }
    }
    public void toggle(String id, boolean checked) {
        if (sending || selectionLocked() || friends.stream().noneMatch(user -> user.id.equals(id))) { return; }
        if (all) { for (User user : friends) { selected.add(user.id); } all = false; }
        if (checked) { selected.add(id); } else { selected.remove(id); }
        saved.remove("idempotencyKey");
        publish();
    }
    public void updateDraftImage(byte[] jpeg, Runnable onComplete) {
        if (drafts == null || id == null || jpeg == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        worker.execute(() -> {
            try {
                File target = drafts.file(id);
                try (var fos = new FileOutputStream(target)) {
                    fos.write(jpeg);
                }
                main.post(() -> {
                    if (current()) {
                        file = target;
                        publish();
                    }
                    if (onComplete != null) onComplete.run();
                });
            } catch (IOException failure) {
                main.post(() -> {
                    if (onComplete != null) onComplete.run();
                });
            }
        });
    }

    public void savePrivate() {
        transfer(false);
    }
    public void sendFriends() {
        transfer(true);
    }
    private void transfer(boolean toFriends) {
        if (file == null || loading || sending || downloaded != null || !current()) { return; }
        if (frozen && toFriends != friendTransfer) { return; }
        if (!frozen && toFriends && ((!all && selected.isEmpty()) || loadingFriends || friendsError || friends.isEmpty())) { return; }
        if (!frozen) {
            friendTransfer = toFriends; pendingCaption = caption();
            pendingRecipients = toFriends && all ? null : toFriends ? List.copyOf(selected) : List.of();
            String key = saved.get("idempotencyKey");
            if (key == null) {
                key = UUID.randomUUID().toString();
                saved.set("idempotencyKey", key);
            }
        }
        final String idempotencyKey = saved.get("idempotencyKey");
        boolean resolveAll = toFriends && pendingRecipients == null;
        frozen = true;
        sending = true; retry = false; progress = 0; message = R.string.moment_transferring; publish();
        worker.execute(() -> {
            boolean attemptedPost = false;
            try {
                drafts.requireCurrent();
                if (resolveAll) {
                    var ids = new LinkedHashSet<String>();
                    for (int next = 0; ; next++) {
                        var result = friendRepository.friends(next);
                        for (User user : result.items) { ids.add(user.id); }
                        if (ids.size() > 100 || next >= 5 && result.hasNext) {
                            throw new ApiFailure(ApiFailure.Kind.VALIDATION);
                        }
                        if (!result.hasNext) { break; }
                    }
                    if (ids.isEmpty()) { throw new ApiFailure(ApiFailure.Kind.VALIDATION); }
                    pendingRecipients = List.copyOf(ids);
                }
                if (momentId == null) {
                    byte[] jpeg = drafts.read(id); attemptedPost = true;
                    momentId = moments.upload(jpeg, pendingCaption, pendingRecipients, idempotencyKey,
                            (percent, bytesWritten, totalBytes) -> {
                                main.post(() -> {
                                    if (current() && sending) {
                                        progress = percent;
                                        publish();
                                    }
                                });
                            });
                }
                main.post(() -> {
                    if (current() && sending) {
                        progress = 100;
                        publish();
                    }
                });
                byte[] result = moments.download(momentId);
                drafts.requireCurrent(); drafts.discard(id);
                main.post(() -> {
                    if (current()) {
                        downloaded = result; file = null; sending = false; progress = 100;
                        saved.remove("idempotencyKey");
                        message = friendTransfer ? R.string.preview_sent : R.string.moment_downloaded; publish();
                    }
                });
            } catch (IOException | RuntimeException failure) {
                boolean nonRetryable = momentId == null && (!attemptedPost || failure instanceof ApiFailure api
                        && (api.status == 400 || api.status == 401 || api.status == 409));
                boolean conflict = failure instanceof ApiFailure api && api.status == 409;
                main.post(() -> {
                    if (current()) {
                        sending = false; retry = !nonRetryable; progress = 0;
                        if (nonRetryable) {
                            frozen = false; pendingRecipients = null;
                            saved.remove("idempotencyKey");
                        }
                        if (conflict) {
                            message = R.string.preview_conflict_error;
                        } else if (nonRetryable && toFriends) {
                            message = R.string.preview_recipient_error;
                        } else {
                            message = R.string.moment_transfer_error;
                        }
                        publish();
                    }
                });
            }
        });
    }
    private boolean current() {
        if (cleared || drafts == null) { return false; }
        try { drafts.requireCurrent(); return true; }
        catch (IOException failure) { invalid(); return false; }
    }
    private void invalid() {
        if (cleared) { return; }
        loading = loadingFriends = sending = retry = false; file = null; downloaded = null; progress = 0;
        friends.clear(); selected.clear(); saved.set("caption", ""); saved.remove("idempotencyKey");
        message = R.string.preview_missing; publish();
    }
    private void publish() {
        if (cleared) { return; }
        saved.set("all", all); saved.set("selected", new ArrayList<>(selected));
        state.setValue(new State(file, downloaded, List.copyOf(friends), loading, loadingFriends, hasNext,
                friendsError, sending, retry, message, all, Set.copyOf(selected), progress));
    }
    @Override protected void onCleared() {
        cleared = true;
        // Task I/O đã bắt đầu được kết thúc trước khi xóa đúng draft thuộc màn này.
        worker.execute(() -> {
            if (drafts != null && id != null) {
                try { drafts.discard(id); }
                catch (IOException failure) { /* Hệ thống vẫn có thể thu hồi cache private. */ }
            }
        });
        worker.shutdown();
    }
}
