# DailyForge AI 下一周期模板生成 详细设计文档（DDD）

> 版本：v0.1
> 日期：2026-08-25
> 文档状态：待开发实现设计稿
> 模块归属：`backend` 单体应用
> 目标 Java 包路径：`com.dailyforge.modules.aicoach`
> 上游契约：`docs/interfaces/ai_coach_next_cycle_接口文档.md`、`docs/prd/ai_coach_next_cycle_PRD.md`

---

## 一、文档说明

### 1.1 上游输入文档

- PRD：[ai_coach_next_cycle_PRD.md](../../prd/ai_coach_next_cycle_PRD.md)
- 接口文档：[ai_coach_next_cycle_接口文档.md](../../interfaces/ai_coach_next_cycle_接口文档.md)
- 现有模块 DDD（沿用结构/风格）：[ai_coach_DDD.md](./ai_coach_DDD.md)
- 现有 AI 接入与提示词上下文设计：[AI接入与提示词上下文设计.md](./AI接入与提示词上下文设计.md)

### 1.2 本文档目标

本文档用于把 `ai_coach` 模块的第三个固定场景 `next_cycle_generation`（根据上一周期实际表现 + 上轮 AI 周期总结，生成下一周期训练模板草稿）收口为一套可直接指导后端实现、代码评审和后续重构的技术方案。重点明确：

- 模块定位、职责边界与「不与模板生成重复造轮子」的复用策略
- 新增组件：`NextCycleGenerationRequest` / `NextCycleGenerationContext(+Builder)` / `NextCycleGenerationPromptBuilder` / `NextCycleGenerationExecutor`
- 如何复用现有 `AiTemplateGenerationService` 的 draft 模板落库（含对其的小重构设计）
- 上轮周期总结读取（`sourceSummaryTaskId` 或自动取最近 succeeded `cycle_summary` 的 `resultJson`）
- 上下文组装数据来源（该 `cycleRun` 的 `aggregatedAnalysis` / `sessionsDetail` / `versionSnapshot`）
- `capabilities` 新增 `nextCycleGeneration`
- Controller 端点 NC1 / NC2
- 校验与异常（含新增错误码 `AI_CYCLE_SUMMARY_REQUIRED`）
- 事务、幂等、配置、日志、Swagger 与测试策略

### 1.3 当前仓库事实

截至本文档编写时，`ai_coach` 模块已实现 `template_generation` 与 `cycle_summary` 两条完整链路，仓库现状：

1. 统一基础设施已存在：
   - `ApiResponse<T>`、`ErrorCode`、`BusinessException`、`GlobalExceptionHandler`
   - JWT 鉴权链路（`AuthSecurityUtils.getCurrentUserId()`）、SpringDoc/OpenAPI 基础配置
2. 异步 AI 任务基础设施已存在：
   - `ai_task_records` / `ai_task_tool_calls`（`AiTaskRecordEntity` / `AiTaskToolCallEntity` + Mapper）
   - `AiTaskExecutor`（按 `taskType` 路由到 `AiScenarioExecutor`，统一状态机、异常收敛、日志）
   - `AiConversationService`、`AiJsonRepairService`（两轮 JSON 修复）、`AiOutputValidationDomainService`
   - `AiCoachProperties`（`dailyforge.ai.*`）、`AiCoachToolConfig`（只读工具 Handler Bean）、`AiToolRegistry`（按 taskType 白名单）
3. `template_generation` 场景已有：
   - `TemplateGenerationRequest` / `TemplateGenerationContext(+Builder)` / `TemplateGenerationPromptBuilder` / `TemplateGenerationExecutor`
   - `AiTemplateGenerationService.persistSuccessfulResult(taskId, inputSummaryJson, validatedResult)` 负责 draft 模板落库 + 版本创建 + `source_task_id` 回写 + `result_json` 写入
   - `AiOutputValidationDomainService.validateTemplateGeneration(json, request)` 输出结构/业务校验（本轮可直接复用）
4. `cycle_summary` 场景已提供上轮总结的数据形态：
   - 任务关联 `cycle_run`，结果存于 `ai_task_records.result_json`，反序列化为 `CycleSummaryTaskResultResponse`（含 `executionOverview / strengths / issues / causeAnalysis / nextCycleSuggestions / risks`）
   - `AiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(...)` 可直接用于「取某 cycleRun 最近 succeeded cycle_summary」
   - `CycleSummaryContextBuilder` 已演示如何从该 cycleRun 组装 `aggregatedAnalysis / sessionsDetail / versionSnapshot`
5. `AiCoachApplicationService` 已封装通用的 `buildTaskRecord` / `scheduleTaskAfterCommit` / 幂等 `findExistingTask` / `requireCompletedCycleRun` / `assertAiEnabled`，本轮可复用同一套提交模式。

> 说明：本文档所有「新增」与「小重构」均为待实现项，不表示仓库中已存在。

---

## 二、方案概述

### 2.1 模块定位

`next_cycle_generation` 是 `ai_coach` 的结构化固定 AI 场景（区别于 `template_generation`、`cycle_summary`），职责：

- 读取「上轮 cycleRun 实际表现」+「上轮 AI 周期总结」+「用户当下意图」
- 生成一份**下一周期模板草稿**（与模板生成同构：同输出 schema、同校验、同 draft 落库）
- 草稿进入现有模板草稿体系，用户可继续编辑、确认、启用

它不负责：

- 开放式对话
- 自主替用户启用模板（始终只生成 draft）
- 在没有上轮周期总结时凭空生成

### 2.2 本期交付范围

