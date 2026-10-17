package com.taktak.server;

import com.taktak.server.friend.SearchRateLimiter;
import com.taktak.server.friend.SearchThrottled;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SearchRateLimiterTest {
    @Test void accountLimitIsSharedAcrossDifferentIps() {
        var limiter = new SearchRateLimiter(new MutableClock()); UUID actor = UUID.randomUUID();
        for (int i = 0; i < 30; i++) { limiter.check(actor, "192.0.2." + i); }
        assertThatThrownBy(() -> limiter.check(actor, "192.0.2.250")).isInstanceOf(SearchThrottled.class);
    }

    @Test void ipLimitIsSharedAcrossDifferentAccounts() {
        var limiter = new SearchRateLimiter(new MutableClock());
        for (int i = 0; i < 60; i++) { limiter.check(UUID.randomUUID(), "192.0.2.1"); }
        assertThatThrownBy(() -> limiter.check(UUID.randomUUID(), "192.0.2.1")).isInstanceOf(SearchThrottled.class);
    }

    @Test void expiredWindowsResetAtSixtySeconds() {
        var clock = new MutableClock(); var limiter = new SearchRateLimiter(clock); UUID actor = UUID.randomUUID();
        for (int i = 0; i < 30; i++) { limiter.check(actor, "192.0.2.1"); }
        clock.now = clock.now.plusSeconds(59);
        assertThatThrownBy(() -> limiter.check(actor, "192.0.2.1")).isInstanceOf(SearchThrottled.class);
        clock.now = clock.now.plusSeconds(1);
        assertThatCode(() -> limiter.check(actor, "192.0.2.1")).doesNotThrowAnyException();
    }

    @Test void concurrentCallsCannotExceedAccountBudget() throws Exception {
        var limiter = new SearchRateLimiter(new MutableClock()); UUID actor = UUID.randomUUID();
        var pool = Executors.newFixedThreadPool(16); var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<Boolean>>();
        try {
            for (int i = 0; i < 64; i++) {
                String ip = "192.0.2." + i;
                futures.add(pool.submit(() -> {
                    if (!start.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("QA timeout"); }
                    try { limiter.check(actor, ip); return true; }
                    catch (SearchThrottled expected) { return false; }
                }));
            }
            start.countDown(); int allowed = 0;
            for (var future : futures) { if (future.get(5, TimeUnit.SECONDS)) { allowed++; } }
            assertThat(allowed).isEqualTo(30);
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void capacityFailsClosedAndExpiredEntriesAreReclaimed() {
        var clock = new MutableClock(); var limiter = new SearchRateLimiter(clock);
        UUID known = UUID.randomUUID(); limiter.check(known, "known");
        for (int i = 0; i < 4999; i++) { limiter.check(UUID.randomUUID(), "unique-" + i); }
        assertThatThrownBy(() -> limiter.check(UUID.randomUUID(), "new")).isInstanceOf(SearchThrottled.class);
        assertThatCode(() -> limiter.check(known, "known")).doesNotThrowAnyException();
        clock.now = clock.now.plusSeconds(60);
        assertThatCode(() -> limiter.check(UUID.randomUUID(), "new")).doesNotThrowAnyException();
    }

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-17T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
