package com.taktak.app.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.repository.CaptureDraftRepository;
import com.taktak.app.ui.camera.PhotoProcessor;
import java.io.IOException;
import java.util.concurrent.*;

public final class CaptureViewModel extends AndroidViewModel {
    public record State(boolean busy, int message, String draftId) { }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(false, R.string.preview_capture_hint, null));
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private CaptureDraftRepository drafts;
    private volatile boolean cleared;
    public CaptureViewModel(Application application) { super(application); }
    public LiveData<State> state() { return state; }
    public long generation() { return drafts.generation; }
    public boolean beginCapture() {
        if (cleared || state.getValue().busy() || state.getValue().draftId() != null) { return false; }
        try {
            drafts = new CaptureDraftRepository(getApplication(), AuthGraph.repository(getApplication()).sessionGeneration());
            state.setValue(new State(true, R.string.moment_capturing, null)); return true;
        } catch (IOException failure) { captureFailed(); return false; }
    }
    public void acceptCapture(Bitmap bitmap, int rotation, boolean mirror) {
        if (cleared) { bitmap.recycle(); return; }
        worker.execute(() -> {
            try {
                String id = drafts.write(PhotoProcessor.squareJpeg(bitmap, rotation, mirror));
                main.post(() -> {
                    if (cleared) { discard(id); }
                    else { state.setValue(new State(false, R.string.preview_capture_hint, id)); }
                });
            } catch (IOException | RuntimeException failure) { main.post(this::captureFailed); }
            finally { bitmap.recycle(); }
        });
    }
    public void captureFailed() { if (!cleared) { state.setValue(new State(false, R.string.moment_camera_error, null)); } }
    public void consume() { state.setValue(new State(false, R.string.preview_capture_hint, null)); }
    private void discard(String id) {
        try { drafts.discard(id); }
        catch (IOException failure) { /* Cache private có TTL và có thể bị hệ thống thu hồi. */ }
    }
    @Override protected void onCleared() {
        cleared = true;
        if (state.getValue().draftId() != null) { discard(state.getValue().draftId()); }
        worker.shutdown();
    }
}
