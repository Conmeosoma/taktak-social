package com.taktak.app.ui.qr;

import android.content.Context;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels;
import com.taktak.app.data.model.FriendModels;
import com.taktak.app.data.remote.*;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.session.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import retrofit2.Response;

/** Chặn mọi API bằng RAM; không đọc/ghi phiên cá nhân hay gửi lời mời thật. */
final class QrFixture implements AutoCloseable {
    static final String SELF="00000000-0000-4000-8000-000000000011", PEER="00000000-0000-4000-8000-000000000022";
    static final String GALLERY="00000000-0000-4000-8000-000000000044";
    final ApiClient client;
    final AtomicInteger sends=new AtomicInteger(), pages=new AtomicInteger();
    volatile boolean offline, missing, friendOnSecondPage;
    private final Field graph;
    private final Object previous;
    QrFixture(Context context) throws Exception {
        var memory=new SessionStore(){
            Session value=new Session("qa.qr.session","q".repeat(43),System.currentTimeMillis()+3600000,0);
            public Session load(){return value;} public void save(Session next){value=next;} public void clear(){value=null;}
        };
        client=new ApiClient("http://127.0.0.1:9/",memory);
        replace("users",UserApi.class);replace("friends",FriendApi.class);replace("devices",DeviceApi.class);
        graph=AuthGraph.class.getDeclaredField("repository");graph.setAccessible(true);
        previous=graph.get(null);graph.set(null,new AuthRepository(client));
    }
    private void replace(String field,Class<?> api)throws Exception {
        Object proxy=Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(target,method,args)->call(()->response(field,method.getName(),args)));
        Field declared=ApiClient.class.getDeclaredField(field);declared.setAccessible(true);declared.set(client,proxy);
    }
    private FriendModels.User user(){var user=new FriendModels.User();user.id=PEER;user.username="qa_qr_alice";user.displayName="QA QR Alice";return user;}
    private Response<?> response(String api,String method,Object[] args)throws Exception {
        if(offline)throw new java.io.IOException("QA offline");
        if(api.equals("users")&&method.equals("me")){
            for(var frame:Thread.currentThread().getStackTrace())if(frame.getClassName().equals("com.taktak.app.viewmodel.SessionViewModel"))return error();
            var me=new AuthModels.UserProfile();me.id=SELF;me.username="qa_qr_self";me.displayName="QA QR Self";me.email="qr@example.invalid";me.createdAt="2026-10-20T00:00:00Z";return Response.success(me);
        }
        if(api.equals("friends")){
            switch(method){
                case "search":{
                    var found=user();
                    if("qa_qr_gallery".equals(args[0])){found.id=GALLERY;found.username="qa_qr_gallery";found.displayName="QA QR Gallery";}
                    return Response.success(missing?List.of():List.of(found));
                }
                case "friends":{
                    pages.incrementAndGet();var result=new FriendModels.Page<FriendModels.User>();result.page=(Integer)args[0];
                    result.items=friendOnSecondPage&&result.page==1?List.of(user()):List.of();result.hasNext=friendOnSecondPage&&result.page==0;return Response.success(result);
                }
                case "send":{
                    sends.incrementAndGet();var invitation=new FriendModels.Invitation();invitation.id=UUID.randomUUID().toString();
                    var sender=new FriendModels.User();sender.id=SELF;sender.username="qa_qr_self";sender.displayName="QA QR Self";
                    invitation.sender=sender;invitation.receiverId=PEER;invitation.status="pending";invitation.createdAt="2026-10-20T00:00:00Z";return Response.success(invitation);
                }
            }
        }
        return error();
    }
    private Response<?> error(){return Response.error(503,ResponseBody.create("{}",MediaType.get("application/json")));}
    private interface Result{Response<?> get()throws Exception;}
    private Object call(Result response){return Proxy.newProxyInstance(retrofit2.Call.class.getClassLoader(),new Class<?>[]{retrofit2.Call.class},(target,method,args)->switch(method.getName()){
        case "execute"->response.get();case "clone"->call(response);case "isExecuted","isCanceled"->false;case "cancel"->null;case "timeout"->new okio.Timeout();
        case "request"->new Request.Builder().url("http://127.0.0.1:9/qa").build();default->"QR QA call";
    });}
    @Override public void close()throws Exception{graph.set(null,previous);}
}
