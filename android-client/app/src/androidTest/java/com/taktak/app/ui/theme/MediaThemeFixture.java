package com.taktak.app.ui.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.FriendModels;
import com.taktak.app.data.remote.*;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.session.*;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import retrofit2.Response;

/** Chỉ test: proxy Retrofit trong RAM, không HTTP/Internet, không dùng token hoặc account thật. */
final class MediaThemeFixture implements AutoCloseable {
    static final String FIRST = "00000000-0000-4000-8000-000000000011";
    static final String SECOND = "00000000-0000-4000-8000-000000000022";
    static final String MOMENT = "00000000-0000-4000-8000-000000000033";
    final byte[] jpeg;
    final AuthRepository auth;
    final AtomicInteger posts = new AtomicInteger(), deletes = new AtomicInteger();
    final List<String> keys = new CopyOnWriteArrayList<>(), captions = new CopyOnWriteArrayList<>(), recipients = new CopyOnWriteArrayList<>();
    volatile boolean denyImage, failFirstPost;
    volatile boolean mockProfile;
    volatile String profileDisplayName = "QA Profile User";
    final AtomicInteger avatarUploads = new AtomicInteger(), nameUpdates = new AtomicInteger();
    private final Field graph;
    private final Object previous;

    MediaThemeFixture(Context context) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(480,480,Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.rgb(190,115,170));
        try (var output = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG,85,output); jpeg=output.toByteArray();
        } finally { bitmap.recycle(); }
        SessionStore store = new SessionStore() {
            private Session value = new Session("qa.media.session","q".repeat(43),System.currentTimeMillis()+3600000,0);
            public Session load() { return value; }
            public void save(Session next) { value=next; }
            public void clear() { value=null; }
        };
        // Không request nào đi qua transport: thay cả ba interface dùng trong các màn QA.
        var client = new ApiClient("http://127.0.0.1:9/",store);
        replace(client,"users",UserApi.class);
        replace(client,"friends",FriendApi.class);
        replace(client,"moments",MomentApi.class);
        replace(client,"devices",DeviceApi.class);
        auth = new AuthRepository(client);
        graph=AuthGraph.class.getDeclaredField("repository"); graph.setAccessible(true);
        previous=graph.get(null); graph.set(null,auth);
    }
    private void replace(ApiClient client,String field,Class<?> api) throws Exception {
        Object proxy = Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(object,method,args) -> {
            if (method.getDeclaringClass()==Object.class) return "Media QA proxy";
            return call(() -> response(field,method.getName(),args));
        });
        Field target=ApiClient.class.getDeclaredField(field); target.setAccessible(true); target.set(client,proxy);
    }
    private Response<?> response(String api,String method,Object[] args) throws Exception {
        if (api.equals("users")) {
            if (!mockProfile) return error(503);
            if (method.equals("me")) {
                var user = new com.taktak.app.data.model.AuthModels.UserProfile();
                user.id = FIRST;
                user.email = "qa_profile@example.com";
                user.username = "qa_profile";
                user.displayName = profileDisplayName;
                user.createdAt = "2026-10-19T20:00:00Z";
                return Response.success(user);
            }
            if (method.equals("update")) {
                nameUpdates.incrementAndGet();
                var req = (com.taktak.app.data.model.AuthModels.UpdateProfileRequest) args[0];
                var f = com.taktak.app.data.model.AuthModels.UpdateProfileRequest.class.getDeclaredField("displayName");
                f.setAccessible(true);
                profileDisplayName = (String) f.get(req);
                var user = new com.taktak.app.data.model.AuthModels.UserProfile();
                user.id = FIRST;
                user.email = "qa_profile@example.com";
                user.username = "qa_profile";
                user.displayName = profileDisplayName;
                user.createdAt = "2026-10-19T20:00:00Z";
                return Response.success(user);
            }
            if (method.equals("uploadAvatar")) {
                avatarUploads.incrementAndGet();
                var user = new com.taktak.app.data.model.AuthModels.UserProfile();
                user.id = FIRST;
                user.email = "qa_profile@example.com";
                user.username = "qa_profile";
                user.displayName = profileDisplayName;
                user.createdAt = "2026-10-19T20:00:00Z";
                return Response.success(user);
            }
            if (method.equals("avatar")) {
                return Response.success(ResponseBody.create(jpeg, MediaType.get("image/jpeg")));
            }
            return error(503);
        }
        if (api.equals("friends") && method.equals("friends")) {
            var page = new FriendModels.Page<FriendModels.User>();
            var first = new FriendModels.User(); first.id=FIRST; first.username="qa_first"; first.displayName="QA First";
            var second = new FriendModels.User(); second.id=SECOND; second.username="qa_second"; second.displayName="QA Second";
            page.items=List.of(first,second); page.page=(Integer)args[0]; page.hasNext=false;
            return Response.success(page);
        }
        if (api.equals("moments") && method.equals("uploadToFriends")) {
            int attempt=posts.incrementAndGet(); keys.add((String)args[0]);
            var bytes = new okio.Buffer(); ((MultipartBody.Part)args[1]).body().writeTo(bytes);
            var caption = new okio.Buffer(); ((RequestBody)args[3]).writeTo(caption); captions.add(caption.readUtf8());
            var selected = new StringBuilder();
            for(Object value : (List<?>)args[2]) { var buffer=new okio.Buffer(); ((RequestBody)value).writeTo(buffer); selected.append(buffer.readUtf8()); }
            recipients.add(selected.toString());
            Thread.sleep(1300);
            if (failFirstPost && attempt==1) return error(503);
            var result=new MomentApi.CreatedMoment(); result.id=MOMENT; return Response.success(result);
        }
        if (api.equals("friends") && method.equals("incoming")) {
            var page = new FriendModels.Page<FriendModels.Invitation>();
            var invitation = new FriendModels.Invitation();
            invitation.id = "00000000-0000-4000-8000-0000000000aa";
            var sender = new FriendModels.User();
            sender.id = "00000000-0000-4000-8000-0000000000bb";
            sender.username = "qa_incoming";
            sender.displayName = "QA Incoming";
            invitation.sender = sender;
            invitation.receiverId = FIRST;
            invitation.status = "pending";
            invitation.createdAt = "2026-10-19T20:00:00Z";
            page.items = List.of(invitation); page.page = (Integer)args[0]; page.hasNext = false;
            return Response.success(page);
        }
        if (api.equals("friends") && method.equals("search")) {
            var searchUser = new FriendModels.User();
            searchUser.id = "00000000-0000-4000-8000-0000000000cc";
            searchUser.username = "qa_searched";
            searchUser.displayName = "QA Searched";
            return Response.success(List.of(searchUser));
        }
        if (api.equals("friends") && (method.equals("send") || method.equals("respond") || method.equals("unfriend") || method.equals("block"))) {
            return Response.success(null);
        }
        if (api.equals("moments") && method.equals("listMoments")) {
            var page = new com.taktak.app.data.model.MomentModels.MomentsPage();
            var item = new com.taktak.app.data.model.MomentModels.MomentItem();
            item.id = MOMENT;
            item.caption = "QA moment caption";
            item.createdAt = "2026-10-19T20:00:00Z";
            var sender = new com.taktak.app.data.model.MomentModels.PublicUser();
            sender.id = FIRST;
            sender.username = "qa_first";
            sender.displayName = "QA First";
            item.sender = sender;
            page.items = List.of(item);
            page.page = (Integer)args[1];
            page.hasNext = false;
            return Response.success(page);
        }
        if (api.equals("moments") && method.equals("image")) {
            return denyImage ? error(403) : Response.success(ResponseBody.create(jpeg,MediaType.get("image/jpeg")));
        }
        if (api.equals("moments") && method.equals("deleteMoment")) deletes.incrementAndGet();
        if (api.equals("devices")) return Response.success(null);
        return error(503);
    }
    private Response<?> error(int code) { return Response.error(code,ResponseBody.create("{}",MediaType.get("application/json"))); }
    private interface Result { Response<?> get() throws Exception; }
    private Object call(Result response) {
        return Proxy.newProxyInstance(retrofit2.Call.class.getClassLoader(),new Class<?>[]{retrofit2.Call.class},(object,method,args) -> switch(method.getName()) {
            case "execute" -> response.get();
            case "clone" -> call(response);
            case "isExecuted","isCanceled" -> false;
            case "cancel" -> null;
            case "timeout" -> new okio.Timeout();
            case "request" -> new Request.Builder().url("http://127.0.0.1:9/qa").build();
            case "toString" -> "Media QA call";
            default -> throw new AssertionError("Unexpected call operation: "+method.getName());
        });
    }
    @Override public void close() throws Exception {
        graph.set(null,previous);
        com.taktak.app.ui.history.HistoryImageCache.remove(MOMENT);
    }
}
