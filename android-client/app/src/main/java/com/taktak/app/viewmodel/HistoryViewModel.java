package com.taktak.app.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.MomentModels.MomentItem;
import com.taktak.app.data.model.MomentModels.MomentsPage;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.MomentRepository;
import com.taktak.app.ui.history.HistoryImageCache;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HistoryViewModel extends AndroidViewModel {

    public enum Tab {
        RECEIVED, SENT
    }

    public interface ImageCallback {
        void onLoaded(Bitmap bitmap);
        void onError();
    }

    public static final class State {
        public final Tab activeTab;
        public final List<MomentItem> moments;
        public final boolean busy;
        public final boolean loaded;
        public final boolean hasNext;
        public final boolean expired;
        public final int message;

        public State(List<MomentItem> moments, boolean busy, boolean loaded, boolean hasNext, boolean expired, int message) {
            this(Tab.RECEIVED, moments, busy, loaded, hasNext, expired, message);
        }

        public State(Tab activeTab, List<MomentItem> moments, boolean busy, boolean loaded, boolean hasNext, boolean expired, int message) {
            this.activeTab = activeTab;
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

    private MomentRepository repository;
    private Tab activeTab = Tab.RECEIVED;
    private final List<MomentItem> receivedMoments = new ArrayList<>();
    private final List<MomentItem> sentMoments = new ArrayList<>();
    private boolean receivedLoaded, sentLoaded;
    private boolean receivedHasNext, sentHasNext;
    private int receivedPage = 0, sentPage = 0;
    private boolean busy, expired;
    private int message = 0;
    private volatile boolean cleared;

    public HistoryViewModel(Application application) {
        super(application);
        PushEventBus.register(pushListener);
        publish();
    }

    private final com.taktak.app.data.push.PushEventBus.Listener pushListener = new com.taktak.app.data.push.PushEventBus.Listener() {
        @Override public void onSessionCleared() {
            main.post(() -> {
                if (cleared) { return; }
                if (repository != null && repository.publishIfCurrent(() -> { })) { return; }
                receivedMoments.clear(); sentMoments.clear();
                expired = true; publish();
            });
        }
        @Override
        public void onMomentCreated(String momentId) {
            main.post(() -> {
                if (cleared || expired || busy) { return; }
                if (activeTab == Tab.RECEIVED && receivedLoaded) {
                    sentLoaded = false;
                    refresh();
                } else if (activeTab == Tab.SENT && sentLoaded) {
                    receivedLoaded = false;
                    refresh();
                }
            });
        }

        @Override
        public void onMomentDeleted(String momentId) {
            main.post(() -> {
                if (cleared || expired) { return; }
                boolean removedR = receivedMoments.removeIf(m -> m.id.equals(momentId));
                boolean removedS = sentMoments.removeIf(m -> m.id.equals(momentId));
                if (removedR || removedS) {
                    HistoryImageCache.remove(momentId);
                    publish();
                }
            });
        }
    };

    public LiveData<State> state() { return state; }
    public Tab currentTab() { return activeTab; }

    public void switchTab(Tab tab) {
        if (tab == null || tab == activeTab) { return; }
        activeTab = tab;
        message = 0;
        publish();
        boolean loaded = (activeTab == Tab.RECEIVED) ? receivedLoaded : sentLoaded;
        if (!loaded && !busy && !expired && !cleared) {
            refresh();
        }
    }

    public void acknowledgeSessionChange() {
        if (!expired || busy) { return; }
        repository = null;
        receivedLoaded = sentLoaded = false;
        receivedHasNext = sentHasNext = false;
        expired = false;
        receivedMoments.clear();
        sentMoments.clear();
        HistoryImageCache.clear();
        receivedPage = sentPage = 0;
        message = 0;
        publish();
    }

    public void load() {
        boolean loaded = (activeTab == Tab.RECEIVED) ? receivedLoaded : sentLoaded;
        if (!loaded && !busy && !expired) {
            refresh();
        }
    }

    public void refresh() {
        if (busy || expired || cleared) { return; }
        final Tab targetTab = activeTab;
        final String apiType = (targetTab == Tab.RECEIVED) ? "received" : "sent";
        run(repo -> {
            MomentsPage firstPage = repo.list(apiType, 0);
            return () -> {
                if (targetTab == Tab.RECEIVED) {
                    receivedMoments.clear();
                    receivedMoments.addAll(firstPage.items);
                    receivedPage = 0;
                    receivedHasNext = firstPage.hasNext;
                    receivedLoaded = true;
                } else {
                    sentMoments.clear();
                    sentMoments.addAll(firstPage.items);
                    sentPage = 0;
                    sentHasNext = firstPage.hasNext;
                    sentLoaded = true;
                }
                message = 0;
            };
        });
    }

    public void more() {
        if (busy || expired || cleared) { return; }
        final Tab targetTab = activeTab;
        final boolean hasNext = (targetTab == Tab.RECEIVED) ? receivedHasNext : sentHasNext;
        if (!hasNext) { return; }
        final int nextPage = (targetTab == Tab.RECEIVED ? receivedPage : sentPage) + 1;
        final String apiType = (targetTab == Tab.RECEIVED) ? "received" : "sent";
        run(repo -> {
            MomentsPage nextPageData = repo.list(apiType, nextPage);
            return () -> {
                List<MomentItem> targetList = (targetTab == Tab.RECEIVED) ? receivedMoments : sentMoments;
                var existingIds = new HashSet<String>();
                for (var item : targetList) { existingIds.add(item.id); }
                for (var item : nextPageData.items) {
                    if (existingIds.add(item.id)) {
                        targetList.add(item);
                    }
                }
                if (targetTab == Tab.RECEIVED) {
                    receivedPage = nextPage;
                    receivedHasNext = nextPageData.hasNext;
                } else {
                    sentPage = nextPage;
                    sentHasNext = nextPageData.hasNext;
                }
                message = 0;
            };
        });
    }

    public void loadImage(String momentId, ImageCallback callback) {
        if (momentId == null || cleared) {
            callback.onError();
            return;
        }
        Bitmap cached = HistoryImageCache.get(momentId);
        if (cached != null && repository != null && repository.publishIfCurrent(() -> callback.onLoaded(cached))) {
            return;
        }
        worker.execute(() -> {
            try {
                MomentRepository owner = repository;
                if (owner == null) {
                    owner = AuthGraph.repository(getApplication()).moments();
                    repository = owner;
                }
                byte[] bytes = owner.download(momentId);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) {
                    throw new IOException("Failed to decode JPEG bytes");
                }
                MomentRepository producer = owner;
                main.post(() -> {
                    if (!cleared) {
                        if (!producer.publishIfCurrent(() -> {
                            HistoryImageCache.put(momentId, bitmap); callback.onLoaded(bitmap);
                        })) {
                            failed(new ApiFailure(ApiFailure.Kind.CANCELLED)); publish(); callback.onError();
                        }
                    }
                });
            } catch (Exception failure) {
                android.util.Log.e("TaktakQa", "Failed to download/decode image: " + momentId, failure);
                main.post(() -> {
                    if (!cleared) {
                        if (failure instanceof ApiFailure api && (api.status == 404 || api.status == 403
                                || api.kind == ApiFailure.Kind.UNAUTHORIZED || api.kind == ApiFailure.Kind.CANCELLED)) {
                            HistoryImageCache.remove(momentId);
                        }
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
            MomentRepository owner = null;
            try {
                if (repository == null) {
                    repository = AuthGraph.repository(getApplication()).moments();
                }
                owner = repository;
                result = action.execute(owner);
            } catch (IOException | RuntimeException failure) {
                result = () -> failed(failure);
            }
            Runnable completion = result;
            MomentRepository producer = owner;
            main.post(() -> {
                if (cleared) { return; }
                Runnable deliver = () -> { completion.run(); busy = false; publish(); };
                if (producer == null) { deliver.run(); }
                else if (!producer.publishIfCurrent(deliver)) {
                    failed(new ApiFailure(ApiFailure.Kind.CANCELLED)); busy = false; publish();
                }
            });
        });
    }

    private void failed(Exception failure) {
        if (failure instanceof ApiFailure api) {
            if (api.kind == ApiFailure.Kind.UNAUTHORIZED || api.kind == ApiFailure.Kind.CANCELLED) {
                receivedMoments.clear();
                sentMoments.clear();
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
        List<MomentItem> currentMoments = (activeTab == Tab.RECEIVED) ? receivedMoments : sentMoments;
        boolean currentLoaded = (activeTab == Tab.RECEIVED) ? receivedLoaded : sentLoaded;
        boolean currentHasNext = (activeTab == Tab.RECEIVED) ? receivedHasNext : sentHasNext;
        state.setValue(new State(activeTab, Collections.unmodifiableList(new ArrayList<>(currentMoments)),
                busy, currentLoaded, currentHasNext, expired, message));
    }

    @Override
    protected void onCleared() {
        cleared = true;
        com.taktak.app.data.push.PushEventBus.unregister(pushListener);
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
    }
}
