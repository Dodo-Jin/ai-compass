package com.aicompass.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aicompass.common.ApiException;
import com.aicompass.pipeline.PipelineService.BlockIn;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineServiceTest {

    @Test
    void titleAndBlockValidation() {
        assertEquals("제목", PipelineService.normalizeTitle("  제목 "));
        assertEquals(400, assertThrows(ApiException.class, () -> PipelineService.normalizeTitle("  ")).getStatus().value());
        assertEquals(400, assertThrows(ApiException.class, () -> PipelineService.normalizeTitle("가".repeat(101))).getStatus().value());

        // 순서는 배열 순서로 1부터, 빈 purpose 는 null
        var blocks = PipelineService.normalizeBlocks(List.of(new BlockIn(7, "초안"), new BlockIn(3, "  ")));
        assertEquals(2, blocks.get(1).order());
        assertEquals(null, blocks.get(1).purpose());

        assertThrows(ApiException.class, () -> PipelineService.normalizeBlocks(List.of()));
        assertThrows(ApiException.class, () -> PipelineService.normalizeBlocks(List.of(new BlockIn(null, "x"))));
        assertThrows(ApiException.class, () -> PipelineService.normalizeBlocks(List.of(new BlockIn(1, "가".repeat(201)))));
        assertThrows(ApiException.class, () -> PipelineService.normalizeBlocks(java.util.Collections.nCopies(21, new BlockIn(1, "x"))));
    }
}
