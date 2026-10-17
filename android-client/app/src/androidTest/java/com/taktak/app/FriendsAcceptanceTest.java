package com.taktak.app;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels.UserProfile;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.*;
import com.taktak.app.data.session.*;
import com.taktak.app.viewmodel.FriendsViewModel;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
public class FriendsAcceptanceTest {
    @Rule public final org.junit.rules.Timeout timeout = org.junit.rules.Timeout.seconds(120);
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private AuthRepository primary, secondary;
    private ApiClient secondaryClient;
    private boolean ownsSession;
    private UserProfile me, other;
    private static final class Memory implements SessionStore {
        Session value;
        public Session load() { return value; }
        public void save(Session next) { value = next; }
        public void clear() { value = null; }
    }
    @Before public void start() throws Exception {
        android.util.Log.i("TaktakQa", "FRIENDS_FIXTURE_CHECK_SESSION");
        primary = AuthGraph.repository(context);
        if (new EncryptedSessionStore(context, BuildConfig.BASE_URL).load() != null) {
            String owner = context.getSharedPreferences("qa4c_fixture", Context.MODE_PRIVATE).getString("owner", "");
            if (owner.isEmpty()) { owner = InstrumentationRegistry.getArguments().getString("taktakRecoverOwnedUserId", ""); }
            assertEquals("Do not replace a session not owned by this fixture", owner, primary.profile().id);
            primary.logout();
        }
        android.util.Log.i("TaktakQa", "FRIENDS_FIXTURE_REGISTER");
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        me = account(primary, "Friends QA A"); ownsSession = true;
        secondaryClient = new ApiClient(BuildConfig.BASE_URL, new Memory());
        secondary = new AuthRepository(secondaryClient); other = account(secondary, "Friends QA B");
        android.util.Log.i("TaktakQa", "FRIENDS_FIXTURE_LAUNCH");
        shell("am start -W -n com.taktak.app/.MainActivity");
        SessionRoutingTest.awaitView(R.id.friendsButton);
        act(activity -> activity.findViewById(R.id.friendsButton).performClick());
        ready();
    }
    @After public void stop() throws Exception {
        try {
            try {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                    var activities = new java.util.ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED));
                    for (var activity : activities) { activity.finish(); }
                });
            }
            finally {
                if (ownsSession) {
                    primary.logout();
                    assertTrue(context.getSharedPreferences("qa4c_fixture", Context.MODE_PRIVATE).edit().remove("owner").commit());
                }
            }
        } finally { if (secondary != null) { secondary.logout(); } }
    }
    private UserProfile account(AuthRepository repository, String name) throws Exception {
        String username = repository == primary ? InstrumentationRegistry.getArguments().getString("taktakQaUsername", "") : "";
        if (username.isEmpty()) { username = "friendqa_" + UUID.randomUUID().toString().replace("-", ""); }
        String password = UUID.randomUUID().toString();
        var user = repository.register(username + "@example.test", username, name, password);
        if (repository == primary) {
            ownsSession = true;
            assertTrue(context.getSharedPreferences("qa4c_fixture", Context.MODE_PRIVATE).edit().putString("owner", user.id).commit());
        }
        repository.login(username + "@example.test", password); return user;
    }
    @Test public void emptySearchValidationRecreationBackAndLogout() throws Exception {
        capture("empty.png");
        assertTrue(state().loaded); assertTrue(state().friends.isEmpty()); assertTrue(state().incoming.isEmpty());
        search("%"); awaitMessage(R.string.friends_invalid_query);
        search("no_match_" + UUID.randomUUID().toString().replace("-", ""));
        assertTrue(state().searched); assertTrue(state().results.isEmpty());
        capture("empty-search.png");
        act(MainActivity::recreate); ready(); assertTrue(state().searched);
        act(activity -> activity.findViewById(R.id.friendsBack).performClick());
        SessionRoutingTest.awaitView(R.id.cameraTitle);
        act(activity -> activity.findViewById(R.id.friendsButton).performClick()); ready();
        act(activity -> activity.findViewById(R.id.friendsLogout).performClick());
        SessionRoutingTest.awaitView(R.id.emailInput);
        SessionRoutingTest.onResumed(activity -> { assertTrue(activity.isTaskRoot()); assertNull(activity.findViewById(R.id.friendsRecycler)); });
        assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        capture("logout.png");
    }
    @Test public void invitationsAcceptanceRejectionUnfriendAndBlockUseRealApi() throws Exception {
        search(other.username.toUpperCase(java.util.Locale.ROOT));
        assertEquals(other.id, state().results.get(0).id); capture("search-result.png");
        rowAction(other.username, R.id.friendPrimary); ready(); awaitMessage(R.string.friends_sent);
        var request = secondary.friends().incoming(0).items.get(0);
        assertEquals(me.id, request.sender.id);
        secondary.friends().respond(request.id, true);
        refresh(); assertEquals(other.id, state().friends.get(0).id); capture("friends.png");
        options(other.username, R.string.friends_unfriend);
        capture("unfriend-confirm.png");
        onView(withId(android.R.id.button2)).perform(click());
        assertEquals(1, primary.friends().friends(0).items.size());
        options(other.username, R.string.friends_unfriend);
        onView(withId(android.R.id.button1)).perform(click()); ready();
        assertTrue(primary.friends().friends(0).items.isEmpty()); assertTrue(secondary.friends().friends(0).items.isEmpty());
        secondary.friends().send(me.id); refresh(); assertEquals(1, state().incoming.size()); capture("incoming.png");
        rowAction(other.username, R.id.friendSecondary); ready();
        assertTrue(primary.friends().incoming(0).items.isEmpty()); assertTrue(primary.friends().friends(0).items.isEmpty());
        secondary.friends().send(me.id); refresh();
        rowAction(other.username, R.id.friendPrimary); ready();
        assertEquals(other.id, primary.friends().friends(0).items.get(0).id);
        options(other.username, R.string.friends_block); capture("block-confirm.png");
        onView(withId(android.R.id.button1)).perform(click()); ready();
        assertTrue(primary.friends().friends(0).items.isEmpty()); assertTrue(secondary.friends().friends(0).items.isEmpty());
        search(other.username); assertTrue(state().results.isEmpty());
        assertTrue(secondary.friends().search(me.username).isEmpty()); capture("blocked-hidden.png");
    }
    @Test public void duplicateInvitationShowsConflictAndRefreshDoesNotReplayMutation() throws Exception {
        primary.friends().send(other.id);
        search(other.username); rowAction(other.username, R.id.friendPrimary); ready();
        awaitMessage(R.string.friends_conflict); capture("conflict.png");
        refresh(); assertEquals(1, secondary.friends().incoming(0).items.size());
    }
    @Test public void revokedRefreshClearsFriendsAndReturnsToLogin() throws Exception {
        search(other.username); assertFalse(state().results.isEmpty());
        Session stored = new EncryptedSessionStore(context, BuildConfig.BASE_URL).load();
        assertTrue(secondaryClient.auth.logout(new com.taktak.app.data.model.AuthModels.RefreshRequest(stored.refreshToken)).execute().isSuccessful());
        new EncryptedSessionStore(context, BuildConfig.BASE_URL).save(new Session("invalid.jwt.token", stored.refreshToken, 0, 0));
        // Đọc lại store như cold process, không thay production graph bằng mock API.
        var field = AuthGraph.class.getDeclaredField("repository"); field.setAccessible(true);
        field.set(null, new AuthRepository(new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(context, BuildConfig.BASE_URL))));
        act(MainActivity::recreate);
        SessionRoutingTest.awaitView(R.id.emailInput);
        assertNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        capture("expired-session.png");
    }
    @Test public void accountSwitchDiscardsOldSnapshotAndRebindsWithoutLoop() throws Exception {
        search(other.username); assertFalse(state().results.isEmpty());
        primary.logout();
        var next = account(primary, "Friends QA switched");
        refresh(); ready();
        assertFalse(state().expired); assertTrue(state().results.isEmpty());
        assertTrue(state().incoming.isEmpty()); assertTrue(state().friends.isEmpty());
        assertEquals(next.id, primary.profile().id);
        capture("account-switched.png");
    }
    @Test public void networkFailureKeepsSessionAndRefreshRecovers() throws Exception {
        Assume.assumeTrue("Requires host-controlled QA connector", "true".equals(InstrumentationRegistry.getArguments().getString("taktakFriendsOffline")));
        android.util.Log.i("TaktakQa", "FRIENDS_READY_FOR_DISCONNECT");
        long deadline = android.os.SystemClock.elapsedRealtime() + 45000;
        while (healthy() && android.os.SystemClock.elapsedRealtime() < deadline) { android.os.SystemClock.sleep(200); }
        assertFalse("Host must disconnect only QA backend", healthy());
        refresh(); awaitMessage(R.string.friends_network_error); capture("offline.png");
        assertNotNull(new EncryptedSessionStore(context, BuildConfig.BASE_URL).load());
        android.util.Log.i("TaktakQa", "FRIENDS_READY_FOR_RESTORE");
        deadline = android.os.SystemClock.elapsedRealtime() + 45000;
        while (!healthy() && android.os.SystemClock.elapsedRealtime() < deadline) { android.os.SystemClock.sleep(200); }
        assertTrue("Host must restore QA backend", healthy());
        refresh(); assertEquals(0, state().message); assertTrue(state().loaded); capture("retry-restored.png");
    }
    @Test public void twoDevicesInviteAcceptAndBlockWithThirdAccountIsolated() throws Exception {
        String peer = InstrumentationRegistry.getArguments().getString("taktakQaPeer", "");
        Assume.assumeFalse("Requires a second device and unique host-generated usernames", peer.isEmpty());
        boolean sender = "sender".equals(InstrumentationRegistry.getArguments().getString("taktakQaRole"));
        assertTrue(primary.friends().friends(0).items.isEmpty());
        assertTrue(secondary.friends().friends(0).items.isEmpty());
        if (sender) {
            long end = android.os.SystemClock.elapsedRealtime() + 30000;
            while (primary.friends().search(peer).isEmpty() && android.os.SystemClock.elapsedRealtime() < end) { android.os.SystemClock.sleep(2000); }
            search(peer); assertEquals(1, state().results.size());
            rowAction(peer, R.id.friendPrimary); ready();
            end = android.os.SystemClock.elapsedRealtime() + 45000;
            while (primary.friends().friends(0).items.isEmpty() && android.os.SystemClock.elapsedRealtime() < end) { android.os.SystemClock.sleep(500); }
            refresh(); assertEquals(peer, state().friends.get(0).username); capture("two-device-sender-friends.png");
            options(peer, R.string.friends_block); capture("two-device-block-confirm.png");
            onView(withId(android.R.id.button1)).perform(click()); ready();
            assertTrue(primary.friends().friends(0).items.isEmpty());
        } else {
            long end = android.os.SystemClock.elapsedRealtime() + 45000;
            while (primary.friends().incoming(0).items.isEmpty() && android.os.SystemClock.elapsedRealtime() < end) { android.os.SystemClock.sleep(500); }
            refresh(); assertEquals(peer, state().incoming.get(0).sender.username);
            String peerId = state().incoming.get(0).sender.id;
            capture("two-device-receiver-incoming.png"); rowAction(peer, R.id.friendPrimary); ready();
            assertEquals(peer, state().friends.get(0).username); capture("two-device-receiver-friends.png");
            end = android.os.SystemClock.elapsedRealtime() + 45000;
            while (!primary.friends().friends(0).items.isEmpty() && android.os.SystemClock.elapsedRealtime() < end) { android.os.SystemClock.sleep(500); }
            refresh(); assertTrue(state().friends.isEmpty());
            try { primary.friends().send(peerId); fail("Blocked user must not send invitations manually"); }
            catch (com.taktak.app.data.remote.ApiFailure failure) { assertEquals(404, failure.status); }
            assertTrue(primary.friends().search(peer).isEmpty()); capture("two-device-blocked.png");
        }
        // Tài khoản C độc lập không được tự xuất hiện trong quan hệ A/B.
        assertTrue(secondary.friends().friends(0).items.isEmpty());
        assertTrue(secondary.friends().incoming(0).items.isEmpty());
    }
    private boolean healthy() {
        try (var response = new okhttp3.OkHttpClient.Builder().callTimeout(1, java.util.concurrent.TimeUnit.SECONDS).build()
                .newCall(new okhttp3.Request.Builder().url(BuildConfig.BASE_URL + "actuator/health").build()).execute()) { return response.isSuccessful(); }
        catch (java.io.IOException failure) { return false; }
    }
    private void search(String input) {
        act(activity -> { ((TextView) activity.findViewById(R.id.friendsQuery)).setText(input); activity.findViewById(R.id.friendsSearch).performClick(); }); ready();
    }
    private void refresh() { act(activity -> activity.findViewById(R.id.friendsRefresh).performClick()); ready(); }
    private void options(String username, int action) {
        rowAction(username, R.id.friendOptions);
        android.os.SystemClock.sleep(1200);
        onView(withText(action)).perform(click());
    }
    private void rowAction(String username, int id) {
        AtomicReference<View> target = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger();
        act(activity -> count.set(((RecyclerView) activity.findViewById(R.id.friendsRecycler)).getAdapter().getItemCount()));
        for (int position = 0; position < count.get(); position++) {
            int index = position;
            act(activity -> ((RecyclerView) activity.findViewById(R.id.friendsRecycler)).scrollToPosition(index));
            android.os.SystemClock.sleep(120);
            act(activity -> {
                RecyclerView recycler = activity.findViewById(R.id.friendsRecycler);
                var holder = recycler.findViewHolderForAdapterPosition(index);
                if (holder == null) { return; }
                TextView name = holder.itemView.findViewById(R.id.friendUsername);
                if (name != null && name.getText().toString().equals("@" + username)) {
                    View button = holder.itemView.findViewById(id);
                    assertTrue(button.isShown()); assertTrue(button.isEnabled());
                    button.requestRectangleOnScreen(new android.graphics.Rect(0, 0, button.getWidth(), button.getHeight()), true);
                    target.set(button);
                }
            });
            if (target.get() != null) {
                android.os.SystemClock.sleep(300);
                onView(org.hamcrest.CoreMatchers.is(target.get())).perform(click());
                return;
            }
        }
        fail("Expected actionable row not found");
    }
    private FriendsViewModel.State state() {
        AtomicReference<FriendsViewModel.State> result = new AtomicReference<>();
        act(activity -> result.set(new androidx.lifecycle.ViewModelProvider(activity).get(FriendsViewModel.class).state().getValue()));
        return result.get();
    }
    private void ready() {
        SessionRoutingTest.awaitView(R.id.friendsRecycler);
        long end = android.os.SystemClock.elapsedRealtime() + 35000;
        while (state().busy && android.os.SystemClock.elapsedRealtime() < end) { android.os.SystemClock.sleep(50); }
        assertFalse("Friends operation timed out", state().busy);
    }
    private void awaitMessage(int resource) {
        assertEquals(resource, state().message);
        act(activity -> assertEquals(context.getString(resource), ((TextView) activity.findViewById(R.id.friendsMessage)).getText().toString()));
    }
    private void act(java.util.function.Consumer<MainActivity> action) {
        SessionRoutingTest.onResumed(activity -> action.accept((MainActivity) activity));
    }
    private void shell(String command) throws Exception {
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))) {
            byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
        }
    }
    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(1200);
        var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        var directory = new java.io.File(context.getExternalFilesDir(null), "artifacts/4c");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new java.io.FileOutputStream(new java.io.File(directory, name))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }
}
