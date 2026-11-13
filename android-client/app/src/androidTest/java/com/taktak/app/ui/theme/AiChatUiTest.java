package com.taktak.app.ui.theme;

import android.content.*;
import android.widget.EditText;
import androidx.lifecycle.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.taktak.app.*;
import com.taktak.app.data.model.AiModels.*;
import com.taktak.app.data.remote.*;
import com.taktak.app.ui.ai.AiFragment;
import com.taktak.app.viewmodel.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import retrofit2.Response;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.click;

/** API fixture RAM, không thay phiên lưu trên máy và không gọi Gemini thật. */
@RunWith(AndroidJUnit4.class)
public class AiChatUiTest {
    private MainActivity activity;
    private ChatThemeFixture fixture;
    private AiViewModel model;
    private final List<Send> sends = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile boolean fail = true, deleted;
    private static final String ID="00000000-0000-4000-8000-000000000077";
    private static final String TIME="2026-11-13T00:00:00Z";
    @Test public void consentRetryHistoryDeleteAndSessionWipe() throws Exception {
        var inst=InstrumentationRegistry.getInstrumentation(); var context=inst.getTargetContext();
        try {
            fixture=new ChatThemeFixture(context);
            var proxy=Proxy.newProxyInstance(AiApi.class.getClassLoader(),new Class<?>[]{AiApi.class},(o,m,a)->call(()->{
                switch(m.getName()) {
                    case "status": { var s=new Status();s.availability="CONFIGURED";s.privacyNotice="QA Google";return Response.success(s); }
                    case "conversations": { var p=new Page<Conversation>();p.items=List.of();return Response.success(p); }
                    case "send": {
                        sends.add((Send)a[0]); if(fail){fail=false;return Response.error(503,okhttp3.ResponseBody.create("{}",okhttp3.MediaType.get("application/json")));}
                        var msg=new Message();msg.id=UUID.randomUUID().toString();msg.conversationId=ID;msg.createdAt=TIME;msg.role="assistant";msg.content="QA: Xin chào!";return Response.success(msg);
                    }
                    case "delete": deleted=true;return Response.success(204,null);
                    default: throw new AssertionError(m.getName());
                }
            }));
            var field=ApiClient.class.getField("ai");field.setAccessible(true);field.set(fixture.client,proxy);
            // Khởi chạy qua shell: MIUI chặn startActivitySync từ test đang ở background.
            try(var launch=inst.getUiAutomation().executeShellCommand("am start -n com.taktak.app/.MainActivity --ez qa true")) {
                try(var input=new java.io.FileInputStream(launch.getFileDescriptor())) { input.readAllBytes(); }
            }
            await(()->{main(()->{for(var screen:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) if(screen instanceof MainActivity found) activity=found;});return activity!=null;});
            await(()->{var value=new AtomicReference<SessionViewModel.State>();main(()->value.set(new ViewModelProvider(activity).get(SessionViewModel.class).state().getValue()));return value.get()!=null&&value.get().destination==SessionViewModel.Destination.ERROR;});
            main(()->((MutableLiveData<SessionViewModel.State>)new ViewModelProvider(activity).get(SessionViewModel.class).state()).setValue(new SessionViewModel.State(SessionViewModel.Destination.CAMERA,0)));
            main(()->((BottomNavigationView)activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_messages));
            await(()->visible(R.id.messagesAi)); main(()->activity.findViewById(R.id.messagesAi).performClick());
            await(()->visible(R.id.aiNew));
            main(()->{var parent=activity.getSupportFragmentManager().getFragments().stream().filter(f->f instanceof com.taktak.app.ui.chat.MessagesFragment).findFirst().orElseThrow();var ai=(AiFragment)parent.getChildFragmentManager().findFragmentById(R.id.messages_ai_container);model=new ViewModelProvider(ai).get(AiViewModel.class);});
            await(()->ready()); screenshot("ai-usb-empty.png");
            main(()->activity.findViewById(R.id.aiNew).performClick());await(()->ready());
            main(()->((EditText)activity.findViewById(R.id.aiInput)).setText("QA hello"));
            main(()->activity.findViewById(R.id.aiSend).performClick());onView(withText(android.R.string.cancel)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());assertEquals(0,sends.size());
            inst.waitForIdleSync();
            main(()->activity.findViewById(R.id.aiSend).performClick());onView(withText(R.string.ai_agree)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
            await(()->ready()&&sends.size()==1);assertTrue(state().pending); screenshot("ai-usb-retry.png");
            main(()->activity.findViewById(R.id.aiRetry).performClick());await(()->ready()&&sends.size()==2);
            assertEquals(sends.get(0).clientRequestId,sends.get(1).clientRequestId);assertNull(sends.get(1).conversationId);
            assertEquals(2,state().messages.size());assertFalse(state().pending);screenshot("ai-usb-chat.png");
            main(()->model.delete(ID));await(()->ready()&&deleted);
            main(()->com.taktak.app.data.push.PushEventBus.emitSessionCleared());await(()->state().expired);
            assertTrue(state().messages.isEmpty());assertEquals("",state().draft);
        } finally { if(activity!=null)main(()->activity.finish());inst.waitForIdleSync();if(fixture!=null)fixture.close(); }
    }
    private AiViewModel.State state(){var r=new AtomicReference<AiViewModel.State>();main(()->r.set(model.state().getValue()));return r.get();}
    private boolean ready(){return model!=null&&!state().busy;}
    private boolean visible(int id){var r=new java.util.concurrent.atomic.AtomicBoolean();main(()->{var v=activity.findViewById(id);r.set(v!=null&&v.isShown());});return r.get();}
    private interface Result {Response<?> get() throws Exception;}
    private Object call(Result result){return Proxy.newProxyInstance(retrofit2.Call.class.getClassLoader(),new Class<?>[]{retrofit2.Call.class},(o,m,a)->switch(m.getName()){
        case "execute"->result.get();case "clone"->call(result);case "isExecuted","isCanceled"->false;case "cancel"->null;case "timeout"->new okio.Timeout();
        case "request"->new okhttp3.Request.Builder().url("http://127.0.0.1:9/qa").build();default->"QA call";
    });}
    private void main(Runnable task){InstrumentationRegistry.getInstrumentation().runOnMainSync(task);}
    private void await(BooleanSupplier test)throws Exception{long end=System.currentTimeMillis()+20000;while(System.currentTimeMillis()<end){if(test.getAsBoolean())return;Thread.sleep(80);}throw new AssertionError("UI condition timed out");}
    private void screenshot(String name)throws Exception{var inst=InstrumentationRegistry.getInstrumentation();inst.waitForIdleSync();var bitmap=inst.getUiAutomation().takeScreenshot();assertNotNull(bitmap);var dir=new java.io.File(inst.getTargetContext().getExternalFilesDir(null),"artifacts");dir.mkdirs();try(var out=new java.io.FileOutputStream(new java.io.File(dir,name))){assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}}
}