| 能力 | 说明 | 状态 |
|------|------|:---:|
| NC1 提交下一周期模板生成任务 | `POST /ai-coach/next-cycle-generations`，创建 `next_cycle_generation` 异步任务 | 待开发 |
| NC2 查询任务结果 | `GET /ai-coach/next-cycle-generations/{taskId}`，复用模板生成结果 VO | 待开发 |
| CAP 能力字段 | `GET /ai-coach/capabilities` 新增 `nextCycleGeneration` | 待开发 |
| 上轮总结读取 | `sourceSummaryTaskId` 或自动取该 cycleRun 最近 succeeded `cycle_summary` 的 `resultJson` | 待开发 |
| draft 落库复用 | 复用 `AiTemplateGenerationService`（小重构参数化 `taskType`） | 待开发 |
| 持久化复用重构 | `AiTemplateGenerationService.persistSuccessfulResult` 小重构 | 待开发 |
| 方案 A 共享 Schema 变更 | V8 迁移 + `CycleTemplateEntity` / `AiTemplateGenerationService` / `CycleTemplateDetailResponse`（支撑预填） | 待开发 |

### 2.3 明确不做（本版）

- 独立的「下一周期」历史列表接口（NC3 后续补）
- 周期总结自动触发下一周期（人工点击发起）
- 直接启用模板（始终 draft）
- 改造 `template_generation` / `cycle_summary` 的现有契约

---

## 三、数据库设计

### 3.1 本场景直接涉及的表

| 表名 | 角色 |
|------|------|
| `ai_task_records` | AI 异步任务主表（本场景新增 `task_type = 'next_cycle_generation'` 记录，`related_entity_type = 'cycle_run'`） |
| `cycle_template_versions` | 生成草稿的来源追溯落点（`source_type = 'ai_generated'`，`source_task_id = ai_task_records.id`） |
| `cycle_templates` / `cycle_template_days` / `cycle_day_exercises` / `cycle_day_exercise_items` / `cycle_day_exercise_item_metrics` | 生成 draft 模板落库 |
| `cycle_runs` | 上轮数据来源（`aggregatedAnalysis` / `sessionsDetail` / `versionSnapshot`） |
| `users` / `user_profiles` / `user_current_body_metrics` | 权限判断、档案与身体指标上下文 |

> 本场景**不新增任何表**。上轮周期总结直接读取 `ai_task_records.result_json`，不落新表。
> 但为支撑 next_cycle 预填，需对既有 `cycle_templates` 做一次**非破坏式**加列（方案 A，见 3.2 新增迁移 `V8__ai_next_cycle_schema_upgrade.sql`）。

### 3.2 方案 A 共享 Schema 变更（预填支持）

主控已确认「方案 A」：将 AI 模板生成时的场景/有氧偏好持久化到 `cycle_templates`，供「根据上一周期生成下一周期」精确预填 `sceneType` / `includeCardio`。本变更是共享的，同时服务 `template_generation` 与 `next_cycle_generation` 两个场景。

#### 3.2.1 新增迁移 `V8__ai_next_cycle_schema_upgrade.sql`

已创建（`backend/src/main/resources/db/migration/V8__ai_next_cycle_schema_upgrade.sql`），内容：

```sql
ALTER TABLE cycle_templates
    ADD COLUMN scene_type VARCHAR(32) NULL COMMENT '生成场景类型(gym/home)' AFTER goal_type,
    ADD COLUMN include_cardio TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许有氧(1=是 0=否)' AFTER scene_type;
```

设计约束：

- `scene_type` 允许 NULL：手动模板或历史行无此概念，仅 AI 生成模板写入。
- `include_cardio` 默认 1（允许有氧），与现有前端默认一致。
- 非破坏式：仅新增两列，不影响既有数据与查询。

#### 3.2.2 `CycleTemplateEntity` 新增字段

`backend/src/main/java/com/dailyforge/modules/plan/infrastructure/persistence/entity/CycleTemplateEntity.java` 新增：

```java
private String sceneType;        // scene_type
private Boolean includeCardio;   // include_cardio
```

（`@TableField` 映射到 `scene_type` / `include_cardio`，遵循 MyBatis-Plus 驼峰映射约定。）

#### 3.2.3 `AiTemplateGenerationService` 写入点

`AiTemplateGenerationService.persistSuccessfulResult(...)` 创建 `CycleTemplateEntity` 时写入（沿用 §10 小重构后的底层 `persistTemplateDraft` 内）：

```java
CycleTemplateEntity template = new CycleTemplateEntity();
template.setUserId(task.getUserId());
template.setName(validatedResult.templateName());
template.setCycleLength(validatedResult.cycleLength());
template.setGoalType(request.goalType());
template.setSceneType(request.sceneType());            // 新增
template.setIncludeCardio(request.includeCardio());    // 新增
template.setStatus("draft");
templateMapper.insert(template);
```

> 注意：现有 `persistSuccessfulResult` 从 `TemplateGenerationRequest` 读 `request.goalType()`；方案 A 需同步取 `request.sceneType()` / `request.includeCardio()`。小重构后，`goalType / sceneType / includeCardio` 均作为参数传入底层方法，`template_generation` 传 `TemplateGenerationRequest` 的对应值，`next_cycle_generation` 传 `NextCycleGenerationRequest` 的对应值，两场景共用同一套写入。

#### 3.2.4 `CycleTemplateDetailResponse` 新增字段（plan 模块）

`backend/src/main/java/com/dailyforge/modules/plan/interfaces/vo/CycleTemplateDetailResponse.java`（record）新增两个字段：

```java
public record CycleTemplateDetailResponse(
        ...,
        @Schema(description = "Scene type", example = "gym") String sceneType,
        @Schema(description = "Whether cardio is allowed", example = "true") Boolean includeCardio,
        ...) {
}
```

该 VO 由 plan 模块组装，需在对应 Assembler/映射处回填 `entity.getSceneType()` / `entity.getIncludeCardio()`。

#### 3.2.5 如何支撑 next_cycle 预填

1. 上轮 `template_generation` 或 `next_cycle_generation` 生成并落库后，`cycle_templates` 已持久化 `scene_type` / `include_cardio`。
2. 用户发起「下一周期生成」时，前端通过该 cycleRun 关联模板的 `CycleTemplateDetailResponse`（或模板详情接口）读取上轮 `sceneType` / `goalType` / `cycleLength` / `includeCardio` 作预填（PRD §5.2：`sourceCycleRun` 对应模板的值）。
3. 后端 NC1 收到完整 `NextCycleGenerationRequest`（含预填可编辑后的字段），按 `request.sceneType()` / `request.includeCardio()` 落库到新一轮 draft，形成闭环。

