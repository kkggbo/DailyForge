# DailyForge AI 下一周期模板生成 PRD

> 版本：v0.1
> 日期：2026-08-25
> 状态：待评审
> 归属范围：MVP 之后的 AI 增强（roadmap ③-b）
> 模块名称：`ai_coach`
> 依赖：`ai_coach` 模板生成链路、`ai_coach` 周期总结链路、`workout` 性能聚合层

---

## 1. 文档目标

定义「根据上一周期表现 + 上轮 AI 周期总结，生成下一周期训练模板草稿」的产品需求。该功能是 `ai_coach` 的第三个固定 AI 场景（`next_cycle_generation`），在现有「模板生成」「周期总结」两条链路基础上，打通「总结 → 下轮计划」的闭环。

本文档主要服务于：

- 明确三个已确认决策：双入口、必须有总结才能生成、预填可编辑
- 明确「上一周期表现」数据来源（该 cycleRun 自身 sessions 聚合，而非最近 5 次）
- 为后续接口文档、提示词设计、上下文组装设计、前后端任务拆分提供依据

---

## 2. 背景与目标

DailyForge 已具备完整训练闭环，并已有两个 AI 场景：

1. **模板生成**：按用户当下意图生成新模板草稿。
2. **周期总结**：分析一个已完成循环，输出执行情况、优劣、原因、下轮建议与风险。

当前缺口：用户完成一轮、拿到周期总结后，**需要手动把「下轮建议」翻译成新的训练计划**。本功能让 AI 直接基于「上轮实际表现 + 上轮 AI 总结」，生成下一周期的模板草稿，实现「复盘 → 落地」闭环。

### 与「模板生成」(a) 的区别

| 维度 | 模板生成 (a) | 下一周期模板生成 (b) |
| --- | --- | --- |
| 输入信号 | 用户当下意图、资料、身体指标、最近表现 | 用户当下意图、资料、身体指标、**上轮表现 + 上轮 AI 总结** |
| 生成规则 | 意图优先，历史作参照 | **延续上轮结构 + 渐进负荷 + 吸收上轮建议**，意图仍优先 |
| 输出 | 一份 draft 模板 | 一份 draft 模板（复用同一套结构/校验/持久化） |

---

## 3. 目标用户

- 已完成一个训练循环、并已生成 AI 周期总结的用户。
- 希望快速把「下一轮建议」落成可执行模板的有经验用户。
- 有 AI 权限的用户（邀请解锁 / 管理员 / 会员）。

---

## 4. 模块定位

`next_cycle_generation` 是 `ai_coach` 的一个**结构化固定场景**，职责：

- 读取上轮 cycleRun 实际表现 + 上轮周期总结 + 用户当下意图
- 生成一份**下一周期模板草稿**（与模板生成同构）
- 草稿进入现有模板草稿体系，用户可继续编辑、确认、启用

它不负责：

- 开放式对话
- 自主替用户启用模板（只生成草稿）
- 在没有上轮周期总结时凭空生成

---

## 5. 功能需求

### 5.1 触发入口（前端）

**入口 A：周期总结结果页**
- 位置：`/ai-coach/cycle-summary/tasks/:taskId`，任务 succeeded 且 result 存在时，在底部现有「查看对应模板」按钮**旁**新增「生成下一周期模板」入口。
- 行为：点击打开「生成下一周期模板」表单。

**入口 B：AI 任务历史 → 周期总结历史 tab**
- 位置：`/ai-coach/history?tab=cycle-summaries`，每个总结卡片在现有「查看总结详情」按钮**旁**新增「生成下一周期模板」入口。
- 行为：仅对 **succeeded** 的总结卡片显示；点击打开同一表单，`sourceCycleRunId` / `sourceSummaryTaskId` 取该卡片的 cycleRunId 与其对应总结任务。

### 5.2 生成表单（前端）

- 复用/扩展现有 `TemplateGenerationForm`。
- **预填（来自上轮）**：`sceneType`、`goalType`、`cycleLength`、`includeCardio` 取 sourceCycleRun 对应模板的值；`additionalRequirements` 留空。
- **用户可编辑全部字段**（决策 3）。
- `sourceCycleRunId`、`sourceSummaryTaskId` 作为隐藏字段随表单提交。
- 提交后进入轮询态，完成后展示草稿（复用 `TemplateGenerationResult` + `GenerationRationale`）。

### 5.3 生成请求（后端）

新增 DTO `NextCycleGenerationRequest`：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `clientRequestId` | 否 | 幂等 |
| `sourceCycleRunId` | 是 | 上一已完成 cycle run |
| `sourceSummaryTaskId` | 否 | 为空时后端自动取该 cycleRun 最近一次 succeeded 周期总结 |
| `sceneType` | 是 | 复用模板生成语义 |
| `goalType` | 是 | 复用模板生成语义 |
| `cycleLength` | 是 | 复用模板生成语义 |
| `includeCardio` | 是 | 复用模板生成语义 |
| `additionalRequirements` | 否 | 用户当下意图（一等信号） |

### 5.4 生成流程（后端）

