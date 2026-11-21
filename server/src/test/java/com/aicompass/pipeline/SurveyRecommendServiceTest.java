package com.aicompass.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SurveyRecommendServiceTest {

    @Test
    void budgetConditionAndPriceText() {
        assertEquals("price_min_usd = 0", SurveyRecommendService.budgetCondition("free"));
        assertEquals("price_min_usd <= 100 AND price_max_usd >= 20", SurveyRecommendService.budgetCondition("30k-150k"));
        assertEquals("$20 ~ $100 / 월", SurveyRecommendService.priceText("30k-150k", new BigDecimal("20.00"), new BigDecimal("100.00")));
        assertEquals("$9.99 / 월", SurveyRecommendService.priceText("under30k", new BigDecimal("9.99"), new BigDecimal("9.99")));
        var svc = new SurveyRecommendService(null);
        assertThrows(SurveyRecommendService.InvalidSurveyException.class, () -> svc.recommend("x", "team", "free"));
        assertThrows(SurveyRecommendService.InvalidSurveyException.class, () -> svc.recommend(null, "team", "free")); // 필드 누락
    }
}
