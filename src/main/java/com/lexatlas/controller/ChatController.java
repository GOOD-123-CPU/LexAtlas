package com.lexatlas.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexatlas.common.result.Result;
import com.lexatlas.entity.LawConversation;
import com.lexatlas.entity.LawMessage;
import com.lexatlas.entity.dto.ChatRequestDTO;
import com.lexatlas.entity.vo.PageVO;
import com.lexatlas.mapper.LawConversationMapper;
import com.lexatlas.mapper.LawMessageMapper;
import com.lexatlas.service.UserService;
import com.lexatlas.service.rag.RagPipeline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 对话接口
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "智能对话", description = "RAG 流式问答、会话历史管理与回答导出")
public class ChatController {

    private final RagPipeline ragPipeline;
    private final UserService userService;
    private final LawConversationMapper conversationMapper;
    private final LawMessageMapper messageMapper;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 发送消息（SSE 流式返回）
     * GET /api/chat/stream?conversationId=xxx&message=xxx
     */
    @Operation(summary = "流式问答", description = "基于 RAG 流水线的法律咨询问答，" +
            "SSE 事件流依次为：rewrite（查询改写）→ retrieval（多路召回）→ rerank（重排序）→ " +
            "start → token（逐 token 生成）→ done（含引用来源与检索日志）")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @Parameter(description = "会话 ID，不传则自动创建新会话") @RequestParam(required = false) Long conversationId,
            @Parameter(description = "用户问题") @RequestParam String message) {

        SseEmitter emitter = new SseEmitter(120_000L); // 120秒超时

        Long userId = userService.getCurrentUserId();
        String legalProfile = userService.getCurrentUser().getLegalProfile();

        executor.execute(() -> {
            ragPipeline.execute(userId, conversationId, message, legalProfile, emitter);
        });

        return emitter;
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<PageVO<LawConversation>> listConversations(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {

        Long userId = userService.getCurrentUserId();
        Page<LawConversation> page = new Page<>(current, size);
        conversationMapper.selectPage(page, new LambdaQueryWrapper<LawConversation>()
                .eq(LawConversation::getUserId, userId)
                .eq(LawConversation::getDeleted, 0)
                .orderByDesc(LawConversation::getLastActive));

        return Result.success(PageVO.of(page));
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/history/{conversationId}")
    public Result<PageVO<LawMessage>> getHistory(
            @PathVariable Long conversationId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "50") Integer size) {

        Long userId = userService.getCurrentUserId();
        // 验证会话属于当前用户
        LawConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            return Result.error("会话不存在");
        }

        Page<LawMessage> page = new Page<>(current, size);
        messageMapper.selectPage(page, new LambdaQueryWrapper<LawMessage>()
                .eq(LawMessage::getConversationId, conversationId)
                .orderByAsc(LawMessage::getCreateTime));

        return Result.success(PageVO.of(page));
    }

    /**
     * 获取检索过程详情（答辩演示专用）
     */
    @GetMapping("/retrieval-log/{messageId}")
    public Result<Object> getRetrievalLog(@PathVariable Long messageId) {
        Long userId = userService.getCurrentUserId();
        LawMessage message = messageMapper.selectById(messageId);
        if (message == null || !isMessageOwnedBy(message, userId)) {
            return Result.error("消息不存在");
        }
        String log = message.getRetrievalLog();
        if (log == null) return Result.success(null);
        return Result.success(com.alibaba.fastjson2.JSON.parse(log));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(@PathVariable Long conversationId) {
        Long userId = userService.getCurrentUserId();
        LawConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            return Result.error("会话不存在");
        }
        conversationMapper.deleteById(conversationId);
        return Result.success();
    }

    /**
     * 提交消息反馈
     */
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(@RequestBody ChatRequestDTO.FeedbackDTO dto) {
        Long userId = userService.getCurrentUserId();
        LawMessage message = messageMapper.selectById(dto.getMessageId());
        if (message == null || !isMessageOwnedBy(message, userId)) {
            return Result.error("消息不存在");
        }
        if (dto.getRating() == null || (dto.getRating() != 1 && dto.getRating() != -1)) {
            return Result.error("反馈值只能是 1 或 -1");
        }
        message.setFeedback(dto.getRating());
        messageMapper.updateById(message);
        return Result.success();
    }

    /**
     * 导出会话为 Markdown 文件
     * GET /api/chat/export/{conversationId}
     */
    @GetMapping("/export/{conversationId}")
    public ResponseEntity<byte[]> exportMarkdown(@PathVariable Long conversationId) {
        Long userId = userService.getCurrentUserId();
        LawConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }

        List<LawMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<LawMessage>()
                        .eq(LawMessage::getConversationId, conversationId)
                        .orderByAsc(LawMessage::getCreateTime));

        String markdown = buildMarkdown(conv, messages);
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);

        String filename = "lexatlas-" + conversationId + ".md";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/markdown; charset=UTF-8"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());

        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    // ==================== 私有方法 ====================

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private boolean isMessageOwnedBy(LawMessage message, Long userId) {
        LawConversation conversation = conversationMapper.selectById(message.getConversationId());
        return conversation != null && conversation.getUserId().equals(userId);
    }

    private String buildMarkdown(LawConversation conv, List<LawMessage> messages) {
        StringBuilder md = new StringBuilder();
        String exportTime = LocalDateTime.now().format(DT_FMT);
        String createTime = conv.getCreateTime() != null ? conv.getCreateTime().format(DT_FMT) : "-";

        // ===== 文件头 =====
        md.append("# ").append(conv.getTitle()).append("\n\n");
        md.append("> **平台**: LexAtlas 智能法律咨询  \n");
        md.append("> **创建时间**: ").append(createTime).append("  \n");
        md.append("> **导出时间**: ").append(exportTime).append("  \n");
        md.append("> **消息数量**: ").append(messages.size()).append("  \n\n");
        md.append("---\n\n");

        // ===== 对话内容 =====
        for (LawMessage msg : messages) {
            String time = msg.getCreateTime() != null ? msg.getCreateTime().format(DT_FMT) : "";
            if ("user".equals(msg.getRole())) {
                md.append("### 👤 用户");
                if (!time.isEmpty()) md.append(" · `").append(time).append("`");
                md.append("\n\n");
                md.append(msg.getContent()).append("\n\n");
            } else {
                md.append("### 🤖 LexAtlas");
                if (!time.isEmpty()) md.append(" · `").append(time).append("`");
                if (msg.getResponseTime() != null) {
                    md.append(" · ⏱ ").append(msg.getResponseTime()).append("ms");
                }
                md.append("\n\n");
                md.append(msg.getContent()).append("\n\n");

                // 来源引用
                appendSources(md, msg.getSources());

                // 反馈标记
                if (msg.getFeedback() != null && msg.getFeedback() == 1) {
                    md.append("> 👍 用户认为此回答有用\n\n");
                } else if (msg.getFeedback() != null && msg.getFeedback() == -1) {
                    md.append("> 👎 用户认为此回答无用\n\n");
                }
            }
            md.append("---\n\n");
        }

        // ===== 文件尾 =====
        md.append("*本文档由 LexAtlas 自动生成，仅供参考，不构成正式法律意见。*\n");
        return md.toString();
    }

    private void appendSources(StringBuilder md, String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank()) return;
        try {
            JSONArray sources = JSON.parseArray(sourcesJson);
            if (sources == null || sources.isEmpty()) return;
            md.append("**参考来源**\n\n");
            for (int i = 0; i < sources.size(); i++) {
                JSONObject s = sources.getJSONObject(i);
                String name = s.getString("name");
                String chapter = s.getString("chapter");
                Integer page = s.getInteger("pageNumber");
                md.append(i + 1).append(". 《").append(name != null ? name : "法律文献").append("》");
                if (chapter != null && !chapter.isBlank()) md.append(" · ").append(chapter);
                if (page != null && page > 0) md.append(" · 第 ").append(page).append(" 页");
                md.append("\n");
            }
            md.append("\n");
        } catch (Exception ignored) {
        }
    }
}
