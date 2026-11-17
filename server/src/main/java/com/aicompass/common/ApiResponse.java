package com.aicompass.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * api-spec.md 공통 응답 형식.
 * <pre>
 * 성공: { "success": true,  "data": { ... } }
 * 실패: { "success": false, "error": { "code": "MODEL_NOT_FOUND", "message": "..." } }
 * </pre>
 * 사용: {@code return ApiResponse.ok(data);} / {@code ResponseEntity.status(201).body(ApiResponse.ok(data))}
 * 실패는 직접 만들지 말고 {@link ApiException} 을 던진다 ({@link GlobalExceptionHandler} 가 이 형식으로 바꾼다).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, Error error) {

    public record Error(String code, String message) {}

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new Error(code, message));
    }
}
