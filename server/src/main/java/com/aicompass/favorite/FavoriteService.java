package com.aicompass.favorite;

import com.aicompass.common.ApiException;
import com.aicompass.common.ErrorCodes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 즐겨찾기 (담당 C). 모든 메서드는 로그인한 사용자(userId)의 즐겨찾기만 다룬다.
 * 목록·비교표는 AI_Models 확장 컬럼(db/migrations/001_add_model_columns.sql)을 함께 읽는다.
 */
@Service
public class FavoriteService {

    static final int SIZE_DEFAULT = 50;
    static final int SIZE_MAX = 100;
    static final int COMPARE_MIN = 2;
    static final int COMPARE_MAX = 4;

    /** 추가 결과. created 가 false 면 이미 즐겨찾기한 툴이었다 (오류 아님) */
    public record AddResult(boolean created, Map<String, Object> body) {}

    /** 평균 별점·리뷰 수·즐겨찾기 수 (모델별 하위 쿼리) */
    private static final String STATS_COLUMNS = """
            (SELECT ROUND(AVG(r.rating), 1) FROM Reviews r WHERE r.model_id = m.model_id) AS avg_rating,
            (SELECT COUNT(*) FROM Reviews r WHERE r.model_id = m.model_id) AS review_count,
            (SELECT COUNT(*) FROM Favorites f2 WHERE f2.model_id = m.model_id) AS favorite_count
            """;

    private final JdbcTemplate jdbc;

    public FavoriteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ 추가·삭제

