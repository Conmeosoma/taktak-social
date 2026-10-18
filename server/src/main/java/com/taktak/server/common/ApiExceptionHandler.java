package com.taktak.server.common;

import com.taktak.server.auth.AuthFailure;
import com.taktak.server.auth.LoginThrottled;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(com.taktak.server.friend.FriendFailure.class)
    ResponseEntity<ApiError> friendFailure(com.taktak.server.friend.FriendFailure failure, HttpServletRequest request) {
        return error(failure.status, failure.code, "Không thể xử lý yêu cầu kết bạn hoặc dữ liệu tìm kiếm.", request);
    }

    @ExceptionHandler(com.taktak.server.friend.SearchThrottled.class)
    ResponseEntity<ApiError> searchThrottled(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                .header("Retry-After", "60").body(new ApiError(clock.instant(), 429, "SEARCH_RATE_LIMITED",
                        "Quá nhiều yêu cầu tìm kiếm. Vui lòng thử lại sau.", request.getRequestURI()));
    }

    @ExceptionHandler(com.taktak.server.moment.MomentFailure.class)
    ResponseEntity<ApiError> momentFailure(com.taktak.server.moment.MomentFailure failure, HttpServletRequest request) {
        return error(failure.status, failure.code, "Không thể xử lý ảnh hoặc bạn không có quyền truy cập.", request);
    }

    @ExceptionHandler(com.taktak.server.device.DeviceFailure.class)
    ResponseEntity<ApiError> deviceFailure(com.taktak.server.device.DeviceFailure failure, HttpServletRequest request) {
        return error(failure.status, failure.code, "Không thể xử lý yêu cầu thiết bị.", request);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge(HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE", "Ảnh vượt quá dung lượng cho phép.", request);
    }

    @ExceptionHandler({org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.multipart.MultipartException.class})
    ResponseEntity<ApiError> invalidMultipart(Exception ignored, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu yêu cầu không hợp lệ.", request);
    }

    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiError> notFound(Exception ignored, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy tài nguyên.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException exception,
                                             HttpServletRequest request) {
        var supported = exception.getSupportedHttpMethods();
        HttpMethod[] methods = supported == null ? new HttpMethod[0] : supported.toArray(HttpMethod[]::new);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).contentType(MediaType.APPLICATION_JSON).allow(methods)
                .body(new ApiError(clock.instant(), 405, "METHOD_NOT_ALLOWED",
                        "Phương thức không được hỗ trợ.", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpectedFailure(Exception ignored, HttpServletRequest request) {
        // Không log exception/message vì có thể chứa body hoặc credential từ dependency.
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error("Request processing failed.");
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Không thể xử lý yêu cầu.", request);
    }

    @ExceptionHandler(JwtEncodingException.class)
    ResponseEntity<ApiError> tokenFailure(JwtEncodingException ignored, HttpServletRequest request) {
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error("JWT encoding failed.");
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Không thể xử lý yêu cầu.", request);
    }

    @ExceptionHandler(AuthFailure.class)
    ResponseEntity<ApiError> invalidCredentials(AuthFailure ignored, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Thông tin xác thực không hợp lệ hoặc đã hết hạn.", request);
    }

    @ExceptionHandler(LoginThrottled.class)
    ResponseEntity<ApiError> throttled(LoginThrottled ignored, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON).header("Retry-After", "60")
                .body(new ApiError(clock.instant(), 429, "LOGIN_RATE_LIMITED",
                        "Quá nhiều yêu cầu đăng nhập. Vui lòng thử lại sau.", request.getRequestURI()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalidRequest(Exception ignored, HttpServletRequest request) {
        // Không phản chiếu rejected value, JSON hoặc password vào response/log.
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu yêu cầu không hợp lệ.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupportedMedia(HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Yêu cầu JSON.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ApiError> unacceptableResponse(HttpServletRequest request) {
        return error(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", "Response chỉ hỗ trợ JSON.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrityViolation(DataIntegrityViolationException exception,
                                                HttpServletRequest request) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                String msg = sql.getMessage();
                if (msg != null && msg.contains("uq_moments_sender_idempotency")) {
                    return error(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Yêu cầu upload đang được xử lý hoặc xung đột khóa idempotency.", request);
                }
                return error(HttpStatus.CONFLICT, "ACCOUNT_CONFLICT", "Email hoặc username đã được sử dụng.", request);
            }
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Không thể xử lý yêu cầu.", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                            HttpServletRequest request) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(new ApiError(clock.instant(), status.value(), code,
                message, request.getRequestURI()));
    }
}
