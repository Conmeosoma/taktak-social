package com.taktak.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.*;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.color.MaterialColors;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.session.*;
import com.taktak.app.viewmodel.FriendsViewModel;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Chỉ sở hữu phiên QA riêng, không thay phiên cá nhân hoặc cấu hình hệ thống. */
@RunWith(AndroidJUnit4.class)
public class FriendsM3Test {
    @Rule public final org.junit.rules.Timeout timeout = org.junit.rules.Timeout.seconds(120);
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final android.content.SharedPreferences fixture = context.getSharedPreferences("qa_m3_friends", Context.MODE_PRIVATE);
    private boolean ownsSession;
    private AuthRepository peer;
    private static final class Memory implements SessionStore {
        private Session value;
        public Session load() { return value; }
        public void save(Session session) { value = session; }
        public void clear() { value = null; }
    }
    @Before public void preservePersonalSession() throws Exception {
        if (new EncryptedSessionStore(context, BuildConfig.BASE_URL).load() != null) {
            String owner = fixture.getString("owner", "");
            assertFalse("Refuse personal session", owner.isEmpty());
            assertEquals(owner, AuthGraph.repository(context).profile().id);
            AuthGraph.repository(context).logout();
        }
        assertTrue(fixture.edit().clear().commit());
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
    }
    @After public void cleanupOwnSession() throws Exception {
        try {
            if (ownsSession) {
                AuthGraph.repository(context).logout();
                assertTrue(fixture.edit().clear().commit());
            }
        } finally {
            if (peer != null) { peer.logout(); }
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : new java.util.ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                        .getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))) { activity.finish(); }
            });
        }
    }

    @Test public void responsiveCardsAndSemanticColorsInBothThemes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                var config = new Configuration(context.getResources().getConfiguration()); config.fontScale = 1.5f;
                config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                var themed = new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config), R.style.Theme_TakTak_Camera);
                float density = themed.getResources().getDisplayMetrics().density;
                for (int[] size : new int[][]{{280, 560}, {360, 800}, {840, 420}}) {
                    View root = LayoutInflater.from(themed).inflate(R.layout.fragment_friends, null, false);
                    measure(root, size[0], size[1], density, true); bounds(root, root.getWidth());
                    assertEquals(themed.getColor(R.color.m3_primary), MaterialColors.getColor(root.getContext(), androidx.appcompat.R.attr.colorPrimary, 0));
                    View list = root.findViewById(R.id.friendsRecycler);
                    assertTrue(list.getWidth() <= Math.round(480 * density));
                    assertTrue("Recycler retains scrollable viewport", list.getHeight() >= Math.round(100 * density));
                    assertTrue(root.findViewById(R.id.friendsSearch).getHeight() >= Math.round(56 * density));
                    assertTrue(root.findViewById(R.id.friendsRefresh).getHeight() >= Math.round(56 * density));
                    for (int layout : new int[]{R.layout.item_friend, R.layout.item_friends_section}) {
                        View row = LayoutInflater.from(root.getContext()).inflate(layout, null, false);
                        TextView name = row.findViewById(R.id.friendName);
                        if (name != null) {
                            name.setText("Tên hiển thị dài có dấu tiếng Việt và nhiều từ");
                            ((TextView) row.findViewById(R.id.friendUsername)).setText("@" + "long_username_".repeat(19));
                            ((TextView) row.findViewById(R.id.friendPrimary)).setText(R.string.friends_accept);
                        } else {
                            ((TextView) row.findViewById(R.id.sectionTitle)).setText(R.string.friends_results);
                            ((TextView) row.findViewById(R.id.sectionMessage)).setText(R.string.friends_empty_list);
                        }
                        measure(row, Math.min(size[0], 480) - 32, 0, density, false); bounds(row, row.getWidth());
                    }
                }
                for (int[] pair : new int[][]{{R.color.m3_on_surface, R.color.m3_surface_low},
                        {R.color.m3_on_surface_variant, R.color.m3_surface_low},
                        {R.color.m3_on_secondary_container, R.color.m3_secondary_container},
                        {R.color.m3_on_error_container, R.color.m3_error_container},
                        {R.color.m3_on_success_container, R.color.m3_success_container}}) {
                    assertTrue(ColorUtils.calculateContrast(themed.getColor(pair[0]), themed.getColor(pair[1])) >= 4.5);
                }
            }
        });
    }

    @Test public void realIncomingLightDarkBusyAndSuccessPresentation() throws Exception {
        login();
        peer = new AuthRepository(new ApiClient(BuildConfig.BASE_URL, new Memory()));
        account(peer, "Mai Anh — bạn của Taktak");
        peer.friends().send(AuthGraph.repository(context).profile().id);
        int previous = AppCompatDelegate.getDefaultNightMode();
        try {
            for (int night : new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES}) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> AppCompatDelegate.setDefaultNightMode(night));
                if (night == AppCompatDelegate.MODE_NIGHT_NO) { launchFriends(); }
                else { SessionRoutingTest.awaitView(R.id.friendsQuery); }
                ready();
                capture(night == AppCompatDelegate.MODE_NIGHT_NO ? "light-incoming.png" : "dark-incoming.png");
                swipeHeader(false);
                capture(night == AppCompatDelegate.MODE_NIGHT_NO ? "light-scrolled.png" : "dark-scrolled.png");
                swipeHeader(true);
                onMain(activity -> {
                    var vm = new androidx.lifecycle.ViewModelProvider(activity).get(FriendsViewModel.class);
                    assertEquals(1, vm.state().getValue().incoming.size());
                    assertTrue(ViewCompat.isAccessibilityHeading(activity.findViewById(R.id.friendsTitle)));
                    renderBusy(vm, true);
                    assertTrue(activity.findViewById(R.id.friendsProgress).isShown());
                    for (int id : new int[]{R.id.friendsSearch, R.id.friendsQuery, R.id.friendsRefresh}) {
                        assertFalse(activity.findViewById(id).isEnabled());
                    }
                });
                capture(night == AppCompatDelegate.MODE_NIGHT_NO ? "light-busy.png" : "dark-busy.png");
                onMain(activity -> renderBusy(new androidx.lifecycle.ViewModelProvider(activity).get(FriendsViewModel.class), false));
            }
            onMain(activity -> {
                var vm = new androidx.lifecycle.ViewModelProvider(activity).get(FriendsViewModel.class);
                vm.respond(vm.state().getValue().incoming.get(0).id, true);
            });
            ready();
            onMain(activity -> {
                var state = new androidx.lifecycle.ViewModelProvider(activity).get(FriendsViewModel.class).state().getValue();
                assertTrue(state.incoming.isEmpty()); assertEquals(1, state.friends.size());
                TextView notice = activity.findViewById(R.id.friendsMessage);
                assertEquals(activity.getString(R.string.friends_updated), notice.getText().toString());
                assertEquals(activity.getColor(R.color.m3_on_success_container), notice.getCurrentTextColor());
            });
            capture("success.png");
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> AppCompatDelegate.setDefaultNightMode(previous));
        }
    }

    @Test public void keyboardQueryVisibleAndImeSearchWorks() throws Exception {
        login(); launchFriends(); ready();
        onMain(activity -> {
            ((AppBarLayout) activity.findViewById(R.id.friendsAppBar)).setExpanded(true, false);
            TextView query = activity.findViewById(R.id.friendsQuery);
            query.setText("nom3_" + UUID.randomUUID().toString().replace("-", "")); query.requestFocus();
            ((android.view.inputmethod.InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(query, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        await(() -> {
            AtomicBoolean visible = new AtomicBoolean();
            onMain(activity -> {
                var insets = ViewCompat.getRootWindowInsets(activity.getWindow().getDecorView());
                if (insets != null) { visible.set(insets.isVisible(WindowInsetsCompat.Type.ime())); }
            }); return visible.get();
        }, "IME visible");
        SystemClock.sleep(500);
        onMain(activity -> {
            View query = activity.findViewById(R.id.friendsQuery); var rect = new android.graphics.Rect();
            assertTrue(query.getGlobalVisibleRect(rect));
            int[] location = new int[2]; query.getLocationOnScreen(location);
            // TextInputEditText có thể mở rộng global rect tới TextInputLayout; kiểm tra toàn bộ vùng EditText thật.
            assertTrue(rect.contains(location[0], location[1], location[0] + query.getWidth(), location[1] + query.getHeight()));
            var insets = ViewCompat.getRootWindowInsets(activity.getWindow().getDecorView());
            assertTrue(location[1] + query.getHeight() <= activity.getWindow().getDecorView().getHeight() - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
        });
        capture("keyboard.png");
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.friendsQuery))
                .perform(androidx.test.espresso.action.ViewActions.pressImeActionButton());
        ready();
        onMain(activity -> {
            var state = new androidx.lifecycle.ViewModelProvider(activity).get(FriendsViewModel.class).state().getValue();
            assertTrue(state.searched); assertTrue(state.results.isEmpty());
        });
        capture("search-empty.png");
    }

    private void login() throws Exception { account(AuthGraph.repository(context), "M3 Friends QA"); }
    private void account(AuthRepository repository, String name) throws Exception {
        String username = "m3friend_" + UUID.randomUUID().toString().replace("-", ""), password = UUID.randomUUID().toString();
        var user = repository.register(username + "@example.test", username, name, password);
        if (repository != peer) { assertTrue(fixture.edit().putString("owner", user.id).commit()); ownsSession = true; }
        repository.login(username + "@example.test", password);
    }
    private void launchFriends() throws Exception {
        var descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor); var reader = new BufferedReader(new InputStreamReader(input))) {
            StringBuilder result = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) { result.append(line); }
            assertTrue(result.toString().contains("Status: ok"));
        }
        SessionRoutingTest.awaitView(R.id.bottomNavigation);
        onMain(MainNavigationTestSupport::friends);
        SessionRoutingTest.awaitView(R.id.friendsQuery);
    }
    private void ready() {
        await(() -> {
            AtomicBoolean done = new AtomicBoolean();
            onMain(activity -> {
                var state = new androidx.lifecycle.ViewModelProvider(activity).get(FriendsViewModel.class).state().getValue();
                done.set(state != null && state.loaded && !state.busy && !state.expired);
            }); return done.get();
        }, "Friends API finishes");
    }
    private void renderBusy(FriendsViewModel model, boolean busy) {
        try {
            var field = FriendsViewModel.class.getDeclaredField("busy"); field.setAccessible(true); field.setBoolean(model, busy);
            var publish = FriendsViewModel.class.getDeclaredMethod("publish"); publish.setAccessible(true); publish.invoke(model);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private void swipeHeader(boolean expand) throws Exception {
        var rect = new android.graphics.Rect();
        onMain(activity -> {
            assertTrue(activity.findViewById(R.id.friendsRecycler).getGlobalVisibleRect(rect));
            View decor = activity.getWindow().getDecorView();
            var insets = ViewCompat.getRootWindowInsets(decor).getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            assertTrue(rect.intersect(insets.left, insets.top, decor.getWidth() - insets.right, decor.getHeight() - insets.bottom));
        });
        int inset = rect.height() / 4;
        android.util.Log.i("TaktakQa", "M3_FRIENDS_SWIPE_VISIBLE_RECT=" + rect.toShortString());
        String command = "input swipe " + rect.centerX() + " " + (expand ? rect.top + inset : rect.bottom - inset)
                + " " + rect.centerX() + " " + (expand ? rect.bottom - inset : rect.top + inset) + " 500";
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))) {
            byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
        }
        await(() -> {
            AtomicBoolean matched = new AtomicBoolean();
            onMain(activity -> {
                View appBar = activity.findViewById(R.id.friendsAppBar);
                matched.set(expand ? appBar.getTop() == 0 : appBar.getTop() < 0);
            }); return matched.get();
        }, "Header collapses/expands after real swipe");
        SystemClock.sleep(500);
    }
    private void onMain(java.util.function.Consumer<MainActivity> action) {
        long deadline = SystemClock.elapsedRealtime() + 10000;
        while (SystemClock.elapsedRealtime() < deadline) {
            AtomicBoolean handled = new AtomicBoolean();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                var activities = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED);
                assertTrue("At most one resumed QA Activity", activities.size() <= 1);
                if (activities.size() == 1 && activities.iterator().next() instanceof MainActivity activity) {
                    action.accept(activity); handled.set(true);
                }
            });
            if (handled.get()) { return; }
            SystemClock.sleep(50);
        }
        fail("MainActivity must resume after theme recreation");
    }
    private void await(java.util.function.BooleanSupplier condition, String message) {
        long deadline = SystemClock.elapsedRealtime() + 30000;
        while (SystemClock.elapsedRealtime() < deadline) { if (condition.getAsBoolean()) { return; } SystemClock.sleep(100); }
        fail(message);
    }
    private void measure(View root, int width, int height, float density, boolean window) {
        root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width * density), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(Math.round(height * density), window ? View.MeasureSpec.EXACTLY : View.MeasureSpec.UNSPECIFIED));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    }
    private void bounds(View view, int parentWidth) {
        if (view.getVisibility() == View.GONE) { return; }
        assertTrue("No horizontal overflow: " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= parentWidth);
        if (view instanceof TextView text && text.getLayout() != null) {
            assertTrue("Text height fits", text.getLayout().getHeight() <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof ViewGroup group) { for (int i = 0; i < group.getChildCount(); i++) { bounds(group.getChildAt(i), group.getWidth()); } }
    }
    private void capture(String name) throws Exception {
        SystemClock.sleep(1000); var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        var directory = new File(context.getExternalFilesDir(null), "artifacts/m3e-friends"); assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new FileOutputStream(new File(directory, name))) { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)); }
        finally { bitmap.recycle(); }
    }
}