    /** 같은 툴을 두 번 추가해도 오류 없이 현재 상태를 돌려준다 (버튼 연타 대비). DB 에 UQ_FAVORITES 제약이 있다 */
    public AddResult add(int userId, Integer modelId) {
        if (modelId == null || modelId < 1) {
            throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "modelId 가 필요합니다.");
        }
        requireModel(modelId);
        boolean created;
        try {
            jdbc.update("INSERT INTO Favorites (user_id, model_id) VALUES (?, ?)", userId, modelId);
            created = true;
        } catch (DuplicateKeyException alreadyFavorited) {
            created = false;
        }
        return new AddResult(created, status(modelId, true));
    }

    /** 없는 즐겨찾기를 지워도 오류 없이 끝난다 (결과는 같으므로) */
    public void remove(int userId, int modelId) {
        jdbc.update("DELETE FROM Favorites WHERE user_id = ? AND model_id = ?", userId, modelId);
    }

    private Map<String, Object> status(int modelId, boolean favorited) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM Favorites WHERE model_id = ?", Long.class, modelId);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("modelId", modelId);
        res.put("isFavorited", favorited);
        res.put("favoriteCount", count == null ? 0 : count);
        return res;
    }

    // ------------------------------------------------------------------ 목록

    /** 최근에 추가한 순. page 는 1부터. category 를 주면 그 카테고리만. categories 는 필터 드롭다운용(전체 기준) */
    public Map<String, Object> list(int userId, String category, int page, int size) {
        int pageNo = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), SIZE_MAX);

        String where = "f.user_id = ?";
        List<Object> args = new ArrayList<>(List.of(userId));
        if (category != null && !category.isBlank()) {
            where += " AND m.category = ?";
            args.add(category.trim());
        }

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM Favorites f JOIN AI_Models m ON m.model_id = f.model_id WHERE " + where,
                Long.class, args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((long) (pageNo - 1) * pageSize);
        List<Map<String, Object>> items = jdbc.query("""
                SELECT f.favorite_id, f.created_at AS favorited_at,
                       m.model_id, m.name, m.category, m.description, m.pricing_type,
                       m.logo_url, m.tags, m.price_text,
                """ + STATS_COLUMNS + """
                FROM Favorites f JOIN AI_Models m ON m.model_id = f.model_id
                WHERE """ + " " + where + """
                 ORDER BY f.created_at DESC, f.favorite_id DESC
                LIMIT ? OFFSET ?
                """,
                (rs, i) -> listItem(rs), pageArgs.toArray());

        List<String> categories = jdbc.queryForList("""
                SELECT DISTINCT m.category FROM Favorites f JOIN AI_Models m ON m.model_id = f.model_id
                WHERE f.user_id = ? ORDER BY m.category
                """, String.class, userId);

        long totalElements = total == null ? 0 : total;
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", items);
        res.put("categories", categories);
        res.put("page", pageNo);
        res.put("size", pageSize);
        res.put("totalElements", totalElements);
        res.put("totalPages", (int) Math.ceil(totalElements / (double) pageSize));
        return res;
    }

    private static Map<String, Object> listItem(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("favoriteId", rs.getInt("favorite_id"));
        item.put("modelId", rs.getInt("model_id"));
        item.put("name", rs.getString("name"));
        item.put("category", rs.getString("category"));
        item.put("description", rs.getString("description"));
        item.put("logoUrl", rs.getString("logo_url"));
        item.put("tags", splitList(rs.getString("tags")));
        item.put("pricingType", rs.getString("pricing_type"));
        item.put("priceText", rs.getString("price_text"));
        putStats(item, rs);
        Timestamp at = rs.getTimestamp("favorited_at");
        item.put("favoritedAt", at == null ? null : at.toLocalDateTime().withNano(0).toString()); // B 와 같은 형식
        return item;
    }

    // ------------------------------------------------------------------ 비교표

    /**
     * modelIds("1,2,3") 순서대로 비교 항목을 돌려준다. 2~4개.
     * 즐겨찾기에 없는 툴도 비교할 수 있고, 각 항목의 isFavorited 로 구분한다.
     */
    public Map<String, Object> compare(int userId, String modelIdsParam) {
        List<Integer> ids = parseModelIds(modelIdsParam);
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));

        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.addAll(ids);
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT m.model_id, m.name, m.category, m.description, m.pricing_type,
                       m.logo_url, m.site_url, m.tags, m.price_text, m.price_min_usd, m.price_max_usd,
                       m.input_types, m.korean_support, m.strengths, m.limitations,
                       (SELECT COUNT(*) FROM Favorites mine WHERE mine.user_id = ? AND mine.model_id = m.model_id) AS mine,
                """ + STATS_COLUMNS + """
                FROM AI_Models m
                WHERE m.model_id IN (""" + placeholders + ")",
                (rs, i) -> compareItem(rs), args.toArray());

        Map<Integer, Map<String, Object>> byId = new HashMap<>();
        rows.forEach(r -> byId.put((Integer) r.get("modelId"), r));
        List<Integer> missing = ids.stream().filter(id -> !byId.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw ApiException.notFound("MODEL_NOT_FOUND", "AI 툴을 찾을 수 없습니다: " + missing);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", ids.stream().map(byId::get).toList());
        return res;
    }

    private static Map<String, Object> compareItem(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("modelId", rs.getInt("model_id"));
        item.put("name", rs.getString("name"));
        item.put("category", rs.getString("category"));
        item.put("description", rs.getString("description"));
        item.put("logoUrl", rs.getString("logo_url"));
        item.put("siteUrl", rs.getString("site_url"));
        item.put("tags", splitList(rs.getString("tags")));
        item.put("pricingType", rs.getString("pricing_type"));
        item.put("priceText", rs.getString("price_text"));
        item.put("priceMinUsd", rs.getBigDecimal("price_min_usd"));
        item.put("priceMaxUsd", rs.getBigDecimal("price_max_usd"));
        item.put("inputTypes", splitList(rs.getString("input_types")));
        item.put("koreanSupport", rs.getBoolean("korean_support"));
        item.put("strengths", rs.getString("strengths"));
        item.put("limitations", rs.getString("limitations"));
        putStats(item, rs);
        item.put("isFavorited", rs.getInt("mine") > 0);
        return item;
    }

    // ------------------------------------------------------------------ 공통

    private void requireModel(int modelId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM AI_Models WHERE model_id = ?", Integer.class, modelId);
        if (n == null || n == 0) {
            throw ApiException.notFound("MODEL_NOT_FOUND", "AI 툴을 찾을 수 없습니다.");
        }
    }

    private static void putStats(Map<String, Object> item, ResultSet rs) throws SQLException {
        double avg = rs.getDouble("avg_rating"); // 리뷰가 없으면 NULL → 0.0
        item.put("averageRating", avg);
        item.put("reviewCount", rs.getLong("review_count"));
        item.put("favoriteCount", rs.getLong("favorite_count"));
    }

    /** "1, 2,3" → [1, 2, 3]. 중복은 한 번만. 2~4개가 아니거나 숫자가 아니면 400 */
    static List<Integer> parseModelIds(String param) {
        if (param == null || param.isBlank()) {
            throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "modelIds 가 필요합니다. 예: ?modelIds=1,2,3");
        }
        Set<Integer> ids = new LinkedHashSet<>();
        for (String part : param.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                int id = Integer.parseInt(s);
                if (id < 1) {
                    throw new NumberFormatException();
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "modelIds 는 숫자여야 합니다: " + s);
            }
        }
        if (ids.size() < COMPARE_MIN || ids.size() > COMPARE_MAX) {
            throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST,
                    "비교는 서로 다른 AI 툴 " + COMPARE_MIN + "~" + COMPARE_MAX + "개로 할 수 있습니다.");
        }
        return List.copyOf(ids);
    }

    /** DB 의 쉼표 구분 문자열("텍스트, 이미지") → ["텍스트", "이미지"]. 비어 있으면 [] */
    static List<String> splitList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
