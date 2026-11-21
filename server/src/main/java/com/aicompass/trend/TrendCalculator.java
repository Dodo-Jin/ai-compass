package com.aicompass.trend;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 트렌드 점수·순위 계산 (담당 C). DB 를 모르는 순수 계산이라 단위 테스트로 확인한다.
 *
 * <p>점수(0~100) = 조회수 40% + 즐겨찾기 수 40% + 파이프라인 사용 수 20%.
 * 각 값은 전체 툴 중 최댓값을 100 으로 놓고 비율로 바꾼 뒤 더한다.
 *
 * <p>기간
 * <ul>
 *   <li>realtime: 지금까지 전체. 순위 변동은 하루 전 시점과 비교</li>
 *   <li>weekly: 최근 7일 동안 생긴 즐겨찾기·파이프라인. 변동은 그 전 7일과 비교</li>
 *   <li>monthly: 최근 30일. 변동은 그 전 30일과 비교</li>
 * </ul>
 * 조회수(AI_Models.search_count)는 날짜별 기록이 없어 모든 기간에 누적값을 쓴다.
 */
public final class TrendCalculator {

    static final double W_SEARCH = 0.4;
    static final double W_FAVORITE = 0.4;
    static final double W_PIPELINE = 0.2;
    /** 처음 시드로 한꺼번에 넣은 툴은 NEW 로 보지 않는다. 그 뒤 7일 안에 추가된 툴만 NEW */
    static final int NEW_DAYS = 7;

    public enum Period {
        REALTIME("realtime"), WEEKLY("weekly"), MONTHLY("monthly");

        final String value;

        Period(String value) {
            this.value = value;
        }

        /** 알 수 없는 값이면 null */
        public static Period of(String value) {
            for (Period p : values()) {
                if (p.value.equals(value)) {
                    return p;
                }
            }
            return null;
        }

        /** [현재 구간 시작, 끝], [이전 구간 시작, 끝]. 시작이 null 이면 처음부터 */
        LocalDateTime[] windows(LocalDateTime now) {
            return switch (this) {
                case REALTIME -> new LocalDateTime[] {null, now, null, now.minusDays(1)};
                case WEEKLY -> new LocalDateTime[] {now.minusDays(7), now, now.minusDays(14), now.minusDays(7)};
                case MONTHLY -> new LocalDateTime[] {now.minusDays(30), now, now.minusDays(60), now.minusDays(30)};
            };
        }
    }

    public record Model(int modelId, String name, String category, String tags, String logoUrl, int searchCount, LocalDateTime createdAt) {}

    public record Favorite(int modelId, int userId, LocalDateTime createdAt) {}

    /** 파이프라인 하나에 들어 있는 툴 번호들 (block_data 에서 꺼낸 것) */
    public record Pipeline(Set<Integer> modelIds, LocalDateTime createdAt) {}

    public record Entry(Model model, int rank, int rankChange, boolean isNew, int trendScore,
                        int favoriteCount, int pipelineCount, int pipelineRate, boolean isFavorited) {}

    private record Counts(Map<Integer, Integer> favorites, Map<Integer, Integer> pipelines, int pipelineTotal) {}

    private TrendCalculator() {}

