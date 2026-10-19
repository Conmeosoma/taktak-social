package com.taktak.app.ui.theme;

import android.content.Context;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels;
import com.taktak.app.data.model.ChatModels;
import com.taktak.app.data.model.FriendModels;
import com.taktak.app.data.remote.*;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.session.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import retrofit2.Response;

/** Proxy RAM độc lập, không Internet/DB và không dùng phiên cá nhân của điện thoại. */
final class ChatThemeFixture implements AutoCloseable {
    static final String SELF="00000000-0000-4000-8000-000000000011";
    static final String PEER="00000000-0000-4000-8000-000000000022";
    static final String ROOM="00000000-0000-4000-8000-000000000033";
    final ApiClient client;
    final AuthRepository auth;
    final List<ChatModels.Message> messages=new CopyOnWriteArrayList<>();
    final List<ChatModels.Send> payloads=new CopyOnWriteArrayList<>();
    final AtomicInteger inboxCalls=new AtomicInteger(), reads=new AtomicInteger(), opens=new AtomicInteger();
    volatile boolean profileEnabled, emptyInbox, failNextSend, denyImage;
    static final String MOMENT="00000000-0000-4000-8000-000000000044";
    private final byte[] jpeg;
    private final Field graph;
    private final Object previous;
    ChatThemeFixture(Context context) throws Exception {
        var bitmap=android.graphics.Bitmap.createBitmap(240,240,android.graphics.Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.rgb(135,105,190));
        try(var output=new java.io.ByteArrayOutputStream()){bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,85,output);jpeg=output.toByteArray();}finally{bitmap.recycle();}
        var memory=new SessionStore(){
            Session value=new Session("qa.chat.session","q".repeat(43),System.currentTimeMillis()+3600000,0);
            public Session load(){return value;}
            public void save(Session next){value=next;}
            public void clear(){value=null;}
        };
        client=new ApiClient("http://127.0.0.1:9/",memory);
        replace("users",UserApi.class);replace("friends",FriendApi.class);replace("chat",ChatApi.class);
        replace("moments",MomentApi.class);replace("devices",DeviceApi.class);
        auth=new AuthRepository(client);
        graph=AuthGraph.class.getDeclaredField("repository");graph.setAccessible(true);
        previous=graph.get(null);graph.set(null,auth);
    }
    private void replace(String field,Class<?> api) throws Exception {
        var proxy=Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(target,method,args)->{
            if(method.getDeclaringClass()==Object.class)return "Chat QA proxy";
            return call(()->response(field,method.getName(),args));
        });
        var declared=ApiClient.class.getDeclaredField(field);declared.setAccessible(true);declared.set(client,proxy);
    }
    ChatModels.Conversation room(){
        var room=new ChatModels.Conversation();room.id=ROOM;room.updatedAt="2026-10-20T00:00:00Z";room.unreadCount=1;
        var peer=new ChatModels.Peer();peer.id=PEER;peer.username="qa_chat_friend";peer.displayName="QA Chat Friend";room.peer=peer;
        if(!messages.isEmpty())room.lastMessage=messages.get(messages.size()-1);
        return room;
    }
    private Response<?> response(String api,String method,Object[] args) throws Exception {
        if(api.equals("users")&&method.equals("me")){
            if(!profileEnabled)return error(503);
            // Main kiểm tra lại khi quay từ Chat; vẫn chặn restore trước FCM/widget thật.
            for(StackTraceElement frame:Thread.currentThread().getStackTrace())
                if(frame.getClassName().equals("com.taktak.app.viewmodel.SessionViewModel"))return error(503);
            var profile=new AuthModels.UserProfile();profile.id=SELF;profile.username="qa_chat_self";
            profile.email="qa_chat@example.com";profile.displayName="QA Chat Self";profile.createdAt="2026-10-20T00:00:00Z";
            return Response.success(profile);
        }
        if(api.equals("friends")&&method.equals("friends")){
            var user=new FriendModels.User();user.id=PEER;user.username="qa_chat_friend";user.displayName="QA Chat Friend";
            var page=new FriendModels.Page<FriendModels.User>();page.page=(Integer)args[0];page.items=List.of(user);return Response.success(page);
        }
        if(api.equals("chat")){
            switch(method){
                case "inbox":{inboxCalls.incrementAndGet();var page=new ChatModels.Page<ChatModels.Conversation>();page.items=emptyInbox?List.of():List.of(room());page.totalUnreadCount=emptyInbox?0:1;return Response.success(page);}
                case "conversation":return Response.success(room());
                case "withFriend":opens.incrementAndGet();return Response.success(room());
                case "messages":{var page=new ChatModels.Page<ChatModels.Message>();page.items=new ArrayList<>(messages);return Response.success(page);}
                case "read":reads.incrementAndGet();return Response.success(204,null);
                case "send":{
                    var payload=(ChatModels.Send)args[1];payloads.add(payload);
                    Thread.sleep(400);
                    if(failNextSend){failNextSend=false;return error(503);}
                    for(var item:messages)if(item.clientMessageId.equals(payload.clientMessageId))return Response.success(item);
                    var item=new ChatModels.Message();item.id=UUID.randomUUID().toString();item.conversationId=ROOM;
                    item.senderId=SELF;item.recipientId=PEER;item.clientMessageId=payload.clientMessageId;
                    item.messageType=payload.messageType;item.content=payload.content;item.momentId=payload.momentId;item.createdAt=java.time.Instant.now().toString();
                    messages.add(item);return Response.success(item);
                }
            }
        }
        if(api.equals("moments")&&method.equals("listMoments")){
            var item=new com.taktak.app.data.model.MomentModels.MomentItem();item.id=MOMENT;item.caption="QA private moment";item.createdAt="2026-10-20T00:00:00Z";
            var user=new com.taktak.app.data.model.MomentModels.PublicUser();user.id=SELF;user.username="qa_chat_self";user.displayName="QA Chat Self";item.sender=user;
            var page=new com.taktak.app.data.model.MomentModels.MomentsPage();page.page=(Integer)args[1];page.items=List.of(item);return Response.success(page);
        }
        if(api.equals("moments")&&method.equals("image"))return denyImage?error(403):Response.success(ResponseBody.create(jpeg,MediaType.get("image/jpeg")));
        if(api.equals("devices"))return Response.success(null);
        return error(503);
    }
    private Response<?> error(int status){return Response.error(status,ResponseBody.create("{}",MediaType.get("application/json")));}
    private interface Result{Response<?> get()throws Exception;}
    private Object call(Result response){return Proxy.newProxyInstance(retrofit2.Call.class.getClassLoader(),new Class<?>[]{retrofit2.Call.class},(target,method,args)->switch(method.getName()){
        case "execute"->response.get();case "clone"->call(response);case "isExecuted","isCanceled"->false;
        case "cancel"->null;case "timeout"->new okio.Timeout();
        case "request"->new Request.Builder().url("http://127.0.0.1:9/qa").build();
        case "toString"->"Chat QA call";default->throw new AssertionError(method.getName());
    });}
    @Override public void close()throws Exception{graph.set(null,previous);}
}
