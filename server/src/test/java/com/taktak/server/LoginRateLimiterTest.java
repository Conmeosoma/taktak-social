package com.taktak.server;

import com.taktak.server.auth.LoginRateLimiter;
import com.taktak.server.auth.LoginThrottled;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {
    @Test
    void concurrentAttemptsAcrossIpsCannotBypassAccountBudget() throws Exception {
        var limiter = new LoginRateLimiter(Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneOffset.UTC));
        var executor = Executors.newFixedThreadPool(20);
        var ready = new CountDownLatch(20);
        var start = new CountDownLatch(1);
        var calls = new ArrayList<Future<Boolean>>();
        try {
            for (int index = 0; index < 20; index++) {
                String ip = "192.0.2." + (index + 1);
                calls.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Race start timed out."); }
                    try { limiter.check(ip, "test@example.test"); return true; }
                    catch (LoginThrottled expected) { return false; }
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int allowed = 0;
            for (var call : calls) { if (call.get(5, TimeUnit.SECONDS)) { allowed++; } }
            assertThat(allowed).isEqualTo(5);
        } finally { start.countDown(); executor.shutdownNow(); }
    }
}
