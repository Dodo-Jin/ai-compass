package com.aicompass.pipeline;

import com.aicompass.common.ApiException;
import com.aicompass.common.ErrorCodes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** 설문(직업·사용량·예산)으로 AI_Models 를 걸러 ranking_score 가 가장 높은 툴 1개를 고른다. */
@Service
public class SurveyRecommendService {

    // 설문 코드(client/assets/js/chatbot/survey-options.js)와 화면 라벨
    static final Map<String, String> JOBS = Map.of("marketer", "마케터", "developer", "개발자", "creator", "콘텐츠 제작자", "other", "기타");
    static final Map<String, String> USAGES = Map.of("personal", "개인용", "team", "팀용", "enterprise", "엔터프라이즈");
    static final Map<String, String> BUDGETS = Map.of("free", "무료", "under30k", "3만원 이하", "30k-150k", "3~15만원", "over150k", "15만원 이상");

    // AI_Models.category 시드 값(db/migrations/002_seed_models.sql). other 는 카테고리 제한 없음
    static final Map<String, List<String>> JOB_CATEGORIES = Map.of(
            "marketer", List.of("텍스트/LLM", "이미지 생성", "검색/리서치", "번역/글쓰기"),
            "developer", List.of("코딩/개발"),
            "creator", List.of("이미지 생성", "영상 생성", "오디오/음악"));

    public record Result(int modelId, String name, String category, String logoUrl, String reason, String priceText) {}

    /** 잘못된 설문 값 (400) */
    public static class InvalidSurveyException extends ApiException {
        InvalidSurveyException(String message) { super(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST, message); }
    }

    private final JdbcTemplate jdbc;

    public SurveyRecommendService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 일치하는 툴이 없으면 404. 직업 → 사용량 순으로 조건을 풀어 다시 찾는다(예산은 유지). */
    public Result recommend(String job, String usage, String budget) {
        // Map.of 는 containsKey(null) 이 NPE 라서 null 을 먼저 거른다
        if (job == null || usage == null || budget == null
                || !JOBS.containsKey(job) || !USAGES.containsKey(usage) || !BUDGETS.containsKey(budget)) {
            throw new InvalidSurveyException("직업, 사용량, 예산을 모두 선택해 주세요.");
        }
        boolean[][] attempts = { { true, true }, { false, true }, { false, false } };
        for (int i = "other".equals(job) ? 1 : 0; i < attempts.length; i++) { // other 는 직업 조건이 없어 첫 시도가 중복
            boolean[] a = attempts[i];
            Result r = find(a[0] ? job : null, a[1] ? usage : null, budget);
            if (r != null) return r;
        }
        throw ApiException.notFound("MODEL_NOT_FOUND", "조건에 맞는 AI 툴을 찾지 못했어요. 다른 조건으로 시도해 보세요.");
    }

    private Result find(String job, String usage, String budget) {
        StringBuilder sql = new StringBuilder(
                "SELECT model_id, name, category, description, logo_url, price_text, price_min_usd, price_max_usd FROM AI_Models WHERE ")
                .append(budgetCondition(budget));
        List<Object> args = new ArrayList<>();
        List<String> cats = job == null ? null : JOB_CATEGORIES.get(job);
        if (cats != null) {
            sql.append(" AND category IN (").append(String.join(",", cats.stream().map(c -> "?").toList())).append(")");
            args.addAll(cats);
        }
        if (usage != null) {
            sql.append(" AND (target_scales IS NULL OR FIND_IN_SET(?, REPLACE(LOWER(target_scales), ' ', '')) > 0)");
            args.add(usage);
        }
        sql.append(" ORDER BY ranking_score DESC, model_id LIMIT 1");

        List<Result> rows = jdbc.query(sql.toString(), (rs, i) -> {
            String description = rs.getString("description");
            // 실제로 적용한 조건만 이유에 적는다(조건을 푼 경우 거짓말하지 않도록)
            List<String> used = new ArrayList<>();
            if (cats != null) used.add(JOBS.get(job));
            if (usage != null) used.add(USAGES.get(usage));
            used.add(BUDGETS.get(budget));
            String reason = String.join(" · ", used) + " 예산에 적합한 " + rs.getString("category") + " AI입니다."
                    + (description == null ? "" : " " + description);
            String priceText = rs.getString("price_text"); // 시드의 상세 요금 안내가 있으면 그대로, 없으면 가격 범위로 만든다
            if (priceText == null || priceText.isBlank()) {
                priceText = priceText(budget, rs.getBigDecimal("price_min_usd"), rs.getBigDecimal("price_max_usd"));
            }
            return new Result(rs.getInt("model_id"), rs.getString("name"), rs.getString("category"), rs.getString("logo_url"), reason, priceText);
        }, args.toArray());
        return rows.isEmpty() ? null : rows.get(0);
    }

    // 예산 구간(달러): 3만원 ≈ $20, 15만원 ≈ $100. NULL 가격은 어느 구간에도 안 걸린다.
    static String budgetCondition(String budget) {
        return switch (budget) {
            case "free" -> "price_min_usd = 0";
            case "under30k" -> "price_min_usd <= 20";
            case "30k-150k" -> "price_min_usd <= 100 AND price_max_usd >= 20";
            case "over150k" -> "price_max_usd >= 100";
            default -> throw new InvalidSurveyException("알 수 없는 예산입니다.");
        };
    }

    static String priceText(String budget, BigDecimal min, BigDecimal max) {
        if ("free".equals(budget)) return "무료 티어로 시작할 수 있어요";
        if (min == null || max == null) return null;
        return min.compareTo(max) == 0 ? "$" + plain(min) + " / 월" : "$" + plain(min) + " ~ $" + plain(max) + " / 월";
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
