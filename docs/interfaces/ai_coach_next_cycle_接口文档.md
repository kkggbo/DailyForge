# DailyForge AI 下一周期模板生成 接口文档

> 版本：v0.1
> 日期：2026-08-25
> 状态：待评审
> 关联 PRD：`docs/prd/ai_coach_next_cycle_PRD.md`
> 模块名称：`ai_coach`

---

## 1. 文档范围

本文档定义 `next_cycle_generation`（根据上一周期表现 + 上轮 AI 周期总结，生成下一周期模板草稿）的后端接口契约。它复用 `ai_coach_接口文档.md` 的公共约定（鉴权、异步任务、幂等、轮询、历史），仅新增本场景的请求/响应与能力字段。

本文档假设读者已阅读：
- `docs/interfaces/ai_coach_接口文档.md`（公共类型与通用约定）
- `docs/prd/ai_coach_next_cycle_PRD.md`（业务规则）

---

## 2. 通用约定

本场景沿用 `ai_coach_接口文档.md` 的下列约定，不再重复展开：

- **路由与鉴权**：均需 `Authorization: Bearer <token>`。
- **异步任务语义**：提交接口立即返回受理响应，结果异步生成，前端轮询查询。
- **请求去重**：`clientRequestId` 幂等，重复提交返回已有任务。
- **统一响应体**：所有响应包在 `ApiResponse<T>` 中。

**新增任务类型**：`next_cycle_generation`（区别于 `template_generation`、`cycle_summary`）。

---

## 3. 接口列表

| 编号 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| NC1 | POST | `/ai-coach/next-cycle-generations` | 提交下一周期模板生成任务 |
| NC2 | GET | `/ai-coach/next-cycle-generations/{taskId}` | 查询任务结果 |
| CAP | GET | `/ai-coach/capabilities` | 能力摘要（新增 `nextCycleGeneration` 字段） |

> 独立历史列表接口本版不做（见 PRD §9 范围），草稿产物从训练模板列表查看。

---

## 4. 公共数据结构

### 4.1 下一周期生成请求 `NextCycleGenerationRequest`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `clientRequestId` | string | 否 | 幂等 id，≤64 |
| `sourceCycleRunId` | number | 是 | 上一已完成 cycle run id，≥1 |
| `sourceSummaryTaskId` | number | 否 | 上轮周期总结任务 id；为空时后端自动取该 cycleRun 最近一次 succeeded 的 `cycle_summary` |
| `sceneType` | string | 是 | `gym` / `home` |
| `goalType` | string | 是 | `fat_loss` / `muscle_gain` / `health_maintenance` |
| `cycleLength` | number | 是 | 1~7 |
| `includeCardio` | boolean | 是 | 是否允许有氧 |
| `additionalRequirements` | string | 否 | 用户当下意图（一等信号），≤500 |

示例：
```json
{
  "clientRequestId": "1f5c0d6e-a2f9-4d3d-8b11-577a5906b651",
  "sourceCycleRunId": 1201,
  "sourceSummaryTaskId": 88,
  "sceneType": "gym",
  "goalType": "muscle_gain",
  "cycleLength": 4,
  "includeCardio": true,
  "additionalRequirements": "本周工作量大，想适当降低强度。"
}
```

### 4.2 受理响应

复用 `AiAsyncTaskAcceptedResponse`（`ai_coach_接口文档.md` §4.2），其中 `taskType = "next_cycle_generation"`。

### 4.3 任务结果

复用 `TemplateGenerationTaskResult`（`ai_coach_接口文档.md` §4.7），含 `draftTemplate`（draft 状态草稿）与 `generationRationale`。

### 4.4 能力字段 `NextCycleGenerationCapability`

`GET /ai-coach/capabilities` 的响应体新增字段 `nextCycleGeneration`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `available` | boolean | AI 功能可用（与 `templateGeneration`/`cycleSummary` 语义一致，仅表示账号具备该能力） |
| `ready` | boolean | 存在已完成 cycle run 且其已有 succeeded 周期总结（前端据此门控入口） |
| `latestCompletedCycleRunId` | number \| null | 最新已完成 cycle run id |
| `latestCompletedAt` | string \| null | 最新已完成时间 |
| `missingReason` | string \| null | 不可就绪的原因（`ai_not_available` / `no_completed_cycle` / `no_cycle_summary`） |

