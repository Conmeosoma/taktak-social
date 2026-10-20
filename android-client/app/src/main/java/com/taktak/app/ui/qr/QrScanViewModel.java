package com.taktak.app.ui.qr;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels.UserProfile;
import com.taktak.app.data.model.FriendModels.User;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.repository.FriendRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Chỉ cho phép gửi tới danh tính đã đối chiếu API trong cùng thế hệ phiên. */
public final class QrScanViewModel extends AndroidViewModel {
    public enum Status { CHECKING, READY, SELF, FRIEND, NOT_FOUND, CONFLICT, ERROR, SENDING, SENT }
    public record State(QrCodeUtils.FriendPayload payload, User target, Status status) { }
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<Integer> profileError = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AuthRepository repository;
    private final FriendRepository friendRepository;
    private long generation = -1;
    private int ticket;
    private volatile boolean cleared;
    private final com.taktak.app.data.push.PushEventBus.Listener sessionListener = new com.taktak.app.data.push.PushEventBus.Listener() {
        public void onMomentCreated(String id) { }
        public void onMomentDeleted(String id) { }
        @Override public void onSessionCleared() {
            main.post(() -> {
                if (cleared) { return; }
                ticket++; profile.setValue(null); profileError.setValue(R.string.qr_session_changed);
                State previous = state.getValue();
                if (previous != null) { state.setValue(new State(previous.payload(), null, Status.ERROR)); }
            });
        }
    };

    public QrScanViewModel(@NonNull Application app) {
        super(app);
        AuthRepository available = null;
        try { available = AuthGraph.repository(app); } catch (java.io.IOException ignored) { }
        repository = available;
        friendRepository = available == null ? null : available.friends();
        try { generation = repository.sessionGeneration(); } catch (Exception ignored) { }
        com.taktak.app.data.push.PushEventBus.register(sessionListener);
        loadProfile();
    }
    public LiveData<State> state() { return state; }
    public LiveData<UserProfile> profile() { return profile; }
    public LiveData<Integer> profileError() { return profileError; }
    public boolean hasCurrentProfile() { return current() && profile.getValue() != null; }
    private boolean current() {
        try { return !cleared && repository.sessionGeneration() == generation; }
        catch (Exception ignored) { return false; }
    }
    public void loadProfile() {
        if (cleared) { return; }
        profileError.setValue(null);
        executor.execute(() -> {
            try {
                if (!current()) { throw new IllegalStateException(); }
                UserProfile me = repository.profile();
                main.post(() -> {
                    if (current()) { profile.setValue(me); }
                    else { profileError.setValue(R.string.qr_session_changed); }
                });
            } catch (Exception ignored) {
                main.post(() -> { if (!cleared) { profileError.setValue(R.string.qr_lookup_failed); } });
            }
        });
    }
    public void resolve(QrCodeUtils.FriendPayload payload) {
        int request = ++ticket;
        state.setValue(new State(payload, null, Status.CHECKING));
        executor.execute(() -> {
            try {
                if (!current()) { throw new IllegalStateException(); }
                FriendRepository friends = friendRepository;
                UserProfile me = repository.profile();
                friends.requireCurrent();
                if (payload.username.equalsIgnoreCase(me.username)) {
                    publish(request, new State(payload, null, Status.SELF)); return;
                }
                User target = null;
                for (User user : friends.search(payload.username)) {
                    if (payload.username.equalsIgnoreCase(user.username)) { target = user; break; }
                }
                if (target == null || (payload.userId != null && !payload.userId.equalsIgnoreCase(target.id))) {
                    publish(request, new State(payload, null, Status.NOT_FOUND)); return;
                }
                boolean existing = false;
                for (int page = 0; page <= 10000; page++) {
                    var result = friends.friends(page);
                    for (User user : result.items) { if (user.id.equals(target.id)) { existing = true; break; } }
                    if (existing || !result.hasNext) { break; }
                    if (page == 10000) { throw new IllegalStateException(); }
                }
                publish(request, new State(payload, target, existing ? Status.FRIEND : Status.READY));
            } catch (Exception ignored) { publish(request, new State(payload, null, Status.ERROR)); }
        });
    }
    private void publish(int request, State value) {
        main.post(() -> {
            if (!cleared && request == ticket) {
                if (current()) { state.setValue(value); }
                else { state.setValue(new State(value.payload(), null, Status.ERROR)); }
            }
        });
    }
    public void send() {
        State value = state.getValue();
        if (value == null || value.status() != Status.READY || value.target() == null || !current()) { return; }
        int request = ticket;
        state.setValue(new State(value.payload(), value.target(), Status.SENDING));
        executor.execute(() -> {
            try {
                if (!current()) { throw new IllegalStateException(); }
                friendRepository.send(value.target().id);
                publish(request, new State(value.payload(), value.target(), Status.SENT));
            } catch (Exception failure) {
                Status result = failure instanceof com.taktak.app.data.remote.ApiFailure api
                        && api.kind == com.taktak.app.data.remote.ApiFailure.Kind.CONFLICT ? Status.CONFLICT : Status.ERROR;
                publish(request, new State(value.payload(), null, result));
            }
        });
    }
    public void reset() { ticket++; state.setValue(null); }
    @Override protected void onCleared() {
        cleared = true; com.taktak.app.data.push.PushEventBus.unregister(sessionListener);
        executor.shutdownNow(); main.removeCallbacksAndMessages(null);
    }
}
