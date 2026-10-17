package com.taktak.server.friend;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Hạn mức single-JVM bounded; không dùng query hoặc X-Forwarded-For làm khóa. */
@Component
public class SearchRateLimiter {
    private static final int MAX_KEYS = 10_000;
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;
    public SearchRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized void check(UUID actor, String ip) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(now));
        String accountKey = "user:" + actor;
        String ipKey = "ip:" + com.taktak.server.auth.TokenService.hash(ip);
        int additions = (windows.containsKey(accountKey) ? 0 : 1) + (windows.containsKey(ipKey) ? 0 : 1);
        if (windows.size() + additions > MAX_KEYS) { throw new SearchThrottled(); }
        Window account = windows.computeIfAbsent(accountKey, ignored -> new Window(now.plusSeconds(60)));
        Window address = windows.computeIfAbsent(ipKey, ignored -> new Window(now.plusSeconds(60)));
        if (account.attempts >= 30 || address.attempts >= 60) { throw new SearchThrottled(); }
        account.attempts++; address.attempts++;
    }

    private static final class Window {
        private int attempts;
        private final Instant expiresAt;
        private Window(Instant expiresAt) { this.expiresAt = expiresAt; }
    }
}
