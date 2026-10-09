package com.aicompass.auth;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 담당 A. 이 패키지(com.aicompass.auth) 안에서 controller/service/repository/entity 를 자유롭게 추가
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping
    public Map<String, String> ping() {
        return Map.of("ok", "auth");
    }
}
