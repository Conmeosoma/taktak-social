package com.taktak.app.data;

import android.content.Context;
import com.taktak.app.BuildConfig;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.session.EncryptedSessionStore;
import java.io.IOException;
import com.taktak.app.data.remote.ApiFailure;

public final class AuthGraph {
    private static AuthRepository repository;
    private AuthGraph() { }

    public static synchronized AuthRepository repository(Context context) throws IOException {
        if (repository == null) {
            try {
                Context app = context.getApplicationContext();
                var client = new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(app, BuildConfig.BASE_URL));
                repository = createRepository(app, client);
            } catch (IOException failure) { throw new ApiFailure(ApiFailure.Kind.STORAGE); }
        }
        return repository;
    }

    /** Dùng chung cleanup phiên cho graph thật và transport QA được inject. */
    public static AuthRepository createRepository(Context context, ApiClient client) {
        Context app=context.getApplicationContext();
        client.sessions.setOnCleared(() -> {
            com.taktak.app.ui.history.HistoryImageCache.clear();
            com.taktak.app.data.push.PushEventBus.emitSessionCleared();
            com.taktak.app.data.push.ChatNotifications.clear(app);
            com.taktak.app.widget.WidgetUpdater.clearForLogout(app);
        });
        return new AuthRepository(client);
    }
}
