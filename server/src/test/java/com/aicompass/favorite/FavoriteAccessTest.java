package com.aicompass.favorite;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aicompass.common.ApiException;
import com.aicompass.common.ErrorCodes;
import com.aicompass.common.GlobalExceptionHandler;
import com.aicompass.common.SessionKeys;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 로그인 필수(401), 상태 코드(201/200/204), 응답 형식. DB 없이 가짜 서비스로 확인한다. */
class FavoriteAccessTest {

    FavoriteService favorites = mock(FavoriteService.class);
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FavoriteController(favorites))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginRequired() throws Exception {
        mvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));
        mvc.perform(get("/api/v1/favorites/compare").param("modelIds", "1,2")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/favorites/3")).andExpect(status().isUnauthorized());
        // 본문이 없어도 로그인부터 확인한다
        mvc.perform(post("/api/v1/favorites")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/favorites").contentType(MediaType.APPLICATION_JSON).content("{\"modelId\":3}"))
                .andExpect(status().isUnauthorized());

        verify(favorites, never()).add(anyInt(), any());
        verify(favorites, never()).remove(anyInt(), anyInt());
        verify(favorites, never()).list(anyInt(), any(), anyInt(), anyInt());
        verify(favorites, never()).compare(anyInt(), any());
    }

    @Test
    void addReturns201ThenOkWhenAlreadyFavorited() throws Exception {
        Map<String, Object> body = Map.of("modelId", 3, "isFavorited", true, "favoriteCount", 5);
        when(favorites.add(7, 3))
                .thenReturn(new FavoriteService.AddResult(true, body))
                .thenReturn(new FavoriteService.AddResult(false, body));

        mvc.perform(post("/api/v1/favorites").sessionAttr(SessionKeys.USER_ID, 7)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"modelId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isFavorited").value(true))
                .andExpect(jsonPath("$.data.favoriteCount").value(5));
        mvc.perform(post("/api/v1/favorites").sessionAttr(SessionKeys.USER_ID, 7)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"modelId\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    void addErrorsUseCommonFormat() throws Exception {
        when(favorites.add(eq(7), isNull()))
                .thenThrow(ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "modelId 가 필요합니다."));
        when(favorites.add(7, 999)).thenThrow(ApiException.notFound("MODEL_NOT_FOUND", "AI 툴을 찾을 수 없습니다."));

        mvc.perform(post("/api/v1/favorites").sessionAttr(SessionKeys.USER_ID, 7)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        mvc.perform(post("/api/v1/favorites").sessionAttr(SessionKeys.USER_ID, 7)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"modelId\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MODEL_NOT_FOUND"));
    }

    @Test
    void removeReturns204() throws Exception {
        mvc.perform(delete("/api/v1/favorites/3").sessionAttr(SessionKeys.USER_ID, 7))
                .andExpect(status().isNoContent());
        verify(favorites).remove(7, 3);

        // 숫자가 아닌 modelId 는 공용 형식의 400
        mvc.perform(delete("/api/v1/favorites/abc").sessionAttr(SessionKeys.USER_ID, 7))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void listPassesFiltersAndDefaults() throws Exception {
        when(favorites.list(7, null, 1, FavoriteService.SIZE_DEFAULT))
                .thenReturn(Map.of("items", List.of(), "totalElements", 0));
        mvc.perform(get("/api/v1/favorites").sessionAttr(SessionKeys.USER_ID, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        when(favorites.list(7, "챗봇", 2, 10)).thenReturn(Map.of("items", List.of(), "totalElements", 0));
        mvc.perform(get("/api/v1/favorites").sessionAttr(SessionKeys.USER_ID, 7)
                        .param("category", "챗봇").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());
        verify(favorites).list(7, "챗봇", 2, 10);
    }

    @Test
    void compareUsesServiceValidation() throws Exception {
        when(favorites.compare(7, "1,2")).thenReturn(Map.of("items", List.of()));
        mvc.perform(get("/api/v1/favorites/compare").sessionAttr(SessionKeys.USER_ID, 7).param("modelIds", "1,2"))
                .andExpect(status().isOk());

        when(favorites.compare(eq(7), anyString()))
                .thenThrow(ApiException.badRequest(ErrorCodes.INVALID_REQUEST, "비교는 서로 다른 AI 툴 2~4개로 할 수 있습니다."));
        mvc.perform(get("/api/v1/favorites/compare").sessionAttr(SessionKeys.USER_ID, 7).param("modelIds", "1"))
                .andExpect(status().isBadRequest());
    }
}