### 3.3 `ai_task_records` 本场景写入约定

| 字段 | 约定 |
|------|------|
| `task_type` | `next_cycle_generation` |
| `client_request_id` | 幂等，复用 `uk_ai_task_records_user_task_request` |
| `related_entity_type` | `cycle_run` |
| `related_entity_id` | `sourceCycleRunId` |
| `prompt_version` | `aiCoachProperties.getNextCycleGenerationPromptVersion()` |
| `request_payload_json` | `NextCycleGenerationRequest` 序列化 |
| `result_json` | `TemplateGenerationTaskResultResponse`（draftTemplate + generationRationale） |
| `status` | `pending -> running -> succeeded / failed` |

### 3.4 `cycle_template_versions.source_task_id`

复用现有机制：AI 生成模板成功后，`source_type = 'ai_generated'`、`source_task_id = ai_task_records.id`。这样可从此模板反查回 `next_cycle_generation` 任务。

### 3.5 关系说明

```text
users
  -> ai_task_records (task_type = next_cycle_generation)
       -> cycle_template_versions (source_task_id)   # 生成的 draft 模板
       -> cycle_run (related_entity)                 # 上轮数据来源

cycle_run
  -> ai_task_records (task_type = cycle_summary, succeeded)  # 上轮总结，读 result_json
```

---

## 四、核心业务规则与状态机

### 4.1 权限规则

- 所有 `next_cycle_generation` 接口必须登录（`AuthSecurityUtils.getCurrentUserId()`）
- AI 权限判断复用 `AiCoachApplicationService.assertAiEnabled(user)`：
  - `aiCoachProperties.isEnabled()` 且 `apiKey/baseUrl/model` 非空
  - `platform_role = admin` 或 `account_tier ∈ {invited_ai, premium}`
- 无权限时：`capabilities.nextCycleGeneration.available=false`；NC1 直接拒绝并返回 `AI_FEATURE_NOT_AVAILABLE`（403）

### 4.2 必须有上轮周期总结（决策 2，硬性）

NC1 提交时按以下优先级定位上轮总结，取不到则拒绝：

1. 若 `sourceSummaryTaskId` 非空：校验该 task 属于当前用户、`task_type = cycle_summary`、关联 `cycle_run = sourceCycleRunId`、`status = succeeded`，并从其 `result_json` 读取总结。
2. 若为空：调用 `AiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(userId, "cycle_summary", "cycle_run", sourceCycleRunId)` 取最近一条 succeeded 总结。
3. 无总结 → 抛 `AI_CYCLE_SUMMARY_REQUIRED`（409，提示「请先生成该周期的 AI 总结」）。

> 注意：总结**存在性校验**放在 NC1 提交阶段（同步拒绝，避免白白创建任务）；总结**详情读取**放在 `NextCycleGenerationContextBuilder` 组装上下文阶段（异步执行时）。

### 4.3 输入信号优先级（高 → 低）

按 PRD §6.1，Prompt 中必须显式声明以下优先级：

1. **用户表单意图**（`request.additionalRequirements`）——当下明确意图，严格遵循
2. **`injuryNotes`（伤病注意事项）**——常驻健康约束，作硬限制；与 1 冲突时健康优先并写入 warnings
3. **上轮 AI 周期总结**（`previousCycleSummary.nextCycleSuggestions` 等）——指导调整方向
4. **上轮实际表现**（该 cycleRun sessions 聚合 `aggregatedAnalysis` / `sessionsDetail`）——作渐进基准与上限
5. 个人资料 / 请求默认值兜底

### 4.4 关键规则

- **必须有上轮总结**才可生成（见 4.2）
- **上轮表现取该 cycleRun 自身 sessions 聚合**，不使用「最近 5 次」（PRD 决策 4）
- 延续上轮模板结构：保留有效动作/排期，调整问题项，吸收下轮建议
- 渐进负荷：基于上轮实际表现推进；对「降强度 / 恢复 / 避开某动作」类意图，上轮表现**只作上限封顶**，绝不当目标抬高
- `cycleLength` 必须等于 `request.cycleLength`（与模板生成一致）
- 生成产物始终为 `draft`

### 4.5 AI 任务状态机

完全复用现有状态机：`pending -> running -> succeeded / failed`。由 `AiTaskExecutor.execute(taskId)` 统一驱动，无需新状态。

### 4.6 输出校验与 JSON 修复

- `NextCycleGenerationExecutor` 复用 `AiOutputValidationDomainService.validateTemplateGeneration(json, request)`（因为输出 schema 与模板生成完全相同）
- 复用 `AiJsonRepairService` 两轮修复流程与 `templateGenerationSchemaDescription()`

---

## 五、API 设计

### 5.1 Base Path 与鉴权

- 外部访问前缀：`/api/ai-coach`（Controller 映射 `/ai-coach`）
- 统一返回 `ApiResponse<T>`，统一 Bearer Token 鉴权

### 5.2 接口总览

| 编号 | 方法 | 路径 | 作用 |
|------|------|------|------|
| NC1 | POST | `/ai-coach/next-cycle-generations` | 提交下一周期模板生成任务 |
| NC2 | GET | `/ai-coach/next-cycle-generations/{taskId}` | 查询任务结果 |
| CAP | GET | `/ai-coach/capabilities` | 能力摘要（新增 `nextCycleGeneration` 字段，向后兼容） |

### 5.3 NC1 提交下一周期模板生成任务

**请求体**：`NextCycleGenerationRequest`

**核心实现步骤**：
1. 鉴权 → 取 `userId`
2. `assertAiEnabled(user)`（403）
3. 校验 `sceneType / goalType / cycleLength / includeCardio / additionalRequirements`（400，复用 `validateSceneType / validateGoalType`）
4. `requireCompletedCycleRun(userId, sourceCycleRunId)`：
   - 不存在 / 非本人 → 404 `RESOURCE_NOT_FOUND`
   - 未完成 → 409 `AI_CYCLE_RUN_NOT_COMPLETED`
