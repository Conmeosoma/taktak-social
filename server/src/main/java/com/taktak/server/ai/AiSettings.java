package com.taktak.server.ai;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public final class AiSettings {
    @Value("${GEMINI_TEMPERATURE:}") String temperature="";
    public final boolean enabled;
    public final String model, credentials;
    public final int outputTokens, timeout, retries, concurrency, perUser, perMinute, perDay, contextChars;
    public AiSettings(@Value("${GEMINI_ENABLED:false}") boolean enabled,
            @Value("${GEMINI_MODEL:}") String model,@Value("${GEMINI_CREDENTIALS_FILE:}") String credentials,
            @Value("${GEMINI_MAX_OUTPUT_TOKENS:1024}") int outputTokens,
            @Value("${GEMINI_REQUEST_TIMEOUT_SECONDS:30}") int timeout,
            @Value("${GEMINI_MAX_RETRIES:2}") int retries,
            @Value("${GEMINI_MAX_CONCURRENT:2}") int concurrency,
            @Value("${GEMINI_USER_RPM:5}") int perUser,@Value("${GEMINI_APP_RPM:20}") int perMinute,
            @Value("${GEMINI_APP_DAILY_REQUESTS:100}") int perDay,@Value("${GEMINI_CONTEXT_CHARS:16000}") int contextChars) {
        this.enabled=enabled;this.model=model;this.credentials=credentials;
        this.outputTokens=Math.max(32,Math.min(4096,outputTokens));this.timeout=Math.max(1,Math.min(60,timeout));
        this.retries=Math.max(0,Math.min(2,retries));this.concurrency=Math.max(1,Math.min(4,concurrency));
        this.perUser=Math.max(1,perUser);this.perMinute=Math.max(1,perMinute);this.perDay=Math.max(1,perDay);
        this.contextChars=Math.max(4000,Math.min(32000,contextChars));
    }
}
