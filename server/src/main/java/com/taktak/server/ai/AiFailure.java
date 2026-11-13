package com.taktak.server.ai;
import org.springframework.http.HttpStatus;
public final class AiFailure extends RuntimeException {
    public final String code;
    public final HttpStatus status;
    public final long retryAfter;
    public AiFailure(String code, HttpStatus status) { this(code,status,0); }
    public AiFailure(String code, HttpStatus status,long retryAfter) {
        super(code); this.code=code;this.status=status;this.retryAfter=retryAfter;
    }
}
