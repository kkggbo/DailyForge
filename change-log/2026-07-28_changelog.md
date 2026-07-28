# 2026-07-28 Changelog

## 今日概览

本轮完成了核心 MVP 模块的提交前补测、认证会话稳定性修复、cycle template 接口契约对齐，以及多 Agent 协作规则落地。

## 今日完成内容

### 1. 补充核心模块测试

- 前端新增 auth、profile、exercise、cycle_template 关键交互测试。
- 后端补充 auth、profile、exercise、cycle_template 关键规则集成测试。
- 前端 `pnpm test:run` 通过：10 个测试文件、13 个测试。
- 后端指定集成测试通过：51 个测试，0 失败、0 错误、0 跳过。

### 2. 修复前端认证会话刷新链路

- 新增 `POST /api/auth/refresh-token` 前端 API client。
- 持久化 `expiresAt`，并在 token 临近过期时自动刷新。
- `GET /api/auth/me` 遇到可刷新认证错误时自动重试一次。
- 刷新失败时清理本地会话并退出登录态。

### 3. 对齐 cycle_template AI 草稿契约

- AI 草稿占位接口按 `ApiResponse<Void>` 处理，不再假设返回 `templateId`。
- 草稿和正式模板更新接口的前端返回类型与后端响应对齐。
- AI 生成表单增加 `prompt` 非空校验，避免提交必填参数为空的请求。

### 4. 完善工程协作规则

- 新增仓库级 `AGENTS.md` 和详细多 Agent 协作规范。
- 明确测试、契约校验、代码审查和 Git 提交门禁。
- 允许在满足门禁的前提下直接提交到 `main`、`master`、`develop`。
- 继续禁止 `git push --force`。

## 验证结果

- `frontend/pnpm test:run`：通过。
- `frontend/pnpm build`：通过。
- 后端核心模块指定集成测试：通过。
- 增量代码审查：低风险，无高/中风险阻塞项。

## 后续建议

- 为 `AuthProvider` 的自动刷新、401 重试和刷新失败分支补充直接组件测试。
- 继续推进 `training session` 模块，打通模板、训练日、打卡和循环推进闭环。
