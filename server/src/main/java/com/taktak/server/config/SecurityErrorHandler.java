package com.taktak.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper json;
    private final Clock clock;

    public SecurityErrorHandler(ObjectMapper json, Clock clock) { this.json = json; this.clock = clock; }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ignored)
            throws IOException {
        write(request, response, 401, "UNAUTHORIZED", "Thông tin xác thực không hợp lệ hoặc đã hết hạn.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ignored)
            throws IOException {
        write(request, response, 403, "FORBIDDEN", "Bạn không có quyền thực hiện yêu cầu này.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status, String code,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        json.writeValue(response.getOutputStream(), new ApiError(clock.instant(), status, code, message,
                request.getRequestURI()));
    }
}
