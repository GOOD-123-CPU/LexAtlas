package com.lexatlas.service.rag;

import com.lexatlas.entity.LawMessage;
import com.lexatlas.mapper.LawMessageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("RagPipeline message persistence")
class RagPipelineMessageTest {

    private RagPipeline pipelineWith(LawMessageMapper messageMapper) {
        return new RagPipeline(
                null, null, null, null, null, null, null,
                null, messageMapper, null, null, null
        );
    }

    @Test
    @DisplayName("fallback assistant message should persist isFallback=1")
    void shouldPersistFallbackFlag() {
        LawMessageMapper mapper = mock(LawMessageMapper.class);
        RagPipeline pipeline = pipelineWith(mapper);

        LawMessage message = pipeline.saveMessage(
                42L, "assistant", "fallback answer", "[]", "{}", true
        );

        assertEquals(1, message.getIsFallback());
        verify(mapper).insert(message);
    }

    @Test
    @DisplayName("normal message should persist isFallback=0")
    void shouldPersistNormalFlag() {
        LawMessageMapper mapper = mock(LawMessageMapper.class);
        RagPipeline pipeline = pipelineWith(mapper);

        LawMessage message = pipeline.saveMessage(
                42L, "assistant", "grounded answer", "[]", "{}", false
        );

        assertEquals(0, message.getIsFallback());
        verify(mapper).insert(message);
    }
}
