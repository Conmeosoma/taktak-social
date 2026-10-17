package com.taktak.server.friend;

import org.springframework.http.HttpStatus;

public class FriendFailure extends RuntimeException {
    public final HttpStatus status;
    public final String code;
    public FriendFailure(HttpStatus status, String code) {
        super(code); this.status = status; this.code = code;
    }
}
