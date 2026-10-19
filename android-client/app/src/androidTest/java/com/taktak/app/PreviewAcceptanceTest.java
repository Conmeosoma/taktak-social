package com.taktak.app;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.SystemClock;
import android.text.SpannableString;
import android.view.*;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels.UserProfile;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.*;
import com.taktak.app.data.session.*;
import com.taktak.app.ui.camera.*;
import com.taktak.app.viewmodel.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PreviewAcceptanceTest {
    @Rule public final org.junit.rules.Timeout timeout = org.junit.rules.Timeout.seconds(180);
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final android.content.SharedPreferences fixture = context.getSharedPreferences("qa5b_fixture", Context.MODE_PRIVATE);
    private AuthRepository auth;
    private UserProfile me;
    private boolean owned;
    private final List<AuthRepository> sharingPeers = new ArrayList<>();
    private static final class Memory implements SessionStore {
        private Session session;
        public Session load() { return session; }
        public void save(Session session) { this.session = session; }
        public void clear() { session = null; }
    }
    @Before public void ownFixture() throws Exception {
        auth = AuthGraph.repository(context);
        if (auth.hasSession()) {
            assertEquals("Never replace personal session", fixture.getString("owner", ""), auth.profile().id); auth.logout();
        }
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        String name = "qa5b_" + UUID.randomUUID().toString().replace("-", ""), password = UUID.randomUUID().toString();
        me = auth.register(name + "@example.test", name, "Preview QA", password);
        assertTrue(fixture.edit().putString("owner", me.id).commit()); owned = true;
        auth.login(name + "@example.test", password);
        InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), android.Manifest.permission.CAMERA);
    }
    @After public void cleanup() throws Exception {
        try {
            for (AuthRepository peer : sharingPeers) { peer.logout(); }
            if (owned) { auth.logout(); fixture.edit().clear().commit(); }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : new ArrayList<>(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))) { activity.finish(); }
            });
        }
    }
    @Test public void realCaptureCachesSquarePhotoAndRetakeDeletesOnlyDraft() throws Exception {
        launchCamera();
        main(activity -> activity.findViewById(R.id.captureButton).performClick());
        readyPreview(); AtomicReference<File> file = new AtomicReference<>();
        preview(activity -> {
            var state = model(activity).state().getValue(); file.set(state.file());
            assertNull(state.downloaded()); assertTrue(state.all()); assertFalse(state.sending());
            assertFalse(activity.findViewById(R.id.sendFriendsButton).isEnabled());
        });
        assertTrue(file.get().getCanonicalPath().startsWith(context.getCacheDir().getCanonicalPath() + File.separator));
        Bitmap image = BitmapFactory.decodeFile(file.get().getPath()); assertNotNull(image);
        assertEquals(image.getWidth(), image.getHeight()); image.recycle(); capture("preview-camera.png");
        preview(activity -> activity.findViewById(R.id.retakeButton).performClick()); readyCamera();
        await(() -> !file.get().exists(), "Retake discards private draft");
    }
    @Test public void unicodeCaptionAndMultipleRealFriendsSurviveRecreate() throws Exception {
        addFriend("An"); addFriend("Binh"); openSynthetic(); readyFriends(2);
        String emoji = new String(Character.toChars(0x1F642));
        preview(activity -> {
            var input = (EditText) activity.findViewById(R.id.captionInput);
            input.setText(emoji.repeat(51));
            assertEquals(50, input.getText().toString().codePointCount(0, input.length())); assertEquals(100, input.length());
            assertEquals("50/50 ký tự", ((TextView) activity.findViewById(R.id.captionCount)).getText().toString());
            activity.findViewById(R.id.allFriendsCheck).performClick();
        });
        clickRecipient(0); clickRecipient(1);
        preview(activity -> { assertFalse(model(activity).state().getValue().all()); assertEquals(2, model(activity).state().getValue().selected().size()); activity.recreate(); });
        readyPreview(); readyFriends(2);
        preview(activity -> {
            assertEquals(emoji.repeat(50), ((EditText) activity.findViewById(R.id.captionInput)).getText().toString());
            assertEquals(2, model(activity).state().getValue().selected().size());
        }); capture("preview-selected.png");
        preview(activity -> activity.findViewById(R.id.allFriendsCheck).performClick());
        preview(activity -> assertTrue(model(activity).state().getValue().all()));
    }
    @Test public void realCameraSendsSelectedFriendsAndDeniesUnselectedFriend() throws Exception {
        var first = connectedPeer("Selected A"); var second = connectedPeer("Selected B"); var excluded = connectedPeer("Not selected");
        String firstId = first.profile().id, secondId = second.profile().id;
        launchCamera(); main(activity -> activity.findViewById(R.id.captureButton).performClick()); readyPreview(); readyFriends(3);
        preview(activity -> {
            var vm = model(activity); vm.caption("Khoảnh khắc 5C 📷");
            ((EditText)activity.findViewById(R.id.captionInput)).setText(vm.caption());
            vm.selectAll(false); vm.toggle(firstId, true); vm.toggle(secondId, true);
            assertTrue(activity.findViewById(R.id.sendFriendsButton).isEnabled());
        });
        capture("5c-selected-before-send.png");
        preview(activity -> activity.findViewById(R.id.sendFriendsButton).performClick());
        assertShared(first, second, excluded); capture("5c-selected-sent.png");
    }
    @Test public void allFriendsSendResolvesBeyondFirstPage() throws Exception {
        for (int i = 0; i < 21; i++) { connectedPeer("All " + i); }
        openSynthetic(); readyFriends(20);
        preview(activity -> {
            assertTrue(model(activity).state().getValue().hasNext());
            assertTrue(model(activity).state().getValue().all());
            activity.findViewById(R.id.sendFriendsButton).performClick();
        });
        await(() -> previewStateIs(R.string.preview_sent), "All pages sent");
        AtomicReference<String> id = new AtomicReference<>(); AtomicReference<byte[]> bytes = new AtomicReference<>();
        preview(activity -> { id.set(field(model(activity), "momentId", String.class)); bytes.set(model(activity).state().getValue().downloaded()); });
        for (var peer : sharingPeers) { assertArrayEquals(bytes.get(), peer.moments().download(id.get())); }
    }
    @Test public void staleSelectedRecipientFailsWithoutSwitchingToPrivateAndCanBeChanged() throws Exception {
        var peer = connectedPeer("Block QA"); String peerId = peer.profile().id;
        File draft = openSynthetic(); readyFriends(1);
        preview(activity -> { model(activity).selectAll(false); model(activity).toggle(peerId, true); });
        peer.friends().block(me.id);
        preview(activity -> activity.findViewById(R.id.sendFriendsButton).performClick());
        await(() -> previewStateIs(R.string.preview_recipient_error), "Server rejects stale selection");
        preview(activity -> {
            var vm = model(activity); assertNull(field(vm, "momentId", String.class));
            assertNull(vm.state().getValue().downloaded()); assertFalse(vm.selectionLocked());
            assertTrue(activity.findViewById(R.id.captionInput).isEnabled());
            vm.selectAll(false); assertFalse(activity.findViewById(R.id.sendFriendsButton).isEnabled());
        });
        assertTrue(draft.isFile()); capture("5c-recipient-rejected.png");
    }
    @Test public void detailedPhotosHaveBoundedBytesDimensionsAndDoNotUpscaleSmallPhotos() throws Exception {
        Bitmap input = Bitmap.createBitmap(1600,1600,Bitmap.Config.ARGB_8888);
        int[] pixels = new int[1600*1600]; Random random = new Random(5);
        for (int i=0; i<pixels.length; i++) { pixels[i]=0xff000000 | random.nextInt(0x1000000); }
        input.setPixels(pixels,0,1600,0,0,1600,1600);
        byte[] bytes;
        try { bytes = PhotoProcessor.squareJpeg(input,90,true); } finally { input.recycle(); }
        assertTrue(bytes.length <= 800*1024);
        Bitmap result = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        assertEquals(result.getWidth(),result.getHeight()); assertTrue(result.getWidth()<=1280); result.recycle();
        Bitmap small = Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);
        try { bytes=PhotoProcessor.squareJpeg(small,0); } finally { small.recycle(); }
        result=BitmapFactory.decodeByteArray(bytes,0,bytes.length); assertEquals(64,result.getWidth()); result.recycle();
    }
    @Test public void friendSendOfflineRetryKeepsFrozenPayloadAndNeverSavesPrivate() throws Exception {
        org.junit.Assume.assumeTrue("Host-managed offline case", "true".equals(InstrumentationRegistry.getArguments().getString("taktakSharingOffline")));
        var peer=connectedPeer("Offline recipient"); String peerId=peer.profile().id;
        File file=openSynthetic(); readyFriends(1);
        preview(activity -> {
            var vm=model(activity); ((EditText)activity.findViewById(R.id.captionInput)).setText("Payload stays 📷");
            vm.selectAll(false); vm.toggle(peerId,true);
        });
        String run=InstrumentationRegistry.getArguments().getString("taktakQaRun", "default");
        android.util.Log.i("TaktakQa", "SHARE_READY_OFFLINE_"+run);
        long end=SystemClock.elapsedRealtime()+45000;
        while(healthy() && SystemClock.elapsedRealtime()<end) { SystemClock.sleep(200); }
        assertFalse("Host disconnected smoke HTTP",healthy());
        preview(activity -> activity.findViewById(R.id.sendFriendsButton).performClick());
        await(() -> previewStateIs(R.string.moment_transfer_error),"Offline send retained");
        preview(activity -> {
            var vm=model(activity); assertTrue(vm.friendTransfer()); assertTrue(vm.selectionLocked());
            assertNull(field(vm,"momentId",String.class)); assertTrue(file.isFile());
            assertFalse(activity.findViewById(R.id.savePrivateButton).isEnabled());
            vm.caption("Changed"); vm.selectAll(true); vm.toggle(peerId,false);
            assertEquals("Payload stays 📷",vm.caption()); assertFalse(vm.state().getValue().all());
            assertEquals(Set.of(peerId),vm.state().getValue().selected());
        });
        capture("5c-send-offline.png"); android.util.Log.i("TaktakQa","SHARE_WAIT_RESTORE_"+run);
        end=SystemClock.elapsedRealtime()+45000;
        while(!healthy() && SystemClock.elapsedRealtime()<end) { SystemClock.sleep(200); }
        assertTrue("Host restored smoke HTTP",healthy());
        preview(activity -> activity.findViewById(R.id.sendFriendsButton).performClick());
        await(() -> previewStateIs(R.string.preview_sent),"Retry sends original selected payload");
        AtomicReference<String> id=new AtomicReference<>(); AtomicReference<byte[]> bytes=new AtomicReference<>();
        preview(activity -> { id.set(field(model(activity),"momentId",String.class)); bytes.set(model(activity).state().getValue().downloaded()); });
        assertArrayEquals(bytes.get(),peer.moments().download(id.get())); assertFalse(file.exists()); capture("5c-send-retry-success.png");
    }
    private AuthRepository connectedPeer(String name) throws Exception {
        var peer = new AuthRepository(new ApiClient(BuildConfig.BASE_URL,new Memory())); sharingPeers.add(peer);
        String username="peer5c_"+UUID.randomUUID().toString().replace("-", ""),password=UUID.randomUUID().toString();
        var user=peer.register(username+"@example.test",username,name,password);
        try { peer.login(username+"@example.test",password); }
        catch (com.taktak.app.data.remote.ApiFailure failure) {
            if(failure.kind!=com.taktak.app.data.remote.ApiFailure.Kind.RATE_LIMIT) throw failure;
            SystemClock.sleep(failure.retryAfterSeconds*1000L+200); peer.login(username+"@example.test",password);
        }
        auth.friends().send(user.id); peer.friends().respond(peer.friends().incoming(0).items.get(0).id,true);
        return peer;
    }
    private void assertShared(AuthRepository first, AuthRepository second, AuthRepository excluded) throws Exception {
        await(() -> previewStateIs(R.string.preview_sent), "Selected friends uploaded and downloaded");
        AtomicReference<String> id=new AtomicReference<>(); AtomicReference<byte[]> bytes=new AtomicReference<>();
        preview(activity -> {
            id.set(field(model(activity),"momentId",String.class)); bytes.set(model(activity).state().getValue().downloaded());
            assertFalse(activity.findViewById(R.id.sendFriendsButton).isEnabled());
            assertFalse(activity.findViewById(R.id.savePrivateButton).isEnabled());
            assertEquals(context.getString(R.string.preview_sent_title),((TextView)activity.findViewById(R.id.previewTitle)).getText().toString());
        });
        assertArrayEquals(bytes.get(),first.moments().download(id.get())); assertArrayEquals(bytes.get(),second.moments().download(id.get()));
        try { excluded.moments().download(id.get()); fail("Unselected friend denied"); }
        catch(com.taktak.app.data.remote.ApiFailure failure) { assertEquals(404,failure.status); }
    }
    @Test public void missingExpiredAndOtherGenerationDraftsAreNeverShown() throws Exception {
        long generation = auth.sessionGeneration();
        var repository = new CaptureDraftRepository(context, generation);
        String id = repository.write(jpeg()); File file = repository.file(id);
        assertTrue(file.setLastModified(System.currentTimeMillis() - CaptureDraftRepository.MAX_AGE_MILLIS - 10000));
        try { repository.file(id); fail("Expired draft"); } catch (IOException expected) { }
        try { repository.file("../escape"); fail("Traversal"); } catch (IOException expected) { }
        launchCamera(); main(activity -> activity.startActivity(new Intent(activity, PreviewActivity.class)
                .putExtra(PreviewActivity.DRAFT_ID, id).putExtra(PreviewActivity.GENERATION, generation)));
        await(() -> previewStateIs(R.string.preview_missing), "Expired draft shows recovery");
        preview(activity -> assertFalse(activity.findViewById(R.id.savePrivateButton).isEnabled()));
        preview(Activity::finish); readyCamera(); await(() -> !file.exists(), "Expired owned draft cleaned");
        main(activity -> activity.startActivity(new Intent(activity, PreviewActivity.class)
                .putExtra(PreviewActivity.DRAFT_ID, UUID.randomUUID().toString()).putExtra(PreviewActivity.GENERATION, generation + 1)));
        await(() -> previewStateIs(R.string.preview_missing), "Cross-session draft rejected");
        preview(activity -> assertNull(model(activity).state().getValue().file()));
    }
    @Test public void recipientPaginationLoadsRealSecondPageWithoutLosingSelection() throws Exception {
        for(int i=0;i<21;i++) { addFriend("Friend " + i); }
        openSynthetic(); readyFriends(20);
        preview(activity -> {
            assertTrue(model(activity).state().getValue().all()); assertTrue(model(activity).state().getValue().hasNext());
            activity.findViewById(R.id.allFriendsCheck).performClick();
        });
        clickRecipient(0);
        preview(activity -> activity.findViewById(R.id.friendsMoreButton).performClick()); readyFriends(21);
        preview(activity -> {
            assertEquals(1,model(activity).state().getValue().selected().size());
            assertFalse(model(activity).state().getValue().hasNext());
            assertEquals(View.GONE,activity.findViewById(R.id.friendsMoreButton).getVisibility());
        });
        clickRecipient(20);
        preview(activity -> assertEquals(2,model(activity).state().getValue().selected().size()));
    }
    @Test public void accountSwitchCannotReusePreviousDraftOrCaption() throws Exception {
        File oldFile=openSynthetic(); readyPreview();
        preview(activity -> ((EditText)activity.findViewById(R.id.captionInput)).setText("Private draft A"));
        auth.logout();
        String name="qa5b_switch_"+UUID.randomUUID().toString().replace("-", ""), password=UUID.randomUUID().toString();
        UserProfile next=auth.register(name+"@example.test",name,"Preview QA B",password);
        assertTrue(fixture.edit().putString("owner",next.id).commit()); auth.login(name+"@example.test",password);
        preview(activity -> new ViewModelProvider(activity).get(SessionViewModel.class).check(true));
        await(() -> previewStateIs(R.string.preview_missing),"New session rejects previous draft");
        preview(activity -> {
            assertNull(model(activity).state().getValue().file()); assertNull(model(activity).state().getValue().downloaded());
            assertEquals("",((EditText)activity.findViewById(R.id.captionInput)).getText().toString());
            assertFalse(activity.findViewById(R.id.savePrivateButton).isEnabled());
            activity.finish();
        });
        readyCamera(); await(() -> !oldFile.exists(),"Old owned draft discarded");
    }
    @Test public void sessionOfflineHidesPreviewAndRetryKeepsDraftAndCaption() throws Exception {
        Assume.assumeTrue("Host-managed connector required", "true".equals(InstrumentationRegistry.getArguments().getString("taktakPreviewOffline")));
        File file=openSynthetic(); readyPreview();
        preview(activity -> ((EditText)activity.findViewById(R.id.captionInput)).setText("Giữ bản nháp"));
        android.util.Log.i("TaktakQa","PREVIEW_READY_OFFLINE_" + InstrumentationRegistry.getArguments().getString("taktakQaRun","default"));
        await(() -> !healthy(),"Host stops QA connector");
        preview(activity -> new ViewModelProvider(activity).get(SessionViewModel.class).check(true));
        await(() -> {
            AtomicBoolean hidden=new AtomicBoolean(); preview(activity -> hidden.set(!activity.findViewById(R.id.previewScroll).isShown()
                    && activity.findViewById(R.id.previewSessionRetry).isShown())); return hidden.get();
        },"Offline session hides private preview");
        assertTrue(file.exists()); capture("preview-session-offline.png");
        android.util.Log.i("TaktakQa","PREVIEW_WAITING_RESTORE");
        await(this::healthy,"Host restores QA connector");
        preview(activity -> activity.findViewById(R.id.previewSessionRetry).performClick()); readyPreview();
        preview(activity -> assertEquals("Giữ bản nháp",((EditText)activity.findViewById(R.id.captionInput)).getText().toString()));
        assertTrue(file.exists()); capture("preview-session-recovered.png");
    }
    @Test public void privateSaveUsesExplicitConfirmationAndDownloadsJwtImage() throws Exception {
        openSynthetic(); readyPreview(); readyFriends(0);
        preview(activity -> ((EditText) activity.findViewById(R.id.captionInput)).setText("Riêng tư 🙂"));
        // Dùng chính button/dialog người dùng; không chụp là upload ngầm.
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.savePrivateButton))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo(), androidx.test.espresso.action.ViewActions.click());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button2))
                .perform(androidx.test.espresso.action.ViewActions.click());
        preview(activity -> {
            assertNull(field(model(activity),"momentId",String.class)); assertNotNull(model(activity).state().getValue().file());
        });
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.savePrivateButton))
                .perform(androidx.test.espresso.action.ViewActions.scrollTo(), androidx.test.espresso.action.ViewActions.click());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button1))
                .perform(androidx.test.espresso.action.ViewActions.click());
        await(() -> previewStateIs(R.string.moment_downloaded), "Explicit private upload and GET");
        AtomicReference<byte[]> displayed = new AtomicReference<>(); AtomicReference<String> id = new AtomicReference<>();
        preview(activity -> {
            var state = model(activity).state().getValue(); displayed.set(state.downloaded());
            id.set(field(model(activity), "momentId", String.class)); assertNotNull(displayed.get());
            assertFalse(activity.findViewById(R.id.savePrivateButton).isEnabled());
            assertNull(state.file());
        });
        assertArrayEquals(auth.moments().download(id.get()), displayed.get()); capture("preview-private-success.png");
    }
    @Test public void mirrorRotationAndUnicodeReplacementKeepCorrectPixelsAndText() throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888); Canvas canvas = new Canvas(bitmap); Paint paint = new Paint();
        int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
        for (int i = 0; i < 4; i++) { paint.setColor(colors[i]); canvas.drawRect((i % 2) * 200, (i / 2) * 150, (i % 2 + 1) * 200, (i / 2 + 1) * 150, paint); }
        int[][] rotations = {{0,1,2,3},{2,0,3,1},{3,2,1,0},{1,3,0,2}};
        try {
            for (int angle = 0; angle < 4; angle++) for (boolean mirror : new boolean[]{false,true}) {
                byte[] bytes = PhotoProcessor.squareJpeg(bitmap, angle * 90, mirror);
                Bitmap result = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                assertEquals(result.getWidth(), result.getHeight()); assertTrue(bytes.length <= 800*1024);
                int a=result.getWidth()/4,b=result.getWidth()*3/4;
                int[] actual={result.getPixel(a,a),result.getPixel(b,a),result.getPixel(a,b),result.getPixel(b,b)};
                for(int corner=0;corner<4;corner++) {
                    int expected=colors[rotations[angle][mirror ? corner^1 : corner]];
                    assertTrue(Math.abs(Color.red(expected)-Color.red(actual[corner]))<20);
                    assertTrue(Math.abs(Color.green(expected)-Color.green(actual[corner]))<20);
                    assertTrue(Math.abs(Color.blue(expected)-Color.blue(actual[corner]))<20);
                } result.recycle();
            }
        } finally { bitmap.recycle(); }
        CaptionFilter filter = new CaptionFilter(); String emoji=new String(Character.toChars(0x1F642));
        assertNull(filter.filter("",0,0,new SpannableString(emoji.repeat(50)),0,2));
        assertNull(filter.filter(emoji,0,2,new SpannableString(emoji.repeat(50)),0,2));
        assertEquals("",filter.filter(emoji,0,2,new SpannableString(emoji.repeat(50)),100,100));
        assertEquals("a",filter.filter("\0",0,1,new SpannableString("abc"),0,1).toString());
    }
    @Test public void previewLogoutRemovesDraftAndReturnsToRootLogin() throws Exception {
        File file=openSynthetic(); readyPreview();
        preview(activity -> activity.findViewById(R.id.previewLogoutButton).performClick());
        SessionRoutingTest.awaitView(R.id.emailInput);
        assertFalse(auth.hasSession()); await(() -> !file.exists(), "Logout deletes owned draft");
        SessionRoutingTest.onResumed(activity -> { assertTrue(activity.isTaskRoot()); assertNull(activity.findViewById(R.id.previewImage)); });
        capture("preview-logout.png");
    }
    @Test public void previewFitsBothThemesLargeTextAndNarrowWideViewports() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (int night : new int[]{android.content.res.Configuration.UI_MODE_NIGHT_NO,android.content.res.Configuration.UI_MODE_NIGHT_YES}) {
                var config=new android.content.res.Configuration(context.getResources().getConfiguration()); config.fontScale=1.5f;
                config.uiMode=(config.uiMode & ~android.content.res.Configuration.UI_MODE_NIGHT_MASK)|night;
                var themed=new androidx.appcompat.view.ContextThemeWrapper(context.createConfigurationContext(config),R.style.Theme_TakTak_Media_Preview);
                float density=themed.getResources().getDisplayMetrics().density;
                for(int[] size:new int[][]{{280,560},{360,800},{840,420}}) {
                    View root=LayoutInflater.from(themed).inflate(R.layout.activity_preview,null,false);
                    root.findViewById(R.id.previewScroll).setVisibility(View.VISIBLE);
                    root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(size[0]*density),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.round(size[1]*density),View.MeasureSpec.EXACTLY));
                    root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight()); bounds(root,root.getWidth());
                    View image=root.findViewById(R.id.previewImage); assertEquals(image.getWidth(),image.getHeight());
                    assertTrue(root.findViewById(R.id.previewContent).getWidth()<=480*density+1);
                }
            }
        });
    }
    @Test public void actualThemesAndKeyboardKeepCaptionVisible() throws Exception {
        int original=androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        try {
            openSynthetic();
            for(int mode:new int[]{androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES}) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode));
                readyPreview(); SystemClock.sleep(1000); readyPreview();
                capture(mode==androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO?"preview-light.png":"preview-dark.png");
            }
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.captionInput))
                    .perform(androidx.test.espresso.action.ViewActions.scrollTo(),androidx.test.espresso.action.ViewActions.click());
            await(() -> { AtomicBoolean shown=new AtomicBoolean(); preview(activity -> {
                var insets=androidx.core.view.ViewCompat.getRootWindowInsets(activity.findViewById(R.id.previewRoot));
                shown.set(insets!=null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()));
            }); return shown.get(); },"IME visible for caption");
            preview(activity -> {
                Rect field=new Rect(); assertTrue(activity.findViewById(R.id.captionInput).getGlobalVisibleRect(field));
                View root=activity.findViewById(R.id.previewRoot); int[] xy=new int[2]; root.getLocationOnScreen(xy);
                assertTrue("Caption above keyboard",field.bottom<=xy[1]+root.getHeight()-root.getPaddingBottom());
            }); capture("preview-keyboard.png"); androidx.test.espresso.Espresso.closeSoftKeyboard();
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(original));
        }
    }
    private void bounds(View view,int width) {
        if(view.getVisibility()==View.GONE) return;
        assertTrue(view.getClass().getSimpleName(),view.getLeft()>=0 && view.getRight()<=width);
        if(view instanceof TextView text && text.getLayout()!=null) assertTrue("Text fits",text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        if(view instanceof ViewGroup group) for(int i=0;i<group.getChildCount();i++) bounds(group.getChildAt(i),group.getWidth());
    }
    private void addFriend(String name) throws Exception {
        AuthRepository peer=new AuthRepository(new ApiClient(BuildConfig.BASE_URL,new Memory()));
        String username="peer5b_"+UUID.randomUUID().toString().replace("-", ""),password=UUID.randomUUID().toString();
        try {
            UserProfile user=peer.register(username+"@example.test",username,name,password);
            try { peer.login(username+"@example.test",password); }
            catch(com.taktak.app.data.remote.ApiFailure failure) {
                if(failure.kind!=com.taktak.app.data.remote.ApiFailure.Kind.RATE_LIMIT) throw failure;
                android.util.Log.i("TaktakQa","PREVIEW_QA_LOGIN_COOLDOWN_SECONDS="+failure.retryAfterSeconds);
                SystemClock.sleep(failure.retryAfterSeconds*1000L+200);
                peer.login(username+"@example.test",password);
            }
            auth.friends().send(user.id); peer.friends().respond(peer.friends().incoming(0).items.get(0).id,true);
        } finally { peer.logout(); }
    }
    private File openSynthetic() throws Exception {
        var drafts=new CaptureDraftRepository(context,auth.sessionGeneration()); String id=drafts.write(jpeg()); File file=drafts.file(id);
        launchCamera(); main(activity -> activity.startActivity(new Intent(activity,PreviewActivity.class)
                .putExtra(PreviewActivity.DRAFT_ID,id).putExtra(PreviewActivity.GENERATION,drafts.generation)));
        readyPreview(); return file;
    }
    private byte[] jpeg() throws Exception {
        Bitmap image=Bitmap.createBitmap(320,240,Bitmap.Config.ARGB_8888); image.eraseColor(Color.rgb(240,180,160));
        try { return PhotoProcessor.squareJpeg(image,0); } finally { image.recycle(); }
    }
    private void launchCamera() throws Exception {
        try(var input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("am start -W -n com.taktak.app/.MainActivity"))) { byte[] buffer=new byte[1024]; while(input.read(buffer)!=-1) { } }
        readyCamera();
    }
    private void readyCamera() { await(() -> {
        AtomicBoolean ready=new AtomicBoolean(); inspect(activity -> { View button=activity.findViewById(R.id.captureButton); ready.set(activity instanceof MainActivity && button!=null && button.isShown() && button.isEnabled()); }); return ready.get();
    },"Camera ready"); }
    private void readyPreview() { await(() -> {
        AtomicBoolean ready=new AtomicBoolean(); inspect(activity -> { if(activity instanceof PreviewActivity preview) { var state=model(preview).state().getValue(); ready.set(preview.findViewById(R.id.previewScroll).isShown() && !state.loading() && state.file()!=null); } }); return ready.get();
    },"Preview file ready"); }
    private void readyFriends(int size) { await(() -> {
        AtomicBoolean ready=new AtomicBoolean(); inspect(activity -> { if(activity instanceof PreviewActivity preview) { var state=model(preview).state().getValue(); ready.set(!state.loadingFriends() && state.friends().size()==size); } }); return ready.get();
    },"Real accepted friends loaded"); }
    private boolean previewStateIs(int resource) {
        AtomicBoolean matches=new AtomicBoolean(); inspect(activity -> { if(activity instanceof PreviewActivity preview) matches.set(model(preview).state().getValue().message()==resource); }); return matches.get();
    }
    private void clickRecipient(int position) {
        preview(activity -> ((RecyclerView)activity.findViewById(R.id.recipientsList)).scrollToPosition(position));
        await(() -> { AtomicBoolean clicked=new AtomicBoolean(); preview(activity -> {
            var row=((RecyclerView)activity.findViewById(R.id.recipientsList)).findViewHolderForAdapterPosition(position);
            if(row!=null) { row.itemView.performClick(); clicked.set(true); }
        }); return clicked.get(); },"Recipient row click");
    }
    private PreviewViewModel model(PreviewActivity activity) { return new ViewModelProvider(activity).get(PreviewViewModel.class); }
    private void main(Consumer<MainActivity> action) { awaitActivity(MainActivity.class,action); }
    private void preview(Consumer<PreviewActivity> action) { awaitActivity(PreviewActivity.class,action); }
    private <T extends Activity> void awaitActivity(Class<T> type,Consumer<T> action) {
        await(() -> { AtomicBoolean done=new AtomicBoolean(); inspect(activity -> { if(type.isInstance(activity)) { action.accept(type.cast(activity)); done.set(true); } }); return done.get(); },type.getSimpleName()+" resumed");
    }
    private void inspect(Consumer<Activity> action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
        var activities=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED);
        for(Activity activity:new ArrayList<>(activities)) action.accept(activity);
    }); }
    private void await(BooleanSupplier condition,String message) { long end=SystemClock.elapsedRealtime()+40000; while(SystemClock.elapsedRealtime()<end) { if(condition.getAsBoolean()) return; SystemClock.sleep(100); } fail(message); }
    private <T> T field(Object object,String name,Class<T> type) {
        try { var field=object.getClass().getDeclaredField(name); field.setAccessible(true); return type.cast(field.get(object)); } catch(ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private void capture(String name) throws Exception {
        SystemClock.sleep(700); var image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(image);
        File dir=new File(context.getExternalFilesDir(null),"artifacts/5b"); assertTrue(dir.isDirectory()||dir.mkdirs());
        try(var output=new FileOutputStream(new File(dir,name))) { assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,output)); } finally { image.recycle(); }
    }
    private boolean healthy() {
        java.net.HttpURLConnection connection=null;
        try {
            connection=(java.net.HttpURLConnection)new java.net.URL(BuildConfig.BASE_URL+"actuator/health").openConnection();
            connection.setConnectTimeout(500); connection.setReadTimeout(500); return connection.getResponseCode()==200;
        } catch(IOException unavailable) { return false; }
        finally { if(connection!=null) connection.disconnect(); }
    }
}
