package com.aicompass.common;

import org.springframework.http.HttpStatus;

/**
 * 컨트롤러·서비스에서 던지면 {@link GlobalExceptionHandler} 가
 * {@code { success:false, error:{ code, message } }} 와 해당 상태 코드로 응답한다.
 * <pre>
 * throw ApiException.notFound("MODEL_NOT_FOUND", "AI 툴을 찾을 수 없습니다.");
 * throw new ApiException(HttpStatus.CONFLICT, "EMAIL_DUPLICATED", "이미 가입된 이메일입니다.");
 * </pre>
 * 기능별 예외가 필요하면 이 클래스를 상속하면 된다 (예: {@code class AuthException extends ApiException}).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    // ---- 자주 쓰는 상태 코드 (api-spec.md 공통 규칙) ----

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    /** 401. 로그인이 필요한 API 에서 세션이 없을 때 */
    public static ApiException loginRequired() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.LOGIN_REQUIRED, "로그인이 필요합니다.");
    }

    public static ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
