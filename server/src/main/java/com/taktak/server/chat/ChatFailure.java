package com.taktak.server.chat;

import org.springframework.http.HttpStatus;

public final class ChatFailure extends RuntimeException {
    public final HttpStatus status;
    public final String code;
    public ChatFailure(HttpStatus status, String code) { this.status=status; this.code=code; }
}
