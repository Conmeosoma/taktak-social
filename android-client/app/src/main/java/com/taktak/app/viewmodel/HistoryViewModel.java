package com.taktak.app.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import androidx.collection.LruCache;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.MomentModels.MomentItem;
import com.taktak.app.data.model.MomentModels.MomentsPage;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.MomentRepository;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HistoryViewModel extends AndroidViewModel {

    public interface ImageCallback {
        void onLoaded(Bitmap bitmap);
        void onError();
    }

    public static final class State {
        public final List<MomentItem> moments;
        public final boolean busy;
        public final boolean loaded;
        public final boolean hasNext;
        public final boolean expired;
        public final int message;

        public State(List<MomentItem> moments, boolean busy, boolean loaded, boolean hasNext, boolean expired, int message) {
            this.moments = moments;
            this.busy = busy;
            this.loaded = loaded;
            this.hasNext = hasNext;
            this.expired = expired;
            this.message = message;
        }
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final LruCache<String, Bitmap> imageCache;

    private MomentRepository repository;
    private final List<MomentItem> moments = new ArrayList<>();
    private boolean busy, loaded, hasNext, expired;
    private int page = 0, message = 0;
    private volatile boolean cleared;

    public HistoryViewModel(Application application) {
        super(application);
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = Math.min(maxMemory / 8, 20 * 1024); // max 20 MB or 1/8 heap
        imageCache = new LruCache<>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
        publish();
    }

    public LiveData<State> state() { return state; }

    public void acknowledgeSessionChange() {
        if (!expired || busy) { return; }
        repository = null;
        loaded = hasNext = expired = false;
        moments.clear();
        imageCache.evictAll();
        page = 0;
        message = 0;
        publish();
    }

    public void load() {
        if (!loaded && !busy && !expired) {
            refresh();
        }
    }

    public void refresh() {
        if (busy || expired || cleared) { return; }
        run(repo -> {
            MomentsPage firstPage = repo.received(0);
            return () -> {
                moments.clear();
                moments.addAll(firstPage.items);
                page = 0;
                hasNext = firstPage.hasNext;
                loaded = true;
                message = 0;
            };
        });
    }

    public void more() {
        if (busy || expired || cleared || !hasNext) { return; }
        int nextPage = page + 1;
        run(repo -> {
            MomentsPage nextPageData = repo.received(nextPage);
            return () -> {
                var existingIds = new HashSet<String>();
                for (var item : moments) { existingIds.add(item.id); }
                for (var item : nextPageData.items) {
                    if (existingIds.add(item.id)) {
                        moments.add(item);
                    }
                }
                page = nextPage;
                hasNext = nextPageData.hasNext;
                message = 0;
            };
        });
    }

    public void loadImage(String momentId, ImageCallback callback) {
        if (momentId == null || cleared) {
            callback.onError();
            return;
        }
        Bitmap cached = imageCache.get(momentId);
        if (cached != null) {
            callback.onLoaded(cached);
            return;
        }
        worker.execute(() -> {
            try {
                if (repository == null) {
                    repository = AuthGraph.repository(getApplication()).moments();
                }
                byte[] bytes = repository.download(momentId);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) {
                    throw new IOException("Failed to decode JPEG bytes");
                }
                imageCache.put(momentId, bitmap);
                main.post(() -> {
                    if (!cleared) {
                        callback.onLoaded(bitmap);
                    }
                });
            } catch (Exception failure) {
                android.util.Log.e("TaktakQa", "Failed to download/decode image: " + momentId, failure);
                main.post(() -> {
                    if (!cleared) {
                        callback.onError();
                    }
                });
            }
        });
    }

    private interface Work {
        Runnable execute(MomentRepository repo) throws IOException;
    }

    private void run(Work action) {
        if (busy || expired || cleared) { return; }
        busy = true;
        message = 0;
        publish();
        worker.execute(() -> {
            Runnable result;
            try {
                if (repository == null) {
                    repository = AuthGraph.repository(getApplication()).moments();
                }
                result = action.execute(repository);
            } catch (IOException | RuntimeException failure) {
                result = () -> failed(failure);
            }
            Runnable completion = result;
            main.post(() -> {
                if (cleared) { return; }
                completion.run();
                busy = false;
                publish();
            });
        });
    }

    private void failed(Exception failure) {
        if (failure instanceof ApiFailure api) {
            if (api.kind == ApiFailure.Kind.UNAUTHORIZED || api.kind == ApiFailure.Kind.CANCELLED) {
                moments.clear();
                expired = true;
                message = R.string.auth_invalid_credentials;
                return;
            }
            if (api.kind == ApiFailure.Kind.NETWORK) {
                message = R.string.history_network_error;
                return;
            }
            message = R.string.history_server_error;
            return;
        }
        message = failure instanceof IOException ? R.string.history_network_error : R.string.history_server_error;
    }

    private void publish() {
        state.setValue(new State(Collections.unmodifiableList(new ArrayList<>(moments)), busy, loaded, hasNext, expired, message));
    }

    @Override
    protected void onCleared() {
        cleared = true;
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
    }
}
