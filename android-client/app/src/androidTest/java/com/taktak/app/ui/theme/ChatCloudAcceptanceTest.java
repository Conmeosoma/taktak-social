package com.taktak.app.ui.theme;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.ChatModels;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.repository.DeviceRepository;
import com.taktak.app.data.session.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** FCM cloud thật tới project Firebase đã cấu hình, account/DB QA riêng qua adb reverse. */
@RunWith(AndroidJUnit4.class)
public class ChatCloudAcceptanceTest {
    @Rule public final org.junit.rules.Timeout timeout=org.junit.rules.Timeout.seconds(180);
    private static final class Memory implements SessionStore {
        private Session value;
        public Session load(){return value;}
        public void save(Session next){value=next;}
        public void clear(){value=null;}
    }
    @Test public void realCloudDataMessageReachesServiceAndProtectedReceiverInbox()throws Exception{
        var arguments=InstrumentationRegistry.getArguments();
        assertEquals("Explicit cloud QA opt-in","true",arguments.getString("taktak.allowChatCloud"));
        String base=arguments.getString("taktak.chatBaseUrl");
        assertNotNull("Dedicated adb reverse QA URL required",base);
        assertTrue("Only dedicated loopback QA backend allowed",base.matches("http://127\\.0\\.0\\.1:[1-9][0-9]{3,4}/"));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File sessionFile=new File(context.getApplicationInfo().dataDir,"shared_prefs/auth_session_v1.xml");
        byte[] original=sessionFile.exists()?java.nio.file.Files.readAllBytes(sessionFile.toPath()):null;
        var senderClient=new ApiClient(base,new Memory());var receiverClient=new ApiClient(base,new Memory());
        var sender=new AuthRepository(senderClient);var receiver=new AuthRepository(receiverClient);
        DeviceRepository device=null;String roomId=null;
        Field graph=AuthGraph.class.getDeclaredField("repository");graph.setAccessible(true);Object previous=graph.get(null);
        var receivedRoom=new AtomicReference<String>();var receivedMessage=new AtomicReference<String>();
        var expectedRoom=new AtomicReference<String>();
        var delivered=new CountDownLatch(1);
        PushEventBus.Listener listener=new PushEventBus.Listener(){
            public void onMomentCreated(String id) { }
            public void onMomentDeleted(String id) { }
            public void onChatChanged(String room,String message){if(room.equals(expectedRoom.get())){receivedRoom.set(room);receivedMessage.set(message);delivered.countDown();}}
        };
        try{
            String suffix=UUID.randomUUID().toString().replace("-","");
            String password="Qa!"+UUID.randomUUID();
            var senderProfile=sender.register("qa_sender_"+suffix+"@example.test","qa_sender_"+suffix,"QA Cloud Sender",password);
            var receiverProfile=receiver.register("qa_receiver_"+suffix+"@example.test","qa_receiver_"+suffix,"QA Cloud Receiver",password);
            sender.login(senderProfile.email,password);receiver.login(receiverProfile.email,password);
            sender.friends().send(receiverProfile.id);
            var requests=receiver.friends().incoming(0);
            var invitation=requests.items.stream().filter(item->senderProfile.id.equals(item.sender.id)).findFirst().orElseThrow();
            receiver.friends().respond(invitation.id,true);
            graph.set(null,receiver);
            String deviceId=UUID.randomUUID().toString();device=new DeviceRepository(receiverClient,()->deviceId);
            String token=Tasks.await(FirebaseMessaging.getInstance().getToken(),45,TimeUnit.SECONDS);
            assertNotNull("Cloud registration token received without logging its value",token);device.register(token);
            var room=sender.chat().withFriend(receiverProfile.id);roomId=room.id;
            expectedRoom.set(roomId);
            PushEventBus.register(listener);
            var payload=new ChatModels.Send(UUID.randomUUID().toString(),"TEXT","QA cloud delivery "+suffix,null);
            var sent=sender.chat().send(roomId,payload);
            assertTrue("Actual FCM service callback must arrive (no synthetic event emit)",delivered.await(75,TimeUnit.SECONDS));
            assertEquals(roomId,receivedRoom.get());assertEquals(sent.id,receivedMessage.get());
            var page=receiver.chat().messages(roomId,0);
            assertTrue("Protected GET confirms recipient data",page.items.stream().anyMatch(item->sent.id.equals(item.id)&&payload.content.equals(item.content)));
            assertEquals("Idempotent retry returns same persisted message",sent.id,sender.chat().send(roomId,payload).id);
            boolean permitted=android.os.Build.VERSION.SDK_INT<33||context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==PackageManager.PERMISSION_GRANTED;
            if(permitted){
                NotificationManager manager=context.getSystemService(NotificationManager.class);
                android.service.notification.StatusBarNotification actual=null;
                long until=android.os.SystemClock.uptimeMillis()+6000;
                while(actual==null&&android.os.SystemClock.uptimeMillis()<until){
                    for(var item:manager.getActiveNotifications())if(("chat."+roomId).equals(item.getTag()))actual=item;
                    if(actual==null)android.os.SystemClock.sleep(100);
                }
                assertNotNull("Generic background notification for QA room",actual);
                assertEquals(context.getString(R.string.chat_notification),actual.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString());
                assertFalse(actual.getNotification().extras.toString().contains(payload.content));
                System.out.println("CHAT_CLOUD: service delivery, recipient GET, idempotency and generic background notification verified.");
            }else System.out.println("CHAT_CLOUD: service delivery, recipient GET and idempotency verified; notification permission denied, notification display not claimed.");
        }finally{
            PushEventBus.unregister(listener);
            if(roomId!=null)context.getSystemService(NotificationManager.class).cancel("chat."+roomId,1);
            if(device!=null)device.unregister();
            try{if(sender.hasSession())sender.logout();}finally{try{if(receiver.hasSession())receiver.logout();}finally{graph.set(null,previous);}}
            assertArrayEquals("Personal encrypted session bytes unchanged",original,sessionFile.exists()?java.nio.file.Files.readAllBytes(sessionFile.toPath()):null);
        }
    }
}
