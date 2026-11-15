package com.aicompass.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aicompass.pipeline.ChatPipelineService.ChatException;
import com.aicompass.pipeline.ChatPipelineService.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChatPipelineServiceTest {

    static final Map<Integer, ChatPipelineService.Model> MODELS = Map.of(
            1, new ChatPipelineService.Model(1, "ChatGPT", "텍스트", null),
            4, new ChatPipelineService.Model(4, "Midjourney", "이미지", null));
    static final ObjectMapper OM = new ObjectMapper();

    @Test
    void validatesAiOutput() {
        // 코드펜스 + DB 에 없는 id(99) + 중복(1)은 버리고, 이름·카테고리는 DB 값, 순서는 1부터
        String ai = "```json\n{\"title\":\"블로그\",\"reply\":\"추천드려요\",\"blocks\":["
                + "{\"modelId\":1,\"purpose\":\"초안\"},{\"modelId\":99,\"purpose\":\"x\"},{\"modelId\":1,\"purpose\":\"중복\"},{\"modelId\":4}]}\n```";
        Result r = ChatPipelineService.toResult(ai, MODELS, OM);
        assertEquals("블로그", r.title());
        assertEquals(2, r.blocks().size());
        assertEquals("ChatGPT", r.blocks().get(0).modelName());
        assertEquals(2, r.blocks().get(1).order());
        assertNull(r.blocks().get(1).purpose());
    }

    @Test
    void emptyBlocksAndErrors() {
        Result r = ChatPipelineService.toResult("{\"reply\":\"더 알려주세요\",\"blocks\":[]}", MODELS, OM);
        assertEquals(0, r.blocks().size());
        assertEquals("나만의 AI 파이프라인", r.title());
        assertEquals(502, assertThrows(ChatException.class, () -> ChatPipelineService.toResult("죄송합니다", MODELS, OM)).status);
        assertEquals(400, assertThrows(ChatException.class, () -> new ChatPipelineService(null, OM, "http://x", "k", "m").generate(" ")).status);
    }
}
