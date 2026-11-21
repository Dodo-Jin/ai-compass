package com.aicompass.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.aicompass.common.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 로그인 필수(401)와 "본인 것만"(남의 것은 404) 규칙. DB 없이 가짜 객체로 확인한다. */
class PipelineAccessTest {

    PipelineService pipelines = mock(PipelineService.class);
    ChatPipelineService chat = mock(ChatPipelineService.class);
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PipelineController(mock(SurveyRecommendService.class), chat, pipelines))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ------------------------------------------------------------------ 로그인 필수

    @Test
    void loginRequired() throws Exception {
        mvc.perform(get("/api/v1/pipelines")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));
        mvc.perform(get("/api/v1/pipelines/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/pipelines/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/pipelines").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\",\"blocks\":[]}"))
                .andExpect(status().isUnauthorized());
        // 챗봇도 로그인 필수: AI 를 부르기 전에 막혀야 한다
        mvc.perform(post("/api/v1/pipelines/generate").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
        verify(chat, never()).generate(anyString());
    }

    @Test
    void loggedInUserIdIsPassedToService() throws Exception {
        when(pipelines.list(7, null, "latest", 1, 12)).thenReturn(Map.of("items", List.of()));
        mvc.perform(get("/api/v1/pipelines").sessionAttr("userId", 7)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(pipelines).list(7, null, "latest", 1, 12);
    }

    // ------------------------------------------------------------------ 본인 것만

    @Test
    void otherUsersPipelineIsNotFound() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PipelineService service = new PipelineService(jdbc, new ObjectMapper());

        // 삭제: pipeline_id 와 user_id 를 함께 조건으로 걸고, 지운 행이 0개면 404
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
        ApiException del = assertThrows(ApiException.class, () -> service.delete(7, 99));
        assertEquals(404, del.getStatus().value());
        assertEquals("PIPELINE_NOT_FOUND", del.getCode());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(sql.capture(), args.capture());
        assertTrue(sql.getValue().contains("pipeline_id = ? AND user_id = ?"));
        assertEquals(List.of(99, 7), List.of(args.getValue()));

        // 조회: 내 것이 아니면 행이 안 나오고, 그러면 404(남의 것이 있다는 사실도 알려주지 않는다)
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        assertEquals(404, assertThrows(ApiException.class, () -> service.get(7, 99)).getStatus().value());
        ArgumentCaptor<String> getSql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(getSql.capture(), any(RowMapper.class), any(Object[].class));
        assertTrue(getSql.getValue().contains("pipeline_id = ? AND user_id = ?"));
    }
}
