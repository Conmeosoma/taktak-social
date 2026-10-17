package com.taktak.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels.UserProfile;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.repository.MomentRepository;
import com.taktak.app.data.session.EncryptedSessionStore;
import com.taktak.app.data.session.Session;
import com.taktak.app.data.session.SessionStore;
import com.taktak.app.viewmodel.HistoryViewModel;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HistoryAcceptanceTest {
    @Rule public final Timeout timeout = Timeout.seconds(180);
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private AuthRepository primary;
    private AuthRepository secondary;
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
        primary = AuthGraph.repository(context);
        if (new EncryptedSessionStore(context, BuildConfig.BASE_URL).load() != null) {
            String owner = context.getSharedPreferences("qa5e_fixture", Context.MODE_PRIVATE).getString("owner", "");
            if (!owner.isEmpty()) {
                assertEquals("Do not replace a session not owned by this fixture", owner, primary.profile().id);
                primary.logout();
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        me = account(primary, "Receiver Bob");
        ownsSession = true;

        secondaryClient = new ApiClient(BuildConfig.BASE_URL, new Memory());
        secondary = new AuthRepository(secondaryClient);
        other = account(secondary, "Sender Alice");

        shell("am start -W -n com.taktak.app/.MainActivity");
        SessionRoutingTest.awaitView(R.id.bottomNavigation);
    }

    @After public void stop() throws Exception {
        try {
            try {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                    var activities = new ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED));
                    for (var activity : activities) { activity.finish(); }
                });
            } finally {
                if (ownsSession) {
                    primary.logout();
                    assertTrue(context.getSharedPreferences("qa5e_fixture", Context.MODE_PRIVATE).edit().remove("owner").commit());
                }
            }
        } finally {
            if (secondary != null) { secondary.logout(); }
        }
    }

    private UserProfile account(AuthRepository repository, String name) throws Exception {
        String username = "qa5e_" + UUID.randomUUID().toString().replace("-", "");
        String password = UUID.randomUUID().toString();
        var user = repository.register(username + "@example.test", username, name, password);
        if (repository == primary) {
            ownsSession = true;
            assertTrue(context.getSharedPreferences("qa5e_fixture", Context.MODE_PRIVATE).edit().putString("owner", user.id).commit());
        }
        repository.login(username + "@example.test", password);
        return user;
    }

    private void act(java.util.function.Consumer<MainActivity> action) {
        SessionRoutingTest.onResumed(activity -> action.accept((MainActivity) activity));
    }

    private HistoryViewModel.State state() {
        AtomicReference<HistoryViewModel.State> result = new AtomicReference<>();
        act(activity -> result.set(new androidx.lifecycle.ViewModelProvider(activity).get(HistoryViewModel.class).state().getValue()));
        return result.get();
    }

    private void ready() {
        SessionRoutingTest.awaitView(R.id.historyPage);
        long end = android.os.SystemClock.elapsedRealtime() + 35000;
        while ((state() == null || state().busy) && android.os.SystemClock.elapsedRealtime() < end) {
            android.os.SystemClock.sleep(50);
        }
        assertNotNull(state());
        assertFalse("History operation timed out", state().busy);
    }

    private void shell(String command) throws Exception {
        try (var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))) {
            byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
        }
    }

    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(1200);
        var bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        var directory = new File(context.getExternalFilesDir(null), "artifacts/5e");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (var output = new FileOutputStream(new File(directory, name))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }

    @Test public void historyTabShowsEmptyStateWhenNoMomentsReceived() throws Exception {
        act(MainNavigationTestSupport::history);
        ready();
        capture("history-empty.png");

        act(activity -> {
            TextView title = activity.findViewById(R.id.historyTitle);
            assertEquals(context.getString(R.string.history_title), title.getText().toString());
            assertTrue(activity.findViewById(R.id.historyEmptyView).isShown());
            assertEquals(context.getString(R.string.history_empty),
                    ((TextView) activity.findViewById(R.id.historyEmptyTitle)).getText().toString());
            assertFalse(activity.findViewById(R.id.historyRecycler).isShown());
        });

        // Test refresh
        act(activity -> activity.findViewById(R.id.historyRefresh).performClick());
        ready();
        act(activity -> assertTrue(activity.findViewById(R.id.historyEmptyView).isShown()));
    }

    @Test public void receivedMomentAppearsInHistoryWithSenderCaptionAndDownloadedImage() throws Exception {
        // 1. Establish friendship between Alice (other) and Bob (me)
        secondary.friends().send(me.id);
        var incoming = primary.friends().incoming(0);
        assertFalse(incoming.items.isEmpty());
        primary.friends().respond(incoming.items.get(0).id, true);

        // 2. Alice generates and uploads a test JPEG moment targeting Bob
        Bitmap testBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        testBitmap.eraseColor(android.graphics.Color.BLUE);
        byte[] jpegBytes;
        try (var stream = new java.io.ByteArrayOutputStream()) {
            assertTrue(testBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream));
            jpegBytes = stream.toByteArray();
        } finally {
            testBitmap.recycle();
        }

        String caption = "Khoảnh khắc 5E test";
        String momentId = new MomentRepository(secondaryClient).upload(jpegBytes, caption, List.of(me.id));
        assertNotNull(momentId);

        // 3. Bob switches to History tab and refreshes
        act(MainNavigationTestSupport::history);
        ready();
        act(activity -> activity.findViewById(R.id.historyRefresh).performClick());
        ready();

        assertEquals(1, state().moments.size());
        assertEquals(momentId, state().moments.get(0).id);

        // 4. Verify moment card in RecyclerView
        long deadline = android.os.SystemClock.elapsedRealtime() + 20000;
        AtomicReference<ImageView> imageRef = new AtomicReference<>();
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            act(activity -> {
                RecyclerView recycler = activity.findViewById(R.id.historyRecycler);
                if (recycler.isShown() && recycler.getAdapter() != null && recycler.getAdapter().getItemCount() > 0) {
                    var holder = recycler.findViewHolderForAdapterPosition(0);
                    if (holder == null && recycler.getChildCount() > 0) {
                        holder = recycler.getChildViewHolder(recycler.getChildAt(0));
                    }
                    if (holder != null) {
                        TextView sender = holder.itemView.findViewById(R.id.momentSenderName);
                        TextView cap = holder.itemView.findViewById(R.id.momentCaption);
                        ImageView img = holder.itemView.findViewById(R.id.momentImage);
                        if (sender != null && other.displayName.equals(sender.getText().toString())
                                && cap != null && caption.equals(cap.getText().toString())
                                && img != null && img.getDrawable() != null) {
                            imageRef.set(img);
                        }
                    }
                }
            });
            if (imageRef.get() != null) { break; }
            android.os.SystemClock.sleep(300);
        }

        assertNotNull("Moment card image must be loaded with valid drawable", imageRef.get());
        capture("history-received-moment.png");

        // 5. Test activity recreation preserves History
        act(MainActivity::recreate);
        ready();
        act(activity -> {
            RecyclerView recycler = activity.findViewById(R.id.historyRecycler);
            assertTrue(recycler.isShown());
            assertTrue(recycler.getAdapter().getItemCount() > 0);
        });
        capture("history-recreated.png");
    }
}
