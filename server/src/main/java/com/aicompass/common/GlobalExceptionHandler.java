package com.aicompass.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 모든 @RestController 의 예외를 api-spec.md 실패 형식으로 바꾼다.
 * <ul>
 *   <li>컨트롤러 안에 직접 만든 {@code @ExceptionHandler} 가 있으면 그쪽이 먼저 처리된다 (기존 코드 영향 없음).</li>
 *   <li>정적 파일(client/) 요청은 대상이 아니다. annotations = RestController.class 로 범위를 묶었다.</li>
 * </ul>
 */
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 직접 던진 ApiException (및 이를 상속한 기능별 예외) */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException e) {
        return respond(e.getStatus(), e.getCode(), e.getMessage());
    }

    /** @Valid 검증 실패. 첫 번째 필드 오류를 메시지로 쓴다 (spring-boot-starter-validation 필요) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        FieldError field = e.getBindingResult().getFieldError();
        String message = field != null
                ? field.getField() + ": " + field.getDefaultMessage()
                : "입력값이 올바르지 않습니다.";
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST, message);
    }

    /** @RequestParam, @PathVariable 등에 붙인 검증 실패 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException e) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST, "입력값이 올바르지 않습니다.");
    }

    /** 요청 본문이 없거나 JSON 형식이 틀림 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST, "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해 주세요.");
    }

    /** 필수 쿼리 파라미터 누락 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST, "필수 값이 없습니다: " + e.getParameterName());
    }

    /** 숫자 자리에 문자가 오는 등 타입 불일치 (예: /models/abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST, "값의 형식이 올바르지 않습니다: " + e.getName());
    }

    /**
     * 그 밖의 오류.
     * 스프링이 상태 코드를 정해 둔 예외(ResponseStatusException, 415 미디어 타입 등)는 상태를 그대로 두고 형식만 맞춘다.
     * 나머지는 예상하지 못한 오류로 보고 내부 내용은 로그에만 남긴다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        if (e instanceof ErrorResponse spring) {
            HttpStatusCode status = spring.getStatusCode();
            String code = switch (status.value()) {
                case 404 -> ErrorCodes.NOT_FOUND;
                case 405 -> ErrorCodes.METHOD_NOT_ALLOWED;
                default -> status.is4xxClientError() ? ErrorCodes.INVALID_REQUEST : ErrorCodes.INTERNAL_ERROR;
            };
            String detail = spring.getBody().getDetail();
            return respond(status, code, detail != null ? detail : "요청을 처리할 수 없습니다.");
        }
        log.error("처리되지 않은 서버 오류", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private static ResponseEntity<ApiResponse<Void>> respond(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(code, message));
    }
}
