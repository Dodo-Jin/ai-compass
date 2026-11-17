package com.aicompass.auth;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 담당 A. 이 패키지(com.aicompass.auth) 안에서 controller/service/repository/entity 를 자유롭게 추가
@RestController
public class AuthController {

    public static final String SESSION_USER_ID = "userId";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record SignupRequest(String email, String password, String username) {}

    public record LoginRequest(String email, String password) {}

    @GetMapping("/api/auth")
    public Map<String, String> ping() {
        return Map.of("ok", "auth");
    }

    @PostMapping("/api/v1/auth/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody(required = false) SignupRequest request) {
        if (request == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 본문이 비어 있습니다.");
        }
        Map<String, Object> user = authService.signup(request.email(), request.password(), request.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(successResponse(user));
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody(required = false) LoginRequest request,
            HttpSession session) {
        if (request == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 본문이 비어 있습니다.");
        }
        Map<String, Object> user = authService.login(request.email(), request.password());
        session.setAttribute(SESSION_USER_ID, user.get("userId"));
        return ResponseEntity.ok(successResponse(user));
    }

    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(successResponse(Map.of("message", "로그아웃 되었습니다.")));
    }

    @GetMapping("/api/v1/auth/check")
    public ResponseEntity<Map<String, Object>> checkDuplicate(
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "username", required = false) String username) {
        Map<String, Object> result = authService.checkDuplicate(email, username);
        return ResponseEntity.ok(successResponse(result));
    }

    @GetMapping("/api/v1/users/me")
    public ResponseEntity<Map<String, Object>> getMe(HttpSession session) {
        Integer userId = extractUserId(session.getAttribute(SESSION_USER_ID));
        Map<String, Object> user = authService.getMe(userId);
        return ResponseEntity.ok(successResponse(user));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException ex) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", ex.getCode());
        errorBody.put("message", ex.getMessage());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("error", errorBody);

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    private Integer extractUserId(Object rawUserId) {
        if (rawUserId instanceof Number number) {
            return number.intValue();
        }
        if (rawUserId instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> successResponse(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }
}