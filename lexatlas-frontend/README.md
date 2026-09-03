# LexAtlas Frontend

LexAtlas（法枢）智能法律咨询系统的前端应用，基于 Vue 3 + TypeScript + Vite + Element Plus 构建。

## 技术栈

- **Vue 3.5** — Composition API + `<script setup>`
- **TypeScript** — 严格类型检查
- **Vite 7** — 极速开发与构建
- **Element Plus** — 企业级 UI 组件库
- **Pinia** — 状态管理
- **Vue Router** — 路由管理
- **ECharts** — 数据可视化（管理端统计）

## 开发环境准备

```sh
# 安装依赖
npm install

# 启动开发服务器（热更新）
npm run dev

# 类型检查 + 生产构建
npm run build

# 仅生产构建（跳过类型检查）
npm run build-only

# 运行单元测试（Vitest）
npm run test

# 代码检查（ESLint）
npm run lint
```

## 推荐开发工具

- [VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) 扩展（请禁用 Vetur）
- Chromium 内核浏览器安装 [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)

## 项目结构

```
src/
├── api/            # 后端 API 封装
├── assets/         # 静态资源
├── components/     # 通用组件
├── router/         # 路由配置
├── stores/         # Pinia 状态仓库
├── utils/          # 工具函数（含单元测试 __tests__/）
├── views/          # 页面视图（chat / admin / cases 等）
├── App.vue         # 根组件
└── main.ts         # 应用入口
```

## SSE 流式对话

聊天页面通过 Server-Sent Events 接收后端流式响应，事件流依次为：
`rewrite`（查询改写）→ `retrieval`（检索结果）→ `rerank`（重排序）→ `token`（增量文本）→ `done`。

## 后端对接

默认对接 `http://localhost:8080`，可通过 `.env` 文件中的 `VITE_API_BASE_URL` 覆盖。
