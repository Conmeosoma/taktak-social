package com.taktak.app.viewmodel;

import android.app.Application;
import android.os.*;
import androidx.lifecycle.*;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.AccountRecoveryRepository;
import java.io.IOException;
import java.util.concurrent.*;

public final class RecoveryViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean loading, retry, changed;
        public final int message, fieldError;
        State(boolean loading,int message,boolean retry,boolean changed,int fieldError) {
            this.loading=loading; this.message=message; this.retry=retry; this.changed=changed; this.fieldError=fieldError;
        }
    }
    private final MutableLiveData<State> state=new MutableLiveData<>(new State(false,0,false,false,0));
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private boolean busy; private volatile boolean cleared; private long retryAfter;
    public RecoveryViewModel(Application app) { super(app); }
    public LiveData<State> state() { return state; }
    public void forgot(String email) { execute(r -> r.forgot(email),R.string.recovery_generic,false); }
    public void requestVerification(String email,String password) { execute(r -> r.requestVerification(email,password),R.string.recovery_code_sent,false); }
    public void verify(String email,String password,String code) { execute(r -> r.verify(email,password,code),R.string.recovery_verified,false); }
    public void reset(String temporary,String password,String confirm) { execute(r -> r.reset(temporary,password,confirm),R.string.recovery_changed,true); }
    private void execute(Operation op,int success,boolean changed) {
        if (busy || cleared) return;
        if (SystemClock.elapsedRealtime()<retryAfter) { state.setValue(new State(false,R.string.recovery_wait,false,false,0)); return; }
        busy=true; state.setValue(new State(true,0,false,false,0));
        executor.execute(() -> {
            State result; int seconds=0;
            try { op.run(AuthGraph.repository(getApplication()).recovery()); result=new State(false,success,false,changed,0); }
            catch (ApiFailure e) {
                seconds=e.retryAfterSeconds;
                int message=e.kind==ApiFailure.Kind.UNAUTHORIZED ? R.string.recovery_invalid
                        : e.kind==ApiFailure.Kind.VALIDATION ? R.string.auth_server_validation
                        : e.kind==ApiFailure.Kind.RATE_LIMIT ? R.string.recovery_wait
                        : e.kind==ApiFailure.Kind.NETWORK ? R.string.auth_network_error : R.string.auth_server_error;
                result=new State(false,message,e.kind==ApiFailure.Kind.NETWORK || e.kind==ApiFailure.Kind.SERVER,false,
                        e.kind==ApiFailure.Kind.VALIDATION ? R.string.recovery_different : 0);
            } catch (IOException e) { result=new State(false,R.string.auth_network_error,true,false,0); }
            catch (RuntimeException e) { result=new State(false,R.string.auth_server_error,true,false,0); }
            State completed=result; int cooldown=seconds;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!cleared) { busy=false; retryAfter=SystemClock.elapsedRealtime()+cooldown*1000L; state.setValue(completed); }
            });
        });
    }
    @Override protected void onCleared() { cleared=true; executor.shutdownNow(); }
    private interface Operation { void run(AccountRecoveryRepository repository) throws IOException; }
}
