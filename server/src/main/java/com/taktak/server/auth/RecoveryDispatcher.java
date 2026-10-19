package com.taktak.server.auth;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;

/** Mọi email recovery đi qua cùng hàng đợi bounded, không lộ account qua thời gian SMTP. */
@Component
public class RecoveryDispatcher {
    private final RecoveryService recovery;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100), runnable -> {
                Thread thread = new Thread(runnable, "account-recovery-mail"); thread.setDaemon(true); return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    public RecoveryDispatcher(RecoveryService recovery) { this.recovery = recovery; }
    public void request(String email) {
        try {
            executor.execute(() -> {
                try { recovery.issueTemporary(email); }
                catch (RuntimeException ignored) {
                    org.slf4j.LoggerFactory.getLogger(RecoveryDispatcher.class).error("Recovery mail processing failed; no delivery confirmed.");
                }
            });
        } catch (RejectedExecutionException ignored) {
            org.slf4j.LoggerFactory.getLogger(RecoveryDispatcher.class).warn("Recovery mail queue full; request not processed.");
        }
    }
    @PreDestroy void close() { executor.shutdownNow(); }
}
