package com.taktak.app.ui.theme;

import android.app.Activity;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.taktak.app.*;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.ui.chat.*;
import com.taktak.app.viewmodel.ChatViewModel;
import com.taktak.app.viewmodel.SessionViewModel;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** CHAT-02..05: thiết bị USB, fixture RAM và khôi phục appearance/phiên cá nhân. */
@RunWith(AndroidJUnit4.class)
public class ChatThemeUiTest {
    @Rule public final org.junit.rules.Timeout timeout=org.junit.rules.Timeout.seconds(360);
    private Context context;
    private ChatThemeFixture fixture;
    private Map<String,?> preferences;
    private File sessionFile;
    private byte[] sessionBytes;
    @Before public void setup()throws Exception{
        assertEquals("Explicit QA opt-in required","true",InstrumentationRegistry.getArguments().getString("taktak.allowChatQa"));
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        preferences=new HashMap<>(context.getSharedPreferences(ThemePreferences.FILE_NAME,0).getAll());
        sessionFile=new File(context.getApplicationInfo().dataDir,"shared_prefs/auth_session_v1.xml");
        sessionBytes=sessionFile.exists()?java.nio.file.Files.readAllBytes(sessionFile.toPath()):null;
        fixture=new ChatThemeFixture(context);
        // Loại Activity còn lại từ lượt QA trước sau khi đã cô lập AuthGraph.
        closeScreens();
    }
    @After public void cleanup()throws Exception{
        closeScreens();if(fixture!=null)fixture.close();
        if(preferences!=null){
            var edit=context.getSharedPreferences(ThemePreferences.FILE_NAME,0).edit();
            for(String key:new String[]{ThemePreferences.STYLE,ThemePreferences.MODE,ThemePreferences.SCHEMA,ThemePreferences.REVISION}){
                Object value=preferences.get(key);
                if(!preferences.containsKey(key))edit.remove(key);
                else if(value instanceof String s)edit.putString(key,s);
                else if(value instanceof Integer n)edit.putInt(key,n);
                else if(value instanceof Long n)edit.putLong(key,n);
                else throw new AssertionError("Unexpected appearance type");
            }
            assertTrue(edit.commit());assertEquals(preferences,context.getSharedPreferences(ThemePreferences.FILE_NAME,0).getAll());
            assertArrayEquals("Personal encrypted session must remain unchanged",sessionBytes,sessionFile.exists()?java.nio.file.Files.readAllBytes(sessionFile.toPath()):null);
        }
    }
    @Test public void allFourThemesDayNightLayoutsFitNarrowAndLandscape(){
        main(()->{
            for(AppTheme style:AppTheme.values())for(int night:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES})
                for(float font:new float[]{1f,2f})for(int[] size:new int[][]{{280,560},{360,720},{840,420}}){
                    var configuration=new Configuration(context.getResources().getConfiguration());configuration.fontScale=font;
                    configuration.uiMode=(configuration.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|night;
                    var themed=new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(configuration),ThemeManager.resolve(new ThemeSelection(style,ColorMode.SYSTEM,0),ThemeManager.ScreenRole.MAIN).themeResource());
                    float density=themed.getResources().getDisplayMetrics().density;int width=Math.round(size[0]*density),height=Math.round(size[1]*density);
                    for(int layout:new int[]{R.layout.fragment_messages,R.layout.activity_new_chat,R.layout.activity_chat,R.layout.item_conversation}){
                        View root=LayoutInflater.from(themed).inflate(layout,null,false);
                        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,layout==R.layout.item_conversation?View.MeasureSpec.UNSPECIFIED:View.MeasureSpec.EXACTLY));
                        root.layout(0,0,width,root.getMeasuredHeight());bounds(root,width);
                        if(layout==R.layout.activity_chat){
                            var back=(com.google.android.material.button.MaterialButton)root.findViewById(R.id.chatBack);
                            assertNotNull("Back uses a vector icon",back.getIcon());
                            assertEquals("Back must not render a clipped glyph","",back.getText().toString());
                            int surface=com.google.android.material.color.MaterialColors.getColor(root,
                                    com.google.android.material.R.attr.colorSurface);
                            var field=(com.google.android.material.textfield.TextInputLayout)root.findViewById(R.id.chatInputLayout);
                            assertTrue("Composer hint contrast >= 4.5:1",
                                    androidx.core.graphics.ColorUtils.calculateContrast(field.getDefaultHintTextColor().getDefaultColor(),surface)>=4.5);
                            View row=LayoutInflater.from(themed).inflate(R.layout.item_chat_message,null,false);
                            var receipt=(TextView)row.findViewById(R.id.messageReceipt);
                            var plate=(android.graphics.drawable.GradientDrawable)receipt.getBackground();
                            assertNotNull("Receipt has an opaque reading surface",plate.getColor());
                            assertEquals(255,android.graphics.Color.alpha(plate.getColor().getDefaultColor()));
                            assertTrue("Receipt contrast >= 4.5:1",androidx.core.graphics.ColorUtils.calculateContrast(
                                    receipt.getCurrentTextColor(),plate.getColor().getDefaultColor())>=4.5);
                        }
                    }
                }
        });
    }
    @Test public void inboxEmptySearchFiveTabsAndNewFriendConversationAcrossFourThemes()throws Exception{
        for(AppTheme theme:AppTheme.values()){
            new ThemePreferences(context).save(theme,ColorMode.LIGHT);fixture.emptyInbox=true;
            MainActivity activity=launchMain();var model=model(activity);
            main(()->((BottomNavigationView)activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_messages));
            await(()->state(model).loaded&&!state(model).busy,"Inbox fixture loaded");
            main(()->{
                assertEquals(5,((BottomNavigationView)activity.findViewById(R.id.bottomNavigation)).getMenu().size());
                assertTrue(activity.findViewById(R.id.messagesEmpty).isShown());
                activity.findViewById(R.id.messagesFindFriends).performClick();
                assertEquals(R.id.nav_friends,((BottomNavigationView)activity.findViewById(R.id.bottomNavigation)).getSelectedItemId());
                ((BottomNavigationView)activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_messages);
            });
            fixture.emptyInbox=false;main(model::refreshInbox);await(()->!state(model).busy&&state(model).conversations.size()==1,"Conversation appears");
            main(()->{var badge=((BottomNavigationView)activity.findViewById(R.id.bottomNavigation)).getBadge(R.id.nav_messages);assertNotNull(badge);assertTrue(badge.isVisible());assertEquals(1,badge.getNumber());});
            main(()->((EditText)activity.findViewById(R.id.messagesSearch)).setText("no_such_peer"));
            main(()->assertEquals(0,((RecyclerView)activity.findViewById(R.id.messagesList)).getAdapter().getItemCount()));
            main(()->((EditText)activity.findViewById(R.id.messagesSearch)).setText("qa_chat"));
            main(()->assertEquals(1,((RecyclerView)activity.findViewById(R.id.messagesList)).getAdapter().getItemCount()));
            capture("inbox-"+theme.id());main(()->activity.findViewById(R.id.messagesNew).performClick());
            NewChatActivity picker=awaitActivity(NewChatActivity.class,null);var pickerModel=model(picker);
            await(()->state(pickerModel).loaded&&!state(pickerModel).busy,"Friends loaded");
            main(()->((EditText)picker.findViewById(R.id.newChatSearch)).setText("QA Chat"));
            RecyclerView list=picker.findViewById(R.id.newChatList);await(()->list.findViewHolderForAdapterPosition(0)!=null,"Friend row laid out");
            capture("new-chat-"+theme.id());main(()->list.findViewHolderForAdapterPosition(0).itemView.performClick());
            ChatActivity room=awaitActivity(ChatActivity.class,null);var roomModel=model(room);
            await(()->state(roomModel).loaded&&!state(roomModel).busy,"Room opened");
            main(()->assertEquals("QA Chat Friend",((TextView)room.findViewById(R.id.chatTitle)).getText().toString()));
            capture("room-"+theme.id());closeScreens();
        }
        assertEquals(4,fixture.opens.get());
    }
    @Test public void textRetryKeepsKeyDraftAndRoomAfterRecreationAndIme()throws Exception{
        MainActivity mainActivity=launchMain();
        ChatActivity room=launchChat(mainActivity);var model=model(room);
        await(()->state(model).loaded&&!state(model).busy,"Room ready");fixture.failNextSend=true;
        main(()->{((EditText)room.findViewById(R.id.chatInput)).setText("QA retry text 😀");room.findViewById(R.id.chatSend).performClick();});
        await(()->!state(model).busy&&state(model).hasPendingSend,"Failed send remains pending");assertEquals(1,fixture.payloads.size());
        main(room::recreate);ChatActivity recreated=awaitActivity(ChatActivity.class,room);var retained=model(recreated);
        await(()->!state(retained).busy,"Recreated room ready");
        main(()->{
            assertEquals("QA retry text 😀",((EditText)recreated.findViewById(R.id.chatInput)).getText().toString());
            recreated.findViewById(R.id.chatRetry).performClick();
        });
        await(()->!state(retained).busy&&!state(retained).hasPendingSend&&state(retained).sentRevision==1,"Retry succeeded");
        assertEquals(2,fixture.payloads.size());assertEquals(fixture.payloads.get(0).clientMessageId,fixture.payloads.get(1).clientMessageId);
        assertEquals(fixture.payloads.get(0).content,fixture.payloads.get(1).content);assertEquals(1,fixture.messages.size());
        main(()->{
            EditText input=recreated.findViewById(R.id.chatInput);input.requestFocus();
            ((android.view.inputmethod.InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        android.os.SystemClock.sleep(1000);
        main(()->{
            View input=recreated.findViewById(R.id.chatInput);android.graphics.Rect visible=new android.graphics.Rect();
            recreated.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);int[] location=new int[2];input.getLocationOnScreen(location);
            assertTrue("Composer above keyboard",location[1]+input.getHeight()<=visible.bottom);
        });capture("room-retry-ime");
    }
    @Test public void foregroundPushRefreshAndSessionClearRemovePrivateState()throws Exception{
        MainActivity activity=launchMain();var model=model(activity);main(model::refreshInbox);
        await(()->state(model).loaded&&!state(model).busy,"Inbox loaded");int previous=fixture.inboxCalls.get();
        main(()->{model.startSync();PushEventBus.emitChatChanged(ChatThemeFixture.ROOM,UUID.randomUUID().toString());});
        await(()->fixture.inboxCalls.get()>previous&&!state(model).busy,"Push refreshed inbox");
        fixture.client.sessions.beginLogin();PushEventBus.emitSessionCleared();
        await(()->state(model).expired,"Fixture session clear observed");
        main(()->{assertTrue(state(model).conversations.isEmpty());assertTrue(state(model).messages.isEmpty());assertEquals("",model.draft());});
    }
    @Test public void incomingReadAndPrivateMomentAttachmentUseAuthenticatedFixture()throws Exception{
        var incoming=new com.taktak.app.data.model.ChatModels.Message();incoming.id=UUID.randomUUID().toString();incoming.conversationId=ChatThemeFixture.ROOM;
        incoming.senderId=ChatThemeFixture.PEER;incoming.recipientId=ChatThemeFixture.SELF;incoming.clientMessageId=UUID.randomUUID().toString();
        incoming.messageType="TEXT";incoming.content="QA incoming";incoming.createdAt="2026-10-20T00:00:00Z";fixture.messages.add(incoming);
        MainActivity activity=launchMain();ChatActivity room=launchChat(activity);var model=model(room);
        await(()->state(model).loaded&&!state(model).busy&&fixture.reads.get()>0,"Incoming message marked read");
        var attachmentRequested=new AtomicBoolean();
        // Kiem tra va dispatch cung main-loop, nhu nut Attach chi enabled khi canSend.
        // Khong retry request da gui; chi doi action co the duoc chap nhan.
        await(()->{
            main(()->{
                if(model.canSend()) {
                    model.loadAttachableMoments();
                    attachmentRequested.set(true);
                }
            });
            return attachmentRequested.get();
        },"Attachment action accepted while idle");
        await(()->!state(model).busy&&state(model).attachments.size()==1,"Private attachment choices deduplicated");
        main(()->model.sendMoment(ChatThemeFixture.MOMENT));
        await(()->!state(model).busy&&state(model).sentRevision==1,"Moment sent only to RAM fixture");
        assertEquals("MOMENT",fixture.payloads.get(0).messageType);assertEquals(ChatThemeFixture.MOMENT,fixture.payloads.get(0).momentId);
        var loaded=new AtomicBoolean();main(()->model.loadImage(ChatThemeFixture.MOMENT,new com.taktak.app.viewmodel.HistoryViewModel.ImageCallback(){
            public void onLoaded(Bitmap image){assertEquals(image.getWidth(),image.getHeight());loaded.set(true);}
            public void onError(){fail("Fixture private image should load");}
        }));await(loaded::get,"Private square image downloaded");
        fixture.denyImage=true;var rejected=new AtomicBoolean();
        main(()->model.loadImage(ChatThemeFixture.MOMENT,new com.taktak.app.viewmodel.HistoryViewModel.ImageCallback(){
            public void onLoaded(Bitmap image){fail("Revoked image must not use stale cache");}
            public void onError(){rejected.set(true);}
        }));await(rejected::get,"Revoked private image rejected without cache fallback");capture("room-private-moment");
    }
    @Test public void restoredSavedStateFromDifferentProcessOrAccountCannotExposeDraft(){
        main(()->{
            var saved=new androidx.lifecycle.SavedStateHandle();
            saved.set("chat.ownerProcess","old-test-process");saved.set("chat.ownerGeneration",0L);
            saved.set("chat.draft","Private previous account draft");saved.set("chat.key",UUID.randomUUID().toString());
            saved.set("chat.type","TEXT");saved.set("chat.text","Private pending text");saved.set("chat.conversation",ChatThemeFixture.ROOM);
            var store=new androidx.lifecycle.ViewModelStore();
            var factory=new ViewModelProvider.Factory(){
                @SuppressWarnings("unchecked") @Override public <T extends androidx.lifecycle.ViewModel>T create(Class<T> type){
                    return (T)new ChatViewModel((android.app.Application)context.getApplicationContext(),saved);
                }
            };
            var model=new ViewModelProvider(store,factory).get(ChatViewModel.class);
            assertEquals("",model.draft());assertFalse(model.state().getValue().hasPendingSend);assertNull(saved.get("chat.conversation"));
            model.setDraft("Current process draft");store.clear();
            var secondStore=new androidx.lifecycle.ViewModelStore();
            var sameOwner=new ViewModelProvider(secondStore,factory).get(ChatViewModel.class);
            assertEquals("Current process draft",sameOwner.draft());secondStore.clear();
            try{fixture.client.sessions.beginLogin();}catch(IOException error){throw new AssertionError(error);}
            var thirdStore=new androidx.lifecycle.ViewModelStore();
            var noOwner=new ViewModelProvider(thirdStore,factory).get(ChatViewModel.class);
            assertEquals("",noOwner.draft());assertFalse(noOwner.state().getValue().hasPendingSend);thirdStore.clear();
        });
    }
    private MainActivity launchMain()throws Exception{
        fixture.profileEnabled=false;
        String launch;
        // NEW_TASK | CLEAR_TOP, không SINGLE_TOP: Main standard phải được tạo mới,
        // không giao intent cho instance/task cũ ngoài lifecycle monitor của runner.
        try(var descriptor=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("am start -W -f 0x14000000 -n com.taktak.app/.MainActivity");
            var input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor);
            var output=new ByteArrayOutputStream()){
            byte[] buffer=new byte[1024];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
            launch=output.toString(java.nio.charset.StandardCharsets.UTF_8);
        }
        System.out.println("CHAT_QA_LAUNCH: "+launch.replace('\n',' '));
        assertTrue("Shell must confirm successful Main launch: "+launch,launch.matches("(?s).*Status:\\s*ok\\b.*"));
        assertFalse("Main must be created rather than delivered to stale instance: "+launch,launch.contains("Activity not started"));
        MainActivity activity=awaitActivity(MainActivity.class,null);
        var session=new AtomicReference<SessionViewModel>();main(()->session.set(new ViewModelProvider(activity).get(SessionViewModel.class)));
        await(()->{var state=session.get().state().getValue();return state!=null&&state.destination==SessionViewModel.Destination.ERROR;},"Mock restore blocked before FCM registration");
        fixture.profileEnabled=true;
        main(()->((MutableLiveData<SessionViewModel.State>)session.get().state()).setValue(new SessionViewModel.State(SessionViewModel.Destination.CAMERA,0)));
        return activity;
    }
    private ChatActivity launchChat(Activity parent)throws Exception{
        main(()->parent.startActivity(ChatActivity.intent(parent,ChatThemeFixture.ROOM)));return awaitActivity(ChatActivity.class,null);
    }
    private ChatViewModel model(Activity activity){var ref=new AtomicReference<ChatViewModel>();main(()->ref.set(new ViewModelProvider((androidx.lifecycle.ViewModelStoreOwner)activity).get(ChatViewModel.class)));return ref.get();}
    private ChatViewModel.State state(ChatViewModel model){var ref=new AtomicReference<ChatViewModel.State>();main(()->ref.set(model.state().getValue()));return ref.get();}
    private <T extends Activity>T awaitActivity(Class<T> type,Activity old)throws Exception{
        var ref=new AtomicReference<T>();await(()->{main(()->{for(Activity item:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))if(type.isInstance(item)&&item!=old)ref.set(type.cast(item));});return ref.get()!=null;},"Resumed "+type.getSimpleName());return ref.get();
    }
    private void await(BooleanSupplier condition,String message)throws Exception{long until=android.os.SystemClock.uptimeMillis()+20000;while(android.os.SystemClock.uptimeMillis()<until){if(condition.getAsBoolean())return;android.os.SystemClock.sleep(80);}fail(message);}
    private void main(Runnable action){if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())action.run();else InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private void bounds(View view,int width){if(view.getVisibility()==View.GONE)return;assertTrue("Horizontal bounds "+view.getClass().getSimpleName(),view.getLeft()>=0&&view.getRight()<=width);if(view instanceof ViewGroup group)for(int index=0;index<group.getChildCount();index++)bounds(group.getChildAt(index),group.getWidth());}
    private void closeScreens(){main(()->{var monitor=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance();for(var stage:androidx.test.runner.lifecycle.Stage.values())for(Activity item:new ArrayList<>(monitor.getActivitiesInStage(stage)))if(item instanceof MainActivity||item instanceof ChatActivity||item instanceof NewChatActivity||item instanceof com.taktak.app.ui.auth.LoginActivity)item.finish();});InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    private void capture(String name)throws Exception{
        android.os.SystemClock.sleep(400);Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        File folder=new File(context.getExternalFilesDir(null),"artifacts/chat");assertTrue(folder.isDirectory()||folder.mkdirs());
        try(var output=new FileOutputStream(new File(folder,name+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output));}finally{image.recycle();}
    }
}
