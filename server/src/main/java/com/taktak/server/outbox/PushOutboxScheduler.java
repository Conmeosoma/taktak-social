package com.taktak.server.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods=false)
@EnableScheduling
@ConditionalOnProperty(name="taktak.outbox.enabled",havingValue="true",matchIfMissing=true)
public class PushOutboxScheduler {
    private final PushOutboxDispatcher dispatcher;
    public PushOutboxScheduler(PushOutboxDispatcher dispatcher) { this.dispatcher=dispatcher; }
    @Scheduled(fixedDelayString="${taktak.outbox.poll-ms:5000}",initialDelayString="${taktak.outbox.initial-delay-ms:10000}")
    public void poll() { dispatcher.dispatchBatch(); }
}
