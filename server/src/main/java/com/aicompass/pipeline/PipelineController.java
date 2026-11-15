package com.aicompass.pipeline;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 담당 B. 이 패키지(com.aicompass.pipeline) 안에서 controller/service/repository/entity 를 자유롭게 추가
@RestController
@RequestMapping("/api/v1/pipelines")
public class PipelineController {

    private final SurveyRecommendService survey;

    public PipelineController(SurveyRecommendService survey) {
        this.survey = survey;
    }

    @GetMapping
    public Map<String, String> ping() {
        return Map.of("ok", "pipeline");
    }

    public record SurveyRequest(String job, String usage, String budget) {}

    /** 설문 추천. 응답은 클라이언트(pipeline-api.js)가 읽는 평평한 JSON, 오류는 { message } */
    @PostMapping("/recommend-by-survey")
    public ResponseEntity<?> recommendBySurvey(@RequestBody SurveyRequest req) {
        try {
            SurveyRecommendService.Result r = survey.recommend(req.job(), req.usage(), req.budget());
            if (r == null) {
                return ResponseEntity.status(404).body(Map.of("message", "조건에 맞는 AI 툴을 찾지 못했어요. 다른 조건으로 시도해 보세요."));
            }
            return ResponseEntity.ok(r);
        } catch (SurveyRecommendService.InvalidSurveyException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
