package com.taktak.server.auth;

public record TokenPair(String accessToken, String refreshToken, long expiresIn, String tokenType) {
    @Override
    public String toString() { return "TokenPair[redacted]"; }
}
