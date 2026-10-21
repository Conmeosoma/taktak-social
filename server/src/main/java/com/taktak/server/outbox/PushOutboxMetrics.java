package com.taktak.server.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Nhãn chỉ là enum trạng thái hữu hạn; tuyệt đối không gắn user/resource/token. */
@Component
public class PushOutboxMetrics {
    private final JdbcTemplate jdbc;
    public PushOutboxMetrics(JdbcTemplate jdbc,MeterRegistry registry) {
        this.jdbc=jdbc;
        for(var status:java.util.List.of("PENDING","PROCESSING","ACCEPTED","CANCELLED","DEAD"))
            Gauge.builder("taktak.push.outbox.rows",this,value->value.count(status)).tag("state",status).register(registry);
        Gauge.builder("taktak.push.outbox.oldest.seconds",this,PushOutboxMetrics::oldest).register(registry);
    }
    private double count(String status) {
        return jdbc.queryForObject("SELECT count(*) FROM push_outbox WHERE status=?",Long.class,status);
    }
    private double oldest() {
        return jdbc.queryForObject("""
                SELECT coalesce(greatest(0,extract(epoch from now()-min(created_at))),0)
                FROM push_outbox WHERE status IN ('PENDING','PROCESSING')
                """,Double.class);
    }
    @Scheduled(fixedDelayString="${taktak.observability.summary-interval-ms:60000}")
    public void warnOnBacklog() {
        double age=oldest(),dead=count("DEAD");
        if(age>300 || dead>0) org.slf4j.LoggerFactory.getLogger(PushOutboxMetrics.class)
                .warn("OPS_PUSH_OUTBOX_BACKLOG oldestSeconds={} deadRows={}",age,dead);
    }
}
