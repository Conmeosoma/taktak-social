package com.taktak.server.moment;

import org.springframework.http.HttpStatus;

public final class MomentFailure extends RuntimeException {
    public final HttpStatus status;
    public final String code;
    public MomentFailure(HttpStatus status, String code) {
        super("Moment operation failed.");
        this.status = status; this.code = code;
    }
}
