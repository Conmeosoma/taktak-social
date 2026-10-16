package com.taktak.app.data.remote;

import com.google.gson.Gson;
import com.taktak.app.data.model.AuthModels.ApiError;
import java.io.IOException;
import java.io.Reader;
import retrofit2.Response;

public final class ApiFailure extends IOException {
    public enum Kind { NETWORK, UNAUTHORIZED, CONFLICT, VALIDATION, RATE_LIMIT, SERVER, STORAGE, CANCELLED }
    public final Kind kind;
    public final int status;
    public final String code;
    public final int retryAfterSeconds;

    public ApiFailure(Kind kind) { this(kind, 0, "", 0); }
    private ApiFailure(Kind kind, int status, String code, int retryAfterSeconds) {
        super("Authentication operation failed.");
        this.kind = kind; this.status = status; this.code = code; this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ApiFailure from(Response<?> response) {
        String code = "";
        if (response.errorBody() != null) {
            try (Reader reader = response.errorBody().charStream()) {
                char[] chars = new char[8192];
                int count = 0;
                int read;
                while (count < chars.length && (read = reader.read(chars, count, chars.length - count)) != -1) { count += read; }
                ApiError error = new Gson().fromJson(new String(chars, 0, count), ApiError.class);
                if (error != null && error.status == response.code() && error.code != null && error.code.matches("[A-Z_]{1,64}")) {
                    code = error.code;
                }
            } catch (IOException | RuntimeException ignored) { /* Không hiển thị raw body hoặc message từ proxy. */ }
        }
        Kind kind;
        switch (response.code()) {
            case 400: kind = Kind.VALIDATION; break;
            case 401: kind = Kind.UNAUTHORIZED; break;
            case 409: kind = Kind.CONFLICT; break;
            case 429: kind = Kind.RATE_LIMIT; break;
            default: kind = Kind.SERVER;
        }
        int retry = 60;
        try { retry = Math.max(1, Math.min(3600, Integer.parseInt(response.headers().get("Retry-After")))); }
        catch (RuntimeException ignored) { }
        return new ApiFailure(kind, response.code(), code, kind == Kind.RATE_LIMIT ? retry : 0);
    }
}
