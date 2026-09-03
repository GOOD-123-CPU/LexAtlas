package com.lexatlas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lexatlas.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
