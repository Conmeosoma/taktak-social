package com.taktak.server.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Bộ đếm bounded trong một JVM; không tin X-Forwarded-For từ client. */
@Component
public class LoginRateLimiter {
    private static final int MAX_KEYS = 10_000;
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;

    public LoginRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized void check(String ip, String email) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(now));
        String ipKey = "ip:" + TokenService.hash(ip);
        String accountKey = "account:" + TokenService.hash(email);
        if (windows.size() + (windows.containsKey(ipKey) ? 0 : 1)
                + (windows.containsKey(accountKey) ? 0 : 1) > MAX_KEYS) { throw new LoginThrottled(); }
        Window ipWindow = windows.computeIfAbsent(ipKey, key -> new Window(now.plusSeconds(60)));
        Window accountWindow = windows.computeIfAbsent(accountKey, key -> new Window(now.plusSeconds(60)));
        if (ipWindow.attempts >= 20 || accountWindow.attempts >= 5) { throw new LoginThrottled(); }
        ipWindow.attempts++;
        accountWindow.attempts++;
    }

    private static final class Window {
        private int attempts;
        private final Instant expiresAt;
        private Window(Instant expiresAt) { this.expiresAt = expiresAt; }
    }
}
