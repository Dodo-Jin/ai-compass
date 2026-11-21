package com.aicompass.pipeline;

import com.aicompass.common.ApiResponse;
import com.aicompass.common.LoginUser;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 담당 B. 챗봇·설문 추천(로그인 없이 사용)과 파이프라인 보관함(로그인 필요). 오류는 ApiException → GlobalExceptionHandler
@RestController
@RequestMapping("/api/v1/pipelines")
public class PipelineController {

    private final SurveyRecommendService survey;
    private final ChatPipelineService chat;
    private final PipelineService pipelines;

    public PipelineController(SurveyRecommendService survey, ChatPipelineService chat, PipelineService pipelines) {
        this.survey = survey;
        this.chat = chat;
        this.pipelines = pipelines;
    }

    // ---------------------------------------------------------------- 챗봇·설문 (저장 안 함)

    public record GenerateRequest(String message) {}

    @PostMapping("/generate")
    public ApiResponse<ChatPipelineService.Result> generate(@RequestBody GenerateRequest req) {
        return ApiResponse.ok(chat.generate(req.message()));
    }

    public record SurveyRequest(String job, String usage, String budget) {}

    @PostMapping("/recommend-by-survey")
    public ApiResponse<SurveyRecommendService.Result> recommendBySurvey(@RequestBody SurveyRequest req) {
        return ApiResponse.ok(survey.recommend(req.job(), req.usage(), req.budget()));
    }

    // ---------------------------------------------------------------- 보관함 (로그인 필요, 본인 것만)

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody PipelineService.SaveRequest req, HttpSession session) {
        return ResponseEntity.status(201).body(ApiResponse.ok(pipelines.create(LoginUser.require(session), req)));
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            HttpSession session) {
        return ApiResponse.ok(pipelines.list(LoginUser.require(session), q, sort, page, size));
    }

    @GetMapping("/{pipelineId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable int pipelineId, HttpSession session) {
        return ApiResponse.ok(pipelines.get(LoginUser.require(session), pipelineId));
    }

    @PutMapping("/{pipelineId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable int pipelineId, @RequestBody PipelineService.SaveRequest req, HttpSession session) {
        return ApiResponse.ok(pipelines.update(LoginUser.require(session), pipelineId, req));
    }

    @DeleteMapping("/{pipelineId}")
    public ResponseEntity<Void> delete(@PathVariable int pipelineId, HttpSession session) {
        pipelines.delete(LoginUser.require(session), pipelineId);
        return ResponseEntity.noContent().build();
    }
}
