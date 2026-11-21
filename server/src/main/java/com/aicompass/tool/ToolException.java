package com.aicompass.tool;

import org.springframework.http.HttpStatus;

public class ToolException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ToolException(HttpStatus status, String code, String message) {
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
}