package com.taktak.app.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.ui.camera.PhotoProcessor;
import java.io.IOException;
import java.util.concurrent.*;

public final class MomentViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean busy, retry;
        public final int message;
        public final byte[] downloaded;
        public State(boolean busy, boolean retry, int message, byte[] downloaded) {
            this.busy = busy; this.retry = retry; this.message = message; this.downloaded = downloaded;
        }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(false, false, R.string.moment_self_only, null));
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private byte[] captured;
    private String momentId;
    private com.taktak.app.data.repository.MomentRepository repository;
    private boolean busy;
    private volatile boolean cleared;
    public MomentViewModel(Application application) { super(application); }
    public LiveData<State> state() { return state; }

    public boolean beginCapture() {
        if (busy || cleared) { return false; }
        try { repository = AuthGraph.repository(getApplication()).moments(); }
        catch (IOException failure) { captureFailed(); return false; }
        busy = true; captured = null; momentId = null;
        state.setValue(new State(true, false, R.string.moment_capturing, null)); return true;
    }
    public void captureFailed() { busy = false; state.setValue(new State(false, false, R.string.moment_camera_error, null)); }
    public void acceptCapture(Bitmap bitmap, int rotation) {
        if (cleared) { bitmap.recycle(); return; }
        worker.execute(() -> {
            try {
                captured = PhotoProcessor.squareJpeg(bitmap, rotation);
                transfer();
            } catch (IOException | RuntimeException failure) { failed(failure); }
            finally { bitmap.recycle(); }
        });
    }
    public void retry() {
        if (busy || captured == null || cleared) { return; }
        busy = true; state.setValue(new State(true, false, R.string.moment_transferring, null));
        worker.execute(() -> { try { transfer(); } catch (IOException | RuntimeException failure) { failed(failure); } });
    }
    private void transfer() throws IOException {
        if (cleared) { throw new ApiFailure(ApiFailure.Kind.CANCELLED); }
        if (momentId == null) { momentId = repository.upload(captured); }
        byte[] downloaded = repository.download(momentId);
        main.post(() -> {
            if (!cleared) { busy = false; state.setValue(new State(false, false, R.string.moment_downloaded, downloaded)); }
        });
    }
    private void failed(Exception failure) {
        int message = R.string.moment_transfer_error;
        if (failure instanceof ApiFailure && ((ApiFailure) failure).kind == ApiFailure.Kind.UNAUTHORIZED) { message = R.string.auth_invalid_credentials; }
        int notice = message;
        main.post(() -> {
            if (!cleared) { busy = false; state.setValue(new State(false, captured != null, notice, null)); }
        });
    }
    @Override protected void onCleared() {
        cleared = true; worker.shutdownNow(); captured = null; momentId = null;
    }
}