5. `assertTemplateGenerationReady(profile, metrics)`（400 `AI_REQUIRED_PROFILE_MISSING` / `AI_REQUIRED_BODY_METRIC_MISSING`，与模板生成一致）
6. **上轮总结存在性校验**（4.2）→ 无总结抛 409 `AI_CYCLE_SUMMARY_REQUIRED`
7. `clientRequestId` 幂等：`findExistingTask(userId, "next_cycle_generation", clientRequestId)` → 命中直接返回已受理
8. `buildTaskRecord(userId, "next_cycle_generation", clientRequestId, nextCycleGenerationPromptVersion, request, "cycle_run", sourceCycleRunId)`
9. `aiTaskRecordMapper.insert(task)`，捕获 `DuplicateKeyException` 兜底幂等
10. `scheduleTaskAfterCommit(task.getId())` → 返回 `AiAsyncTaskAcceptedResponse`

**事务边界**：方法整体 `@Transactional`（创建任务 + 提交执行信号同一事务），与现有 `submitTemplateGeneration` / `submitCycleSummary` 一致。

### 5.4 NC2 查询任务结果

**核心实现步骤**：
1. 鉴权
2. `requireTask(taskId, userId, "next_cycle_generation")` → 不存在/类型不符/越权 → 404 `AI_TASK_NOT_FOUND`
3. 加载 `latestToolCall`
4. 反序列化 `requestPayloadJson` 为 `NextCycleGenerationRequest`（回显 requestSnapshot）
5. `succeeded` 时反序列化 `result_json` 为 `TemplateGenerationTaskResultResponse`
6. 经 `AiCoachAssembler.toTaskDetailResponse(...)` 返回 `AiTaskDetailResponse<TemplateGenerationTaskResultResponse>`

### 5.5 CAP 能力字段

`getCapabilities()` 在现有 `templateGeneration` / `cycleSummary` 之外**追加** `nextCycleGeneration` 字段（`AiCoachCapabilitiesResponse` 新增成员，旧字段不动，向后兼容）。

| 字段 | 类型 | 计算逻辑 |
|------|------|------|
| `available` | boolean | `aiEnabled` |
| `ready` | boolean | `aiEnabled` 且存在已完成 cycleRun 且其已有 succeeded 周期总结 |
| `latestCompletedCycleRunId` | number \| null | 最新已完成 cycleRun id（复用 `selectLatestCompletedRun`） |
| `latestCompletedAt` | string \| null | 最新已完成时间 |
| `missingReason` | string \| null | 不可就绪原因：`ai_not_available` / `no_completed_cycle` / `no_cycle_summary` / `null` |

---

## 六、安全设计

### 6.1 认证链路

`next_cycle_generation` 接口自然走现有 JWT 鉴权链路（`SecurityConfig` 已拦截，无需改放行规则）。

### 6.2 身份获取

统一通过 `AuthSecurityUtils.getCurrentUserId()`；Controller 不解析 JWT Claims，不接受前端传 userId。

### 6.3 数据归属控制

必须校验归属：

- `sourceCycleRunId` 属于当前用户（`requireCompletedCycleRun` 已按 `userId + id` 过滤）
- `sourceSummaryTaskId` 属于当前用户且关联该 cycleRun
- 生成的 draft 模板归当前用户（复用 `AiTemplateGenerationService`，它从 `task.getUserId()` 写入）

### 6.4 工具白名单

`next_cycle_generation` 场景工具白名单（`AiToolRegistry.getAllowedToolNames` 需新增分支）：

```text
get_user_profile_context
get_user_current_body_metrics_context
get_template_generation_constraints
search_candidate_exercises
get_exercise_detail
get_cycle_run_aggregated_analysis        # 上轮实际表现（新增给本场景）
get_cycle_run_sessions_detail            # 上轮逐 session 明细（新增给本场景）
```

> 说明：本场景在模板生成基础工具之上，为模型增加 `get_cycle_run_aggregated_analysis` 与 `get_cycle_run_sessions_detail` 两个**只读**上轮表现工具（Handler Bean 在 `AiCoachToolConfig` 已存在，只需在 `AiToolRegistry` 白名单放行）。

禁止工具（沿用现有约束）：一切写操作工具。

### 6.5 日志脱敏策略

沿用 `ai_coach_DDD.md` §6.5 / §16.4：

- 允许记录：`taskId / userId / taskType / provider / model / promptVersion / toolName / roundNo / repairAttempt / latencyMs / finalStatus / stage`
- 禁止记录：完整 access token、完整原始 prompt、完整原始模型全文输出、API Key、完整上游响应体、完整工具调用参数原文、用户长文本备注全量拼接

---

## 七、错误码设计

### 7.1 复用现有错误码

- `UNAUTHORIZED` / `FORBIDDEN` / `INVALID_ARGUMENT` / `RESOURCE_NOT_FOUND` / `INTERNAL_SERVER_ERROR`
- `AI_FEATURE_NOT_AVAILABLE`（403）
- `AI_REQUIRED_PROFILE_MISSING`（400）/ `AI_REQUIRED_BODY_METRIC_MISSING`（400）
- `AI_CYCLE_RUN_NOT_COMPLETED`（409）
- `AI_TASK_NOT_FOUND`（404）
- `AI_OUTPUT_INVALID`（500）/ `AI_SERVICE_TIMEOUT`（504）/ `AI_SERVICE_UNAVAILABLE`（503）

### 7.2 建议新增错误码

| 错误码 | HTTP 状态 | 含义 |
|------|------|------|
| `AI_CYCLE_SUMMARY_REQUIRED` | 409 | 生成下一周期前需先有该周期的 succeeded 周期总结 |

> `ErrorCode` 枚举需新增该常量（参考现有 `AI_CYCLE_RUN_NOT_COMPLETED` 的声明风格：`AI_CYCLE_SUMMARY_REQUIRED("AI_CYCLE_SUMMARY_REQUIRED", "...", HttpStatus.CONFLICT)`）。

