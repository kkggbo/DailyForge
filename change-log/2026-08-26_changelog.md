# 2026-08-26 Changelog

## 今日概览

AI Coach 新增第三个固定场景「下一周期模板生成」(`next_cycle_generation`)，打通「周期总结 → 下一轮计划」闭环；同时完成 `ErrorCode` 全量中文化与一批前端体验/文案优化。

## 今日完成内容

### 1. AI 下一周期模板生成（next_cycle_generation）

**后端**
- 新增第三个 AI 场景 `next_cycle_generation`：根据「上一周期实际表现 + 上轮 AI 周期总结」生成下一周期模板草稿。
- 复用模板生成链路：输出结构、校验（`validateTemplateGeneration`）、修复循环、draft 模板持久化全部复用；`AiTemplateGenerationService` 小重构（底层 `persistTemplateDraft` 参数化 taskType），保持既有 `persistSuccessfulResult` 签名不变。
- 上轮总结读取：`sourceSummaryTaskId` 优先，否则自动取该 cycleRun 最近一次 succeeded 周期总结；**无总结则拒绝**（新增错误码 `AI_CYCLE_SUMMARY_REQUIRED`）。
- 上一周期表现取该 cycleRun 自身聚合（`aggregatedAnalysis` / `sessionsDetail` / `versionSnapshot`），而非最近 5 次。
- 信号优先级：用户表单意图 > `injuryNotes`(硬约束) > 上轮总结建议 > 上轮表现(渐进基准+上限) > 默认值；对降强度/恢复类意图，上轮表现只作上限。
- 新增 `NextCycleGenerationRequest` / Context(+Builder) / PromptBuilder / Executor；`AiToolRegistry` 增白名单分支；capabilities 增 `nextCycleGeneration`；端点 NC1 `POST /ai-coach/next-cycle-generations`、NC2 `GET /ai-coach/next-cycle-generations/{taskId}`。
- 新增 `nextCycleGenerationPromptVersion` 配置（`next_cycle_generation_v1`）。

**前端**
- 双入口：「周期总结结果页」底部与「周期总结历史」卡片（仅 succeeded 显示），均收敛到 `NextCycleGenerationModal`。
- 生成表单复用 `TemplateGenerationForm`，**预填上一轮值**（sceneType / goalType / cycleLength / includeCardio，方案 A 精确还原）且可编辑；`sourceCycleRunId` / `sourceSummaryTaskId` 为隐藏逻辑。
- 新增 `NextCycleGenerationTaskPage` 复用 `TemplateGenerationResult` 展示；polling hook 泛型化 `useAiTaskPolling`。

### 2. 共享 Schema（方案 A，预填支撑）

- 新增迁移 `V8__ai_next_cycle_schema_upgrade.sql`：`cycle_templates` 增加 `scene_type VARCHAR(32) NULL` 与 `include_cardio TINYINT(1) NOT NULL DEFAULT 1`，非破坏式。
- AI 模板生成落库时写入这两列；`CycleTemplateDetailResponse` 新增 `sceneType` / `includeCardio`，供 next_cycle 精确预填。

### 3. ErrorCode 全量中文化

- `ErrorCode` 全部英文默认消息一次性转中文（约 80 个），`code` 与 `httpStatus` 完全不变；`SUCCESS→成功`、`USER_NOT_FOUND→账号不存在`、`INVALID_CREDENTIALS→邮箱或密码错误` 等。
- 同步更新 4 个断言英文消息的测试（Exercise / Workout / Plan 策略测试）。

### 4. 前端体验 / 文案优化

- 模态框去掉内部 DB id 标签（CycleRun / 总结任务），文案用户友好化。
- 按钮文字垂直居中（查看对应模板 / 查看总结详情等补 `inline-flex items-center`）。
- AI 任务时间标签精简：详情页「创建时间 + 完成时间」，历史卡片仅「创建时间」。
- 手机端登录 / 注册卡片顺序调整：表单置顶、介绍在下（桌面保持介绍左/表单右）。
- `AppEntryPage` 按**真实资料完整度**守卫 onboarding：资料已完整则直接进应用并补写本地标志，不再硬拦已填资料用户；接口异常宽松放行。
- 未登录时顶部不再显示 "guest"。
- AI 生成时正文中目标/场景用中文标签（减脂/增肌/保持健康；健身房/居家），不再出现 `health_maintenance` 等英文代码。

## 总结

本轮为 AI Coach 的「下一周期模板生成」闭环上线，前后端与共享 Schema、文档（PRD / 接口 / 前后端 DDD）一并交付；并顺带完成 ErrorCode 全量中文化与一批前端体验/文案修正。后端测试 `mvn test` 130 用例通过，前端 `tsc` 通过（vitest 受本地沙箱限制交由 CI）。
