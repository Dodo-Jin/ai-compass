package com.aicompass.tool;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiModelRepository extends JpaRepository<AiModel, Integer> {

    @Query("""
            SELECT m FROM AiModel m
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(m.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(m.tags, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:category IS NULL OR :category = '' OR m.category = :category)
              AND (:pricingType IS NULL OR :pricingType = '' OR m.pricingType = :pricingType)
            """)
    Page<AiModel> searchModels(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("pricingType") String pricingType,
            Pageable pageable);

    @Query("SELECT DISTINCT m.category FROM AiModel m WHERE m.category IS NOT NULL ORDER BY m.category ASC")
    List<String> findDistinctCategories();

    @Query(value = "SELECT COUNT(*) FROM Favorites WHERE user_id = :userId AND model_id = :modelId", nativeQuery = true)
    long countFavoriteByUserAndModel(@Param("userId") Integer userId, @Param("modelId") Integer modelId);
}