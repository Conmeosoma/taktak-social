package com.taktak.server.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Không đọc body, token hoặc correlation ID do client cung cấp. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestTelemetryFilter extends OncePerRequestFilter {
    public static final String MDC_KEY = "requestId";
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
    private final MeterRegistry registry;
    private final OperationalSummary summary;

    public RequestTelemetryFilter(MeterRegistry registry, OperationalSummary summary) {
        this.registry = registry;
        this.summary = summary;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, requestId);
        response.setHeader("X-Request-ID", requestId);
        long started = System.nanoTime();
        boolean failed = true;
        try {
            chain.doFilter(request, response);
            failed = false;
        } finally {
            try {
                int status = failed ? 500 : response.getStatus();
                String statusClass = status >= 100 && status < 600 ? status / 100 + "xx" : "other";
                String method = METHODS.contains(request.getMethod()) ? request.getMethod() : "OTHER";
                String category = category(request.getServletPath());
                var tags = io.micrometer.core.instrument.Tags.of("method", method, "category", category,
                        "status_class", statusClass);
                registry.counter("taktak.requests", tags).increment();
                Timer.builder("taktak.request.duration").tags(tags).register(registry)
                        .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
                summary.record(status >= 500);
            } finally {
                // Không để ID của request cũ sống qua thread-pool reuse, kể cả khi chain ném lỗi.
                MDC.remove(MDC_KEY);
            }
        }
    }

    private static String category(String path) {
        if (path == null) return "other";
        if (path.equals("/actuator/health")) return "health";
        if (path.startsWith("/api/auth/")) return "auth";
        if (path.equals("/api/moments") || path.startsWith("/api/moments/")) return "moments";
        if (path.equals("/api/conversations") || path.startsWith("/api/conversations/")) return "chat";
        if (path.startsWith("/api/")) return "api";
        return "other";
    }
}
