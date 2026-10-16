package com.taktak.app.data.session;

public final class Session {
    public final String accessToken;
    public final String refreshToken;
    public final long expiresAtMillis;
    public final long generation;

    public Session(String accessToken, String refreshToken, long expiresAtMillis, long generation) {
        this.accessToken = accessToken; this.refreshToken = refreshToken;
        this.expiresAtMillis = expiresAtMillis; this.generation = generation;
    }
    @Override public String toString() { return "Session[redacted]"; }
}
