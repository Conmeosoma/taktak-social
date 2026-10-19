package com.taktak.app.ui.theme;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/** Draft giao diện không phải lựa chọn đã lưu; không giữ Context/Activity hoặc phiên. */
public final class ThemeViewModel extends ViewModel {
    public record State(ThemeSelection saved, AppTheme style, ColorMode mode,
                        boolean busy, boolean error, boolean applied) {
        public boolean dirty() { return style != saved.style() || mode != saved.colorMode(); }
    }
    private final ThemeRepository repository;
    private final Executor worker;
    private final Executor main;
    private final MutableLiveData<State> state;
    private volatile boolean cleared;

    public ThemeViewModel(ThemeRepository repository, ThemeSelection initial, Executor worker, Executor main) {
        this.repository = repository;
        this.worker = worker;
        this.main = main;
        state = new MutableLiveData<>(new State(initial, initial.style(), initial.colorMode(), false, false, false));
    }
    public LiveData<State> state() { return state; }
    public void select(AppTheme style, ColorMode mode) {
        State old = state.getValue();
        if (old.busy() || cleared) { return; }
        state.setValue(new State(old.saved(), java.util.Objects.requireNonNull(style),
                java.util.Objects.requireNonNull(mode), false, false, false));
    }
    public void apply() {
        State requested = state.getValue();
        if (requested.busy() || !requested.dirty() || cleared) { return; }
        state.setValue(new State(requested.saved(), requested.style(), requested.mode(), true, false, false));
        worker.execute(() -> {
            ThemeSelection saved;
            try { saved = repository.save(requested.style(), requested.mode()); }
            catch (IOException | RuntimeException failure) {
                main.execute(() -> {
                    if (!cleared) { state.setValue(new State(requested.saved(), requested.style(), requested.mode(), false, true, false)); }
                });
                return;
            }
            main.execute(() -> {
                if (!cleared) { state.setValue(new State(saved, saved.style(), saved.colorMode(), false, false, true)); }
            });
        });
    }
    @Override protected void onCleared() {
        cleared = true;
        // Không interrupt commit đang chạy: không giả rằng Hủy có thể đảo save đã bắt đầu.
        if (worker instanceof ExecutorService service) { service.shutdown(); }
    }
}