1. 校验：AI 可用；`sourceCycleRunId` 归属当前用户且 `completed`；profile/metrics 完整性（与模板生成一致）。
2. **必须有上轮周期总结**（决策 2）：按 `sourceSummaryTaskId` 或自动取该 cycleRun 最近 succeeded `cycle_summary` 任务。若无 → 业务错误「请先生成该周期的 AI 总结」。
3. 组装上下文：`userProfile`、`currentBodyMetrics`、`previousCycleRun`（该 cycleRun 的 aggregatedAnalysis + sessionsDetail + versionSnapshot）、`previousCycleSummary`（executionOverview / strengths / issues / causeAnalysis / nextCycleSuggestions / risks）、`templateConstraints`。
4. 生成（新 `next_cycle_generation` 场景）→ 校验/修复（复用模板生成校验）→ 创建 draft 模板版本，任务关联 `sourceCycleRunId`。

### 5.5 结果与展示

- 结果 VO 复用 `TemplateGenerationTaskResultResponse`（draftTemplate + generationRationale）。
- 生成产物为 **draft** 状态，不自动启用；进入模板列表，用户可继续编辑。

---

## 6. 业务规则

### 6.1 输入信号优先级（高 → 低）

1. **用户表单意图**（`additionalRequirements`）——当下明确意图，严格遵循。
2. **`injuryNotes`（伤病注意事项）**——常驻健康约束，作硬限制；与 1 冲突时健康优先并写入 warnings。
3. **上轮 AI 周期总结**（`nextCycleSuggestions` 等）——指导调整方向。
4. **上轮实际表现**（该 cycleRun sessions 聚合）——作渐进基准与上限。
5. 个人资料/请求默认值兜底。

### 6.2 关键规则

- **必须有上轮周期总结**才能生成（决策 2）；无总结时引导先做周期总结，不生成。
- **上轮表现取该 cycleRun 自身 sessions 聚合**，不使用「最近 5 次」（决策 4）。
- 延续上轮模板结构：保留有效动作/排期，调整问题项，吸收下轮建议。
- 渐进负荷：基于上轮实际表现推进；对「降强度 / 恢复 / 避开某动作」类意图，上轮表现**只作上限封顶**，绝不当目标抬高。
- 生成产物始终为 draft。

### 6.3 幂等与任务

- 同 `clientRequestId` 复用现有任务（与 template_generation / cycle_summary 一致）。
- 任务类型 `next_cycle_generation`，关联实体 = sourceCycleRun。

---

## 7. 异常场景

| 场景 | 错误 |
| --- | --- |
| AI 未开通 | 403 AI_FEATURE_NOT_AVAILABLE |
| sourceCycleRun 不存在 / 非本人 | 404 RESOURCE_NOT_FOUND |
| sourceCycleRun 未完成 | 409 AI_CYCLE_RUN_NOT_COMPLETED |
| 无上轮周期总结 | 业务错误，提示先生成周期总结 |
| profile / 身体指标缺失 | 400 AI_REQUIRED_*_MISSING（复用模板生成校验） |
| AI 服务不可用 / 超时 | 503 / 504 |

---

## 8. 接口契约（摘要）

- `GET /ai-coach/capabilities`：`capabilities` 增加 `nextCycleGeneration`（可用性、最新已完成 run、是否有上轮总结）。
- `POST /ai-coach/next-cycle-generations`：提交，返回 `AiAsyncTaskAcceptedResponse`。
- `GET /ai-coach/next-cycle-generations/{taskId}`：查结果（复用模板生成结果 VO）。
- `GET /ai-coach/next-cycle-generations/history`：**可选**，本版可暂不做独立历史页。

---

## 9. 范围

### 本版包含

- 后端：新场景 `next_cycle_generation`（DTO / 上下文 / Prompt / Executor / 持久化复用 / 应用服务 / Controller / capabilities）。
- 前端：两个入口、生成表单（预填可编辑）、提交轮询、结果展示。
- 文档：接口文档、DDD、前后端提示词/上下文设计、changelog。

### 本版不含

- 独立的「下一周期」历史页（可后续补）。
- 周期总结自动触发下一周期（人工点击发起）。
- 直接启用模板（始终 draft）。

---

## 10. 验收标准

1. 两个入口出现：周期总结结果页底部、周期总结历史卡片旁；均与现有按钮并排。
2. 无上轮总结的 cycleRun，入口点击后提示「先生成周期总结」，不发起生成。
3. 提交后生成 draft 模板：结构延续上轮、负荷基于上轮表现并体现 `nextCycleSuggestions`，`generationRationale` 诚实反映依据。
4. 表单预填上轮值且全部可编辑。
5. 生成的 draft 进入模板列表，状态为 draft，不自动启用。
6. 本地与 CI：后端 `mvn test`、前端 `pnpm test` 通过；契约联调校验通过。

---

## 11. 待办 / 依赖

- 依赖上一轮已合入的 `TrainingPerformanceAggregationService` 与 AI 上下文接线（本轮复用其结构）。
- 依赖周期总结链路已有结果数据（`ai_task_record.resultJson` 中的 `nextCycleSuggestions`）。
