package com.taktak.app.ui.theme;

import android.app.Activity;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.taktak.app.*;
import com.taktak.app.ui.history.MomentViewerActivity;
import com.taktak.app.viewmodel.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * UI-THEME-07: History (Received/Sent), Friends & List Items test suite across 4 themes (Original, Gradient, Glass, Soft).
 * Fixture Retrofit trong RAM, không network, không thay auth store thật của người dùng.
 */
@RunWith(AndroidJUnit4.class)
public class ListThemeUiTest {
    @Rule public final org.junit.rules.Timeout timeout = org.junit.rules.Timeout.seconds(360);
    private Context context;
    private MediaThemeFixture fixture;
    private Map<String, ?> original;
    private File sessionFile;
    private byte[] sessionBytes;
    private boolean foreground;

    @Before public void setup() throws Exception {
        String optIn = InstrumentationRegistry.getArguments().getString("taktak.allowMediaQa");
        if (optIn != null) {
            assertEquals("Explicit USB QA opt-in", "true", optIn);
        }
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(context.getPackageName(), android.Manifest.permission.CAMERA);
        original = new HashMap<>(context.getSharedPreferences(ThemePreferences.FILE_NAME, Context.MODE_PRIVATE).getAll());
        sessionFile = new File(context.getApplicationInfo().dataDir, "shared_prefs/auth_session_v1.xml");
        sessionBytes = sessionFile.exists() ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null;
        fixture = new MediaThemeFixture(context);
    }

    @After public void cleanup() throws Exception {
        closeScreens();
        if (fixture != null) {
            fixture.close();
        }
        if (original != null) {
            var editor = context.getSharedPreferences(ThemePreferences.FILE_NAME, Context.MODE_PRIVATE).edit();
            for (String key : new String[]{ThemePreferences.STYLE, ThemePreferences.MODE, ThemePreferences.SCHEMA, ThemePreferences.REVISION}) {
                Object value = original.get(key);
                if (!original.containsKey(key)) editor.remove(key);
                else if (value instanceof String s) editor.putString(key, s);
                else if (value instanceof Integer i) editor.putInt(key, i);
                else if (value instanceof Long l) editor.putLong(key, l);
                else throw new AssertionError("Unsupported existing appearance type");
            }
            assertTrue(editor.commit());
            assertEquals(original, context.getSharedPreferences(ThemePreferences.FILE_NAME, Context.MODE_PRIVATE).getAll());
            assertArrayEquals("Personal session bytes unchanged", sessionBytes, sessionFile.exists()
                    ? java.nio.file.Files.readAllBytes(sessionFile.toPath()) : null);
        }
    }

