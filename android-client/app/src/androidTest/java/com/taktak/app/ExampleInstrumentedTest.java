package com.taktak.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.security.NetworkSecurityPolicy;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.taktak.app", appContext.getPackageName());
    }

    @Test
    public void mainActivityOpensWithBootstrapText() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.main));
                TextView message = activity.findViewById(R.id.bootstrapMessage);
                assertEquals("Taktak", message.getText().toString());
                assertFalse(activity.isFinishing());
            });
        }
    }

    @Test
    public void debugNetworkPolicyOnlyAllowsExplicitLocalHosts() {
        NetworkSecurityPolicy policy = NetworkSecurityPolicy.getInstance();
        assertTrue(policy.isCleartextTrafficPermitted("10.0.2.2"));
        assertTrue(policy.isCleartextTrafficPermitted("127.0.0.1"));
        assertFalse(policy.isCleartextTrafficPermitted("example.com"));
    }

    @Test
    public void androidReachesRealBackendHealthAndProtectedEndpoint() throws Exception {
        // Chỉ test harness cấp quyền; UI chưa thực hiện request mạng ở bootstrap.
        if (Build.VERSION.SDK_INT >= 37) {
            var instrumentation = InstrumentationRegistry.getInstrumentation();
            Context context = instrumentation.getTargetContext();
            instrumentation.getUiAutomation().grantRuntimePermission(
                    context.getPackageName(), "android.permission.ACCESS_LOCAL_NETWORK");
            assertEquals(PackageManager.PERMISSION_GRANTED,
                    context.checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK"));
        }
        String healthUrl = InstrumentationRegistry.getArguments().getString(
                "taktakHealthUrl", BuildConfig.BASE_URL + "actuator/health");
        HttpURLConnection health = connect(healthUrl);
        try {
            assertEquals(200, health.getResponseCode());
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    health.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }
            assertEquals("UP", new JSONObject(body.toString()).getString("status"));
        } finally {
            health.disconnect();
        }
        HttpURLConnection protectedEndpoint = connect(new URL(new URL(healthUrl), "/api/users/me").toString());
        try {
            assertEquals(401, protectedEndpoint.getResponseCode());
        } finally {
            protectedEndpoint.disconnect();
        }
    }

    private HttpURLConnection connect(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(false);
        return connection;
    }
}
