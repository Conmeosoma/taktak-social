package com.taktak.server.observability;

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tổng hợp hữu hạn, không giữ dữ liệu từng request hoặc định danh người dùng. */
@Component
public class OperationalSummary {
    private static final Logger log = LoggerFactory.getLogger(OperationalSummary.class);
    private final AtomicReference<Window> window = new AtomicReference<>(new Window(0, 0));
    private final io.micrometer.core.instrument.MeterRegistry registry;
    private int saturatedWindows;

    public OperationalSummary(io.micrometer.core.instrument.MeterRegistry registry) {
        this.registry = registry;
    }

    void record(boolean serverFailure) {
        window.updateAndGet(previous -> new Window(previous.requests + 1,
                previous.serverErrors + (serverFailure ? 1 : 0)));
    }

    @Scheduled(fixedDelayString = "${taktak.observability.summary-interval-ms:60000}")
    public void report() {
        double maximum = registry.find("hikaricp.connections.max").gauges().stream()
                .mapToDouble(io.micrometer.core.instrument.Gauge::value).sum();
        double active = registry.find("hikaricp.connections.active").gauges().stream()
                .mapToDouble(io.micrometer.core.instrument.Gauge::value).sum();
        saturatedWindows = maximum > 0 && active >= maximum ? Math.min(2, saturatedWindows + 1) : 0;
        if (saturatedWindows >= 2) {
            log.warn("OPS_DB_POOL_SATURATED active={} maximum={}", active, maximum);
        }
        Window snapshot = window.getAndSet(new Window(0, 0));
        if (snapshot.requests == 0) return;
        if (snapshot.requests >= 20 && snapshot.serverErrors / (double) snapshot.requests >= 0.05) {
            log.warn("OPS_HTTP_ERROR_RATE requests={} serverErrors={}", snapshot.requests, snapshot.serverErrors);
        } else {
            log.info("OPS_HTTP_SUMMARY requests={} serverErrors={}", snapshot.requests, snapshot.serverErrors);
        }
    }

    private record Window(long requests, long serverErrors) { }
}
