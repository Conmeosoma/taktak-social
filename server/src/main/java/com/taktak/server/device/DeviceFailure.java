package com.taktak.server.device;

import org.springframework.http.HttpStatus;

public final class DeviceFailure extends RuntimeException {
    public final HttpStatus status;
    public final String code;

    public DeviceFailure(HttpStatus status, String code) {
        super("Device operation failed.");
        this.status = status;
        this.code = code;
    }
}
