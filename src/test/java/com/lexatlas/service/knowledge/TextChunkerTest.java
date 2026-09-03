package com.lexatlas.service.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文本切片器单元测试
 * 验证：段落合并、超长切分、内容类型检测、过短过滤
 */
@DisplayName("TextChunker 文本切片")
class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    @DisplayName("短段落应被合并为一个切片")
    void shortParagraphsShouldBeMerged() {
        // 3 个短段落，各自约 50 字，合计 < 400 字目标值
        String text = String.join("\n\n",
                "劳动合同是劳动者与用人单位确立劳动关系、明确双方权利和义务的协议，建立劳动关系应当订立劳动合同。",
                "劳动合同依法订立即具有法律约束力，当事人必须履行劳动合同规定的义务，任何一方不得擅自变更或解除。",
                "订立和变更劳动合同，应当遵循平等自愿、协商一致的原则，不得违反法律、行政法规的规定。");

        List<TextChunker.TextChunk> chunks = chunker.chunk(text, 1L, "labor");

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getContent().contains("劳动合同"));
    }

    @Test
    @DisplayName("超长段落应按句子切分为多个切片，且每个不超过上限")
    void longParagraphShouldBeSplitBySentence() {
        // 构造一段 1500+ 字的长文本（每句约 60 字，共 30 句）
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("这是用于测试超长文本切分功能的第").append(i).append("句话，")
              .append("内容涉及劳动合同解除与经济补偿的相关法律条款规定，用于验证切片器按句子边界正确切分并且每个切片不超过最大长度限制。");
        }

        List<TextChunker.TextChunk> chunks = chunker.chunk(sb.toString(), 1L, "labor");

        assertTrue(chunks.size() > 1, "超长文本应产生多个切片");
        for (TextChunker.TextChunk c : chunks) {
            assertTrue(c.getContent().length() <= 620,
                    "切片长度 " + c.getContent().length() + " 不应显著超过 MAX_CHUNK_SIZE(600)");
        }
    }

    @Test
    @DisplayName("少于 20 字的独立长段切片应被过滤（先合并后过滤语义）")
    void tinyChunksShouldBeFiltered() {
        // 仅一个短段落：合并后仍不足 20 字，应被过滤为空
        List<TextChunker.TextChunk> chunks = chunker.chunk("第一条。", 1L, "labor");
        assertTrue(chunks.isEmpty(), "合并后仍不足 20 字的内容应被过滤");
    }

    @Test
    @DisplayName("元数据应正确填充：knowledgeBaseId / category / chunkIndex")
    void metadataShouldBePopulated() {
        String text = "这是一段用于验证元数据填充逻辑的测试文本内容，长度超过二十个字以通过过滤阈值。";

        List<TextChunker.TextChunk> chunks = chunker.chunk(text, 42L, "civil");

        assertEquals(1, chunks.size());
        assertEquals(42L, chunks.get(0).getKnowledgeBaseId());
        assertEquals("civil", chunks.get(0).getCategory());
        assertTrue(chunks.get(0).getChunkIndex() >= 0);
    }

    @Test
    @DisplayName("空白规范化：CRLF 与连续空行应被压缩")
    void whitespaceShouldBeNormalized() {
        String text = "第一段内容：本条款用于测试换行符规范化处理。\r\n\r\n\r\n\r\n第二段内容：连续空行应被压缩为单个空行分隔符。";

        List<TextChunker.TextChunk> chunks = chunker.chunk(text, 1L, "labor");

        // CRLF 规范化后两段各自有效，应合并或至少不产生乱码切片
        assertFalse(chunks.isEmpty());
        String all = String.join("\n", chunks.stream().map(TextChunker.TextChunk::getContent).toList());
        assertFalse(all.contains("\r"), "不应残留 CR 字符");
    }
}
