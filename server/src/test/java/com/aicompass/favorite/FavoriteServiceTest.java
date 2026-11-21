package com.aicompass.favorite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aicompass.common.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class FavoriteServiceTest {

    @Test
    void parseModelIds() {
        assertEquals(List.of(3, 1), FavoriteService.parseModelIds(" 3 , 1 "));  // 요청 순서 유지
        assertEquals(List.of(1, 2, 3, 4), FavoriteService.parseModelIds("1,2,3,4"));
        assertEquals(List.of(1, 2), FavoriteService.parseModelIds("1,2,2,"));   // 중복·빈칸 무시

        for (String bad : new String[] {null, "", " ", "1", "1,1", "1,2,3,4,5", "a,b", "1,-2", "0,1"}) {
            ApiException e = assertThrows(ApiException.class, () -> FavoriteService.parseModelIds(bad), String.valueOf(bad));
            assertEquals(400, e.getStatus().value());
            assertEquals("INVALID_REQUEST", e.getCode());
        }
    }

    @Test
    void splitList() {
        assertEquals(List.of(), FavoriteService.splitList(null));
        assertEquals(List.of(), FavoriteService.splitList("  "));
        assertEquals(List.of("텍스트", "이미지", "음성"), FavoriteService.splitList("텍스트, 이미지,,음성 "));
    }

    @Test
    void addValidatesBeforeTouchingDb() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        FavoriteService service = new FavoriteService(jdbc);

        assertEquals(400, assertThrows(ApiException.class, () -> service.add(7, null)).getStatus().value());
        assertEquals(400, assertThrows(ApiException.class, () -> service.add(7, 0)).getStatus().value());
        verify(jdbc, never()).update(anyString(), eq(7), eq(0));
    }

    @Test
    void addMissingModelIs404() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doReturn(0).when(jdbc).queryForObject(startsWith("SELECT COUNT(*) FROM AI_Models"), eq(Integer.class), eq(999));

        ApiException e = assertThrows(ApiException.class, () -> new FavoriteService(jdbc).add(7, 999));
        assertEquals(404, e.getStatus().value());
        assertEquals("MODEL_NOT_FOUND", e.getCode());
    }

    @Test
    void addTwiceIsNotAnError() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doReturn(1).when(jdbc).queryForObject(startsWith("SELECT COUNT(*) FROM AI_Models"), eq(Integer.class), eq(3));
        doReturn(5L).when(jdbc).queryForObject(startsWith("SELECT COUNT(*) FROM Favorites"), eq(Long.class), eq(3));
        FavoriteService service = new FavoriteService(jdbc);

        when(jdbc.update(startsWith("INSERT INTO Favorites"), eq(7), eq(3))).thenReturn(1);
        FavoriteService.AddResult first = service.add(7, 3);
        assertTrue(first.created());
        assertEquals(true, first.body().get("isFavorited"));
        assertEquals(5L, first.body().get("favoriteCount"));

        // UQ_FAVORITES(user_id, model_id) 위반 = 이미 즐겨찾기함
        when(jdbc.update(startsWith("INSERT INTO Favorites"), eq(7), eq(3))).thenThrow(new DuplicateKeyException("dup"));
        FavoriteService.AddResult second = service.add(7, 3);
        assertFalse(second.created());
        assertEquals(true, second.body().get("isFavorited"));
    }
}
