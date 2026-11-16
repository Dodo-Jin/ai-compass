package com.aicompass.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "Reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @Column(name = "model_id", nullable = false)
    private Integer modelId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "content", nullable = false, length = 200)
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    protected Review() {
    }

    public Review(Integer modelId, Integer userId, Integer rating, String content) {
        this.modelId = modelId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void update(Integer rating, String content) {
        this.rating = rating;
        this.content = content;
    }

    public Integer getReviewId() {
        return reviewId;
    }

    public Integer getModelId() {
        return modelId;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}