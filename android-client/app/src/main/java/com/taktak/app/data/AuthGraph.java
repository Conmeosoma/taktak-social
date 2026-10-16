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
                repository = new AuthRepository(new ApiClient(BuildConfig.BASE_URL,
                        new EncryptedSessionStore(context.getApplicationContext(), BuildConfig.BASE_URL)));
            } catch (IOException failure) { throw new ApiFailure(ApiFailure.Kind.STORAGE); }
        }
        return repository;
    }
}
