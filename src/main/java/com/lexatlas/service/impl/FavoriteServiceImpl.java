package com.lexatlas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lexatlas.common.exception.BusinessException;
import com.lexatlas.common.result.ResultCode;
import com.lexatlas.entity.LawMessage;
import com.lexatlas.entity.LawConversation;
import com.lexatlas.entity.SysFavorite;
import com.lexatlas.entity.SysUser;
import com.lexatlas.entity.vo.PageVO;
import com.lexatlas.mapper.LawMessageMapper;
import com.lexatlas.mapper.LawConversationMapper;
import com.lexatlas.mapper.SysFavoriteMapper;
import com.lexatlas.mapper.SysUserMapper;
import com.lexatlas.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息收藏服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final SysFavoriteMapper favoriteMapper;
    private final SysUserMapper sysUserMapper;
    private final LawMessageMapper messageMapper;
    private final LawConversationMapper conversationMapper;

    @Override
    @Transactional
    public void addFavorite(Long messageId, Long conversationId, String note) {
        Long userId = getCurrentUserId();

        LawMessage message = messageMapper.selectById(messageId);
        if (message == null || !message.getConversationId().equals(conversationId)) {
            throw new BusinessException("消息不存在或会话不匹配");
        }
        LawConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException("消息不存在或无权操作");
        }

        // 检查是否已收藏
        long existing = favoriteMapper.selectCount(
                new LambdaQueryWrapper<SysFavorite>()
                        .eq(SysFavorite::getUserId, userId)
                        .eq(SysFavorite::getMessageId, messageId));
        if (existing > 0) {
            throw new BusinessException("该消息已收藏");
        }

        SysFavorite favorite = new SysFavorite();
        favorite.setUserId(userId);
        favorite.setMessageId(messageId);
        favorite.setConversationId(conversationId);
        favorite.setNote(note);
        favoriteMapper.insert(favorite);
        log.info("用户 {} 收藏消息 {}", userId, messageId);
    }

    @Override
    @Transactional
    public void removeFavorite(Long favoriteId) {
        Long userId = getCurrentUserId();
        SysFavorite favorite = favoriteMapper.selectById(favoriteId);
        if (favorite == null || !favorite.getUserId().equals(userId)) {
            throw new BusinessException("收藏记录不存在或无权操作");
        }
        favoriteMapper.deleteById(favoriteId);
        log.info("用户 {} 取消收藏 {}", userId, favoriteId);
    }

    @Override
    public PageVO<Map<String, Object>> listFavorites(int current, int size) {
        Long userId = getCurrentUserId();
        Page<SysFavorite> page = new Page<>(current, size);
        favoriteMapper.selectPage(page,
                new LambdaQueryWrapper<SysFavorite>()
                        .eq(SysFavorite::getUserId, userId)
                        .orderByDesc(SysFavorite::getCreateTime));

        List<Map<String, Object>> records = new ArrayList<>();
        for (SysFavorite fav : page.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", fav.getId());
            item.put("messageId", fav.getMessageId());
            item.put("conversationId", fav.getConversationId());
            item.put("note", fav.getNote());
            item.put("createTime", fav.getCreateTime());

            // 填充消息内容
            LawMessage message = messageMapper.selectById(fav.getMessageId());
            item.put("messageContent", message != null ? message.getContent() : null);
            item.put("messageRole", message != null ? message.getRole() : null);
            records.add(item);
        }

        // 构造带富化数据的 PageVO
        PageVO<Map<String, Object>> result = new PageVO<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(records);
        return result;
    }

    @Override
    public boolean isFavorited(Long messageId) {
        Long userId = getCurrentUserId();
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<SysFavorite>()
                        .eq(SysFavorite::getUserId, userId)
                        .eq(SysFavorite::getMessageId, messageId)) > 0;
    }

    /**
     * 从 Security 上下文获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String username = authentication.getName();
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user.getId();
    }
}
