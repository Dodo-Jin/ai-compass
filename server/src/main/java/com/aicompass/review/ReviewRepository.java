package com.aicompass.review;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    Page<Review> findByModelIdOrderByCreatedAtDesc(Integer modelId, Pageable pageable);

    Page<Review> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Optional<Review> findByUserIdAndModelId(Integer userId, Integer modelId);

    boolean existsByUserIdAndModelId(Integer userId, Integer modelId);

    long countByModelId(Integer modelId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.modelId = :modelId")
    Double findAverageRatingByModelId(@Param("modelId") Integer modelId);
}