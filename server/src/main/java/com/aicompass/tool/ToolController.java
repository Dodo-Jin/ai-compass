package com.aicompass.tool;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 담당 A. 이 패키지(com.aicompass.tool) 안에서 controller/service/repository/entity 를 자유롭게 추가
@RestController
@RequestMapping("/api/tool")
public class ToolController {

    @GetMapping
    public Map<String, String> ping() {
        return Map.of("ok", "tool");
    }
}
