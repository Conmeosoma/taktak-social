package com.taktak.app.data.remote;

import com.taktak.app.data.session.Session;
import com.taktak.app.data.session.SessionManager;
import com.taktak.app.data.session.SessionStore;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    public final AuthApi auth;
    public final UserApi users;
    public final MomentApi moments;
    public final FriendApi friends;
    public final SessionManager sessions;
    private final OkHttpClient apiHttp;
    private final HttpUrl base;

    public ApiClient(String baseUrl, SessionStore store) throws IOException {
        base = HttpUrl.get(baseUrl);
        OkHttpClient raw = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS)
                .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
        auth = retrofit(baseUrl, raw).create(AuthApi.class);
        sessions = new SessionManager(store, auth);
        apiHttp = raw.newBuilder().addInterceptor(chain -> {
            Request request = chain.request();
            if (!protectedTarget(request.url())) {
                return chain.proceed(request.newBuilder().removeHeader("Authorization").tag(Session.class, null).build());
            }
            Session session = sessions.snapshot();
            Request.Builder authorized = request.newBuilder().removeHeader("Authorization").tag(Session.class, session);
            if (session != null) { authorized.header("Authorization", "Bearer " + session.accessToken); }
            Response response = chain.proceed(authorized.build());
            Session now = sessions.snapshot();
            if (response.code() != 401 && session != null && (now == null || now.generation != session.generation)) {
                response.close();
                throw new ApiFailure(ApiFailure.Kind.CANCELLED);
            }
            return response;
        }).authenticator((route, response) -> {
            if (!protectedTarget(response.request().url())) { return null; }
            Session sent = response.request().tag(Session.class);
            if (response.priorResponse() != null) { sessions.clearIfCurrent(sent); return null; }
            Session next = sessions.refresh(sent);
            if (next == null) { return null; }
            return response.request().newBuilder().header("Authorization", "Bearer " + next.accessToken)
                    .tag(Session.class, next).build();
        }).build();
        users = retrofit(baseUrl, apiHttp).create(UserApi.class);
        moments = retrofit(baseUrl, apiHttp).create(MomentApi.class);
        friends = retrofit(baseUrl, apiHttp).create(FriendApi.class);
    }

    private boolean protectedTarget(HttpUrl target) {
        String prefix = base.encodedPath() + "api/";
        return target.scheme().equals(base.scheme()) && target.host().equals(base.host()) && target.port() == base.port()
                && target.encodedPath().startsWith(prefix) && !target.encodedPath().startsWith(prefix + "auth/");
    }

    public Response execute(Request request) throws IOException { return apiHttp.newCall(request).execute(); }

    private Retrofit retrofit(String baseUrl, OkHttpClient http) {
        return new Retrofit.Builder().baseUrl(baseUrl).client(http).addConverterFactory(GsonConverterFactory.create()).build();
    }
}