### 7.3 NC1 校验顺序与错误汇总

| 顺序 | 场景 | 错误 |
|------|------|------|
| 1 | AI 未开通 | 403 `AI_FEATURE_NOT_AVAILABLE` |
| 2 | `sourceCycleRunId` 不存在 / 非本人 | 404 `RESOURCE_NOT_FOUND` |
| 3 | `sourceCycleRun` 未完成 | 409 `AI_CYCLE_RUN_NOT_COMPLETED` |
| 4 | 该 cycleRun 无 succeeded 周期总结 | 409 `AI_CYCLE_SUMMARY_REQUIRED` |
| 5 | profile / 身体指标缺失 | 400 `AI_REQUIRED_PROFILE_MISSING` / `AI_REQUIRED_BODY_METRIC_MISSING` |
| 6 | 参数非法（sceneType/goalType/cycleLength） | 400 `INVALID_ARGUMENT` |

---

## 八、Java 代码结构设计

### 8.1 目标包结构与新增类

```text
com.dailyforge.modules.aicoach
├─ application
│  ├─ assembler
│  │  └─ AiCoachAssembler.java                 # 复用，无需改动（toTaskDetailResponse 已泛型化）
│  └─ service
│     ├─ AiCoachApplicationService.java        # 小改：submitNextCycleGeneration / getNextCycleGeneration / getCapabilities 增字段
│     ├─ AiTemplateGenerationService.java      # 小重构：persistSuccessfulResult 参数化 taskType
│     └─ AiCoachToolSupportService.java        # 小改：新增 getLatestCycleSummaryResult(...)
├─ domain
│  ├─ model
│  │  └─ TemplateGenerationValidatedResult.java  # 复用（输出结构同模板生成）
│  └─ service
│     └─ AiOutputValidationDomainService.java    # 复用 validateTemplateGeneration
├─ infrastructure
│  └─ ai
│     ├─ AiCoachProperties.java                # 小改：新增 nextCycleGenerationPromptVersion
│     ├─ context
│     │  ├─ NextCycleGenerationContext.java        # 新增
│     │  └─ NextCycleGenerationContextBuilder.java # 新增
│     ├─ prompt
│     │  └─ NextCycleGenerationPromptBuilder.java  # 新增
│     ├─ tool
│     │  └─ AiToolRegistry.java                  # 小改：getAllowedToolNames 新增分支
│     └─ executor
│        ├─ NextCycleGenerationExecutor.java     # 新增（taskType = "next_cycle_generation"）
│        └─ AiScenarioExecutor.java              # 复用接口
└─ interfaces
   ├─ dto
   │  └─ NextCycleGenerationRequest.java       # 新增
   ├─ rest
   │  └─ AiCoachController.java                # 小改：新增 NC1 / NC2 端点
   └─ vo
      ├─ AiCoachCapabilitiesResponse.java      # 小改：新增 NextCycleGenerationCapability 字段
      └─ TemplateGenerationTaskResultResponse.java  # 复用（结果 VO）
```

### 8.2 核心类职责

| 类名 | 新增/复用 | 职责 |
|------|:---:|------|
| `NextCycleGenerationRequest` | 新增 | NC1 请求 DTO（见 8.3） |
| `NextCycleGenerationContext` | 新增 | 场景上下文 record（见 8.4） |
| `NextCycleGenerationContextBuilder` | 新增 | 组装上下文：档案/身体指标 + 上轮总结 + 上轮表现 + 约束 |
| `NextCycleGenerationPromptBuilder` | 新增 | 构建 System / User Prompt（含信号优先级与输出 schema） |
| `NextCycleGenerationExecutor` | 新增 | 异步执行：读 request → 组上下文 → 调模型 → 校验/修复 → 复用 draft 落库 |
| `AiCoachApplicationService` | 小改 | 新增 `submitNextCycleGeneration` / `getNextCycleGeneration`；`getCapabilities` 增字段 |
| `AiTemplateGenerationService` | 小重构 | `persistSuccessfulResult` 参数化 `taskType`，供两场景共用 |
| `AiCoachToolSupportService` | 小改 | 新增读取上轮总结结果的方法 |
| `AiCoachProperties` | 小改 | 新增 `nextCycleGenerationPromptVersion` |
| `ErrorCode` | 小改 | 新增 `AI_CYCLE_SUMMARY_REQUIRED` |
| `AiCoachController` | 小改 | 新增 NC1 / NC2 端点 |
| `AiCoachCapabilitiesResponse` | 小改 | 新增 `NextCycleGenerationCapability` |
| `AiToolRegistry` | 小改 | 白名单新增 `next_cycle_generation` 分支 |

### 8.3 Request DTO：`NextCycleGenerationRequest`

```java
@Schema(description = "AI next-cycle template generation request")
public record NextCycleGenerationRequest(
        @Size(max = 64) String clientRequestId,
        @NotNull @Min(1) Long sourceCycleRunId,
        @Min(1) Long sourceSummaryTaskId,          // 可选
        @NotBlank @Size(max = 32) String sceneType,  // gym / home
        @NotBlank @Size(max = 32) String goalType,   // fat_loss / muscle_gain / health_maintenance
        @NotNull @Min(1) @Max(7) Integer cycleLength,
        @NotNull Boolean includeCardio,
        @Size(max = 500) String additionalRequirements) {
}
```

> 说明：`additionalRequirements` 只属于请求传输层语义，只进 Prompt 上下文，不写入模板业务表字段（沿用现有约定）。

### 8.4 Context record：`NextCycleGenerationContext`