    @Test public void allStylesModesLayoutsForHistoryAndFriends() {
        main(() -> {
            for (AppTheme style : AppTheme.values()) {
                for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                    for (float font : new float[]{1f, 1.5f, 2f}) {
                        for (int[] size : new int[][]{{280, 560}, {360, 720}, {840, 420}}) {
                            var config = new Configuration(context.getResources().getConfiguration());
                            config.fontScale = font;
                            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                            var themed = new androidx.appcompat.view.ContextThemeWrapper(
                                    context.createConfigurationContext(config),
                                    ThemeManager.resolve(new ThemeSelection(style, ColorMode.SYSTEM, 0), ThemeManager.ScreenRole.MAIN).themeResource());
                            float density = themed.getResources().getDisplayMetrics().density;
                            int w = Math.round(size[0] * density);
                            int h = Math.round(size[1] * density);

                            for (int layout : new int[]{
                                    R.layout.fragment_history,
                                    R.layout.item_history_moment,
                                    R.layout.item_history_footer,
                                    R.layout.fragment_friends,
                                    R.layout.item_friend,
                                    R.layout.item_friends_section
                            }) {
                                View root = LayoutInflater.from(themed).inflate(layout, null, false);
                                if (layout == R.layout.fragment_history) {
                                    TextView title = root.findViewById(R.id.historyTitle);
                                    if (title != null) title.setText(R.string.history_title);
                                    TabLayout tabs = root.findViewById(R.id.historyTabs);
                                    if (tabs != null) {
                                        tabs.addTab(tabs.newTab().setText(R.string.history_tab_received));
                                        tabs.addTab(tabs.newTab().setText(R.string.history_tab_sent));
                                    }
                                } else if (layout == R.layout.item_history_moment) {
                                    TextView name = root.findViewById(R.id.momentSenderName);
                                    if (name != null) name.setText("QA Sender Nguyễn Văn A");
                                    TextView username = root.findViewById(R.id.momentSenderUsername);
                                    if (username != null) username.setText("@qa_sender_username");
                                    TextView time = root.findViewById(R.id.momentTimestamp);
                                    if (time != null) time.setText("20:00, 19/10/2026");
                                    TextView caption = root.findViewById(R.id.momentCaption);
                                    if (caption != null) {
                                        caption.setVisibility(View.VISIBLE);
                                        caption.setText("Khoảnh khắc tuyệt vời cùng bạn bè! ✨");
                                    }
                                } else if (layout == R.layout.fragment_friends) {
                                    EditText query = root.findViewById(R.id.friendsQuery);
                                    if (query != null) query.setText("test_search_user");
                                } else if (layout == R.layout.item_friend) {
                                    TextView name = root.findViewById(R.id.friendName);
                                    if (name != null) name.setText("QA Friend Trần Thị B");
                                    TextView username = root.findViewById(R.id.friendUsername);
                                    if (username != null) username.setText("@qa_friend_username");
                                    Button primary = root.findViewById(R.id.friendPrimary);
                                    if (primary != null) primary.setText(R.string.friends_accept);
                                    Button secondary = root.findViewById(R.id.friendSecondary);
                                    if (secondary != null) secondary.setText(R.string.friends_reject);
                                } else if (layout == R.layout.item_friends_section) {
                                    TextView title = root.findViewById(R.id.sectionTitle);
                                    if (title != null) title.setText(R.string.friends_incoming);
                                    TextView message = root.findViewById(R.id.sectionMessage);
                                    if (message != null) message.setText(R.string.friends_empty_incoming);
                                }

                                boolean isScreen = layout == R.layout.fragment_history || layout == R.layout.fragment_friends;
                                root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                                        View.MeasureSpec.makeMeasureSpec(h, isScreen ? View.MeasureSpec.EXACTLY : View.MeasureSpec.UNSPECIFIED));
                                root.layout(0, 0, w, root.getMeasuredHeight());
                                try { bounds(root, w); }
                                catch (AssertionError failure) {
                                    throw new AssertionError("layout="+themed.getResources().getResourceEntryName(layout)
                                            +" style="+style+" night="+night+" font="+font+" viewport="+size[0]+"x"+size[1], failure);
                                }

                                assertEquals("Layout primary matches themed context",
                                        color(themed, androidx.appcompat.R.attr.colorPrimary),
                                        MaterialColors.getColor(root, androidx.appcompat.R.attr.colorPrimary));
                            }

                            int bg = color(themed, com.google.android.material.R.attr.colorSurface);
                            int ink = color(themed, com.google.android.material.R.attr.colorOnSurface);
                            assertTrue("List surface contrast WCAG >= 4.5",
                                    androidx.core.graphics.ColorUtils.calculateContrast(ink, bg) >= 4.5);
                        }
                    }
                }
            }
        });
    }

    @Test public void historyTabSwitchingAndEmptyStateSurvivesThemeChange() throws Exception {
        for (AppTheme style : AppTheme.values()) {
            select(style, ColorMode.LIGHT);
            MainActivity activity = launch(MainActivity.class);
            authorize(activity);
            main(() -> ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_history));
            await(() -> visible(activity, R.id.historyTabs), "History tabs visible");

            var historyModel = new AtomicReference<HistoryViewModel>();
            main(() -> historyModel.set(new ViewModelProvider(activity).get(HistoryViewModel.class)));
            await(() -> {
                var s = historyModel.get().state().getValue();
                return s != null && s.loaded && !s.busy;
            }, "History loaded for " + style.id());

            capture("history-received-" + style.id());
            main(() -> {
                assertEquals(R.string.history_title, activity.getString(R.string.history_title).equals(
                        ((TextView) activity.findViewById(R.id.historyTitle)).getText().toString()) ? R.string.history_title : 0);
                var tabs = (TabLayout) activity.findViewById(R.id.historyTabs);
                assertEquals(0, tabs.getSelectedTabPosition());
                var tab = tabs.getTabAt(1);
                assertNotNull(tab);
                tab.select();
            });

            await(() -> {
                var s = historyModel.get().state().getValue();
                return s != null && s.activeTab == HistoryViewModel.Tab.SENT && s.loaded && !s.busy;
            }, "Sent tab loaded for " + style.id());

            capture("history-sent-" + style.id());
            main(() -> {
                assertEquals(activity.getString(R.string.history_sent_title),
                        ((TextView) activity.findViewById(R.id.historyTitle)).getText().toString());
                var tabs = (TabLayout) activity.findViewById(R.id.historyTabs);
                assertEquals(1, tabs.getSelectedTabPosition());
            });

            closeScreens();
        }

        // Test theme pending banner and recreation preserving Sent tab
        select(AppTheme.GRADIENT, ColorMode.LIGHT);
        MainActivity activity = launch(MainActivity.class);
        authorize(activity);
        main(() -> ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_history));
        await(() -> visible(activity, R.id.historyTabs), "History tabs visible");
        main(() -> {
            var tabs = (TabLayout) activity.findViewById(R.id.historyTabs);
            var tab = tabs.getTabAt(1);
            if (tab != null) tab.select();
        });
        await(() -> {
            var s = new ViewModelProvider(activity).get(HistoryViewModel.class).state().getValue();
            return s != null && s.activeTab == HistoryViewModel.Tab.SENT;
        }, "Switched to Sent tab");

        main(() -> activity.startActivity(new Intent(activity, ThemeSettingsActivity.class)));
        ThemeSettingsActivity chooser = awaitActivity(ThemeSettingsActivity.class, null);
        select(AppTheme.SOFT, ColorMode.LIGHT);
        main(chooser::finish);
        await(() -> foreground(activity), "Return to MainActivity");
        authorize(activity);

        main(() -> {
            assertEquals(View.VISIBLE, activity.findViewById(R.id.themeMediaNotice).getVisibility());
            assertEquals(R.id.nav_history, ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).getSelectedItemId());
        });

        main(activity::recreate);
        MainActivity recreated = awaitActivity(MainActivity.class, activity);
        authorize(recreated);

        main(() -> {
            assertEquals(R.id.nav_history, ((BottomNavigationView) recreated.findViewById(R.id.bottomNavigation)).getSelectedItemId());
            var tabs = (TabLayout) recreated.findViewById(R.id.historyTabs);
            assertEquals(1, tabs.getSelectedTabPosition());
            assertEquals(recreated.getString(R.string.history_sent_title),
                    ((TextView) recreated.findViewById(R.id.historyTitle)).getText().toString());
        });
        closeScreens();
    }

    @Test public void friendsListSectionsAndConfirmDialogsThemedCorrectly() throws Exception {
        for (AppTheme style : new AppTheme[]{AppTheme.ORIGINAL, AppTheme.GLASS}) {
            select(style, ColorMode.LIGHT);
            MainActivity activity = launch(MainActivity.class);
            authorize(activity);
            main(() -> ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_friends));

            var friendsModel = new AtomicReference<FriendsViewModel>();
            main(() -> friendsModel.set(new ViewModelProvider(activity).get(FriendsViewModel.class)));
            await(() -> {
                var s = friendsModel.get().state().getValue();
                return s != null && s.loaded && !s.busy && !s.incoming.isEmpty() && !s.friends.isEmpty();
            }, "Friends loaded with fixture items for " + style.id());

            capture("friends-" + style.id());

            // Search query
            main(() -> {
                var query = (EditText) activity.findViewById(R.id.friendsQuery);
                query.setText("qa_searched");
                activity.findViewById(R.id.friendsSearch).performClick();
            });

            await(() -> {
                var s = friendsModel.get().state().getValue();
                return s != null && s.searched && !s.results.isEmpty();
            }, "Friends search completed for " + style.id());

            capture("friends-search-" + style.id());

            // Test MaterialAlertDialog theming
            var dialogRef = new AtomicReference<AlertDialog>();
            main(() -> {
                var dialog = new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.friends_block)
                        .setMessage(activity.getString(R.string.friends_confirm_block, "qa_searched"))
                        .setNegativeButton(R.string.friends_cancel, (d, w) -> d.dismiss())
                        .setPositiveButton(R.string.friends_block, (d, w) -> d.dismiss())
                        .show();
                dialogRef.set(dialog);
            });

            await(() -> dialogRef.get() != null && dialogRef.get().isShowing(), "Dialog showing");
            capture("friends-dialog-" + style.id());

            main(() -> {
                if (dialogRef.get() != null) dialogRef.get().dismiss();
            });
            closeScreens();
        }
    }

    @Test public void historyMomentItemInteractionAndNavigationToViewer() throws Exception {
        select(AppTheme.GRADIENT, ColorMode.LIGHT);
        MainActivity activity = launch(MainActivity.class);
        authorize(activity);
        main(() -> ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).setSelectedItemId(R.id.nav_history));

        var historyModel = new AtomicReference<HistoryViewModel>();
        main(() -> historyModel.set(new ViewModelProvider(activity).get(HistoryViewModel.class)));
        await(() -> {
            var s = historyModel.get().state().getValue();
            return s != null && s.loaded && !s.busy && !s.moments.isEmpty();
        }, "History moments loaded with fixture");

        await(() -> {
            var recycler = (RecyclerView) activity.findViewById(R.id.historyRecycler);
            return recycler != null && recycler.getChildCount() > 0 && recycler.findViewHolderForAdapterPosition(0) != null;
        }, "History recycler laid out moment item");
        main(() -> {
            var recycler = (RecyclerView) activity.findViewById(R.id.historyRecycler);
            var holder = recycler.findViewHolderForAdapterPosition(0);
            assertNotNull("Moment holder exists", holder);
            holder.itemView.performClick();
        });

        MomentViewerActivity viewer = awaitActivity(MomentViewerActivity.class, null);
        await(() -> enabled(viewer, R.id.viewerSaveButton), "Moment viewer ready");

        main(() -> {
            assertEquals("QA First", ((TextView) viewer.findViewById(R.id.viewerSenderName)).getText().toString());
            assertEquals("QA moment caption", ((TextView) viewer.findViewById(R.id.viewerCaption)).getText().toString());
        });

        capture("history-viewer-transition");

        main(viewer::finish);
        await(() -> foreground(activity), "Return to MainActivity");
        main(() -> assertEquals(R.id.nav_history, ((BottomNavigationView) activity.findViewById(R.id.bottomNavigation)).getSelectedItemId()));

        closeScreens();
    }

    private int color(Context themed, int attr) { return MaterialColors.getColor(themed, attr, "QA"); }

    private void bounds(View view, int width) {
        if (view.getVisibility() == View.GONE) return;
        String idName;
        try { idName = view.getId() != View.NO_ID ? view.getResources().getResourceEntryName(view.getId()) : "none"; }
        catch (Exception e) { idName = "none"; }
        assertTrue("Horizontal bounds: " + view.getClass().getSimpleName() + " [" + idName + "] left=" + view.getLeft() + " right=" + view.getRight() + " max=" + width,
                view.getLeft() >= 0 && view.getRight() <= width);
        if (view instanceof TextView text && !(text instanceof EditText) && text.getText() != null && text.getText().length() > 0 && text.getLayout() != null)
            assertTrue("Text fits height: " + view.getClass().getSimpleName() + " [" + idName + "] text="+text.getText()
                            +" layoutHeight="+text.getLayout().getHeight()+" viewHeight="+text.getHeight()
                            +" padding="+text.getCompoundPaddingTop()+","+text.getCompoundPaddingBottom()
                            +" parent="+text.getParent().getClass().getSimpleName(),
                    text.getLayout().getHeight() <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) bounds(group.getChildAt(i), group.getWidth());
        }
    }

    private void select(AppTheme style, ColorMode mode) throws Exception { new ThemePreferences(context).save(style, mode); }

    private void warm() throws Exception {
        if (foreground) return;
        try (var descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("am start -W -n com.taktak.app/.MainActivity");
             var input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
            byte[] buffer = new byte[1024]; while (input.read(buffer) != -1) { }
        }
        foreground = true;
    }

    private <T extends Activity> T launch(Class<T> type) throws Exception { return launch(type, new Intent(context, type)); }

    private <T extends Activity> T launch(Class<T> type, Intent intent) throws Exception {
        warm();
        if(type==MainActivity.class) {
            T result=awaitActivity(type,null);
            await(() -> foreground(result),"Main shell has focus after USB shell launch");
            return result;
        }
        var result = type.cast(InstrumentationRegistry.getInstrumentation().startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        await(() -> foreground(result), "Screen has focus");
        return result;
    }

    private <T extends Activity> T awaitActivity(Class<T> type, Activity previous) throws Exception {
        var result = new AtomicReference<T>();
        await(() -> {
            main(() -> {
                for (var activity : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))
                    if (type.isInstance(activity) && activity != previous) result.set(type.cast(activity));
            });
            return result.get() != null;
        }, "Activity resumes: " + type.getSimpleName());
        return result.get();
    }

    @SuppressWarnings("unchecked") private void authorize(Activity activity) throws Exception {
        await(() -> {
            var state = new AtomicReference<SessionViewModel.State>();
            main(() -> state.set(new ViewModelProvider((ViewModelStoreOwner) activity).get(SessionViewModel.class).state().getValue()));
            return state.get() != null && (state.get().destination == SessionViewModel.Destination.ERROR || state.get().destination == SessionViewModel.Destination.CAMERA);
        }, "Mock profile blocked before FCM/widget");
        main(() -> ((MutableLiveData<SessionViewModel.State>) new ViewModelProvider((ViewModelStoreOwner) activity)
                .get(SessionViewModel.class).state()).setValue(new SessionViewModel.State(SessionViewModel.Destination.CAMERA, 0)));
    }

    private boolean enabled(Activity activity, int id) {
        var result = new AtomicBoolean();
        main(() -> { View view = activity.findViewById(id); result.set(view != null && view.isEnabled()); });
        return result.get();
    }

    private boolean visible(Activity activity, int id) {
        var result = new AtomicBoolean();
        main(() -> result.set(activity.findViewById(id) != null && activity.findViewById(id).isShown()));
        return result.get();
    }

    private boolean foreground(Activity activity) {
        var result = new AtomicBoolean();
        main(() -> result.set(!activity.isFinishing() && activity.getWindow().getDecorView().hasWindowFocus()));
        return result.get();
    }

    private void await(BooleanSupplier condition, String message) throws Exception {
        long until = android.os.SystemClock.uptimeMillis() + 18000;
        while (android.os.SystemClock.uptimeMillis() < until) {
            if (condition.getAsBoolean()) return;
            android.os.SystemClock.sleep(80);
        }
        fail(message);
    }

    private void main(Runnable action) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) action.run();
        else InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private void closeScreens() {
        main(() -> {
            var monitor = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance();
            for (var stage : androidx.test.runner.lifecycle.Stage.values())
                for (Activity activity : new ArrayList<>(monitor.getActivitiesInStage(stage)))
                    if (activity instanceof MainActivity || activity instanceof MomentViewerActivity || activity instanceof ThemeSettingsActivity) {
                        activity.finish();
                    }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        foreground = false;
    }

    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(500);
        var image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        var folder = new File(context.getExternalFilesDir(null), "artifacts/theme07");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try (var out = new FileOutputStream(new File(folder, name + ".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally {
            image.recycle();
        }
    }
}
