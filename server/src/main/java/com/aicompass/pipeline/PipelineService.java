package com.aicompass.pipeline;

import com.aicompass.common.ApiException;
import com.aicompass.common.ErrorCodes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

/**
 * 파이프라인 보관함 CRUD. 모든 메서드는 로그인한 사용자(userId)의 것만 다룬다.
 * 블록은 Pipelines.block_data(TEXT)에 JSON 배열로 저장하고, 조회할 때 AI_Models 와 합쳐 이름·카테고리·로고를 채운다.
 */
@Service
public class PipelineService {

    static final int TITLE_MAX = 100;
    static final int BLOCKS_MAX = 20;
    static final int PURPOSE_MAX = 200;
    static final int SIZE_DEFAULT = 12;
    static final int SIZE_MAX = 50;

    public record BlockIn(Integer modelId, String purpose) {}

    public record SaveRequest(String title, List<BlockIn> blocks) {}

    record StoredBlock(int modelId, int order, String purpose) {}

    private record Row(int id, String title, String blockData, Timestamp createdAt) {}

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public PipelineService(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ 생성·수정·삭제

    public Map<String, Object> create(int userId, SaveRequest req) {
        String title = normalizeTitle(req == null ? null : req.title());
        List<StoredBlock> blocks = normalizeBlocks(req == null ? null : req.blocks());
        requireModelsExist(blocks);
        String json = toJson(blocks);

        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Pipelines (user_id, title, block_data) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, json);
            return ps;
        }, keys);
        return get(userId, keys.getKey().intValue());
    }

    public Map<String, Object> update(int userId, int id, SaveRequest req) {
        findRow(userId, id); // 없거나 남의 것이면 404
        String title = normalizeTitle(req == null ? null : req.title());
        List<StoredBlock> blocks = normalizeBlocks(req == null ? null : req.blocks());
        requireModelsExist(blocks);
        jdbc.update("UPDATE Pipelines SET title = ?, block_data = ? WHERE pipeline_id = ? AND user_id = ?", title, toJson(blocks), id, userId);
        return get(userId, id);
    }

    public void delete(int userId, int id) {
        if (jdbc.update("DELETE FROM Pipelines WHERE pipeline_id = ? AND user_id = ?", id, userId) == 0) throw notFound();
    }

    // ------------------------------------------------------------------ 조회

    public Map<String, Object> get(int userId, int id) {
        return toViews(List.of(findRow(userId, id))).get(0);
    }

    /** page 는 1부터(api-spec). sort: latest | oldest | title */
    public Map<String, Object> list(int userId, String q, String sort, int page, int size) {
        String orderBy = switch (sort == null ? "latest" : sort) {
            case "latest" -> "created_at DESC, pipeline_id DESC";
            case "oldest" -> "created_at ASC, pipeline_id ASC";
            case "title" -> "title ASC, pipeline_id DESC";
            default -> throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "sort 는 latest, oldest, title 중 하나여야 합니다.");
        };
        int pageNo = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), SIZE_MAX);

        String where = "user_id = ?";
        List<Object> args = new ArrayList<>(List.of(userId));
        if (q != null && !q.isBlank()) {
            where += " AND title LIKE ? ESCAPE '\\\\'";
            args.add("%" + escapeLike(q.trim()) + "%");
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM Pipelines WHERE " + where, Long.class, args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((long) (pageNo - 1) * pageSize);
        List<Row> rows = jdbc.query(
                "SELECT pipeline_id, title, block_data, created_at FROM Pipelines WHERE " + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
                (rs, i) -> new Row(rs.getInt("pipeline_id"), rs.getString("title"), rs.getString("block_data"), rs.getTimestamp("created_at")),
                pageArgs.toArray());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", toViews(rows));
        res.put("page", pageNo);
        res.put("size", pageSize);
        res.put("totalElements", total);
        res.put("totalPages", (int) Math.ceil(total / (double) pageSize));
        return res;
    }

    // ------------------------------------------------------------------ 내부

    private Row findRow(int userId, int id) {
        List<Row> rows = jdbc.query(
                "SELECT pipeline_id, title, block_data, created_at FROM Pipelines WHERE pipeline_id = ? AND user_id = ?",
                (rs, i) -> new Row(rs.getInt("pipeline_id"), rs.getString("title"), rs.getString("block_data"), rs.getTimestamp("created_at")),
                id, userId);
        if (rows.isEmpty()) throw notFound(); // 남의 파이프라인도 같은 404 (존재 여부를 알려주지 않는다)
        return rows.get(0);
    }

    /** 저장된 행 → 응답. 툴 이름·카테고리·로고는 한 번의 쿼리로 합친다(삭제된 툴은 null) */
    private List<Map<String, Object>> toViews(List<Row> rows) {
        Map<Integer, List<StoredBlock>> parsed = new HashMap<>();
        Set<Integer> ids = new HashSet<>();
        for (Row r : rows) {
            List<StoredBlock> blocks = fromJson(r.blockData());
            parsed.put(r.id(), blocks);
            blocks.forEach(b -> ids.add(b.modelId()));
        }
        Map<Integer, Map<String, Object>> models = new HashMap<>();
        if (!ids.isEmpty()) {
            jdbc.query("SELECT model_id, name, category, logo_url FROM AI_Models WHERE model_id IN (" + placeholders(ids.size()) + ")",
                    rs -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("modelName", rs.getString("name"));
                        m.put("category", rs.getString("category"));
                        m.put("logoUrl", rs.getString("logo_url"));
                        models.put(rs.getInt("model_id"), m);
                    }, ids.toArray());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Row r : rows) {
            List<Map<String, Object>> blocks = new ArrayList<>();
            for (StoredBlock b : parsed.get(r.id())) {
                Map<String, Object> m = models.getOrDefault(b.modelId(), Map.of());
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("modelId", b.modelId());
                view.put("order", b.order());
                view.put("purpose", b.purpose());
                view.put("modelName", m.get("modelName"));
                view.put("logoUrl", m.get("logoUrl"));
                view.put("category", m.get("category"));
                blocks.add(view);
            }
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("pipelineId", r.id());
            v.put("title", r.title());
            v.put("createdAt", r.createdAt() == null ? null : r.createdAt().toLocalDateTime().withNano(0).toString());
            v.put("blocks", blocks);
            out.add(v);
        }
        return out;
    }

    private void requireModelsExist(List<StoredBlock> blocks) {
        Set<Integer> ids = new HashSet<>();
        blocks.forEach(b -> ids.add(b.modelId()));
        Set<Integer> found = new HashSet<>(jdbc.queryForList(
                "SELECT model_id FROM AI_Models WHERE model_id IN (" + placeholders(ids.size()) + ")", Integer.class, ids.toArray()));
        List<Integer> missing = ids.stream().filter(id -> !found.contains(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw ApiException.badRequest("MODEL_NOT_FOUND", "존재하지 않는 툴입니다: " + missing);
        }
    }

    static String normalizeTitle(String title) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "제목을 입력해 주세요.");
        if (t.length() > TITLE_MAX) throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "제목은 " + TITLE_MAX + "자 이하여야 합니다.");
        return t;
    }

    /** 순서는 배열 순서로 1부터 다시 매긴다 */
    static List<StoredBlock> normalizeBlocks(List<BlockIn> input) {
        if (input == null || input.isEmpty()) throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "블록은 1개 이상이어야 합니다.");
        if (input.size() > BLOCKS_MAX) throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "블록은 최대 " + BLOCKS_MAX + "개까지 추가할 수 있습니다.");
        List<StoredBlock> out = new ArrayList<>();
        for (BlockIn b : input) {
            if (b == null || b.modelId() == null) throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "각 블록에는 modelId 가 필요합니다.");
            String purpose = b.purpose() == null || b.purpose().isBlank() ? null : b.purpose().trim();
            if (purpose != null && purpose.length() > PURPOSE_MAX) {
                throw ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "purpose 는 " + PURPOSE_MAX + "자 이하여야 합니다.");
            }
            out.add(new StoredBlock(b.modelId(), out.size() + 1, purpose));
        }
        return out;
    }

    private static ApiException notFound() {
        return ApiException.notFound("PIPELINE_NOT_FOUND", "파이프라인을 찾을 수 없습니다.");
    }

    private static String placeholders(int n) {
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String toJson(List<StoredBlock> blocks) {
        try {
            return mapper.writeValueAsString(blocks);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<StoredBlock> fromJson(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<StoredBlock>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 block_data 를 읽을 수 없습니다.", e);
        }
    }
}