    /**
     * 순위표. category 가 있으면 그 카테고리 안에서 순위를 매긴다 (점수는 전체 기준).
     *
     * @param userId 로그인한 사용자 (isFavorited 표시용), 비로그인은 null
     */
    public static List<Entry> rank(List<Model> models, List<Favorite> favorites, List<Pipeline> pipelines,
                                   Period period, String category, LocalDateTime now, Integer userId) {
        LocalDateTime[] w = period.windows(now);
        Counts current = count(favorites, pipelines, w[0], w[1]);
        Counts previous = count(favorites, pipelines, w[2], w[3]);

        List<Model> scope = models.stream()
                .filter(m -> category == null || category.isBlank() || category.equals(m.category()))
                .toList();

        Map<Integer, Integer> scoreNow = scores(models, current);
        Map<Integer, Integer> scoreBefore = scores(models, previous);
        List<Model> orderedNow = order(scope, scoreNow, current);
        Map<Integer, Integer> rankBefore = new HashMap<>();
        List<Model> orderedBefore = order(scope, scoreBefore, previous);
        for (int i = 0; i < orderedBefore.size(); i++) {
            rankBefore.put(orderedBefore.get(i).modelId(), i + 1);
        }

        Set<Integer> mine = userId == null ? Set.of()
                : Set.copyOf(favorites.stream().filter(f -> f.userId() == userId).map(Favorite::modelId).toList());
        LocalDateTime firstLoad = models.stream().map(Model::createdAt).filter(t -> t != null)
                .min(Comparator.naturalOrder()).orElse(null);

        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < orderedNow.size(); i++) {
            Model m = orderedNow.get(i);
            int rank = i + 1;
            boolean isNew = isNew(m, firstLoad, now);
            int change = isNew ? 0 : rankBefore.getOrDefault(m.modelId(), rank) - rank;
            int pipelineCount = current.pipelines().getOrDefault(m.modelId(), 0);
            int rate = current.pipelineTotal() == 0 ? 0 : (int) Math.round(100.0 * pipelineCount / current.pipelineTotal());
            out.add(new Entry(m, rank, change, isNew, scoreNow.get(m.modelId()),
                    current.favorites().getOrDefault(m.modelId(), 0), pipelineCount, rate, mine.contains(m.modelId())));
        }
        return out;
    }

    static boolean isNew(Model m, LocalDateTime firstLoad, LocalDateTime now) {
        if (m.createdAt() == null || firstLoad == null) {
            return false;
        }
        boolean addedLater = m.createdAt().isAfter(firstLoad.plusHours(1));
        return addedLater && m.createdAt().isAfter(now.minusDays(NEW_DAYS));
    }

    private static boolean inWindow(LocalDateTime t, LocalDateTime from, LocalDateTime to) {
        if (t == null) {
            return from == null; // 날짜가 없는 기록은 "전체" 구간에만 넣는다
        }
        return (from == null || !t.isBefore(from)) && !t.isAfter(to);
    }

    private static Counts count(List<Favorite> favorites, List<Pipeline> pipelines, LocalDateTime from, LocalDateTime to) {
        Map<Integer, Integer> fav = new HashMap<>();
        for (Favorite f : favorites) {
            if (inWindow(f.createdAt(), from, to)) {
                fav.merge(f.modelId(), 1, Integer::sum);
            }
        }
        Map<Integer, Integer> pipe = new HashMap<>();
        int total = 0;
        for (Pipeline p : pipelines) {
            if (inWindow(p.createdAt(), from, to)) {
                total++;
                for (int id : p.modelIds()) {
                    pipe.merge(id, 1, Integer::sum);
                }
            }
        }
        return new Counts(fav, pipe, total);
    }

    /** 모델별 점수 0~100 */
    private static Map<Integer, Integer> scores(List<Model> models, Counts c) {
        int maxSearch = models.stream().mapToInt(Model::searchCount).max().orElse(0);
        int maxFav = c.favorites().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int maxPipe = c.pipelines().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        Map<Integer, Integer> out = new HashMap<>();
        for (Model m : models) {
            double s = ratio(m.searchCount(), maxSearch);
            double f = ratio(c.favorites().getOrDefault(m.modelId(), 0), maxFav);
            double p = ratio(c.pipelines().getOrDefault(m.modelId(), 0), maxPipe);
            out.put(m.modelId(), (int) Math.round(100 * (W_SEARCH * s + W_FAVORITE * f + W_PIPELINE * p)));
        }
        return out;
    }

    private static double ratio(int value, int max) {
        return max <= 0 ? 0 : Math.max(0, value) / (double) max;
    }

    /** 점수 높은 순, 같으면 즐겨찾기 많은 순, 조회수 많은 순, 이름 순 */
    private static List<Model> order(List<Model> scope, Map<Integer, Integer> score, Counts c) {
        return scope.stream()
                .sorted(Comparator.<Model>comparingInt(m -> -score.get(m.modelId()))
                        .thenComparingInt(m -> -c.favorites().getOrDefault(m.modelId(), 0))
                        .thenComparingInt(m -> -m.searchCount())
                        .thenComparing(Model::name, Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}
