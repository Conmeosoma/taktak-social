package com.taktak.app.data.session;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.BuildConfig;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.data.repository.AuthRepository;
import com.taktak.app.data.model.AuthModels.RefreshRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AuthBackendTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    @Before public void localPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
        }
    }

    private String suffix() { return java.util.UUID.randomUUID().toString().replace("-", ""); }
    private EncryptedSessionStore store() throws Exception {
        return new EncryptedSessionStore(context, BuildConfig.BASE_URL, "auth_test_" + suffix());
    }

    @Test public void realRegisterLoginMeEncryptedPersistenceAndRefreshRotation() throws Exception {
        String fileName = "auth_test_" + suffix();
        EncryptedSessionStore store = new EncryptedSessionStore(context, BuildConfig.BASE_URL, fileName);
        ApiClient client = new ApiClient(BuildConfig.BASE_URL, store);
        AuthRepository repository = new AuthRepository(client);
        String username = "android_" + suffix(); String password = java.util.UUID.randomUUID().toString();
        try {
            var registered = repository.register(username + "@example.test", username, "Tiến 📷", password);
            var signedIn = repository.login(username + "@example.test", password);
            assertEquals(registered.id, signedIn.id); assertEquals("Tiến 📷", signedIn.displayName);
            Session session = store.load(); assertNotNull(session);
            String raw = new String(Files.readAllBytes(new File(context.getApplicationInfo().dataDir,
                    "shared_prefs/" + fileName + ".xml").toPath()), StandardCharsets.UTF_8);
            assertFalse("Access must not be plaintext", raw.contains(session.accessToken));
            assertFalse("Refresh must not be plaintext", raw.contains(session.refreshToken));
            EncryptedSessionStore reopened = new EncryptedSessionStore(context, BuildConfig.BASE_URL, fileName);
            assertEquals(registered.id, new AuthRepository(new ApiClient(BuildConfig.BASE_URL, reopened)).profile().id);
            // Giữ refresh thật, access sai buộc server trả 401; không gọi đây là test chờ JWT hết hạn.
            reopened.save(new Session("invalid.jwt.token", session.refreshToken, 0, 0));
            ApiClient restored = new ApiClient(BuildConfig.BASE_URL, reopened);
            assertEquals(registered.id, new AuthRepository(restored).profile().id);
            Session rotated = reopened.load(); assertNotNull(rotated);
            assertFalse(session.refreshToken.equals(rotated.refreshToken));
            var replay = client.auth.refresh(new RefreshRequest(session.refreshToken)).execute();
            assertEquals(401, replay.code()); if (replay.errorBody() != null) { replay.errorBody().close(); }
            restored.sessions.logout(); assertNull(reopened.load());
        } finally { store.clear(); }
    }

    @Test public void realWrongPasswordDuplicateAndRevokedRefreshAreSafe() throws Exception {
        EncryptedSessionStore store = store(); ApiClient client = new ApiClient(BuildConfig.BASE_URL, store);
        AuthRepository repository = new AuthRepository(client);
        String username = "negative_" + suffix(); String email = username + "@example.test";
        String password = java.util.UUID.randomUUID().toString();
        try {
            repository.register(email, username, "Member", password);
            try { repository.register("other_" + email, username, "Other", password); fail(); }
            catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.CONFLICT, failure.kind); }
            try { repository.login(email, java.util.UUID.randomUUID().toString()); fail(); }
            catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.UNAUTHORIZED, failure.kind); }
            assertNull(store.load());
            repository.login(email, password); Session old = store.load();
            assertTrue(client.auth.logout(new RefreshRequest(old.refreshToken)).execute().isSuccessful());
            store.save(new Session("invalid.jwt.token", old.refreshToken, 0, 0));
            ApiClient restored = new ApiClient(BuildConfig.BASE_URL, store);
            try { new AuthRepository(restored).profile(); fail(); }
            catch (ApiFailure failure) { assertEquals(ApiFailure.Kind.UNAUTHORIZED, failure.kind); }
            assertNull(restored.sessions.snapshot()); assertNull(store.load());
        } finally { store.clear(); }
    }

    @Test public void changingBackendOriginCannotReuseStoredTokens() throws Exception {
        String fileName = "auth_test_" + suffix();
        EncryptedSessionStore store = new EncryptedSessionStore(context, BuildConfig.BASE_URL, fileName);
        try {
            store.save(new Session(java.util.UUID.randomUUID().toString(), java.util.UUID.randomUUID().toString(), 1234, 0));
            EncryptedSessionStore other = new EncryptedSessionStore(context, "https://another.example.invalid/", fileName);
            assertNull(other.load()); assertNull(store.load());
        } finally { store.clear(); }
    }

    @Test public void corruptedCiphertextFailsClosedWithoutPlaintextFallback() throws Exception {
        String fileName = "auth_test_" + suffix();
        EncryptedSessionStore store = new EncryptedSessionStore(context, BuildConfig.BASE_URL, fileName);
        android.content.SharedPreferences raw = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        try {
            store.save(new Session(java.util.UUID.randomUUID().toString(), java.util.UUID.randomUUID().toString(), 1234, 0));
            String encryptedKey = null;
            for (String key : raw.getAll().keySet()) { if (!key.startsWith("__androidx")) { encryptedKey = key; break; } }
            assertNotNull(encryptedKey);
            assertTrue(raw.edit().putString(encryptedKey, "invalid-ciphertext").commit());
            try { store.load(); fail("Corrupt secure storage must fail closed."); } catch (java.io.IOException expected) { }
        } finally {
            // Chỉ file fixture UUID của phép thử này; không xóa app data/phiên production.
            assertTrue(raw.edit().clear().commit());
        }
    }
}
