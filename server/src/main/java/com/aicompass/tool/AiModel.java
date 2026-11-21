package com.aicompass.tool;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "AI_Models")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Integer modelId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "pricing_type", length = 30)
    private String pricingType;

    @Column(name = "ranking_score")
    private Integer rankingScore;

    @Column(name = "site_url", length = 500)
    private String siteUrl;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "price_text", length = 100)
    private String priceText;

    @Column(name = "price_min_usd", precision = 8, scale = 2)
    private BigDecimal priceMinUsd;

    @Column(name = "price_max_usd", precision = 8, scale = 2)
    private BigDecimal priceMaxUsd;

    @Column(name = "target_scales", length = 100)
    private String targetScales;

    @Column(name = "input_types", length = 100)
    private String inputTypes;

    @Column(name = "korean_support", nullable = false)
    private Boolean koreanSupport;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "limitations", columnDefinition = "TEXT")
    private String limitations;

    @Column(name = "search_count", nullable = false)
    private Integer searchCount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    protected AiModel() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.rankingScore == null) {
            this.rankingScore = 0;
        }
        if (this.koreanSupport == null) {
            this.koreanSupport = false;
        }
        if (this.searchCount == null) {
            this.searchCount = 0;
        }
    }

    public void incrementSearchCount() {
        this.searchCount = (this.searchCount == null ? 0 : this.searchCount) + 1;
    }

    public Integer getModelId() {
        return modelId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getPricingType() {
        return pricingType;
    }

    public Integer getRankingScore() {
        return rankingScore;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getTags() {
        return tags;
    }

    public String getPriceText() {
        return priceText;
    }

    public BigDecimal getPriceMinUsd() {
        return priceMinUsd;
    }

    public BigDecimal getPriceMaxUsd() {
        return priceMaxUsd;
    }

    public String getTargetScales() {
        return targetScales;
    }

    public String getInputTypes() {
        return inputTypes;
    }

    public Boolean getKoreanSupport() {
        return koreanSupport;
    }

    public String getStrengths() {
        return strengths;
    }

    public String getLimitations() {
        return limitations;
    }

    public Integer getSearchCount() {
        return searchCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}