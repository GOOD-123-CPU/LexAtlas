package com.lexatlas.service.rag;

import com.lexatlas.config.AiConfigHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RRF 融合排序单元测试
 * 验证：双路融合、去重合并、来源标记、TopN 截断、排序正确性
 */
@DisplayName("RRF 融合排序")
class RRFFusionTest {

    private AiConfigHolder aiConfigHolder;

    private RRFFusion fusion;

    @BeforeEach
    void setUp() {
        aiConfigHolder = new AiConfigHolder();
        aiConfigHolder.updateBatch(Map.of("rag.rrf_k_constant", "60"));
        fusion = new RRFFusion(aiConfigHolder);
    }

    private RetrievedChunk chunk(String content, RetrievedChunk.Source source) {
        RetrievedChunk c = new RetrievedChunk();
        c.setContent(content);
        c.setSource(source);
        return c;
    }

    @Test
    @DisplayName("两路都命中的文档应获得更高 RRF 分数并标记为 HYBRID 来源")
    void fusedDocShouldRankHigherAndMarkedHybrid() {
        // 文档 A 同时出现在两路；文档 B 只在向量路
        String docA = "第四十八条 用人单位违反本法规定解除或者终止劳动合同的，应当依照本法第八十七条规定支付赔偿金。";
        String docB = "第八十七条 用人单位违反本法规定解除或者终止劳动合同的，应当依照本法第四十七条规定的经济补偿标准的二倍向劳动者支付赔偿金。";

        List<RetrievedChunk> vectorResults = List.of(chunk(docA, RetrievedChunk.Source.VECTOR),
                chunk(docB, RetrievedChunk.Source.VECTOR));
        List<RetrievedChunk> bm25Results = List.of(chunk(docA, RetrievedChunk.Source.BM25));

        List<RetrievedChunk> fused = fusion.fuse(vectorResults, bm25Results, 10);

        assertEquals(2, fused.size());
        // 双路命中应排名第一
        assertEquals(docA, fused.get(0).getContent());
        assertEquals(RetrievedChunk.Source.HYBRID, fused.get(0).getSource());
        // RRF 分数 = 1/(60+1) + 1/(60+1) > 1/(60+2)
        assertTrue(fused.get(0).getScore() > fused.get(1).getScore());
        // RRF 排名从 1 开始
        assertEquals(1, fused.get(0).getRrfRank());
        assertEquals(2, fused.get(1).getRrfRank());
    }

    @Test
    @DisplayName("topN 截断：融合结果不应超过 topN")
    void shouldTruncateToTopN() {
        List<RetrievedChunk> vectorResults = List.of(
                chunk("条文一的内容用于测试切片排序场景之一", RetrievedChunk.Source.VECTOR),
                chunk("条文二的内容用于测试切片排序场景之二", RetrievedChunk.Source.VECTOR),
                chunk("条文三的内容用于测试切片排序场景之三", RetrievedChunk.Source.VECTOR));
        List<RetrievedChunk> bm25Results = List.of();

        List<RetrievedChunk> fused = fusion.fuse(vectorResults, bm25Results, 2);

        assertEquals(2, fused.size());
    }

    @Test
    @DisplayName("两路输入均为空时应返回空列表而不抛异常")
    void shouldReturnEmptyOnBothEmpty() {
        // 该用例不触发 AiConfigHolder 调用（空输入直接短路），无需 stub
        List<RetrievedChunk> fused = fusion.fuse(List.of(), List.of(), 5);

        assertTrue(fused.isEmpty());
    }

    @Test
    @DisplayName("排名越靠前 RRF 分数应越高（单调递减）")
    void rrfScoreShouldDecreaseWithRank() {
        List<RetrievedChunk> results = List.of(
                chunk("排名第一的测试文档内容", RetrievedChunk.Source.VECTOR),
                chunk("排名第二的测试文档内容", RetrievedChunk.Source.VECTOR),
                chunk("排名第三的测试文档内容", RetrievedChunk.Source.VECTOR));

        List<RetrievedChunk> fused = fusion.fuse(results, List.of(), 10);

        for (int i = 1; i < fused.size(); i++) {
            assertTrue(fused.get(i - 1).getScore() > fused.get(i).getScore(),
                    "rank " + i + " 分数应大于 rank " + (i + 1));
        }
    }
}
