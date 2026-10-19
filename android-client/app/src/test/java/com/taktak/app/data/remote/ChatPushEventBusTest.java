package com.taktak.app.data.remote;

import com.taktak.app.data.push.PushEventBus;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatPushEventBusTest {
    private static final String ROOM="00000000-0000-4000-8000-000000000011";
    private static final String MESSAGE="00000000-0000-4000-8000-000000000022";
    private abstract static class Listener implements PushEventBus.Listener {
        public void onMomentCreated(String id) { }
        public void onMomentDeleted(String id) { }
    }
    @Test public void invalidChatIdentifiersNeverReachListeners(){
        var calls=new AtomicInteger();var listener=new Listener(){public void onChatChanged(String room,String message){calls.incrementAndGet();}};
        PushEventBus.register(listener);
        try{
            PushEventBus.emitChatChanged(null,MESSAGE);PushEventBus.emitChatChanged(ROOM,null);
            PushEventBus.emitChatChanged("../private",MESSAGE);PushEventBus.emitChatChanged(ROOM,"private-message-content");
            assertEquals(0,calls.get());PushEventBus.emitChatChanged(ROOM,MESSAGE);assertEquals(1,calls.get());
        }finally{PushEventBus.unregister(listener);}
    }
    @Test public void brokenListenerDoesNotBlockRefreshAndUnregisterStopsDelivery(){
        var calls=new AtomicInteger();var broken=new Listener(){public void onChatChanged(String room,String message){throw new IllegalStateException("QA listener");}};
        var good=new Listener(){public void onChatChanged(String room,String message){assertEquals(ROOM,room);assertEquals(MESSAGE,message);calls.incrementAndGet();}};
        PushEventBus.register(broken);PushEventBus.register(good);PushEventBus.register(good);
        try{
            PushEventBus.emitChatChanged(ROOM,MESSAGE);assertEquals(1,calls.get());
            PushEventBus.unregister(good);PushEventBus.emitChatChanged(ROOM,MESSAGE);assertEquals(1,calls.get());
        }finally{PushEventBus.unregister(broken);PushEventBus.unregister(good);}
    }
}
