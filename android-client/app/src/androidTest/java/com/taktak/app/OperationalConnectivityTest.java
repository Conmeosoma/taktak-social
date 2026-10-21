package com.taktak.app;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** GET thực từ thiết bị; không đọc phiên, gửi JWT hoặc thay đổi dữ liệu server. */
@RunWith(AndroidJUnit4.class)
public class OperationalConnectivityTest {
    private OkHttpClient client;
    private final Set<String> requestIds = new HashSet<>();

    @Before public void prepare() {
        assertEquals("Explicit remote GET QA opt-in", "true",
                InstrumentationRegistry.getArguments().getString("taktak.allowOperationalQa"));
        assertTrue("Operational QA requires HTTPS", BuildConfig.BASE_URL.startsWith("https://"));
        client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS)
                .followRedirects(false).followSslRedirects(false).build();
    }

    @Test public void httpsHealthHasServerOwnedCorrelationId() throws Exception {
        try (Response response = get("actuator/health")) {
            assertEquals("Health HTTP status", 200, response.code());
            assertNotNull(response.body());
            assertEquals("UP", new JSONObject(response.body().string()).getString("status"));
        }
        try (Response response = get("actuator/health")) {
            assertEquals(200, response.code());
        }
        assertEquals("Different requests need different IDs", 2, requestIds.size());
    }

    @Test public void privateImageApiRejectsAnonymousDevice() throws Exception {
        try (Response response = get("api/moments/latest")) {
            assertEquals("No authenticated interceptor is used by this test", 401, response.code());
        }
    }

    @Test public void managementEndpointsAreNotPublic() throws Exception {
        for (String path : new String[]{"actuator/metrics", "actuator/env", "actuator/configprops"}) {
            try (Response response = get(path)) {
                assertTrue("Management endpoint must not be public",
                        response.code() == 401 || response.code() == 403 || response.code() == 404);
            }
        }
    }

    private Response get(String path) throws Exception {
        Response response = client.newCall(new Request.Builder().url(BuildConfig.BASE_URL + path)
                .header("X-Request-ID", "qa-untrusted-client-id").get().build()).execute();
        try {
            String id = response.header("X-Request-ID");
            assertNotNull("Server request ID", id);
            assertEquals("Canonical server UUID", UUID.fromString(id).toString(), id);
            assertTrue("Request ID must be fresh", requestIds.add(id));
            return response;
        } catch (Throwable failure) {
            response.close();
            throw failure;
        }
    }
}
