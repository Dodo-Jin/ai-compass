package com.aicompass.chat;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 담당 B. 이 패키지(com.aicompass.chat) 안에서 controller/service/repository/entity 를 자유롭게 추가
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @GetMapping
    public Map<String, String> ping() {
        return Map.of("ok", "chat");
    }
}
