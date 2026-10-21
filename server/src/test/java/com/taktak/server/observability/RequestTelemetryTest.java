package com.taktak.server.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.ServletException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;

class RequestTelemetryTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OperationalSummary summary = new OperationalSummary(registry);
    private final RequestTelemetryFilter filter = new RequestTelemetryFilter(registry, summary);

    @AfterEach void close() { registry.close(); MDC.clear(); }

    @Test void ignoresEvenValidClientIdsAndGeneratesUniqueServerUuids() throws Exception {
        String valid = UUID.randomUUID().toString();
        var ids = new java.util.HashSet<String>();
        for (String supplied : new String[]{valid, "bad-id", "private@example.test\r\nforged", "x".repeat(8000)}) {
            var request = request("GET", "/api/users/me");
            request.addHeader("X-Request-ID", supplied);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {
                assertThat(MDC.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
                assertThat(MDC.get("requestId")).isNotEqualTo(supplied);
            });
            String id = response.getHeader("X-Request-ID");
            assertThat(UUID.fromString(id).toString()).isEqualTo(id);
            assertThat(ids.add(id)).isTrue();
            assertThat(MDC.get("requestId")).isNull();
        }
    }

    @Test void exceptionAndAuthenticationFailuresStillClearMdcAndRecord() throws Exception {
        MDC.put("requestId", "stale-worker-value");
        assertThatThrownBy(() -> filter.doFilter(request("POST", "/api/auth/login"),
                new MockHttpServletResponse(), (req, res) -> { throw new ServletException("private detail"); }))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get("requestId")).isNull();
        for (int status : new int[]{401, 403}) {
            var response = new MockHttpServletResponse();
            filter.doFilter(request("GET", "/api/users/me"), response, (req, res) -> response.setStatus(status));
            assertThat(response.getHeader("X-Request-ID")).isNotNull();
            assertThat(MDC.get("requestId")).isNull();
        }
        assertThat(registry.get("taktak.requests").tag("status_class", "5xx").counter().count()).isEqualTo(1);
        assertThat(registry.get("taktak.requests").tag("status_class", "4xx").counter().count()).isEqualTo(2);
    }

    @Test void uniquePathsHeadersMethodsAndBodiesCannotCreateDynamicTags() throws Exception {
        for (int i = 0; i < 200; i++) {
            var request = request("CUSTOM" + i, "/api/moments/" + UUID.randomUUID() + "/image");
            request.setQueryString("email=private" + i + "@example.test");
            request.addHeader("Authorization", "Bearer secret" + i);
            request.setContent(("private-caption-" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
        }
        assertThat(registry.getMeters()).hasSize(2);
        assertThat(registry.get("taktak.requests").counter().count()).isEqualTo(200);
        assertThat(registry.get("taktak.request.duration").timer().count()).isEqualTo(200);
        registry.getMeters().forEach(meter -> assertThat(meter.getId().getTags().toString())
                .contains("OTHER", "moments", "2xx").doesNotContain("secret", "private", "CUSTOM", "/image", "@"));
    }

    @Test void boundedSummaryAlertsResetCountersAndNeverLogPrivateRequestData() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(OperationalSummary.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            for (int i = 0; i < 20; i++) {
                var request = request("GET", "/api/moments/private-person/image");
                request.addHeader("X-Request-ID", "private@example.test");
                var response = new MockHttpServletResponse();
                filter.doFilter(request, response, (req, res) -> response.setStatus(503));
            }
            summary.report(); summary.report();
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage())
                    .isEqualTo("OPS_HTTP_ERROR_RATE requests=20 serverErrors=20");
            assertThat(appender.list.get(0).getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    @Test void poolAlertNeedsTwoSaturatedSamplesAndRecovers() {
        Logger logger = (Logger) LoggerFactory.getLogger(OperationalSummary.class);
        var appender = new ListAppender<ILoggingEvent>(); appender.start(); logger.addAppender(appender);
        AtomicInteger active = registry.gauge("hikaricp.connections.active", new AtomicInteger(10));
        registry.gauge("hikaricp.connections.max", new AtomicInteger(10));
        try {
            summary.report(); assertThat(appender.list).isEmpty();
            summary.report(); assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage()).startsWith("OPS_DB_POOL_SATURATED");
            active.set(2); summary.report(); assertThat(appender.list).hasSize(1);
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    private MockHttpServletRequest request(String method, String path) {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
