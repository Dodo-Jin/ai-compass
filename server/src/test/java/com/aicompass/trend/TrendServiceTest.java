package com.aicompass.trend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aicompass.common.ApiException;
import com.aicompass.common.GlobalExceptionHandler;
import com.aicompass.trend.TrendCalculator.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TrendServiceTest {

    TrendService service = new TrendService(org.mockito.Mockito.mock(JdbcTemplate.class), new ObjectMapper());

    @Test
    void readsModelIdsFromPipelineBlocks() {
        // B 의 PipelineService 가 저장하는 형식
        assertEquals(Set.of(3, 1), service.modelIdsIn("[{\"modelId\":3,\"order\":1,\"purpose\":\"요약\"},{\"modelId\":1,\"order\":2},{\"modelId\":3,\"order\":3}]"));
        assertEquals(Set.of(), service.modelIdsIn(null));
        assertEquals(Set.of(), service.modelIdsIn("not json"));
        assertEquals(Set.of(), service.modelIdsIn("{\"modelId\":3}"));
        assertEquals(Set.of(2), service.modelIdsIn("[{\"modelId\":\"x\"},{\"modelId\":2},{}]"));
    }

    @Test
    void categoriesWithMostToolsFirst() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 1, 0, 0);
        List<Model> models = List.of(
                new Model(1, "A", "이미지 생성", null, null, 0, t),
                new Model(2, "B", "텍스트/LLM", null, null, 0, t),
                new Model(3, "C", "텍스트/LLM", null, null, 0, t),
                new Model(4, "D", "", null, null, 0, t));
        assertEquals(List.of("텍스트/LLM", "이미지 생성"), TrendService.categories(models));
    }

    @Test
    void unknownPeriodIs400() {
        ApiException e = assertThrows(ApiException.class, () -> service.rankings("daily", null, 1, 10, null));
        assertEquals(400, e.getStatus().value());
        assertEquals("INVALID_REQUEST", e.getCode());
    }

    @Test
    void controllerNeedsNoLoginAndUsesCommonFormat() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TrendController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        mvc.perform(get("/api/v1/trends/rankings").param("period", "daily"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}
