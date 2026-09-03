# 更新日志

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 规范，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-09-03

首个开源版本。由内部项目深度重构而来，面向社区发布。

### Added 新增
- RAG 核心流水线：Query 改写 → 向量+BM25 多路召回 → RRF 融合 → Cross-Encoder 重排序 → 法律推理链 Prompt → LLM 流式生成
- 安全兜底机制：紧急人身安全检测（提示报警）、低置信度兜底回答、LLM 答案自评
- 高频问题 Redis 缓存：频次阈值触发写入 + 模拟流式回放
- 知识库管理：PDF/Word 解析、切片、向量化入库（MinIO + Milvus）
- 管理后台：AI 参数热配置（免重启调参）、用户管理、审计日志、数据大屏
- 实时语音识别代理（WebSocket → 后端语音服务）
- Actuator 健康检查、SpringDoc OpenAPI 文档（/swagger-ui.html）
- 生产级日志：滚动归档 + 错误单独归档（logback-spring.xml）
- Docker 一键部署编排（MySQL/Redis/MinIO/Milvus/后端/前端）
- 开源社区文件：README / CONTRIBUTING / SECURITY / CHANGELOG / .env.example / MIT LICENSE

### Changed 变更
- 品牌重塑：LawRAG → **LexAtlas（法枢）**，包名 `com.simon.lawrag` → `com.lexatlas`
- 数据库表名统一 `law_*` / `sys_*` 前缀，数据库更名为 `lexatlas`
- 种子数据全面脱敏：移除固定密码账号，仅保留中性问题模板
- 敏感配置全部支持环境变量注入（JWT_SECRET、数据库、LLM Key 等）
- 全局异常处理覆盖 404/413 场景，不再泄露堆栈信息
- 前端：新增独立 404 页面、路由级页面标题管理

### Removed 移除
- 移除医疗版遗留代码与提示词（medical_qa 等）
- 移除废弃的 Python 重排序微服务（已直连云 API）
- 移除全部真实用户数据（手机号、头像、会话记录、审计日志）

### Security 安全
- JWT 密钥不再硬编码于配置文件
- Swagger 与 Actuator 暴露面按生产标准收紧
- 修复会话检索日志、反馈与收藏接口的跨用户越权风险
- SSE 改用 Authorization Header，Markdown 输出增加 XSS 净化
