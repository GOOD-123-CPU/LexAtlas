package com.lexatlas.service.rag;

import com.lexatlas.config.AiConfigHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SafetyGuard 单元测试（紧急关键词检测 / 置信度兜底判断）
 */
@DisplayName("SafetyGuard 安全兜底")
class SafetyGuardTest {

    private SafetyGuard newGuard(float threshold) {
        AiConfigHolder aiConfigHolder = new AiConfigHolder();
        aiConfigHolder.updateBatch(Map.of("safety.confidence_threshold", String.valueOf(threshold)));
        return new SafetyGuard(null, aiConfigHolder);
    }

    private RetrievedChunk chunkWithScore(float score) {
        RetrievedChunk c = new RetrievedChunk();
        c.setContent("测试内容：劳动合同解除的相关法律规定条款文本，用于置信度评估测试。");
        c.setRerankScore(score);
        return c;
    }

    // ==================== 紧急关键词检测 ====================

    @Test
    @DisplayName("家暴关键词应触发紧急警报")
    void domesticViolenceShouldTriggerEmergency() {
        assertTrue(newGuard(0.3f).isEmergency("我长期遭受家庭暴力，该怎么办"));
    }

    @Test
    @DisplayName("非法拘禁关键词应触发紧急警报")
    void illegalDetentionShouldTriggerEmergency() {
        assertTrue(newGuard(0.3f).isEmergency("被人非法拘禁了怎么报警"));
    }

    @Test
    @DisplayName("普通劳动纠纷不应触发紧急警报")
    void normalLaborDisputeShouldNotTrigger() {
        assertFalse(newGuard(0.3f).isEmergency("公司拖欠工资应该如何申请劳动仲裁"));
    }

    @Test
    @DisplayName("null 输入不应触发紧急警报且不抛异常")
    void nullQueryShouldNotTrigger() {
        assertDoesNotThrow(() -> newGuard(0.3f).isEmergency(null));
        assertFalse(newGuard(0.3f).isEmergency(null));
    }

    // ==================== 置信度兜底 ====================

    @Test
    @DisplayName("最高重排分低于阈值时应触发兜底")
    void lowScoreShouldTriggerFallback() {
        SafetyGuard guard = newGuard(0.3f);
        List<RetrievedChunk> chunks = List.of(chunkWithScore(0.1f), chunkWithScore(0.2f));
        assertTrue(guard.needsFallback(chunks));
    }

    @Test
    @DisplayName("最高重排分高于阈值时不应触发兜底")
    void highScoreShouldNotTriggerFallback() {
        SafetyGuard guard = newGuard(0.3f);
        List<RetrievedChunk> chunks = List.of(chunkWithScore(0.1f), chunkWithScore(0.8f));
        assertFalse(guard.needsFallback(chunks));
    }

    @Test
    @DisplayName("空检索结果应触发兜底")
    void emptyResultsShouldTriggerFallback() {
        assertTrue(newGuard(0.3f).needsFallback(List.of()));
    }

    @Test
    @DisplayName("重排分为 0 时应回退使用原始召回分")
    void shouldFallbackToRawScoreWhenRerankScoreMissing() {
        SafetyGuard guard = newGuard(0.3f);
        RetrievedChunk c = new RetrievedChunk();
        c.setContent("测试内容：用于验证原始分数回退逻辑的条款文本，长度满足要求。");
        c.setRerankScore(0f);
        c.setScore(0.75f);

        assertFalse(guard.needsFallback(List.of(c)));
    }

    @Test
    @DisplayName("置信度评估应在 0-1 区间")
    void confidenceShouldBeBounded() {
        SafetyGuard guard = newGuard(0.3f);
        List<RetrievedChunk> chunks = List.of(chunkWithScore(5.0f));
        float conf = guard.evaluateConfidence(chunks);
        assertTrue(conf >= 0f && conf <= 1f, "置信度 " + conf + " 应在 [0,1]");
    }
}
