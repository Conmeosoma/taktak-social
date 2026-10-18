package com.taktak.app;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.SystemClock;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.viewmodel.ProfileViewModel;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProfileAcceptanceTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private AuthRepository auth;
    private boolean owned;
    @Before public void start() throws Exception {
        auth=AuthGraph.repository(context);assertFalse("Không thay phiên tài khoản cá nhân",auth.hasSession());
        if(android.os.Build.VERSION.SDK_INT>=37) InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(),"android.permission.ACCESS_LOCAL_NETWORK");
        InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(),"android.permission.CAMERA");
        String name="profile_"+UUID.randomUUID().toString().replace("-",""),password=UUID.randomUUID().toString();
        auth.register(name+"@example.test",name,"Profile QA",password);auth.login(name+"@example.test",password);owned=true;
    }
    @After public void stop() throws Exception { if(owned) auth.logout(context); }
    private ProfileViewModel model(MainActivity activity) { return new ViewModelProvider(activity).get(ProfileViewModel.class); }
    private int avatarRequestCode(MainActivity activity) {
        try {
            var registry=activity.getActivityResultRegistry();
            for(var field:androidx.activity.result.ActivityResultRegistry.class.getDeclaredFields()) {
                field.setAccessible(true);
                Object value=field.get(registry);
                if(value instanceof java.util.Map<?,?> map && map.get("profile.avatar") instanceof Integer code) return code;
            }
        } catch(ReflectiveOperationException failure) {throw new AssertionError(failure);}
        throw new AssertionError("Named avatar launcher registered");
    }
    private void profile(ActivityScenario<MainActivity> scenario) {
        long deadline=SystemClock.elapsedRealtime()+30000;
        while(SystemClock.elapsedRealtime()<deadline) {
            AtomicBoolean ready=new AtomicBoolean();scenario.onActivity(activity -> {
                var nav=activity.findViewById(R.id.bottomNavigation);
                if(nav!=null && nav.isShown()) {
                    ((androidx.viewpager2.widget.ViewPager2)activity.findViewById(R.id.mainPager)).setCurrentItem(3,false);
                    var state=model(activity).state().getValue();ready.set(state.profile!=null && !state.busy && activity.findViewById(R.id.profileSave)!=null);
                }
            });
            if(ready.get()) return;SystemClock.sleep(100);
        } fail("Profile page loads");
    }
    private void idle(ActivityScenario<MainActivity> scenario) {
        long deadline=SystemClock.elapsedRealtime()+30000;
        while(SystemClock.elapsedRealtime()<deadline) {
            AtomicBoolean idle=new AtomicBoolean();scenario.onActivity(activity -> idle.set(!model(activity).state().getValue().busy));
            if(idle.get())return;SystemClock.sleep(100);
        } fail("Profile request finishes");
    }
    private void capture(String name) throws IOException {
        var directory=new File(context.getExternalFilesDir(null),"qa-profile");assertTrue(directory.isDirectory() || directory.mkdirs());
        var bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        try(var output=new FileOutputStream(new File(directory,name+".png"))) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output)); }
        finally { bitmap.recycle(); }
    }
    @Test public void profileLoadsValidatesAndSavesUnicodeName() throws Exception {
        try(var scenario=ActivityScenario.launch(MainActivity.class)) {
            profile(scenario);capture("profile-before");
            scenario.onActivity(activity -> {
                ((android.widget.EditText)activity.findViewById(R.id.profileNameInput)).setText("   ");
                activity.findViewById(R.id.profileSave).performClick();
                assertEquals(R.string.profile_invalid_name,model(activity).state().getValue().message);
                ((android.widget.EditText)activity.findViewById(R.id.profileNameInput)).setText("  Taktak 📸 QA  ");
                activity.findViewById(R.id.profileSave).performClick();
            });
            idle(scenario);assertEquals("Taktak 📸 QA",auth.profile().displayName);
            scenario.recreate();profile(scenario);
            scenario.onActivity(activity -> assertEquals("Taktak 📸 QA",((android.widget.EditText)activity.findViewById(R.id.profileNameInput)).getText().toString()));
            capture("profile-name-saved");
        }
    }
    @Test public void selectedImageUploadsAndReloadsPrivateAvatar() throws Exception {
        var file=new File(context.getCacheDir(),"qa-avatar-"+UUID.randomUUID()+".png");
        var bitmap=Bitmap.createBitmap(320,180,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.rgb(240,100,140));
        try(var output=new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG,100,output); }finally{bitmap.recycle();}
        try(var scenario=ActivityScenario.launch(MainActivity.class)) {
            profile(scenario);
            scenario.onActivity(activity -> assertTrue(model(activity).beginAvatarSelection()));
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            scenario.onActivity(activity -> activity.getActivityResultRegistry().dispatchResult(avatarRequestCode(activity),android.app.Activity.RESULT_OK,
                    new android.content.Intent().setData(Uri.fromFile(file))));
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);profile(scenario);idle(scenario);
            byte[] loaded=auth.profiles().avatar(auth.profile().id);assertNotNull(loaded);
            var bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(loaded,0,loaded.length,bounds);
            assertEquals(512,bounds.outWidth);assertEquals(512,bounds.outHeight);
            scenario.recreate();profile(scenario);
            scenario.onActivity(activity -> assertNotNull(model(activity).state().getValue().avatar));capture("profile-avatar-saved");
        } finally { assertTrue(file.delete()); }
    }
    @Test public void cancelledPickerAndInvalidImageKeepExistingAvatar() throws Exception {
        try(var scenario=ActivityScenario.launch(MainActivity.class)) {
            profile(scenario);
            scenario.onActivity(activity -> {
                assertTrue(model(activity).beginAvatarSelection());
                activity.getActivityResultRegistry().dispatchResult(avatarRequestCode(activity),android.app.Activity.RESULT_CANCELED,null);
                assertFalse(model(activity).state().getValue().busy);
            });
            var invalid=new File(context.getCacheDir(),"qa-avatar-invalid-"+UUID.randomUUID()+".txt");
            try(var output=new FileOutputStream(invalid)){output.write("invalid image".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            try {
                scenario.onActivity(activity -> model(activity).upload(Uri.fromFile(invalid)));idle(scenario);
                scenario.onActivity(activity -> assertNotEquals(0,model(activity).state().getValue().message));
                assertNull(auth.profiles().avatar(auth.profile().id));
            } finally {assertTrue(invalid.delete());}
        }
    }
    @Test public void draftSurvivesRecreateAndLogoutRemovesProfileAndSession() throws Exception {
        try(var scenario=ActivityScenario.launch(MainActivity.class)) {
            profile(scenario);
            scenario.onActivity(activity -> ((android.widget.EditText)activity.findViewById(R.id.profileNameInput)).setText("Unsaved draft"));
            scenario.recreate();profile(scenario);
            scenario.onActivity(activity -> {
                assertEquals("Unsaved draft",((android.widget.EditText)activity.findViewById(R.id.profileNameInput)).getText().toString());
                activity.findViewById(R.id.profileLogout).performClick();
            });
            SessionRoutingTest.awaitView(R.id.emailInput);
            assertFalse(auth.hasSession());owned=false;
        }
    }
    @Test public void friendAvatarDownloadsWithJwtAndDarkProfileRemainsUsable() throws Exception {
        var memory=new com.taktak.app.data.session.SessionStore() {
            private com.taktak.app.data.session.Session value;
            public com.taktak.app.data.session.Session load(){return value;}
            public void save(com.taktak.app.data.session.Session next){value=next;}
            public void clear(){value=null;}
        };
        var friend=new AuthRepository(new com.taktak.app.data.remote.ApiClient(BuildConfig.BASE_URL,memory));
        String name="friend_"+UUID.randomUUID().toString().replace("-",""),password=UUID.randomUUID().toString();
        friend.register(name+"@example.test",name,"Avatar Friend QA",password);friend.login(name+"@example.test",password);
        try {
            var bitmap=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.GREEN);
            var output=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,90,output);bitmap.recycle();
            friend.profiles().upload(output.toByteArray());friend.friends().send(auth.profile().id);
            auth.friends().respond(auth.friends().incoming(0).items.get(0).id,true);
            try(var scenario=ActivityScenario.launch(MainActivity.class)) {
                profile(scenario);
                scenario.onActivity(activity -> ((androidx.viewpager2.widget.ViewPager2)activity.findViewById(R.id.mainPager)).setCurrentItem(2,false));
                AtomicBoolean found=new AtomicBoolean();long deadline=SystemClock.elapsedRealtime()+30000;
                while(!found.get() && SystemClock.elapsedRealtime()<deadline) {
                    scenario.onActivity(activity -> {
                        var recycler=(androidx.recyclerview.widget.RecyclerView)activity.findViewById(R.id.friendsRecycler);
                        if(recycler==null)return;
                        for(int i=0;i<recycler.getChildCount();i++) {
                            var row=recycler.getChildAt(i);var label=(android.widget.TextView)row.findViewById(R.id.friendName);
                            var image=(android.widget.ImageView)row.findViewById(R.id.friendAvatar);
                            if(label!=null && "Avatar Friend QA".contentEquals(label.getText()) && image!=null && image.getDrawable() instanceof android.graphics.drawable.BitmapDrawable drawable) {
                                var pixels=drawable.getBitmap();int color=pixels.getPixel(pixels.getWidth()/2,pixels.getHeight()/2);
                                found.set(Color.green(color)>200 && Color.red(color)<40);
                            }
                        }
                    });SystemClock.sleep(100);
                }
                assertTrue("Friend avatar uses authenticated image",found.get());capture("friend-avatar");
                scenario.onActivity(activity -> {
                    ((androidx.viewpager2.widget.ViewPager2)activity.findViewById(R.id.mainPager)).setCurrentItem(3,false);
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                });
                profile(scenario);capture("profile-dark");
                scenario.onActivity(activity -> {
                    assertTrue(activity.findViewById(R.id.profileSave).isEnabled());
                    assertNotNull(((android.widget.ImageView)activity.findViewById(R.id.profileAvatar)).getDrawable());
                });
            }
        } finally {
            friend.logout();InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        }
    }
}
