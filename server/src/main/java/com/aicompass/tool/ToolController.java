package com.aicompass.tool;

import com.aicompass.auth.AuthController;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 담당 A. AI 툴 디렉토리·카테고리·상세 조회 API
@RestController
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping("/api/tool")
    public Map<String, String> ping() {
        return Map.of("ok", "tool");
    }

    @GetMapping("/api/v1/models")
    public ResponseEntity<Map<String, Object>> getModels(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "pricingType", required = false) String pricingType,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        Map<String, Object> data = toolService.getModels(keyword, category, pricingType, sort, page, size);
        return ResponseEntity.ok(successResponse(data));
    }

    @GetMapping("/api/v1/models/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        Map<String, Object> data = toolService.getCategories();
        return ResponseEntity.ok(successResponse(data));
    }

    @GetMapping("/api/v1/models/{modelId}")
    public ResponseEntity<Map<String, Object>> getModelDetail(
            @PathVariable("modelId") Integer modelId,
            HttpSession session) {
        Integer currentUserId = (Integer) session.getAttribute(AuthController.SESSION_USER_ID);
        Map<String, Object> data = toolService.getModelDetail(modelId, currentUserId);
        return ResponseEntity.ok(successResponse(data));
    }

    @ExceptionHandler(ToolException.class)
    public ResponseEntity<Map<String, Object>> handleToolException(ToolException ex) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", ex.getCode());
        errorBody.put("message", ex.getMessage());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("error", errorBody);

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    private Map<String, Object> successResponse(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }
}