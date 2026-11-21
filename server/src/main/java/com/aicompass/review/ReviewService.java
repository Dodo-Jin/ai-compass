package com.aicompass.review;

import com.aicompass.auth.User;
import com.aicompass.auth.UserRepository;
import com.aicompass.tool.AiModel;
import com.aicompass.tool.AiModelRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AiModelRepository aiModelRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            AiModelRepository aiModelRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.aiModelRepository = aiModelRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getModelReviews(Integer modelId, int page, int size) {
        if (!aiModelRepository.existsById(modelId)) {
            throw new ReviewException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "해당 AI 툴을 찾을 수 없습니다.");
        }

        int validPage = Math.max(page, 1);
        int validSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(validPage - 1, validSize);

        Page<Review> reviewPage = reviewRepository.findByModelIdOrderByCreatedAtDesc(modelId, pageable);
        List<Map<String, Object>> items = reviewPage.getContent().stream()
                .map(this::toReviewDto)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", validPage);
        result.put("size", validSize);
        result.put("totalElements", reviewPage.getTotalElements());
        result.put("totalPages", reviewPage.getTotalPages());
        return result;
    }

    @Transactional
    public Map<String, Object> createReview(Integer modelId, Integer userId, Integer rating, String content) {
        if (userId == null) {
            throw new ReviewException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (!aiModelRepository.existsById(modelId)) {
            throw new ReviewException(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "해당 AI 툴을 찾을 수 없습니다.");
        }
        validateRatingAndContent(rating, content);

        if (reviewRepository.existsByUserIdAndModelId(userId, modelId)) {
            throw new ReviewException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "이미 이 AI 툴에 대한 리뷰를 작성하셨습니다.");
        }

        Review saved = reviewRepository.save(new Review(modelId, userId, rating, content.trim()));
        return toReviewDto(saved);
    }

    @Transactional
    public Map<String, Object> updateReview(Integer reviewId, Integer userId, Integer rating, String content) {
        if (userId == null) {
            throw new ReviewException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "리뷰를 찾을 수 없습니다."));

        if (!review.getUserId().equals(userId)) {
            throw new ReviewException(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        validateRatingAndContent(rating, content);
        review.update(rating, content.trim());
        return toReviewDto(review);
    }

    @Transactional
    public void deleteReview(Integer reviewId, Integer userId) {
        if (userId == null) {
            throw new ReviewException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "리뷰를 찾을 수 없습니다."));

        if (!review.getUserId().equals(userId)) {
            throw new ReviewException(HttpStatus.FORBIDDEN, "FORBIDDEN", "본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyReviews(Integer userId, int page, int size) {
        if (userId == null) {
            throw new ReviewException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        int validPage = Math.max(page, 1);
        int validSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(validPage - 1, validSize);

        Page<Review> reviewPage = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<Map<String, Object>> items = reviewPage.getContent().stream()
                .map(this::toMyReviewDto)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", validPage);
        result.put("size", validSize);
        result.put("totalElements", reviewPage.getTotalElements());
        result.put("totalPages", reviewPage.getTotalPages());
        return result;
    }

    private void validateRatingAndContent(Integer rating, String content) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ReviewException(HttpStatus.BAD_REQUEST, "INVALID_RATING", "별점은 1점에서 5점 사이여야 합니다.");
        }
        if (content == null || content.isBlank() || content.trim().length() > 200) {
            throw new ReviewException(HttpStatus.BAD_REQUEST, "INVALID_CONTENT", "리뷰 내용은 1자 이상 200자 이내여야 합니다.");
        }
    }

    private Map<String, Object> toReviewDto(Review review) {
        String username = userRepository.findById(review.getUserId())
                .map(User::getUsername)
                .orElse("알 수 없음");

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("reviewId", review.getReviewId());
        dto.put("modelId", review.getModelId());
        dto.put("userId", review.getUserId());
        dto.put("username", username);
        dto.put("rating", review.getRating());
        dto.put("content", review.getContent());
        dto.put("createdAt", review.getCreatedAt() != null ? review.getCreatedAt().toString() : null);
        return dto;
    }

    private Map<String, Object> toMyReviewDto(Review review) {
        Map<String, Object> dto = toReviewDto(review);
        String modelName = aiModelRepository.findById(review.getModelId())
                .map(AiModel::getName)
                .orElse(null);
        dto.put("modelName", modelName);
        return dto;
    }
}