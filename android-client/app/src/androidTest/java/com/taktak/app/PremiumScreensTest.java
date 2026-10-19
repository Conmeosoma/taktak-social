package com.taktak.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.taktak.app.ui.auth.RegisterActivity;
import java.io.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

/** USB UI-only: không logout, sửa hồ sơ, đăng ký, gửi lời mời hoặc upload ảnh cá nhân. */
@RunWith(AndroidJUnit4.class)
public class PremiumScreensTest {
    @Test public void layoutsFitSmallLargeAndLandscapeWithLargeFont() {
        var runner=InstrumentationRegistry.getInstrumentation();
        runner.runOnMainSync(() -> {
            for(int night:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES}) {
                for(int[] size:new int[][]{{280,560},{360,720},{840,420}}) {
                    var config=new Configuration(runner.getTargetContext().getResources().getConfiguration());
                    config.fontScale=1.5f; config.uiMode=(config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)|night;
                    var theme=new androidx.appcompat.view.ContextThemeWrapper(runner.getTargetContext().createConfigurationContext(config),R.style.Theme_TakTak_Premium);
                    float density=theme.getResources().getDisplayMetrics().density;
                    for(int color:new int[]{R.color.premium_ink,R.color.premium_muted,R.color.premium_pink}) {
                        assertTrue("Body/navigation text contrast >=4.5",androidx.core.graphics.ColorUtils.calculateContrast(
                                theme.getColor(color),theme.getColor(R.color.premium_bg))>=4.5);
                    }
                    for(int layout:new int[]{R.layout.activity_register,R.layout.fragment_profile,R.layout.fragment_history,
                            R.layout.fragment_friends,R.layout.fragment_camera_page,R.layout.fragment_camera,R.layout.activity_main,R.layout.item_friends_section,R.layout.item_history_moment}) {
                        var layoutContext = layout == R.layout.activity_register
                                ? new androidx.appcompat.view.ContextThemeWrapper(theme, R.style.Theme_TakTak_Auth)
                                : layout == R.layout.activity_main || layout == R.layout.fragment_camera || layout == R.layout.fragment_camera_page
                                ? new androidx.appcompat.view.ContextThemeWrapper(theme, R.style.Theme_TakTak_Media_Main) : theme;
                        View root=LayoutInflater.from(layoutContext).inflate(layout,null,false);
                        if(layout==R.layout.fragment_history) {
                            root.findViewById(R.id.historyEmptyView).setVisibility(View.VISIBLE);
                            root.findViewById(R.id.historyRecycler).setVisibility(View.GONE);
                        }
                        int width=Math.round(size[0]*density),height=Math.round(size[1]*density);
                        boolean wrapInRealParent=layout==R.layout.fragment_camera || layout==R.layout.item_friends_section || layout==R.layout.item_history_moment;
                        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(
                                wrapInRealParent ? 0 : height,wrapInRealParent ? View.MeasureSpec.UNSPECIFIED : View.MeasureSpec.EXACTLY));
                        root.layout(0,0,width,root.getMeasuredHeight()); bounds(root,width);
                        if(layout==R.layout.item_history_moment) {
                            View image=root.findViewById(R.id.momentImage);
                            assertTrue("History photo must have nonzero height",image.getHeight()>0);
                            assertTrue("History photo stays square",Math.abs(image.getWidth()-image.getHeight())<=1);
                        }
                    }
                }
            }
        });
    }

    @Test public void mainNavigationAndRealDataScreensWithoutAccountMutation() throws Exception {
        launchMain();
        await(() -> hasShown(R.id.bottomNavigation),"Existing session must reach four tabs; no automatic login/logout allowed");
        select(R.id.nav_camera); await(() -> hasShown(R.id.cameraTitle),"Camera tab visible");
        await(() -> hasShown(R.id.cameraPermissionButton) || enabled(R.id.captureButton),"Camera preview or permission state ready");
        capture("camera.png");
        select(R.id.nav_history); await(() -> hasShown(R.id.historyTitle),"History tab visible");
        await(() -> !enabledLoading(R.id.historyProgress),"History finished loading");
        await(() -> !enabledLoading(R.id.momentImageProgress),"History image request completes");
        capture("history.png");
        onActivity(activity -> ((com.google.android.material.tabs.TabLayout)activity.findViewById(R.id.historyTabs)).getTabAt(1).select());
        await(() -> !enabledLoading(R.id.historyProgress),"Sent history finished loading");
        capture("history-sent.png");
        onActivity(activity -> ((com.google.android.material.tabs.TabLayout)activity.findViewById(R.id.historyTabs)).getTabAt(0).select());
        select(R.id.nav_friends); await(() -> hasShown(R.id.friendsTitle),"Friends tab visible");
        await(() -> !enabledLoading(R.id.friendsProgress),"Friends finished loading");
        capture("friends.png");
        onActivity(activity -> {
            ((EditText)activity.findViewById(R.id.friendsQuery)).setText("invalid query!");
            activity.findViewById(R.id.friendsSearch).performClick();
            assertEquals(activity.getString(R.string.friends_invalid_query),((TextView)activity.findViewById(R.id.friendsMessage)).getText().toString());
            assertTrue(activity.findViewById(R.id.friendsMessage).isShown());
            ((EditText)activity.findViewById(R.id.friendsQuery)).setText("");
            activity.findViewById(R.id.friendsRefresh).performClick();
        });
        select(R.id.nav_profile); await(() -> hasShown(R.id.profileTitle),"Profile tab visible");
        await(() -> !enabledLoading(R.id.profileProgress),"Profile finished loading");
        capture("profile.png");
        onActivity(activity -> {
            assertEquals(enabledView(activity,R.id.profileChangeAvatar),enabledView(activity,R.id.profileAvatarEdit));
            assertTrue(activity.findViewById(R.id.profileLogout).isShown());
        });
        select(R.id.nav_camera);
        await(() -> hasShown(R.id.cameraTitle),"Camera navigation still works");
    }

    @Test public void registerValidationLoadingPasswordAndKeyboardWithoutRequest() throws Exception {
        launchMain();
        var runner=InstrumentationRegistry.getInstrumentation();
        runner.runOnMainSync(() -> runner.getTargetContext().startActivity(new Intent(runner.getTargetContext(),RegisterActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        await(() -> hasShown(R.id.displayNameInput),"Register foreground"); capture("register.png");
        onActivity(activity -> {
            assertTrue(activity instanceof RegisterActivity);
            activity.findViewById(R.id.submitButton).performClick();
            for(int id:new int[]{R.id.displayNameLayout,R.id.usernameLayout,R.id.emailLayout,R.id.passwordLayout,R.id.confirmPasswordLayout}) {
                assertNotNull(((TextInputLayout)activity.findViewById(id)).getError());
            }
            var register=(RegisterActivity)activity; register.setLoading(true);
            assertFalse(activity.findViewById(R.id.submitButton).isEnabled());
            assertFalse(activity.findViewById(R.id.confirmPasswordInput).isEnabled());
            register.setLoading(false);
            var password=(EditText)activity.findViewById(R.id.passwordInput);
            var field=(TextInputLayout)activity.findViewById(R.id.passwordLayout);
            field.setError(null); password.setText(java.util.UUID.randomUUID().toString());
            field.findViewById(com.google.android.material.R.id.text_input_end_icon).performClick();
            assertNull("Register eye reveals password",password.getTransformationMethod());
            field.findViewById(com.google.android.material.R.id.text_input_end_icon).performClick();
            assertNotNull("Register eye masks password",password.getTransformationMethod());
            password.setText("");
        });
        capture("register-validation.png");
        onView(withId(R.id.confirmPasswordInput)).perform(scrollTo(),click());
        onActivity(activity -> androidx.core.view.ViewCompat.getWindowInsetsController(activity.findViewById(android.R.id.content))
                .show(androidx.core.view.WindowInsetsCompat.Type.ime()));
        await(this::imeVisible,"Keyboard opens");
        onView(withId(R.id.submitButton)).perform(scrollTo()).check(matches(isDisplayed())); capture("register-keyboard.png");
        androidx.test.espresso.Espresso.closeSoftKeyboard(); await(() -> !imeVisible(),"Keyboard closes"); SystemClock.sleep(700);
        onActivity(Activity::finish);
    }

    private void select(int id) { onActivity(activity -> ((BottomNavigationView)activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(id)); }
    private void launchMain() throws Exception {
        var runner=InstrumentationRegistry.getInstrumentation();
        try(var fd=runner.getUiAutomation().executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
            var input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) { byte[] b=new byte[1024]; while(input.read(b)!=-1){} }
        SystemClock.sleep(800);
        await(this::appResumed,"Launcher must be foreground before starting next Activity");
    }
    private void onActivity(java.util.function.Consumer<Activity> action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var active=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            assertFalse("A Taktak Activity must be foreground",active.isEmpty()); action.accept(active.iterator().next());
        });
    }
    private boolean appResumed() {var result=new AtomicBoolean();readActivity(a -> result.set(true));return result.get();}
    private void readActivity(java.util.function.Consumer<Activity> action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var active=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            if(!active.isEmpty())action.accept(active.iterator().next());
        });
    }
    private boolean hasShown(int id) { var result=new AtomicBoolean();readActivity(a -> {View v=a.findViewById(id);result.set(v!=null && v.isShown());});return result.get(); }
    private boolean enabled(int id) { var result=new AtomicBoolean();readActivity(a -> result.set(enabledView(a,id)));return result.get(); }
    private boolean enabledView(Activity a,int id) {View v=a.findViewById(id);return v!=null && v.isEnabled();}
    private boolean enabledLoading(int id) { var result=new AtomicBoolean();readActivity(a -> {View v=a.findViewById(id);result.set(v!=null && v.isShown());});return result.get(); }
    private boolean imeVisible() {var result=new AtomicBoolean();readActivity(a -> {var insets=androidx.core.view.ViewCompat.getRootWindowInsets(a.findViewById(android.R.id.content));result.set(insets!=null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()));});return result.get();}
    private void await(java.util.function.BooleanSupplier condition,String message) {long deadline=SystemClock.elapsedRealtime()+30000;while(SystemClock.elapsedRealtime()<deadline){if(condition.getAsBoolean())return;SystemClock.sleep(100);}fail(message);}
    private void bounds(View view,int width) {
        if(view.getVisibility()==View.GONE)return;
        assertTrue("No horizontal overflow: "+view.getClass().getSimpleName(),view.getLeft()>=0 && view.getRight()<=width);
        if(view instanceof TextView text && !(view instanceof EditText) && text.length()>0 && text.getLayout()!=null) {
            assertTrue("No text clipping: "+text.getText(),text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        }
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)bounds(group.getChildAt(i),group.getWidth());
    }
    private void capture(String name) throws Exception {
        SystemClock.sleep(900);var runner=InstrumentationRegistry.getInstrumentation();var image=runner.getUiAutomation().takeScreenshot();assertNotNull(image);
        var directory=new File(runner.getTargetContext().getExternalFilesDir(null),"artifacts/premium-five");assertTrue(directory.isDirectory()||directory.mkdirs());
        try(var output=new FileOutputStream(new File(directory,name))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output));}finally{image.recycle();}
    }
}
