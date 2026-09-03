package com.lexatlas.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lexatlas.entity.SysUser;
import com.lexatlas.mapper.SysUserMapper;
import com.lexatlas.service.knowledge.MilvusService;
import com.lexatlas.service.knowledge.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 应用启动初始化
 * 自动创建 MinIO Bucket 和 Milvus Collection
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppInitConfig implements ApplicationRunner {

    private final MinioService minioService;
    private final MilvusService milvusService;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:}")
    private String adminUsername;

    @Value("${app.bootstrap-admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== LexAtlas 系统初始化 ==========");
        // 初始化 MinIO Bucket
        minioService.initBucket();
        // 初始化 Milvus Collection
        milvusService.initCollection();
        createBootstrapAdmin();
        log.info("========== 初始化完成 ==========");
    }

    private void createBootstrapAdmin() {
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.warn("未配置 ADMIN_USERNAME/ADMIN_PASSWORD，跳过初始管理员创建");
            return;
        }
        if (adminPassword.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD 至少需要 12 个字符");
        }
        long existing = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, adminUsername));
        if (existing > 0) {
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname("系统管理员");
        admin.setRole("admin");
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        admin.setDeleted(0);
        sysUserMapper.insert(admin);
        log.info("已创建初始管理员账号: {}", adminUsername);
    }
}