```java
public record NextCycleGenerationContext(
        Long userId,
        Long sourceCycleRunId,
        NextCycleGenerationRequest request,
        Map<String, Object> userProfile,
        Map<String, Object> currentBodyMetrics,
        Map<String, Object> previousCycleSummary,      // 上轮总结：executionOverview/strengths/issues/causeAnalysis/nextCycleSuggestions/risks
        Map<String, Object> previousCycleAggregated,   // 该 cycleRun aggregatedAnalysis
        Map<String, Object> previousCycleSessions,     // 该 cycleRun sessionsDetail
        Map<String, Object> previousVersionSnapshot,   // 该 cycleRun versionSnapshot（上轮模板结构）
        Map<String, Object> templateConstraints) {
}
```

### 8.5 DTO / VO 清单（本场景）

- Request：`NextCycleGenerationRequest`
- Response VO：
  - `AiAsyncTaskAcceptedResponse`（复用）
  - `AiTaskDetailResponse<TemplateGenerationTaskResultResponse>`（复用）
  - `TemplateGenerationTaskResultResponse`（复用，draftTemplate + generationRationale）
  - `AiCoachCapabilitiesResponse` 新增 `NextCycleGenerationCapability` 内嵌 record

### 8.6 Assembler / Mapping 约定

- 不暴露 Entity；`AiCoachAssembler.toTaskDetailResponse(...)` 已泛型化，`result` 传 `TemplateGenerationTaskResultResponse` 即可复用
- `AiCoachCapabilitiesResponse` 新增字段需同步 `AiCoachAssembler` 或 `AiCoachApplicationService.getCapabilities()` 组装

### 8.7 Swagger / OpenAPI 注解约定

- Controller：`@Tag(name = "AI Coach")`、`@Operation`、`@ApiResponses`、`@SecurityRequirement(name = "bearerAuth")`
- NC1：`@ApiResponses` 覆盖 400 / 401 / 403 / 404 / 409 / 503 / 504，其中 409 描述「cycle run not completed or cycle summary required」
- DTO / VO：统一 `@Schema`，对 `sourceCycleRunId / sourceSummaryTaskId / sceneType / goalType / cycleLength / includeCardio / additionalRequirements / taskType / taskStatus` 给出示例与枚举说明

### 8.8 Debug 日志设计

沿用现有 `AiTaskExecutor` 的日志结构（taskId/taskType/code/message）。本场景在 `NextCycleGenerationContextBuilder` 额外允许记录：

- `sourceCycleRunId`、`sourceSummaryTaskId`、定位到的总结 `taskId`（用于排查"用了哪条总结"）

禁止输出：总结 `result_json` 全文、原始长文本备注全量。

---

## 九、AI 链路与工具设计

### 9.1 执行流程

```text
AiTaskExecutor.execute(taskId)
  -> 按 taskType 路由到 NextCycleGenerationExecutor
  -> 读 requestPayloadJson -> NextCycleGenerationRequest
  -> contextBuilder.build(userId, request, sourceCycleRunId)
       - userProfile / currentBodyMetrics（复用 toolSupport）
       - 定位上轮总结并读取 result_json -> previousCycleSummary
       - 该 cycleRun 的 aggregatedAnalysis / sessionsDetail / versionSnapshot
       - templateConstraints
  -> aiConversationService.generateJson(systemPrompt, userPrompt, toolDefinitions, repair 支持)
  -> validateWithRepair（复用 validateTemplateGeneration + AiJsonRepairService）
  -> aiTemplateGenerationService.persistSuccessfulResult(taskId, inputSummaryJson, validatedResult)  // 复用 draft 落库
```

### 9.2 Prompt 分层

1. **System Prompt**：角色、边界、只返回 JSON 刚性约束、所有用户可见文本默认中文、Prompt version
2. **Scenario Prompt**：任务说明、输出 schema、**信号优先级（§4.3）**、渐进负荷规则、`cycleLength` 必须等于请求值、上轮表现只作上限封顶的约束
3. **Context Payload**：`NextCycleGenerationContext` 序列化

### 9.3 上轮总结读取（详情）

`AiCoachToolSupportService` 新增方法（供 `NextCycleGenerationContextBuilder` 调用）：

```java
CycleSummaryTaskResultResponse getLatestCycleSummaryResult(Long userId, Long sourceCycleRunId, Long sourceSummaryTaskId) {
    AiTaskRecordEntity task = sourceSummaryTaskId != null
        ? aiTaskRecordMapper.selectByIdAndUserIdAndTaskType(sourceSummaryTaskId, userId, "cycle_summary")
        : aiTaskRecordMapper.selectLatestSucceededByUserIdAndTaskTypeAndRelatedEntity(
                userId, "cycle_summary", "cycle_run", sourceCycleRunId);
    // 校验 task != null、status == succeeded、task.relatedEntityId == sourceCycleRunId
    // 否则抛 BusinessException(AI_CYCLE_SUMMARY_REQUIRED)
    // 反序列化 task.resultJson -> CycleSummaryTaskResultResponse
    return ...;
}
```

> 说明：`selectByIdAndUserIdAndTaskType(sourceSummaryTaskId, userId, "cycle_summary")` 已存在。若指定 `sourceSummaryTaskId` 但状态非 succeeded 或关联 cycleRun 不符，同样按「无总结」处理 → `AI_CYCLE_SUMMARY_REQUIRED`。

### 9.4 上下文数据来源汇总

| Context 字段 | 数据来源 |
|------|------|
| `userProfile` | `AiCoachToolSupportService.getUserProfileContext(userId)` |
| `currentBodyMetrics` | `AiCoachToolSupportService.getUserCurrentBodyMetricsContext(userId)` |
| `previousCycleSummary` | `AiCoachToolSupportService.getLatestCycleSummaryResult(...)` 反序列化 result_json |
| `previousCycleAggregated` | `AiCoachToolSupportService.getCycleRunAggregatedAnalysis(userId, sourceCycleRunId)` |
| `previousCycleSessions` | `AiCoachToolSupportService.getCycleRunSessionsDetail(userId, sourceCycleRunId)` |
| `previousVersionSnapshot` | `CycleRunMapper.selectById` → `cycleTemplateVersionDomainService.loadVersionSnapshot(cycleRun.getTemplateVersionId())` → `versionSnapshotToSummary(...)`（复用 CycleSummaryContextBuilder 的组装手法） |
| `templateConstraints` | `AiCoachToolSupportService.getTemplateGenerationConstraints()` |

