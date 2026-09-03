# 贡献指南

感谢你对 **LexAtlas** 的关注！欢迎以任何形式参与贡献：报告 Bug、提出功能建议、改进文档、提交代码。

## 行为准则

参与本项目即表示你同意遵守基本的开源社区礼仪：友善、专业、尊重不同观点。任何形式的骚扰、歧视性言论都将被拒绝。

## 如何贡献

### 报告 Bug

提交 Issue 时请包含：

1. **环境信息**：操作系统、JDK 版本、Node 版本、Docker 版本（如适用）
2. **复现步骤**：最小化复现路径
3. **期望行为 vs 实际行为**
4. **日志片段**：后端 `logs/lexatlas.log` 或浏览器 Console（**注意脱敏，勿包含 API Key、真实用户数据**）

### 提交功能建议

- 先检索现有 Issue，避免重复
- 说明使用场景与预期收益，而不仅仅是"加个 XX 功能"
- 涉及 LLM Prompt 变更的，请附上前后对比效果

### 提交代码（Pull Request）

1. Fork 仓库，从 `main` 创建特性分支：
   ```bash
   git checkout -b feat/your-feature
   ```
2. 提交前自检：
   - 后端：`mvn compile` 通过（JDK 17）
   - 前端：`npm run build` 通过
   - 新增代码有必要的注释与日志
   - **不提交任何真实用户数据、API Key、密码或其他机密**
3. 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)：
   ```
   feat: 支持多知识库分区检索
   fix: 修复 SSE 连接在代理层被缓冲的问题
   docs: 补充 Milvus 集合初始化说明
   refactor: 抽取 RRF 融合公共逻辑
   ```
4. PR 描述中说明：改动动机、实现方案、测试方式

## 开发环境

| 要求 | 版本 |
|------|------|
| JDK | 17（**不要使用 21/25**，Lombok 存在兼容问题） |
| Maven | 3.9+ |
| Node.js | 22.22.2+ |
| MySQL / Redis / MinIO / Milvus | 见 docker-compose.yml |

推荐使用 `docker compose up -d mysql redis minio etcd milvus` 只启动中间件，本地 IDE 跑后端与前端。

## 项目结构约定

- 后端包结构：`com.lexatlas.{controller,service,entity,mapper,config,common,security}`
- RAG 流水线相关类统一放在 `service/rag/`，新增检索策略请实现/参考 `RetrievedChunk` 数据流
- 数据库表名使用 `law_*`（业务表）/ `sys_*`（系统表）前缀
- 前端 API 层统一走 `src/api/*.ts`，勿在组件中直接写 axios 调用

## 安全漏洞

请勿在公开 Issue 中披露安全漏洞，参见 [SECURITY.md](SECURITY.md) 的报告流程。

## 许可

提交代码即表示你同意以 [MIT License](LICENSE) 授权你的贡献。
