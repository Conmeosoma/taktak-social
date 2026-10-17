package com.taktak.app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.FriendModels.*;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.FriendRepository;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public final class FriendsViewModel extends AndroidViewModel {
    public static final class State {
        public final List<User> results, friends;
        public final List<Invitation> incoming;
        public final Set<String> sent;
        public final boolean busy, loaded, searched, moreIncoming, moreFriends, expired, searchAllowed;
        public final int message;
        public final String query;
        private State(FriendsViewModel vm) {
            results = copy(vm.results); friends = copy(vm.friends); incoming = copy(vm.incoming);
            sent = Collections.unmodifiableSet(new HashSet<>(vm.sent)); busy = vm.busy; loaded = vm.loaded; searched = vm.searched;
            moreIncoming = vm.moreIncoming; moreFriends = vm.moreFriends; expired = vm.expired;
            message = vm.message; query = vm.query; searchAllowed = SystemClock.elapsedRealtime() >= vm.searchAfter;
        }
    }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private FriendRepository repository;
    private List<User> results = Collections.emptyList(), friends = Collections.emptyList();
    private List<Invitation> incoming = Collections.emptyList();
    private final Set<String> sent = new HashSet<>();
    private boolean busy, loaded, searched, moreIncoming, moreFriends, expired;
    private volatile boolean cleared;
    private int incomingPage, friendsPage, message;
    private long searchAfter;
    private String query = "";

    public FriendsViewModel(Application application) { super(application); publish(); }
    public LiveData<State> state() { return state; }
    public void acknowledgeSessionChange() {
        if (!expired || busy) { return; }
        repository = null; loaded = searched = moreIncoming = moreFriends = expired = false;
        query = ""; message = 0; searchAfter = 0;
        publish();
    }
    public void load() { if (!loaded && !busy && !expired) { refresh(); } }
    public void search(String input) {
        if (busy || expired) { return; }
        String normalized = FriendRepository.normalizeQuery(input);
        if (normalized == null) { message = R.string.friends_invalid_query; publish(); return; }
        if (SystemClock.elapsedRealtime() < searchAfter) { message = R.string.friends_rate_limit; publish(); return; }
        query = normalized; results = Collections.emptyList(); searched = false;
        run(repo -> { var users = repo.search(query); return () -> { results = users; searched = true; }; });
    }
    public void refresh() {
        run(repo -> {
            var requests = repo.incoming(0);
            var users = repo.friends(0);
            return () -> { replace(requests, users); results = Collections.emptyList(); searched = false; };
        });
    }
    public void more(boolean requests) {
        if ((requests && !moreIncoming) || (!requests && !moreFriends)) { return; }
        int next = (requests ? incomingPage : friendsPage) + 1;
        run(repo -> {
            if (requests) {
                var page = repo.incoming(next);
                return () -> {
                    var unique = new LinkedHashMap<String, Invitation>();
                    for (var item : incoming) { unique.put(item.id, item); }
                    for (var item : page.items) { unique.put(item.id, item); }
                    incoming = copy(unique.values()); incomingPage = next; moreIncoming = page.hasNext && next < 10000;
                };
            }
            var page = repo.friends(next);
            return () -> {
                var unique = new LinkedHashMap<String, User>();
                for (var item : friends) { unique.put(item.id, item); }
                for (var item : page.items) { unique.put(item.id, item); }
                friends = copy(unique.values()); friendsPage = next; moreFriends = page.hasNext && next < 10000;
            };
        });
    }
    public void send(String id) {
        mutate(repo -> repo.send(id), () -> { sent.add(id); message = R.string.friends_sent; });
    }
    public void respond(String id, boolean accept) { mutate(repo -> repo.respond(id, accept), () -> { }); }
    public void unfriend(String id) { mutate(repo -> repo.unfriend(id), () -> { }); }
    public void block(String id) {
        mutate(repo -> repo.block(id), () -> {
            results = results.stream().filter(user -> !user.id.equals(id)).collect(java.util.stream.Collectors.toList());
            sent.remove(id);
        });
    }
    private void mutate(Mutation action, Runnable success) {
        run(repo -> {
            action.execute(repo);
            // Mutation đã thành công: không tự gửi lại nếu lần đọc kế tiếp mất mạng.
            try {
                var requests = repo.incoming(0); var users = repo.friends(0);
                return () -> { replace(requests, users); message = R.string.friends_updated; success.run(); };
            } catch (IOException failure) {
                if (failure instanceof ApiFailure api && (api.kind == ApiFailure.Kind.UNAUTHORIZED || api.kind == ApiFailure.Kind.CANCELLED)) { throw failure; }
                return () -> {
                    incoming = Collections.emptyList(); friends = Collections.emptyList(); moreIncoming = false; moreFriends = false;
                    loaded = false; success.run(); message = R.string.friends_changed_refresh;
                };
            }
        });
    }
    private void replace(Page<Invitation> requests, Page<User> users) {
        incoming = copy(requests.items); friends = copy(users.items);
        incomingPage = friendsPage = 0; moreIncoming = requests.hasNext; moreFriends = users.hasNext; loaded = true;
        for (var friend : friends) { sent.remove(friend.id); }
    }
    private interface Work { Runnable execute(FriendRepository repo) throws IOException; }
    private interface Mutation { void execute(FriendRepository repo) throws IOException; }
    private void run(Work action) {
        if (busy || expired || cleared) { return; }
        busy = true; message = 0; publish();
        worker.execute(() -> {
            Runnable result;
            try {
                if (repository == null) { repository = AuthGraph.repository(getApplication()).friends(); }
                result = action.execute(repository);
            } catch (IOException | RuntimeException failure) { result = () -> failed(failure); }
            Runnable completion = result;
            main.post(() -> {
                if (cleared) { return; }
                try { if (repository != null) { repository.requireCurrent(); } completion.run(); }
                catch (ApiFailure failure) { failed(failure); }
                busy = false; publish();
            });
        });
    }
    private void failed(Exception failure) {
        if (failure instanceof ApiFailure api) {
            if (api.kind == ApiFailure.Kind.UNAUTHORIZED || api.kind == ApiFailure.Kind.CANCELLED) {
                results = Collections.emptyList(); friends = Collections.emptyList(); incoming = Collections.emptyList(); sent.clear(); expired = true;
                message = R.string.auth_invalid_credentials; return;
            }
            if (api.kind == ApiFailure.Kind.RATE_LIMIT) {
                searchAfter = SystemClock.elapsedRealtime() + api.retryAfterSeconds * 1000L;
                message = R.string.friends_rate_limit;
                main.postDelayed(() -> { if (!cleared) { publish(); } }, api.retryAfterSeconds * 1000L);
                return;
            }
            if (api.kind == ApiFailure.Kind.CONFLICT) { message = R.string.friends_conflict; return; }
            if (api.status == 404) { message = R.string.friends_unavailable; return; }
            if (api.kind == ApiFailure.Kind.VALIDATION) { message = R.string.friends_invalid_query; return; }
            if (api.kind == ApiFailure.Kind.STORAGE) { message = R.string.auth_storage_error; return; }
            if (api.kind == ApiFailure.Kind.NETWORK) { message = R.string.friends_network_error; return; }
            message = R.string.friends_server_error; return;
        }
        message = failure instanceof IOException ? R.string.friends_network_error : R.string.friends_server_error;
    }
    private static <T> List<T> copy(Collection<T> values) { return Collections.unmodifiableList(new ArrayList<>(values)); }
    private void publish() { state.setValue(new State(this)); }
    @Override protected void onCleared() { cleared = true; main.removeCallbacksAndMessages(null); worker.shutdownNow(); }
}
