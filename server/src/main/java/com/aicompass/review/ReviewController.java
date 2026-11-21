package com.aicompass.review;

import com.aicompass.auth.AuthController;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 담당 A. 리뷰 목록·등록·수정·삭제·내 리뷰 목록 API
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    public record ReviewRequest(Integer rating, String content) {}

    @GetMapping("/api/review")
    public Map<String, String> ping() {
        return Map.of("ok", "review");
    }

    @GetMapping("/api/v1/models/{modelId}/reviews")
    public ResponseEntity<Map<String, Object>> getModelReviews(
            @PathVariable("modelId") Integer modelId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        Map<String, Object> data = reviewService.getModelReviews(modelId, page, size);
        return ResponseEntity.ok(successResponse(data));
    }

    @PostMapping("/api/v1/models/{modelId}/reviews")
    public ResponseEntity<Map<String, Object>> createReview(
            @PathVariable("modelId") Integer modelId,
            @RequestBody(required = false) ReviewRequest request,
            HttpSession session) {
        if (request == null) {
            throw new ReviewException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 본문이 비어 있습니다.");
        }
        Integer userId = (Integer) session.getAttribute(AuthController.SESSION_USER_ID);
        Map<String, Object> created = reviewService.createReview(modelId, userId, request.rating(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(successResponse(created));
    }

    @PutMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<Map<String, Object>> updateReview(
            @PathVariable("reviewId") Integer reviewId,
            @RequestBody(required = false) ReviewRequest request,
            HttpSession session) {
        if (request == null) {
            throw new ReviewException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 본문이 비어 있습니다.");
        }
        Integer userId = (Integer) session.getAttribute(AuthController.SESSION_USER_ID);
        Map<String, Object> updated = reviewService.updateReview(reviewId, userId, request.rating(), request.content());
        return ResponseEntity.ok(successResponse(updated));
    }

    @DeleteMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable("reviewId") Integer reviewId,
            HttpSession session) {
        Integer userId = (Integer) session.getAttribute(AuthController.SESSION_USER_ID);
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/users/me/reviews")
    public ResponseEntity<Map<String, Object>> getMyReviews(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            HttpSession session) {
        Integer userId = (Integer) session.getAttribute(AuthController.SESSION_USER_ID);
        Map<String, Object> data = reviewService.getMyReviews(userId, page, size);
        return ResponseEntity.ok(successResponse(data));
    }

    @ExceptionHandler(ReviewException.class)
    public ResponseEntity<Map<String, Object>> handleReviewException(ReviewException ex) {
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