---

## 十、事务、一致性与幂等

### 10.1 事务边界

| 方法 | 是否事务 | 说明 |
|------|:---:|------|
| `getCapabilities` | 否 | 只读 |
| `submitNextCycleGeneration` | 是 | 创建任务 + 提交执行信号同一事务 |
| `getNextCycleGeneration` | 否 | 只读 |
| `AiTemplateGenerationService.persistSuccessfulResult` | 是 | draft 模板 + 版本 + result_json + 状态更新同一事务（复用） |

### 10.2 幂等策略

- 锚点：`uk_ai_task_records_user_task_request`（`user_id + task_type + client_request_id`）
- `submitNextCycleGeneration` 复用 `AiCoachApplicationService.findExistingTask`，重复提交返回已有任务受理信息；插入捕获 `DuplicateKeyException` 兜底

### 10.3 并发控制

- 同一用户重复点击创建同一 `next_cycle_generation` 任务 → 唯一索引兜底
- 同一 `cycleRunId` 上轮总结与本次生成并发 → 定位总结用「最近一条 succeeded」语义，前端 `capabilities.ready` 只读消费，不从前端缓存推导状态
- draft 模板写入单事务保证 `cycle_templates` / `cycle_template_versions` / `ai_task_records` 一致

### 10.4 结果一致性

`next_cycle_generation` 成功的定义（与模板生成一致）：

1. AI 任务结果结构通过校验
2. `cycle_template*` draft 写入成功
3. `cycle_template_versions.source_task_id` 回写成功
4. `ai_task_records.status = succeeded`

任一失败 → 整体回滚、任务置 `failed`（由 `AiTaskExecutor` 收敛）。

---

## 十一、配置与扩展点

### 11.1 新增配置项

`AiCoachProperties`（`dailyforge.ai` 前缀）新增：

```yaml
dailyforge:
  ai:
    next-cycle-generation-prompt-version: next_cycle_generation_v1
```

对应 Java 字段：

```java
private String nextCycleGenerationPromptVersion = "next_cycle_generation_v1";
// + getter/setter
```

### 11.2 外部依赖

与现有 `ai_coach` 场景一致：Spring AI、OpenAI 兼容模型客户端、DeepSeek、MySQL。无新增外部依赖。

### 11.3 后续扩展点

- 独立「下一周期」历史列表（NC3）
- 周期总结自动触发下一周期
- 结果版本对比 / Prompt A/B 实验（复用 `prompt_version` 字段）

---

## 十二、测试设计

### 12.1 单元测试重点

- `AiCoachProperties`：`nextCycleGenerationPromptVersion` 默认值
- `NextCycleGenerationContextBuilderTest`：
  - `sourceSummaryTaskId` 指定时取指定总结
  - 为空时自动取最近 succeeded 总结
  - 无总结抛 `AI_CYCLE_SUMMARY_REQUIRED`
  - 上轮表现三块数据（aggregated/sessions/versionSnapshot）正确填充
- `NextCycleGenerationPromptBuilderTest`：信号优先级顺序、`cycleLength` 硬约束、中文默认约束
- `AiOutputValidationDomainService`：`validateTemplateGeneration` 复用（无需新增）

### 12.2 集成测试重点

- NC1 成功创建 `next_cycle_generation` 任务并返回受理
- NC1 无上轮总结 → 409 `AI_CYCLE_SUMMARY_REQUIRED`
- NC1 `sourceCycleRun` 未完成 → 409
- NC1 幂等复用已有任务
- NC2 查询成功任务返回 draftTemplate + generationRationale
- NC2 越权 / 类型不符 → 404 `AI_TASK_NOT_FOUND`
- CAP 返回 `nextCycleGeneration` 字段（available/ready/missingReason）
- 生成的 draft 进入模板列表且状态为 draft

### 12.3 安全测试重点

- 未登录 → 401
- 无 AI 权限提交 → 403
- 不能查询他人任务 / 他人 cycleRun

---

## 十三、实施顺序建议

1. `ErrorCode` 新增 `AI_CYCLE_SUMMARY_REQUIRED`
2. `AiCoachProperties` 新增 `nextCycleGenerationPromptVersion`
3. 新增 `NextCycleGenerationRequest` DTO
4. `AiToolRegistry.getAllowedToolNames` 新增 `next_cycle_generation` 分支（放行 7 个只读工具）
5. `AiCoachToolSupportService` 新增 `getLatestCycleSummaryResult(...)`
6. `AiTemplateGenerationService.persistSuccessfulResult` 小重构（参数化 taskType）
7. 新增 `NextCycleGenerationContext` + `NextCycleGenerationContextBuilder`
8. 新增 `NextCycleGenerationPromptBuilder`
9. 新增 `NextCycleGenerationExecutor`
10. `AiCoachApplicationService` 新增 `submitNextCycleGeneration` / `getNextCycleGeneration`，`getCapabilities` 增字段
11. `AiCoachCapabilitiesResponse` 新增 `NextCycleGenerationCapability`
12. `AiCoachController` 新增 NC1 / NC2 端点
13. 补 Swagger、日志、测试

---

## 十四、当前发现的实现层冲突与缺口

1. **`AiTemplateGenerationService` 的 `requireTask(taskId, "template_generation")` 硬编码 taskType**：
   - 现状：`persistSuccessfulResult` 内部 `requireTask(taskId, "template_generation")` 且从 `requestPayloadJson` 反序列化为 `TemplateGenerationRequest` 取 `goalType`。
   - 方案：小重构，新增私有/受保护方法 `persistTemplateDraft(Long taskId, String taskType, String goalType, String sceneType, Boolean includeCardio, String inputSummaryJson, TemplateGenerationValidatedResult validatedResult)`，`persistSuccessfulResult` 保留为 `template_generation` 薄封装；`next_cycle_generation` 调用同一底层方法（`goalType / sceneType / includeCardio` 从 `NextCycleGenerationRequest` 取）。底层方法按方案 A 写入 `cycle_templates.scene_type / include_cardio`。这样两场景共用 draft 落库 + source_task_id 回写 + result_json 写入，不改外层契约。

