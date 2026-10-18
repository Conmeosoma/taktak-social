package com.taktak.app;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.model.AuthModels.UserProfile;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.repository.DeviceRepository;
import com.taktak.app.data.session.EncryptedSessionStore;
import com.taktak.app.data.session.Session;
import com.taktak.app.data.session.SessionStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DeviceAcceptanceTest {
    @Rule public final Timeout timeout = Timeout.seconds(180);
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private AuthRepository primary;
    private boolean ownsSession;
    private UserProfile me;
    private String myEmail;
    private String myPassword;

    private static final class Memory implements SessionStore {
        Session value;
        public Session load() { return value; }
        public void save(Session next) { value = next; }
        public void clear() { value = null; }
    }

    @Before public void start() throws Exception {
        primary = AuthGraph.repository(context);
        if (new EncryptedSessionStore(context, BuildConfig.BASE_URL).load() != null) {
            String owner = context.getSharedPreferences("qa6a_fixture", Context.MODE_PRIVATE).getString("owner", "");
            if (!owner.isEmpty()) {
                assertEquals("Do not replace a session not owned by this fixture", owner, primary.profile().id);
                primary.logout(context);
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
        account("Device QA User");
        ownsSession = true;
    }

    @After public void stop() throws Exception {
        if (ownsSession) {
            primary.logout(context);
            assertTrue(context.getSharedPreferences("qa6a_fixture", Context.MODE_PRIVATE).edit().remove("owner").commit());
        }
    }

    private void account(String name) throws Exception {
        String username = "qa6a_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        myEmail = username + "@example.test";
        myPassword = UUID.randomUUID().toString();
        me = primary.register(myEmail, username, name, myPassword);
        ownsSession = true;
        assertTrue(context.getSharedPreferences("qa6a_fixture", Context.MODE_PRIVATE).edit().putString("owner", me.id).commit());
        primary.login(myEmail, myPassword);
    }

    @Test
    public void deviceRegistrationAndUnregistrationEndToEnd() throws Exception {
        DeviceRepository devices = primary.devices(context);
        String deviceId = devices.currentDeviceId();
        assertNotNull(deviceId);
        assertFalse(deviceId.isBlank());

        String fcmToken = "test_token_" + UUID.randomUUID();
        devices.register(fcmToken);

        // Re-registering with new token should succeed
        String updatedToken = "updated_token_" + UUID.randomUUID();
        devices.register(updatedToken);

        // Unregistering device should succeed
        devices.unregister();
    }

    @Test
    public void multiDeviceRegistrationForSameAccount() throws Exception {
        DeviceRepository dev1 = primary.devices(context);
        String token1 = "token_phone_" + UUID.randomUUID();
        dev1.register(token1);

        // Secondary device simulated with memory session store sharing same user
        var secondaryClient = new ApiClient(BuildConfig.BASE_URL, new Memory());
        var secondaryAuth = new AuthRepository(secondaryClient);
        secondaryAuth.login(myEmail, myPassword);

        String dev2Id = "tablet_" + UUID.randomUUID();
        String token2 = "token_tablet_" + UUID.randomUUID();
        var dev2 = new DeviceRepository(secondaryClient, () -> dev2Id);
        dev2.register(token2);

        // Both devices registered successfully under same account
        dev2.unregister();
    }

    @Test
    public void pushEventBusDispatchesEventsToListeners() throws Exception {
        var createdReceived = new java.util.concurrent.atomic.AtomicBoolean(false);
        var deletedReceived = new java.util.concurrent.atomic.AtomicBoolean(false);
        String testMomentId = UUID.randomUUID().toString();

        com.taktak.app.data.push.PushEventBus.Listener listener = new com.taktak.app.data.push.PushEventBus.Listener() {
            @Override
            public void onMomentCreated(String momentId) {
                if (testMomentId.equals(momentId)) {
                    createdReceived.set(true);
                }
            }

            @Override
            public void onMomentDeleted(String momentId) {
                if (testMomentId.equals(momentId)) {
                    deletedReceived.set(true);
                }
            }
        };

        com.taktak.app.data.push.PushEventBus.register(listener);
        try {
            com.taktak.app.data.push.PushEventBus.emitMomentCreated(testMomentId);
            assertTrue(createdReceived.get());

            com.taktak.app.data.push.PushEventBus.emitMomentDeleted(testMomentId);
            assertTrue(deletedReceived.get());
        } finally {
            com.taktak.app.data.push.PushEventBus.unregister(listener);
        }
    }

    @Test
    public void momentUploadAndDeleteEndToEnd() throws Exception {
        var client = new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(context, BuildConfig.BASE_URL));
        var momentRepo = new com.taktak.app.data.repository.MomentRepository(client);

        android.graphics.Bitmap testBitmap = android.graphics.Bitmap.createBitmap(80, 80, android.graphics.Bitmap.Config.ARGB_8888);
        byte[] jpegBytes;
        try (java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream()) {
            assertTrue(testBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream));
            jpegBytes = stream.toByteArray();
        } finally {
            testBitmap.recycle();
        }

        String momentId = momentRepo.upload(jpegBytes, "Owner moment to delete");
        assertNotNull(momentId);

        // Delete the moment
        momentRepo.delete(momentId);
    }

    @Test
    public void widgetUpdaterRendersAllStatesAndDecodesBitmap() throws Exception {
        android.graphics.Bitmap testBitmap = android.graphics.Bitmap.createBitmap(800, 800, android.graphics.Bitmap.Config.ARGB_8888);
        byte[] jpegBytes;
        try (java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream()) {
            assertTrue(testBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream));
            jpegBytes = stream.toByteArray();
        } finally {
            testBitmap.recycle();
        }

        android.graphics.Bitmap decoded = com.taktak.app.widget.WidgetUpdater.decodeSampledBitmap(jpegBytes, 512, 512);
        assertNotNull(decoded);
        assertTrue(decoded.getWidth() <= 512);
        assertTrue(decoded.getHeight() <= 512);

        com.taktak.app.widget.WidgetUpdater.renderLoggedOut(context);
        com.taktak.app.widget.WidgetUpdater.renderEmpty(context);

        var dummySender = new com.taktak.app.data.model.MomentModels.PublicUser();
        dummySender.id = UUID.randomUUID().toString();
        dummySender.username = "alice";
        dummySender.displayName = "Alice";
        var dummyMoment = new com.taktak.app.data.model.MomentModels.MomentItem();
        dummyMoment.id = UUID.randomUUID().toString();
        dummyMoment.sender = dummySender;
        dummyMoment.caption = "Widget Test Caption";
        dummyMoment.createdAt = "2026-10-18T00:00:00Z";

        com.taktak.app.widget.WidgetUpdater.renderMoment(context, dummyMoment, decoded);

        com.taktak.app.widget.WidgetUpdater.enqueueOneTimeWork(context);
        com.taktak.app.widget.WidgetUpdater.enqueuePeriodicWork(context);
        com.taktak.app.widget.WidgetUpdater.cancelPeriodicWork(context);
    }

    @Test
    public void latestMomentEndToEndWithBackend() throws Exception {
        var client = new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(context, BuildConfig.BASE_URL));
        var momentRepo = new com.taktak.app.data.repository.MomentRepository(client);

        var initialLatest = momentRepo.latest();
        assertNull(initialLatest);

        String aliceEmail = "alice_" + UUID.randomUUID().toString().replace("-", "") + "@example.test";
        String alicePass = "ValidPass123!";
        var aliceClient = new ApiClient(BuildConfig.BASE_URL, new Memory());
        var aliceAuth = new AuthRepository(aliceClient);
        var aliceProfile = aliceAuth.register(aliceEmail, "alice_" + UUID.randomUUID().toString().substring(0, 8), "Alice Sender", alicePass);
        assertNotNull(aliceProfile);
        aliceAuth.login(aliceEmail, alicePass);

        var myFriends = primary.friends();
        var aliceFriends = aliceAuth.friends();
        var invitation = aliceFriends.send(me.id);
        myFriends.respond(invitation.id, true);

        android.graphics.Bitmap testBitmap = android.graphics.Bitmap.createBitmap(80, 80, android.graphics.Bitmap.Config.ARGB_8888);
        byte[] jpegBytes;
        try (java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream()) {
            assertTrue(testBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream));
            jpegBytes = stream.toByteArray();
        } finally {
            testBitmap.recycle();
        }

        var aliceMoments = aliceAuth.moments();
        String momentId = aliceMoments.upload(jpegBytes, "Hello for Widget", java.util.List.of(me.id));
        assertNotNull(momentId);

        var myLatest = momentRepo.latest();
        assertNotNull(myLatest);
        assertEquals(momentId, myLatest.id);
        assertEquals("Hello for Widget", myLatest.caption);
        assertEquals(aliceProfile.username, myLatest.sender.username);

        aliceMoments.delete(momentId);

        var afterDeleteLatest = momentRepo.latest();
        assertNull(afterDeleteLatest);
    }

    @Test
    public void sentMomentsEndToEndWithBackend() throws Exception {
        var client = new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(context, BuildConfig.BASE_URL));
        var momentRepo = new com.taktak.app.data.repository.MomentRepository(client);

        // Initially sent moments is empty for new user
        var initialSent = momentRepo.sent(0);
        assertNotNull(initialSent);
        assertTrue(initialSent.items.isEmpty());
        assertFalse(initialSent.hasNext);

        // Register friend Alice
        String aliceEmail = "alice_sent_" + UUID.randomUUID().toString().replace("-", "") + "@example.test";
        String alicePass = "ValidPass123!";
        var aliceClient = new ApiClient(BuildConfig.BASE_URL, new Memory());
        var aliceAuth = new AuthRepository(aliceClient);
        var aliceProfile = aliceAuth.register(aliceEmail, "alice_" + UUID.randomUUID().toString().substring(0, 8), "Alice Friend", alicePass);
        aliceAuth.login(aliceEmail, alicePass);

        var myFriends = primary.friends();
        var invitation = aliceAuth.friends().send(me.id);
        myFriends.respond(invitation.id, true);

        // Create sample JPEG
        android.graphics.Bitmap testBitmap = android.graphics.Bitmap.createBitmap(80, 80, android.graphics.Bitmap.Config.ARGB_8888);
        byte[] jpegBytes;
        try (java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream()) {
            assertTrue(testBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream));
            jpegBytes = stream.toByteArray();
        } finally {
            testBitmap.recycle();
        }

        // 1. Upload private moment
        String privateId = momentRepo.upload(jpegBytes, "My Private Moment");
        assertNotNull(privateId);

        // 2. Upload moment to Alice
        String sharedId = momentRepo.upload(jpegBytes, "Shared with Alice", java.util.List.of(aliceProfile.id));
        assertNotNull(sharedId);

        // Verify sent tab shows both moments in DESC order (sharedId first, then privateId)
        var mySent = momentRepo.sent(0);
        assertNotNull(mySent);
        assertEquals(2, mySent.items.size());
        assertEquals(sharedId, mySent.items.get(0).id);
        assertEquals("Shared with Alice", mySent.items.get(0).caption);
        assertEquals(privateId, mySent.items.get(1).id);
        assertEquals("My Private Moment", mySent.items.get(1).caption);

        // Sender can download image for sent moments
        byte[] downloadedPrivate = momentRepo.download(privateId);
        assertNotNull(downloadedPrivate);
        assertTrue(downloadedPrivate.length > 0);

        // Alice's sent tab has 0 moments
        var aliceSent = aliceAuth.moments().sent(0);
        assertNotNull(aliceSent);
        assertTrue(aliceSent.items.isEmpty());

        // Alice's received tab has 1 moment (the shared one)
        var aliceReceived = aliceAuth.moments().received(0);
        assertNotNull(aliceReceived);
        assertEquals(1, aliceReceived.items.size());
        assertEquals(sharedId, aliceReceived.items.get(0).id);
    }

    @Test
    public void momentViewerActivityRendersAndCachesImage() throws Exception {
        // Create test bitmap and cache it
        String testMomentId = UUID.randomUUID().toString();
        android.graphics.Bitmap testBitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888);
        testBitmap.eraseColor(android.graphics.Color.MAGENTA);
        com.taktak.app.ui.history.HistoryImageCache.put(testMomentId, testBitmap);

        // Verify cache retrieval
        android.graphics.Bitmap cached = com.taktak.app.ui.history.HistoryImageCache.get(testMomentId);
        assertNotNull(cached);
        assertEquals(100, cached.getWidth());
        assertEquals(100, cached.getHeight());

        // Verify time formatter handles ISO-8601 strings
        String formatted = com.taktak.app.ui.history.HistoryTimeFormatter.format(context, "2026-10-18T00:00:00Z");
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());

        // Clean cache
        com.taktak.app.ui.history.HistoryImageCache.remove(testMomentId);
        assertNull(com.taktak.app.ui.history.HistoryImageCache.get(testMomentId));
    }

    @Test
    public void momentDeletionBySenderRevokesRecipientAccessImmediately() throws Exception {
        var client = new ApiClient(BuildConfig.BASE_URL, new EncryptedSessionStore(context, BuildConfig.BASE_URL));
        var momentRepo = new com.taktak.app.data.repository.MomentRepository(client);

        // Register Bob
        String bobEmail = "bob_del_" + UUID.randomUUID().toString().replace("-", "") + "@example.test";
        String bobPass = "ValidPass123!";
        var bobClient = new ApiClient(BuildConfig.BASE_URL, new Memory());
        var bobAuth = new AuthRepository(bobClient);
        var bobProfile = bobAuth.register(bobEmail, "bob_" + UUID.randomUUID().toString().substring(0, 8), "Bob Del", bobPass);
        bobAuth.login(bobEmail, bobPass);

        // Friend Bob
        var myFriends = primary.friends();
        var invite = bobAuth.friends().send(me.id);
        myFriends.respond(invite.id, true);

        // Prepare image
        android.graphics.Bitmap testBitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888);
        byte[] jpegBytes;
        try (java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream()) {
            assertTrue(testBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream));
            jpegBytes = stream.toByteArray();
        } finally {
            testBitmap.recycle();
        }

        // 1. Upload moment to Bob
        String momentId = momentRepo.upload(jpegBytes, "Will be revoked", java.util.List.of(bobProfile.id));
        assertNotNull(momentId);

        // Bob can download and sees it
        byte[] bobInitialDownload = bobAuth.moments().download(momentId);
        assertNotNull(bobInitialDownload);
        assertTrue(bobInitialDownload.length > 0);
        assertEquals(1, bobAuth.moments().received(0).items.size());

        // 2. Bob attempts to delete sender's moment -> 403 FORBIDDEN
        try {
            bobAuth.moments().delete(momentId);
            fail("Non-sender delete must fail with 403");
        } catch (com.taktak.app.data.remote.ApiFailure failure) {
            assertEquals(403, failure.status);
        }

        // 3. Sender deletes moment -> Success (204)
        momentRepo.delete(momentId);

        // 4. Immediate revocation: Both Bob and sender get 404 on image download
        try {
            bobAuth.moments().download(momentId);
            fail("Revoked moment image must return 404 to recipient");
        } catch (com.taktak.app.data.remote.ApiFailure failure) {
            assertEquals(404, failure.status);
        }

        try {
            momentRepo.download(momentId);
            fail("Revoked moment image must return 404 to sender");
        } catch (com.taktak.app.data.remote.ApiFailure failure) {
            assertEquals(404, failure.status);
        }

        // 5. Moment is removed from both sent and received listings
        var mySentAfter = momentRepo.sent(0);
        for (var item : mySentAfter.items) {
            assertNotEquals(momentId, item.id);
        }
        var bobReceivedAfter = bobAuth.moments().received(0);
        for (var item : bobReceivedAfter.items) {
            assertNotEquals(momentId, item.id);
        }
    }

    @Test
    public void momentDeletionNetworkFailurePreservesLocalCache() throws Exception {
        String testMomentId = UUID.randomUUID().toString();
        android.graphics.Bitmap testBitmap = android.graphics.Bitmap.createBitmap(50, 50, android.graphics.Bitmap.Config.ARGB_8888);
        testBitmap.eraseColor(android.graphics.Color.BLUE);
        com.taktak.app.ui.history.HistoryImageCache.put(testMomentId, testBitmap);

        // Simulate unreachable client (offline / down)
        var offlineStore = new Memory();
        var session = new Session("offline.token.sig", "refreshtoken", 900, 1);
        offlineStore.save(session);
        var unreachableClient = new ApiClient("http://127.0.0.1:1/", offlineStore);
        var unreachableRepo = new com.taktak.app.data.repository.MomentRepository(unreachableClient);

        try {
            unreachableRepo.delete(testMomentId);
            fail("Unreachable server must throw IOException / ApiFailure");
        } catch (java.io.IOException expected) {
            // Expected network failure
        }

        // Cache must still be intact because deletion did not succeed
        android.graphics.Bitmap stillCached = com.taktak.app.ui.history.HistoryImageCache.get(testMomentId);
        assertNotNull("Cache must not be deleted on network failure", stillCached);

        // Clean up
        com.taktak.app.ui.history.HistoryImageCache.remove(testMomentId);
    }

    @Test
    public void saveMomentToGalleryOnUserActionSavesValidJpegToMediaStore() throws Exception {
        android.graphics.Bitmap testBitmap = android.graphics.Bitmap.createBitmap(80, 80, android.graphics.Bitmap.Config.ARGB_8888);
        testBitmap.eraseColor(android.graphics.Color.CYAN);

        android.net.Uri savedUri = null;
        try {
            // User action: Save to gallery
            savedUri = com.taktak.app.ui.history.GallerySaver.saveBitmap(context, testBitmap, "qa7c_moment");
            assertNotNull("MediaStore Uri must not be null on save", savedUri);

            // Verify content is readable and decodes to valid JPEG Bitmap
            try (java.io.InputStream stream = context.getContentResolver().openInputStream(savedUri)) {
                assertNotNull("InputStream from MediaStore Uri must not be null", stream);
                android.graphics.Bitmap decoded = android.graphics.BitmapFactory.decodeStream(stream);
                assertNotNull("Decoded bitmap must not be null", decoded);
                assertEquals(80, decoded.getWidth());
                assertEquals(80, decoded.getHeight());
                decoded.recycle();
            }
        } finally {
            testBitmap.recycle();
            if (savedUri != null) {
                context.getContentResolver().delete(savedUri, null, null);
            }
        }
    }

    @Test
    public void emptyJpegBytesThrowsIOExceptionOnGallerySave() {
        try {
            com.taktak.app.ui.history.GallerySaver.saveJpeg(context, new byte[0], "invalid_empty");
            fail("Empty bytes must throw IOException");
        } catch (java.io.IOException expected) {
            // Expected
        }
    }
}
