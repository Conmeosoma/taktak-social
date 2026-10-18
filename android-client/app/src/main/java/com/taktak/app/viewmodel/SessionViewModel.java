package com.taktak.app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.AuthRepository;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SessionViewModel extends AndroidViewModel {
    public enum Destination { CHECKING, LOGIN, CAMERA, PERMISSION, ERROR }
    public static final class State {
        public final Destination destination;
        public final int message;
        public State(Destination destination, int message) { this.destination = destination; this.message = message; }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Destination.CHECKING, R.string.session_checking));
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean busy;
    private volatile boolean cleared;
    private boolean logoutStorageFailed;

    public SessionViewModel(Application application) { super(application); }
    public LiveData<State> state() { return state; }
    public void check(boolean networkAllowed) { run(false, networkAllowed); }
    public void logout() { run(true, true); }

    private void run(boolean logout, boolean networkAllowed) {
        if (busy || cleared) { return; }
        busy = true;
        state.setValue(new State(Destination.CHECKING, logout ? R.string.session_logging_out : R.string.session_checking));
        worker.execute(() -> {
            State result;
            try {
                AuthRepository repository = AuthGraph.repository(getApplication());
                result = logout ? signOut(repository) : restore(repository, networkAllowed);
            } catch (ApiFailure failure) {
                result = new State(Destination.ERROR, failure.kind == ApiFailure.Kind.STORAGE
                        ? R.string.auth_storage_error : R.string.auth_server_error);
            } catch (IOException failure) {
                result = new State(Destination.ERROR, R.string.auth_network_error);
            } catch (RuntimeException failure) {
                result = new State(Destination.ERROR, R.string.auth_server_error);
            }
            State completed = result;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!cleared) { busy = false; state.setValue(completed); }
            });
        });
    }

    private State restore(AuthRepository repository, boolean networkAllowed) throws IOException {
        if (logoutStorageFailed) { return new State(Destination.ERROR, R.string.auth_storage_error); }
        if (!repository.hasSession()) {
            com.taktak.app.widget.WidgetUpdater.renderLoggedOut(getApplication());
            return new State(Destination.LOGIN, 0);
        }
        if (!networkAllowed) { return new State(Destination.PERMISSION, R.string.auth_permission_denied); }
        try {
            repository.profile();
            syncFcmToken(repository);
            com.taktak.app.widget.WidgetUpdater.enqueueOneTimeWork(getApplication());
            return new State(Destination.CAMERA, 0);
        } catch (ApiFailure failure) {
            if (failure.kind == ApiFailure.Kind.UNAUTHORIZED && !repository.hasSession()) {
                com.taktak.app.widget.WidgetUpdater.renderLoggedOut(getApplication());
                return new State(Destination.LOGIN, R.string.auth_invalid_credentials);
            }
            throw failure;
        }
    }

    private void syncFcmToken(AuthRepository repository) {
        try {
            long generation = repository.sessionGeneration();
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!cleared && task.isSuccessful() && task.getResult() != null) {
                            String token = task.getResult();
                            try { worker.execute(() -> {
                                try {
                                    if (!cleared && repository.sessionGeneration() == generation) {
                                        repository.devices(getApplication()).register(token);
                                    }
                                } catch (Exception ignored) { }
                            }); } catch (java.util.concurrent.RejectedExecutionException ignored) { }
                        }
                    });
        } catch (Exception ignored) {
            // Google Play Services may not be available in some test environments
        }
    }

    private State signOut(AuthRepository repository) throws IOException {
        try {
            repository.logout(getApplication());
            logoutStorageFailed = false;
            com.taktak.app.widget.WidgetUpdater.renderLoggedOut(getApplication());
            return new State(Destination.LOGIN, R.string.session_logged_out);
        }
        catch (IOException | RuntimeException failure) {
            // Không báo thành công nếu xóa storage thất bại dù memory đã bị xóa.
            if (failure instanceof ApiFailure && ((ApiFailure) failure).kind == ApiFailure.Kind.STORAGE) {
                logoutStorageFailed = true;
                throw failure;
            }
            if (repository.hasSession()) { throw failure; }
            return new State(Destination.LOGIN, R.string.session_logout_local_only);
        }
    }

    @Override protected void onCleared() { cleared = true; worker.shutdownNow(); }
}
