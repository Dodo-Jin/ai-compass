package com.aicompass.common;

/**
 * 여러 기능이 같이 쓰는 오류 코드. 기능 고유 코드(MODEL_NOT_FOUND 등)는 각자 문자열로 쓰고 api-spec.md 에 적는다.
 */
public final class ErrorCodes {
    /** 400. 필수 값 누락, 형식 오류, 검증 실패 */
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    /** 401. 로그인 필요 */
    public static final String LOGIN_REQUIRED = "LOGIN_REQUIRED";
    /** 404. 없는 API 경로 */
    public static final String NOT_FOUND = "NOT_FOUND";
    /** 405. 지원하지 않는 메서드 */
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    /** 500. 예상하지 못한 서버 오류 */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCodes() {}
}
