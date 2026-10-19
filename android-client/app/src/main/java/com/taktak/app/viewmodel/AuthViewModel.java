package com.taktak.app.viewmodel;

import android.app.Application;
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

public final class AuthViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean loading;
        public final int message;
        public final int retryAfterSeconds;
        public final boolean success;
        public final boolean resetRequired;
        private State(boolean loading, int message, int retryAfterSeconds, boolean success) {
            this(loading,message,retryAfterSeconds,success,false);
        }
        private State(boolean loading, int message, int retryAfterSeconds, boolean success, boolean resetRequired) {
            this.loading = loading; this.message = message; this.retryAfterSeconds = retryAfterSeconds; this.success = success;
            this.resetRequired=resetRequired;
        }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(false, 0, 0, false));
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean busy;
    private volatile boolean cleared;
    private long retryLoginAfter;

    public AuthViewModel(Application application) { super(application); }
    public LiveData<State> state() { return state; }

    public void login(String email, String password) {
        execute(repository -> repository.login(email, password), R.string.auth_login_success);
    }
    public void register(String email, String username, String name, String password) {
        execute(repository -> repository.register(email, username, name, password), R.string.auth_register_success);
    }

    private void execute(Operation operation, int successMessage) {
        if (busy || cleared) { return; }
        if (android.os.SystemClock.elapsedRealtime() < retryLoginAfter) {
            int seconds = (int) ((retryLoginAfter - android.os.SystemClock.elapsedRealtime() + 999) / 1000);
            state.setValue(new State(false, R.string.auth_rate_limited, Math.max(1, seconds), false));
            return;
        }
        busy = true;
        state.setValue(new State(true, 0, 0, false));
        executor.execute(() -> {
            State result;
            try {
                operation.run(AuthGraph.repository(getApplication()));
                result = new State(false, successMessage, 0, true);
            } catch (com.taktak.app.data.repository.PasswordResetRequired required) {
                result = new State(false, 0, 0, false, true);
            } catch (ApiFailure failure) {
                int message;
                switch (failure.kind) {
                    case NETWORK: message = R.string.auth_network_error; break;
                    case UNAUTHORIZED: message = R.string.auth_invalid_credentials; break;
                    case CONFLICT:
                        if ("EMAIL_CONFLICT".equals(failure.code)) {
                            message = R.string.auth_email_conflict;
                        } else if ("USERNAME_CONFLICT".equals(failure.code)) {
                            message = R.string.auth_username_conflict;
                        } else {
                            message = R.string.auth_account_conflict;
                        }
                        break;
                    case VALIDATION: message = R.string.auth_server_validation; break;
                    case RATE_LIMIT: message = R.string.auth_rate_limited; break;
                    case STORAGE: message = R.string.auth_storage_error; break;
                    case CANCELLED: message = R.string.auth_session_changed; break;
                    default: message = R.string.auth_server_error;
                }
                result = new State(false, message, failure.retryAfterSeconds, false);
            } catch (IOException failure) {
                result = new State(false, R.string.auth_network_error, 0, false);
            } catch (RuntimeException failure) {
                result = new State(false, R.string.auth_server_error, 0, false);
            }
            State completed = result;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (!cleared) {
                    busy = false;
                    if (completed.retryAfterSeconds > 0) {
                        retryLoginAfter = android.os.SystemClock.elapsedRealtime() + completed.retryAfterSeconds * 1000L;
                    }
                    state.setValue(completed);
                }
            });
        });
    }

    public interface UsernameCheckCallback {
        void onResult(Boolean available);
    }

    public void checkUsername(String username, UsernameCheckCallback callback) {
        if (cleared) { return; }
        executor.execute(() -> {
            Boolean available;
            try {
                available = AuthGraph.repository(getApplication()).checkUsername(username);
            } catch (Exception e) {
                available = null;
            }
            Boolean result = available;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (!cleared && callback != null) {
                    callback.onResult(result);
                }
            });
        });
    }

    @Override protected void onCleared() { cleared = true; executor.shutdownNow(); }
    private interface Operation { void run(AuthRepository repository) throws IOException; }
}
