package com.aicompass.tool;

import com.aicompass.auth.User;
import com.aicompass.auth.UserRepository;
import com.aicompass.review.Review;
import com.aicompass.review.ReviewRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolService {

    private final AiModelRepository aiModelRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ToolService(
            AiModelRepository aiModelRepository,
            ReviewRepository reviewRepository,
            UserRepository userRepository) {
        this.aiModelRepository = aiModelRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getModels(
            String keyword,
            String category,
            String pricingType,
            String sort,
            int page,
            int size) {
        int validPage = Math.max(page, 1);
        int validSize = Math.min(Math.max(size, 1), 50);

        Sort sortOrder = resolveSort(sort);
        Pageable pageable = PageRequest.of(validPage - 1, validSize, sortOrder);

        Page<AiModel> modelPage = aiModelRepository.searchModels(
                keyword != null ? keyword.trim() : null,
                category != null ? category.trim() : null,
                pricingType != null ? pricingType.trim() : null,
                pageable);

        List<Map<String, Object>> items = modelPage.getContent().stream()
                .map(this::toModelSummaryDto)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", validPage);
        result.put("size", validSize);
        result.put("totalElements", modelPage.getTotalElements());
        result.put("totalPages", modelPage.getTotalPages());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCategories() {
        List<String> categories = aiModelRepository.findDistinctCategories();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categories);
        return result;
    }

    @Transactional
    public Map<String, Object> getModelDetail(Integer modelId, Integer currentUserId) {
        AiModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ToolException(
                        HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", "해당 AI 툴을 찾을 수 없습니다."));

        model.incrementSearchCount();

        long reviewCount = reviewRepository.countByModelId(modelId);
        Double avgRatingRaw = reviewRepository.findAverageRatingByModelId(modelId);
        double averageRating = avgRatingRaw != null ? Math.round(avgRatingRaw * 10.0) / 10.0 : 0.0;

        boolean isFavorited = false;
        Map<String, Object> myReviewDto = null;

        if (currentUserId != null) {
            isFavorited = aiModelRepository.countFavoriteByUserAndModel(currentUserId, modelId) > 0;
            Optional<Review> myReviewOpt = reviewRepository.findByUserIdAndModelId(currentUserId, modelId);
            if (myReviewOpt.isPresent()) {
                Review myReview = myReviewOpt.get();
                String username = userRepository.findById(currentUserId)
                        .map(User::getUsername)
                        .orElse("알 수 없음");
                myReviewDto = new LinkedHashMap<>();
                myReviewDto.put("reviewId", myReview.getReviewId());
                myReviewDto.put("modelId", myReview.getModelId());
                myReviewDto.put("userId", myReview.getUserId());
                myReviewDto.put("username", username);
                myReviewDto.put("rating", myReview.getRating());
                myReviewDto.put("content", myReview.getContent());
                myReviewDto.put("createdAt", myReview.getCreatedAt() != null ? myReview.getCreatedAt().toString() : null);
            }
        }

        Map<String, Object> dto = toModelSummaryDto(model);
        dto.put("strengths", model.getStrengths());
        dto.put("limitations", model.getLimitations());
        dto.put("averageRating", averageRating);
        dto.put("reviewCount", reviewCount);
        dto.put("isFavorited", isFavorited);
        dto.put("myReview", myReviewDto);
        return dto;
    }

    private Map<String, Object> toModelSummaryDto(AiModel model) {
        long reviewCount = reviewRepository.countByModelId(model.getModelId());
        Double avgRatingRaw = reviewRepository.findAverageRatingByModelId(model.getModelId());
        double averageRating = avgRatingRaw != null ? Math.round(avgRatingRaw * 10.0) / 10.0 : 0.0;

        List<String> tagList = (model.getTags() == null || model.getTags().isBlank())
                ? List.of()
                : Arrays.stream(model.getTags().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

        List<String> targetScaleList = (model.getTargetScales() == null || model.getTargetScales().isBlank())
                ? List.of()
                : Arrays.stream(model.getTargetScales().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("modelId", model.getModelId());
        dto.put("name", model.getName());
        dto.put("category", model.getCategory());
        dto.put("description", model.getDescription());
        dto.put("pricingType", model.getPricingType());
        dto.put("priceText", model.getPriceText());
        dto.put("priceMinUsd", model.getPriceMinUsd());
        dto.put("priceMaxUsd", model.getPriceMaxUsd());
        dto.put("targetScales", model.getTargetScales());
        dto.put("targetScaleList", targetScaleList);
        dto.put("siteUrl", model.getSiteUrl());
        dto.put("logoUrl", model.getLogoUrl());
        dto.put("tags", model.getTags());
        dto.put("tagList", tagList);
        dto.put("inputTypes", model.getInputTypes());
        dto.put("koreanSupport", model.getKoreanSupport());
        dto.put("searchCount", model.getSearchCount());
        dto.put("rankingScore", model.getRankingScore());
        dto.put("averageRating", averageRating);
        dto.put("reviewCount", reviewCount);
        dto.put("createdAt", model.getCreatedAt() != null ? model.getCreatedAt().toString() : null);
        return dto;
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "searchCount").and(Sort.by(Sort.Direction.ASC, "modelId"));
        }
        return switch (sort.trim().toLowerCase()) {
            case "name", "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
            case "latest", "created_at", "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "price_asc", "price" -> Sort.by(Sort.Direction.ASC, "priceMinUsd").and(Sort.by(Sort.Direction.ASC, "modelId"));
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "priceMaxUsd").and(Sort.by(Sort.Direction.ASC, "modelId"));
            case "search", "popular", "ranking", "score" ->
                    Sort.by(Sort.Direction.DESC, "searchCount").and(Sort.by(Sort.Direction.ASC, "modelId"));
            default -> Sort.by(Sort.Direction.DESC, "searchCount").and(Sort.by(Sort.Direction.ASC, "modelId"));
        };
    }
}