# 安全政策

## 报告安全漏洞

我们高度重视项目安全。如果你发现了安全漏洞，请**不要**在公开 Issue、Discussion 或 PR 中披露。

请通过以下方式私密报告：

1. 使用 GitHub 的 **Private vulnerability reporting** 功能（仓库 Security 标签页）
2. 或在 Issue 中注明 `[SECURITY]` 并仅描述影响范围，细节通过邮件补充

报告请包含：

- 漏洞类型（如注入、越权、信息泄露）
- 影响版本与复现条件
- 概念验证（PoC）或复现步骤
- 修复建议（如有）

我们会在 **72 小时内**确认收到，并在修复后于 Release Notes 中致谢报告者（除非你要求匿名）。

## 安全设计要点

供部署者与审计者参考的本项目安全基线：

| 项目 | 说明 |
|------|------|
| 密码存储 | BCrypt 强度 10 |
| 认证 | JWT 无状态认证，密钥通过环境变量注入 |
| 权限模型 | RBAC（admin/user），接口级 `@PreAuthorize` 校验 |
| 审计 | 管理操作全部记录 `sys_audit_log` |
| 机密管理 | 所有敏感配置通过环境变量注入，仓库中不含真实凭据 |
| 输入校验 | Jakarta Validation 参数校验 + 全局异常处理（不泄露堆栈） |

## 部署安全建议

- **必须**替换 JWT_SECRET 默认值，使用 ≥48 字符随机串
- **必须**通过 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 创建独立的初始管理员，密码至少 12 位
- 生产环境关闭 Swagger：`springdoc.swagger-ui.enabled=false`
- 生产环境收紧 Actuator 暴露面：仅保留 `health,info`
- 通过 `CORS_ALLOWED_ORIGINS` 配置明确的前端域名白名单
- 数据库、MinIO、Redis 不应暴露公网，Docker 网络隔离已默认提供
- 定期更新依赖版本，关注 Spring Boot / Milvus 安全通告

## 已知取舍

- SSE 流式接口的 token 事件未做端到端加密（依赖传输层 TLS）
- 语音 WebSocket 握手令牌使用查询参数传递；默认 Nginx 配置已关闭该路径访问日志，生产环境仍必须使用 HTTPS/WSS
- 文件上传目前仅校验扩展名与大小，未做内容级病毒扫描，部署者应按需增加