2. **`AiCoachCapabilitiesResponse` 是 record，新增字段需同步**：
   - 现为 `record AiCoachCapabilitiesResponse(...)`，`getCapabilities()` 构造处需追加 `nextCycleGeneration` 实参。属小改，向后兼容（前端旧字段不变）。

3. **`NextCycleGenerationCapability` 语义**：`available` 只表示 AI 开通；`ready` 需额外判断「已有 completed cycleRun + 其已有 succeeded 总结」，与 `cycleSummary.ready` 不同。注意实现时避免只复用 `selectLatestCompletedRun` 就断言 ready。

4. **无独立历史接口**：本版不做 NC3，草稿从训练模板列表查看（符合 PRD 范围）。

---

## 十五、验收标准

1. 两个前端入口出现（周期总结结果页底部、周期总结历史卡片旁）——前端职责，后端契约已就绪（`capabilities.nextCycleGeneration.ready` + NC1/NC2）。
2. 无上轮总结的 cycleRun，NC1 返回 409 `AI_CYCLE_SUMMARY_REQUIRED`，提示「先生成周期总结」。
3. 提交后生成 draft 模板：结构延续上轮、负荷基于上轮表现并体现 `nextCycleSuggestions`，`generationRationale` 诚实反映依据。
4. 表单预填上轮值且全部可编辑（前端职责；后端仅接收完整请求字段）。
5. 生成的 draft 进入模板列表，状态为 draft，不自动启用。
6. 本地与 CI：后端 `mvn test` 通过；契约联调校验通过（接口文档 ↔ Controller ↔ DTO/VO ↔ Swagger）。

---

## 十六、本轮改动文件清单（预期）

> 以下为按本文档落地实现后预计改动的文件（本轮仅产出 DDD 文档，不落地代码）。

### 新增

- `backend/src/main/java/com/dailyforge/modules/aicoach/interfaces/dto/NextCycleGenerationRequest.java`
- `backend/src/main/java/com/dailyforge/modules/aicoach/infrastructure/ai/context/NextCycleGenerationContext.java`
- `backend/src/main/java/com/dailyforge/modules/aicoach/infrastructure/ai/context/NextCycleGenerationContextBuilder.java`
- `backend/src/main/java/com/dailyforge/modules/aicoach/infrastructure/ai/prompt/NextCycleGenerationPromptBuilder.java`
- `backend/src/main/java/com/dailyforge/modules/aicoach/infrastructure/ai/executor/NextCycleGenerationExecutor.java`

### 新增（方案 A 共享 Schema 变更）

- `backend/src/main/resources/db/migration/V8__ai_next_cycle_schema_upgrade.sql`（已创建，非本 DDD 产出）

### 小改

- `backend/src/main/java/com/dailyforge/common/ErrorCode.java`（新增 `AI_CYCLE_SUMMARY_REQUIRED`）
- `backend/src/main/java/com/dailyforge/modules/aicoach/infrastructure/ai/AiCoachProperties.java`（新增 `nextCycleGenerationPromptVersion`）
- `backend/src/main/java/com/dailyforge/modules/aicoach/infrastructure/ai/tool/AiToolRegistry.java`（白名单新增分支）
- `backend/src/main/java/com/dailyforge/modules/aicoach/application/service/AiCoachToolSupportService.java`（新增 `getLatestCycleSummaryResult`）
- `backend/src/main/java/com/dailyforge/modules/aicoach/application/service/AiTemplateGenerationService.java`（小重构参数化 taskType + 写入 `sceneType` / `includeCardio`）
- `backend/src/main/java/com/dailyforge/modules/aicoach/application/service/AiCoachApplicationService.java`（新增 NC1/NC2，capabilities 增字段）
- `backend/src/main/java/com/dailyforge/modules/aicoach/interfaces/vo/AiCoachCapabilitiesResponse.java`（新增 `NextCycleGenerationCapability`）
- `backend/src/main/java/com/dailyforge/modules/aicoach/interfaces/rest/AiCoachController.java`（新增 NC1 / NC2 端点）

### 小改（方案 A 共享 Schema 变更）

- `backend/src/main/java/com/dailyforge/modules/plan/infrastructure/persistence/entity/CycleTemplateEntity.java`（新增 `sceneType` / `includeCardio` 字段）
- `backend/src/main/java/com/dailyforge/modules/plan/interfaces/vo/CycleTemplateDetailResponse.java`（新增 `sceneType` / `includeCardio` 字段）
- plan 模块对应 `CycleTemplateDetailResponse` 的组装处（回填两个新字段）

---

## 十七、结论

`next_cycle_generation` 不是一个新的独立 AI 链路，而是在 `ai_coach` 现有「模板生成」+「周期总结」两条链路上的一次组合式扩展。其关键技术收口点：

1. **复用 draft 落库**：通过 `AiTemplateGenerationService` 的小重构，让模板生成与下一周期生成共用同一套 draft 模板落库、版本追溯与 result_json 写入，避免复制落库逻辑。
2. **上轮总结读取**：`sourceSummaryTaskId` 优先，否则自动取该 cycleRun 最近 succeeded `cycle_summary` 的 `result_json`；无总结则在 NC1 同步拒绝（`AI_CYCLE_SUMMARY_REQUIRED`）。
3. **上轮表现取该 cycleRun 自身聚合**：`aggregatedAnalysis` + `sessionsDetail` + `versionSnapshot`，作为渐进基准与上限（PRD 决策 4）。
4. **输出同构**：与模板生成共用 `TemplateGenerationTaskResultResponse` 与 `validateTemplateGeneration`，前端结果展示可直接复用。
5. **复用提交模式**：任务创建、幂等、异步调度、状态机、异常收敛全部沿用现有 `AiCoachApplicationService` / `AiTaskExecutor` 骨架。
