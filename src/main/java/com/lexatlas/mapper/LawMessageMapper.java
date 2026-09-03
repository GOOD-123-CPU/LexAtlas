package com.lexatlas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lexatlas.entity.LawMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息 Mapper
 */
@Mapper
public interface LawMessageMapper extends BaseMapper<LawMessage> {
}
