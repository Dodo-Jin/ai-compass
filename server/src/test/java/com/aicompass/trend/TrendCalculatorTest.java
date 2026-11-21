package com.aicompass.trend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aicompass.trend.TrendCalculator.Entry;
import com.aicompass.trend.TrendCalculator.Favorite;
import com.aicompass.trend.TrendCalculator.Model;
import com.aicompass.trend.TrendCalculator.Period;
import com.aicompass.trend.TrendCalculator.Pipeline;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 트렌드 점수·순위 규칙. DB 없이 계산만 확인한다. */
class TrendCalculatorTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 21, 20, 0);
    static final LocalDateTime SEEDED = NOW.minusDays(30);   // 시드로 처음 넣은 시각
    static final LocalDateTime RECENT = NOW.minusHours(3);   // 하루 안
    static final LocalDateTime OLD = NOW.minusDays(10);      // 하루 전, 7일 전보다 이전

    static Model model(int id, String name, String category, int search) {
        return new Model(id, name, category, "a, b", null, search, SEEDED);
    }

    // A: 조회 100 / B: 조회 50 / C: 조회 0
    static final List<Model> MODELS = List.of(
            model(1, "A", "텍스트/LLM", 100),
            model(2, "B", "텍스트/LLM", 50),
            model(3, "C", "이미지 생성", 0));

    // 즐겨찾기: A 1개(오래됨), B 3개(최근)
    static final List<Favorite> FAVS = List.of(
            new Favorite(1, 9, OLD),
            new Favorite(2, 7, RECENT), new Favorite(2, 8, RECENT), new Favorite(2, 9, RECENT));

    // 파이프라인 2개 모두 C 를 씀(오래됨), 하나는 A 도 씀
    static final List<Pipeline> PIPES = List.of(
            new Pipeline(Set.of(3), OLD),
            new Pipeline(Set.of(3, 1), OLD));

    static Entry find(List<Entry> list, int id) {
        return list.stream().filter(e -> e.model().modelId() == id).findFirst().orElseThrow();
    }

    @Test
    void scoreIsWeightedRatioOfMax() {
        List<Entry> r = TrendCalculator.rank(MODELS, FAVS, PIPES, Period.REALTIME, null, NOW, null);
        // A: 0.4*100/100 + 0.4*1/3 + 0.2*1/2 = 0.6333 → 63
        // B: 0.4*50/100  + 0.4*3/3 + 0          = 0.6    → 60
        // C: 0           + 0       + 0.2*2/2    = 0.2    → 20
        assertEquals(63, find(r, 1).trendScore());
        assertEquals(60, find(r, 2).trendScore());
        assertEquals(20, find(r, 3).trendScore());
        assertEquals(List.of(1, 2, 3), r.stream().map(e -> e.model().modelId()).toList());
        assertEquals(List.of(1, 2, 3), r.stream().map(Entry::rank).toList());

        assertEquals(3, find(r, 2).favoriteCount());
        assertEquals(2, find(r, 3).pipelineCount());
        assertEquals(100, find(r, 3).pipelineRate()); // 파이프라인 2개 중 2개
        assertEquals(50, find(r, 1).pipelineRate());
    }

    @Test
    void realtimeChangeComparesWithOneDayAgo() {
        // 하루 전에는 B 의 즐겨찾기 3개가 없었다 → A 1위, B 2위, C 3위
        // A: 0.4 + 0.4*1/1 + 0.2*1/2 = 0.9, B: 0.2, C: 0.2 (동점 → 조회수 많은 B 먼저)
        List<Entry> r = TrendCalculator.rank(MODELS, FAVS, PIPES, Period.REALTIME, null, NOW, null);
        assertEquals(0, find(r, 1).rankChange()); // 1위 → 1위
        assertEquals(0, find(r, 2).rankChange()); // 2위 → 2위
        assertEquals(0, find(r, 3).rankChange());

        // 최근 즐겨찾기가 더 많아서 B 가 1위로 올라가는 경우
        List<Favorite> more = new java.util.ArrayList<>(FAVS);
        for (int u = 20; u < 26; u++) {
            more.add(new Favorite(2, u, RECENT));
        }
        List<Entry> r2 = TrendCalculator.rank(MODELS, more, PIPES, Period.REALTIME, null, NOW, null);
        assertEquals(1, find(r2, 2).rank());
        assertEquals(1, find(r2, 2).rankChange());   // 2위 → 1위: ▲1
        assertEquals(-1, find(r2, 1).rankChange());  // 1위 → 2위: ▼1
    }

    @Test
    void weeklyCountsOnlyLastSevenDays() {
        List<Entry> r = TrendCalculator.rank(MODELS, FAVS, PIPES, Period.WEEKLY, null, NOW, null);
        assertEquals(0, find(r, 1).favoriteCount());  // A 의 즐겨찾기는 10일 전
        assertEquals(3, find(r, 2).favoriteCount());
        assertEquals(0, find(r, 3).pipelineCount());  // 파이프라인도 10일 전
        assertEquals(0, find(r, 3).pipelineRate());
        assertEquals(2, r.get(0).model().modelId());  // B: 0.2 + 0.4 = 0.6 > A: 0.4
        // 그 전 7일(7~14일 전)에는 A 가 즐겨찾기 1개로 1위였다 → B ▲1
        assertEquals(1, find(r, 2).rankChange());
    }

    @Test
    void categoryRanksWithinCategory() {
        List<Entry> r = TrendCalculator.rank(MODELS, FAVS, PIPES, Period.REALTIME, "이미지 생성", NOW, null);
        assertEquals(1, r.size());
        assertEquals(3, r.get(0).model().modelId());
        assertEquals(1, r.get(0).rank());
        assertEquals(20, r.get(0).trendScore()); // 점수는 전체 기준 그대로
    }

    @Test
    void newOnlyForToolsAddedAfterSeedWithinSevenDays() {
        Model added = new Model(4, "D", "텍스트/LLM", "", null, 0, NOW.minusDays(2));
        Model addedLongAgo = new Model(5, "E", "텍스트/LLM", "", null, 0, NOW.minusDays(9));
        List<Model> models = new java.util.ArrayList<>(MODELS);
        models.add(added);
        models.add(addedLongAgo);

        List<Entry> r = TrendCalculator.rank(models, FAVS, PIPES, Period.REALTIME, null, NOW, null);
        assertTrue(find(r, 4).isNew());
        assertEquals(0, find(r, 4).rankChange());
        assertFalse(find(r, 5).isNew());  // 7일이 지남
        assertFalse(find(r, 1).isNew());  // 시드로 처음 넣은 툴
    }

    @Test
    void favoritedForLoggedInUser() {
        List<Entry> r = TrendCalculator.rank(MODELS, FAVS, PIPES, Period.REALTIME, null, NOW, 7);
        assertTrue(find(r, 2).isFavorited());
        assertFalse(find(r, 1).isFavorited());
        List<Entry> anonymous = TrendCalculator.rank(MODELS, FAVS, PIPES, Period.REALTIME, null, NOW, null);
        assertFalse(find(anonymous, 2).isFavorited());
    }

    @Test
    void emptyDataGivesZeroScoresNotErrors() {
        List<Entry> r = TrendCalculator.rank(MODELS, List.of(), List.of(), Period.MONTHLY, null, NOW, null);
        assertEquals(3, r.size());
        assertEquals(40, find(r, 1).trendScore()); // 조회수만 반영
        assertEquals(0, find(r, 3).trendScore());
        assertTrue(TrendCalculator.rank(List.of(), List.of(), List.of(), Period.REALTIME, null, NOW, null).isEmpty());
    }

    @Test
    void periodParsing() {
        assertEquals(Period.WEEKLY, Period.of("weekly"));
        assertNull(Period.of("daily"));
        assertNull(Period.of(null));
    }
}