---

## 5. 接口详情

### 5.1 NC1 提交下一周期模板生成任务

- **POST** `/ai-coach/next-cycle-generations`
- **请求体**：`NextCycleGenerationRequest`
- **成功响应 200**：`ApiResponse<AiAsyncTaskAcceptedResponse>`
- **校验顺序与错误**：
  - AI 未开通 → `AI_FEATURE_NOT_AVAILABLE`（403）
  - profile / 身体指标缺失 → `AI_REQUIRED_PROFILE_MISSING` / `AI_REQUIRED_BODY_METRIC_MISSING`（400）
  - 参数非法（sceneType/goalType/cycleLength）→ `INVALID_ARGUMENT`（400）
  - 同 `clientRequestId` 已有任务 → 直接返回已受理任务（幂等）
  - `sourceCycleRunId` 不存在或非本人 → `RESOURCE_NOT_FOUND`（404）
  - `sourceCycleRun` 未完成 → `AI_CYCLE_RUN_NOT_COMPLETED`（409）
  - **该 cycleRun 无 succeeded 周期总结** → `AI_CYCLE_SUMMARY_REQUIRED`（409，提示「请先生成该周期的 AI 总结」）

### 5.2 NC2 查询下一周期模板生成任务结果

- **GET** `/ai-coach/next-cycle-generations/{taskId}`
- **成功响应 200**：`ApiResponse<AiTaskDetailResponse<TemplateGenerationTaskResult>>`
- **错误**：任务不存在/非本人/类型不符 → `AI_TASK_NOT_FOUND`（404）

---

## 6. 推荐错误码

新增错误码（后端 `ErrorCode` 需新增）：

| 错误码 | 说明 |
| --- | --- |
| `AI_CYCLE_SUMMARY_REQUIRED` | 生成下一周期前需先有该周期的 succeeded 周期总结 |

其余复用 `ai_coach_接口文档.md` §6 的公共错误码。

---

## 7. 前端调用顺序建议

1. `GET /ai-coach/capabilities` → 读取 `nextCycleGeneration.ready` 决定入口是否可用。
2. 用户在周期总结结果页 / 周期总结历史卡片点击「生成下一周期模板」。
3. `POST /ai-coach/next-cycle-generations`（带 `sourceCycleRunId` + 预填可编辑的生成参数）。
4. 轮询 `GET /ai-coach/next-cycle-generations/{taskId}`，直到终态。
5. succeeded → 展示 `draftTemplate` + `generationRationale`（复用模板生成结果 UI）。

---

## 8. 变更说明

### 8.1 相对现有文档的新增

- 新增任务类型 `next_cycle_generation`。
- 新增请求 DTO `NextCycleGenerationRequest`、能力字段 `NextCycleGenerationCapability`、错误码 `AI_CYCLE_SUMMARY_REQUIRED`。
- 新增端点 NC1 / NC2；结果复用现有模板生成结果契约。

### 8.2 共享 Schema 变更（方案 A，预填支撑）

为实现「预填上轮 sceneType / includeCardio」，`cycle_templates` 增加两列（迁移 `V8__ai_next_cycle_schema_upgrade.sql`）：

| 列 | 类型 | 说明 |
| --- | --- | --- |
| `scene_type` | VARCHAR(32) NULL | 生成场景类型（gym/home）；手动/历史行为 NULL |
| `include_cardio` | TINYINT(1) NOT NULL DEFAULT 1 | 是否允许有氧 |

- AI 模板生成（`template_generation`）落库时写入这两列。
- `GET /cycle-templates/{id}`（`CycleTemplateDetailResponse`）新增 `sceneType`、`includeCardio` 字段，供 next_cycle 前端精确预填。

### 8.3 对旧文档的影响

- 不修改 `template_generation` / `cycle_summary` 的任何现有契约。
- `GET /ai-coach/capabilities` 响应体**新增字段** `nextCycleGeneration`（向后兼容，旧字段不变）。
- `cycle_templates` 新增两列、`CycleTemplateDetailResponse` 新增两字段（向后兼容）。
