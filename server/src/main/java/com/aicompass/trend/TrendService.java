package com.aicompass.trend;

import com.aicompass.common.ApiException;
import com.aicompass.common.ErrorCodes;
import com.aicompass.trend.TrendCalculator.Entry;
import com.aicompass.trend.TrendCalculator.Favorite;
import com.aicompass.trend.TrendCalculator.Model;
import com.aicompass.trend.TrendCalculator.Period;
import com.aicompass.trend.TrendCalculator.Pipeline;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 실시간 AI 랭킹 Top 100 (담당 C). 요청이 올 때 계산한다 (ranking_score 컬럼은 쓰지 않음).
 * 계산 규칙은 {@link TrendCalculator}.
 */
@Service
public class TrendService {

    static final int TOP = 100;
    static final int SIZE_DEFAULT = 10;
    static final int SIZE_MAX = 100;

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public TrendService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public Map<String, Object> rankings(String periodParam, String category, int page, int size, Integer userId) {
        Period period = Period.of(periodParam == null || periodParam.isBlank() ? "realtime" : periodParam.trim());
        if (period == null) {
            throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "period 는 realtime, weekly, monthly 중 하나여야 합니다.");
        }
        int pageNo = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), SIZE_MAX);
        LocalDateTime now = LocalDateTime.now().withNano(0);

        List<Model> models = jdbc.query(
                "SELECT model_id, name, category, tags, logo_url, search_count, created_at FROM AI_Models",
                (rs, i) -> new Model(rs.getInt("model_id"), rs.getString("name"), rs.getString("category"),
                        rs.getString("tags"), rs.getString("logo_url"), rs.getInt("search_count"), time(rs.getTimestamp("created_at"))));
        List<Favorite> favorites = jdbc.query(
                "SELECT model_id, user_id, created_at FROM Favorites",
                (rs, i) -> new Favorite(rs.getInt("model_id"), rs.getInt("user_id"), time(rs.getTimestamp("created_at"))));
        List<Pipeline> pipelines = jdbc.query(
                "SELECT block_data, created_at FROM Pipelines",
                (rs, i) -> new Pipeline(modelIdsIn(rs.getString("block_data")), time(rs.getTimestamp("created_at"))));

        String cat = category == null || category.isBlank() ? null : category.trim();
        List<Entry> ranked = TrendCalculator.rank(models, favorites, pipelines, period, cat, now, userId);
        List<Entry> top = ranked.subList(0, Math.min(TOP, ranked.size()));

        int from = Math.min((pageNo - 1) * pageSize, top.size());
        int to = Math.min(from + pageSize, top.size());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", top.subList(from, to).stream().map(TrendService::view).toList());
        res.put("categories", categories(models));
        res.put("period", period.value);
        res.put("page", pageNo);
        res.put("size", pageSize);
        res.put("totalElements", top.size());
        res.put("totalPages", (int) Math.ceil(top.size() / (double) pageSize));
        res.put("updatedAt", now.toString());
        return res;
    }

    private static Map<String, Object> view(Entry e) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("rank", e.rank());
        v.put("rankChange", e.rankChange());
        v.put("isNew", e.isNew());
        v.put("modelId", e.model().modelId());
        v.put("name", e.model().name());
        v.put("category", e.model().category());
        v.put("tags", splitList(e.model().tags()));
        v.put("logoUrl", e.model().logoUrl());
        v.put("trendScore", e.trendScore());
        v.put("searchCount", e.model().searchCount());
        v.put("favoriteCount", e.favoriteCount());
        v.put("pipelineCount", e.pipelineCount());
        v.put("pipelineRate", e.pipelineRate());
        v.put("isFavorited", e.isFavorited());
        return v;
    }

    /** 툴이 많은 카테고리부터 (탭 순서용) */
    static List<String> categories(List<Model> models) {
        Map<String, Long> counts = models.stream()
                .map(Model::category)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Pipelines.block_data(JSON 배열 [{ modelId, order, purpose }]) 에 들어 있는 툴 번호. 형식이 틀리면 빈 집합 */
    Set<Integer> modelIdsIn(String blockData) {
        if (blockData == null || blockData.isBlank()) {
            return Set.of();
        }
        try {
            JsonNode root = mapper.readTree(blockData);
            Set<Integer> ids = new LinkedHashSet<>();
            if (root != null && root.isArray()) {
                for (JsonNode block : root) {
                    JsonNode id = block.get("modelId");
                    if (id != null && id.canConvertToInt() && id.asInt() > 0) {
                        ids.add(id.asInt());
                    }
                }
            }
            return ids;
        } catch (Exception badJson) {
            return Set.of();
        }
    }

    private static LocalDateTime time(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }

    static List<String> splitList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
