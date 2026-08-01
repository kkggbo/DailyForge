# DailyForge AI Coach 模块接口文档

> 版本：v1.0  
> 更新时间：2026-07-31  
> 模块归属：`backend` 单体应用，建议代码包为 `com.dailyforge.modules.aicoach`  
> 文档状态：接口契约设计阶段，尚未落代码

---

## 1. 文档范围

本文档定义 DailyForge `ai_coach` 模块在 MVP 阶段的接口契约，覆盖：

- AI 能力可用状态与资料完整度检查
- AI 训练模板生成任务提交
- AI 训练模板生成结果查询
- AI 周期总结任务提交
- AI 周期总结结果查询

本文档以 [ai_coach_PRD.md](../prd/ai_coach_PRD.md) 为准；如与旧版 `cycle_template` 文档中的 AI 占位接口存在冲突，以本模块文档为准。

---

## 2. 通用约定

### 2.1 路由与鉴权

外部接口前缀：

- `/api/ai-coach`

后端 Controller 建议映射：

- `/ai-coach`

所有接口都要求登录态：

```http
Authorization: Bearer <accessToken>
```

统一返回包装：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

失败响应示例：

```json
{
  "code": "AI_FEATURE_NOT_AVAILABLE",
  "message": "ai feature is not available for current account",
  "data": null
}
```

### 2.2 异步任务语义

本模块两个核心动作都定义为异步任务：

- AI 训练模板生成
- AI 周期总结

原因：

- AI 处理时长不稳定
- 模型调用和结构校验可能较慢
- 需要保留任务状态与失败信息，便于前端轮询和问题排查

MVP 约定：

- 提交接口只负责“创建任务并返回任务 ID”
- 结果接口负责“查询任务状态与最终结果”
- 当前不提供 WebSocket / SSE 推送
- 当前不提供用户主动取消任务接口

### 2.3 任务状态

`taskStatus` 仅允许以下值：

- `pending`
- `running`
- `succeeded`
- `failed`

终态定义：

- `succeeded`
- `failed`

前端轮询约定：

- 收到 `pending` 或 `running` 时继续轮询
- 收到终态后停止轮询

### 2.4 任务类型

`taskType` 仅允许以下值：

- `template_generation`
- `cycle_summary`

### 2.5 资料缺失字段编码

前后端统一使用以下资料字段编码，不允许前端自行发明：

- `gender`
- `birthDate`
- `heightCm`
- `goalType`
- `trainingLevel`
- `currentWeightKg`

说明：

- 这些编码只用于 AI 场景的“资料完整度提示”
- 不直接等同于数据库字段名或前端表单 label

### 2.6 模板生成条件枚举

`sceneType`：

- `gym`
- `home`

`goalType`：

- `fat_loss`
- `muscle_gain`
- `health_maintenance`

### 2.7 AI 设计说明与模板本体分离

AI 模板生成结果中的：

- `draftTemplate`
- `generationRationale`

必须作为两个独立字段返回。

前端约定：

- 不得把 `generationRationale` 写回模板编辑请求体
- 不得把 `generationRationale` 当成模板业务字段保存

### 2.8 与现有 `cycle_template` AI 占位接口的关系

在 [cycle_template_接口文档_v2.md](./cycle_template_接口文档_v2.md) 中存在：

- `C5 POST /api/cycle-templates/drafts/ai-generate`

该接口是旧占位设计。

从 `ai_coach` 模块开始，AI 生成模板的正式入口应统一切换到：

- `POST /api/ai-coach/template-generations`

约定：

- 前端新版本不得继续调用 `C5`
- 后端后续可以保留 `C5` 作为废弃占位，或返回明确“已迁移到 ai_coach 模块”的错误

### 2.9 请求去重约定

为降低用户重复点击导致的重复任务创建风险，两个提交接口都支持可选字段：

- `clientRequestId`

语义约定：

- 由前端在一次用户点击动作内生成并保持稳定
- 推荐使用 UUID
- 同一用户、同一任务类型、相同 `clientRequestId` 的重复提交，后端应尽量返回同一个已存在任务，而不是重复创建

当前文档只定义语义，不强制规定具体去重时间窗口实现。

---

## 3. 接口列表

| 编号 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|:---:|------|
| AI1 | GET | `/api/ai-coach/capabilities` | 是 | 获取当前用户 AI 可用状态、资料完整度和当前可执行 AI 场景摘要 |
| AI2 | POST | `/api/ai-coach/template-generations` | 是 | 提交 AI 训练模板生成任务 |
| AI3 | GET | `/api/ai-coach/template-generations/{taskId}` | 是 | 查询 AI 训练模板生成任务结果 |
| AI4 | POST | `/api/ai-coach/cycle-summaries` | 是 | 提交 AI 周期总结任务 |
| AI5 | GET | `/api/ai-coach/cycle-summaries/{taskId}` | 是 | 查询 AI 周期总结任务结果 |

---

## 4. 公共数据结构

### 4.1 AI 能力摘要 `AiCoachCapabilitiesResponse`

```json
{
  "aiEnabled": true,
  "accountTier": "invited_ai",
  "platformRole": "user",
  "templateGeneration": {
    "available": true,
    "ready": true,
    "missingRequiredFields": [],
    "allowedSceneTypes": [
      "gym",
      "home"
    ],
    "allowedGoalTypes": [
      "fat_loss",
      "muscle_gain",
      "health_maintenance"
    ],
    "minCycleLength": 1,
    "maxCycleLength": 7
  },
  "cycleSummary": {
    "available": true,
    "ready": true,
    "latestCompletedCycleRunId": 1201,
    "latestCompletedAt": "2026-07-31T09:20:10",
    "recommendedMissingFields": []
  }
}
```

字段说明：

| 字段 | 类型 | 可空 | 说明 |
|------|------|:---:|------|
| `aiEnabled` | `boolean` | 否 | 当前账号是否开通 AI 能力 |
| `accountTier` | `string` | 否 | 当前账号权益层级，取值与 `auth` 模块一致 |
| `platformRole` | `string` | 否 | 当前用户角色，取值与 `auth` 模块一致 |
| `templateGeneration.available` | `boolean` | 否 | 当前账号是否允许使用 AI 模板生成 |
| `templateGeneration.ready` | `boolean` | 否 | 当前资料是否满足 AI 模板生成最低要求 |
| `templateGeneration.missingRequiredFields` | `string[]` | 否 | 缺失的必要资料字段编码列表 |
| `templateGeneration.allowedSceneTypes` | `string[]` | 否 | 允许的 `sceneType` 值 |
| `templateGeneration.allowedGoalTypes` | `string[]` | 否 | 允许的 `goalType` 值 |
| `templateGeneration.minCycleLength` | `number` | 否 | 最小周期天数 |
| `templateGeneration.maxCycleLength` | `number` | 否 | 最大周期天数 |
| `cycleSummary.available` | `boolean` | 否 | 当前账号是否允许使用 AI 周期总结 |
| `cycleSummary.ready` | `boolean` | 否 | 当前是否已有可分析的已完成循环 |
| `cycleSummary.latestCompletedCycleRunId` | `number \| null` | 是 | 最近一个已完成循环 ID |
| `cycleSummary.latestCompletedAt` | `string \| null` | 是 | 最近一个已完成循环的完成时间 |
| `cycleSummary.recommendedMissingFields` | `string[]` | 否 | 建议补充但不阻塞周期总结的资料字段编码列表 |

说明：

- `templateGeneration.ready = false` 时，前端应优先引导用户补齐资料，而不是继续提交任务。
- `cycleSummary.ready = false` 的典型原因是当前没有 `completed cycle_run`。
- 前端不得根据本地表单缓存自行推断 `ready`，必须以后端返回为准。

### 4.2 异步任务受理响应 `AiAsyncTaskAcceptedResponse`

```json
{
  "taskId": 9001,
  "taskType": "template_generation",
  "taskStatus": "pending",
  "createdAt": "2026-07-31T09:30:15",
  "pollAfterSeconds": 2
}
```

字段说明：

| 字段 | 类型 | 可空 | 说明 |
|------|------|:---:|------|
| `taskId` | `number` | 否 | AI 任务 ID |
| `taskType` | `string` | 否 | `template_generation` / `cycle_summary` |
| `taskStatus` | `string` | 否 | 初始状态，通常为 `pending` |
| `createdAt` | `string` | 否 | 任务创建时间 |
| `pollAfterSeconds` | `number` | 否 | 建议前端下一次轮询等待秒数 |

### 4.3 AI 任务基础信息 `AiTaskBase`

```json
{
  "taskId": 9001,
  "taskType": "template_generation",
  "taskStatus": "running",
  "createdAt": "2026-07-31T09:30:15",
  "startedAt": "2026-07-31T09:30:16",
  "completedAt": null,
  "errorCode": null,
  "errorMessage": null
}
```

字段说明：

| 字段 | 类型 | 可空 | 说明 |
|------|------|:---:|------|
| `taskId` | `number` | 否 | 任务 ID |
| `taskType` | `string` | 否 | 任务类型 |
| `taskStatus` | `string` | 否 | 任务状态 |
| `createdAt` | `string` | 否 | 创建时间 |
| `startedAt` | `string \| null` | 是 | 实际开始执行时间 |
| `completedAt` | `string \| null` | 是 | 终态完成时间 |
| `errorCode` | `string \| null` | 是 | 失败时的错误码 |
| `errorMessage` | `string \| null` | 是 | 失败时的错误描述 |

约定：

- `taskStatus = pending/running` 时，`result` 必须为 `null`
- `taskStatus = failed` 时，`result` 必须为 `null`
- `taskStatus = succeeded` 时，`result` 必须非空

### 4.4 模板生成请求体 `TemplateGenerationRequest`

```json
{
  "clientRequestId": "8f1b7665-a2f9-4d3d-a7c8-577a5906b651",
  "sceneType": "gym",
  "goalType": "muscle_gain",
  "cycleLength": 4,
  "includeCardio": true
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `clientRequestId` | `string \| null` | 否 | 前端生成的去重请求 ID，建议 UUID，最大 64 字符 |
| `sceneType` | `string` | 是 | `gym` / `home` |
| `goalType` | `string` | 是 | `fat_loss` / `muscle_gain` / `health_maintenance` |
| `cycleLength` | `number` | 是 | 范围 `1 ~ 7` |
| `includeCardio` | `boolean` | 是 | 本次模板是否允许包含有氧安排 |

语义约定：

- 本次请求中的 `goalType` 代表“本次 AI 生成目标”，不是对 `profile.goalType` 的直接修改。
- `includeCardio = false` 表示本次生成结果中不应主动安排有氧内容。

### 4.5 AI 生成草稿模板预览 `AiGeneratedDraftTemplate`

```json
{
  "templateId": 501,
  "templateName": "AI 生成模板 2026-07-31 09:30",
  "templateStatus": "draft",
  "cycleLength": 4,
  "days": [
    {
      "dayIndex": 1,
      "dayName": "上肢推",
      "isRestDay": false,
      "exercises": [
        {
          "sortOrder": 1,
          "exerciseId": 1001,
          "exerciseName": "Barbell Bench Press",
          "structureType": "set_based",
          "note": null,
          "items": [
            {
              "itemIndex": 1,
              "itemType": "set",
              "itemName": "第1组",
              "note": null,
              "metrics": [
                {
                  "sortOrder": 1,
                  "metricKey": "weight_kg",
                  "metricValueNumber": 50,
                  "metricUnit": "kg"
                },
                {
                  "sortOrder": 2,
                  "metricKey": "reps",
                  "metricValueNumber": 8,
                  "metricUnit": "次"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

语义约定：

- `draftTemplate` 是 AI 生成结果页立即展示用的预览结构。
- 其数据语义应与 `cycle_template` 模块详情结构保持一致。
- 前端后续进入模板详情页编辑时，应以 `cycle_template` 模块正式详情接口返回为准。

### 4.6 AI 设计说明 `GenerationRationale`

```json
{
  "overallDesignSummary": "本次采用 4 天循环，上肢推/上肢拉/下肢/恢复有氧的安排，重点兼顾增肌与恢复。",
  "dayRationales": [
    {
      "dayIndex": 1,
      "dayName": "上肢推",
      "focusSummary": "胸、肩前侧、三头肌",
      "rationale": "将大肌群推类动作集中安排，便于控制训练量并提高动作表现。"
    }
  ],
  "keyExerciseRationales": [
    {
      "dayIndex": 1,
      "exerciseId": 1001,
      "exerciseName": "Barbell Bench Press",
      "rationale": "作为上肢推主动作，用于建立基础推力量并提高胸部训练效率。"
    }
  ],
  "intensityRationale": {
    "basisType": "starting_recommendation",
    "summary": "当前缺少稳定历史力量数据，因此重量仅作为起始建议，请根据实际完成情况调整。"
  },
  "warnings": [
    "如肩部不适，请优先调整推类动作训练量。"
  ]
}
```

字段说明：

| 字段 | 类型 | 可空 | 说明 |
|------|------|:---:|------|
| `overallDesignSummary` | `string` | 否 | 整体设计思路摘要 |
| `dayRationales` | `array` | 否 | 每日训练目标与设计说明 |
| `dayRationales[].dayIndex` | `number` | 否 | 对应 Day 序号 |
| `dayRationales[].dayName` | `string` | 否 | 对应 Day 名称 |
| `dayRationales[].focusSummary` | `string` | 否 | 当日训练重点摘要 |
| `dayRationales[].rationale` | `string` | 否 | 当日设计理由 |
| `keyExerciseRationales` | `array` | 否 | 关键动作解释列表 |
| `keyExerciseRationales[].dayIndex` | `number` | 否 | 所属 Day |
| `keyExerciseRationales[].exerciseId` | `number` | 否 | 动作 ID |
| `keyExerciseRationales[].exerciseName` | `string` | 否 | 动作名称 |
| `keyExerciseRationales[].rationale` | `string` | 否 | 该动作的设计理由 |
| `intensityRationale.basisType` | `string` | 否 | `historical_performance` / `starting_recommendation` |
| `intensityRationale.summary` | `string` | 否 | 强度与重量建议依据摘要 |
| `warnings` | `string[]` | 否 | 风险提示或额外注意事项 |

约定：

- `basisType = historical_performance` 表示有稳定历史训练数据支撑。
- `basisType = starting_recommendation` 表示缺少可依赖历史数据，仅能给出起始建议。
- 前端必须原样展示 `basisType` 对应说明，不得擅自改写成更确定的语气。

### 4.7 模板生成结果 `TemplateGenerationTaskResult`

```json
{
  "draftTemplate": {
    "templateId": 501,
    "templateName": "AI 生成模板 2026-07-31 09:30",
    "templateStatus": "draft",
    "cycleLength": 4,
    "days": []
  },
  "generationRationale": {
    "overallDesignSummary": "采用 4 天循环，兼顾训练与恢复。",
    "dayRationales": [],
    "keyExerciseRationales": [],
    "intensityRationale": {
      "basisType": "starting_recommendation",
      "summary": "当前重量为起始建议。"
    },
    "warnings": []
  }
}
```

### 4.8 周期总结请求体 `CycleSummaryRequest`

```json
{
  "clientRequestId": "59dc7a31-df2f-44b1-a344-2f1cd99f16fc",
  "cycleRunId": 1201
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `clientRequestId` | `string \| null` | 否 | 前端生成的去重请求 ID，建议 UUID，最大 64 字符 |
| `cycleRunId` | `number` | 是 | 待分析的已完成循环 ID |

### 4.9 周期总结结果 `CycleSummaryTaskResult`

```json
{
  "cycleRunId": 1201,
  "templateId": 301,
  "templateName": "四天上/下肢分化",
  "runNo": 3,
  "cycleLength": 4,
  "executionOverview": "本轮 4 个 Day 均完成打卡，其中 1 个动作出现部分完成。",
  "strengths": [
    "整体出勤稳定",
    "上肢推主动作完成度较高"
  ],
  "issues": [
    "腿部训练后段疲劳明显",
    "有氧安排执行不稳定"
  ],
  "causeAnalysis": [
    "下肢日总量偏高",
    "恢复安排与当前生活节奏不够匹配"
  ],
  "nextCycleSuggestions": [
    "下肢日减少 1 个辅助动作",
    "把有氧从 2 次改为 1 次，先保证主训练完成"
  ],
  "risks": [
    "如膝部不适持续，应优先调整腿部训练动作选择"
  ],
  "dataCompletenessNotice": "当前身体指标资料不完整，建议补充后再次分析以获得更准确建议。"
}
```

字段说明：

| 字段 | 类型 | 可空 | 说明 |
|------|------|:---:|------|
| `cycleRunId` | `number` | 否 | 被分析的循环 ID |
| `templateId` | `number` | 否 | 对应模板 ID |
| `templateName` | `string` | 否 | 对应模板名称快照 |
| `runNo` | `number` | 否 | 第几轮循环 |
| `cycleLength` | `number` | 否 | 本轮周期长度 |
| `executionOverview` | `string` | 否 | 本轮执行概览 |
| `strengths` | `string[]` | 否 | 做得好的地方 |
| `issues` | `string[]` | 否 | 主要问题 |
| `causeAnalysis` | `string[]` | 否 | 可能原因分析 |
| `nextCycleSuggestions` | `string[]` | 否 | 下轮调整建议 |
| `risks` | `string[]` | 否 | 风险提醒 |
| `dataCompletenessNotice` | `string \| null` | 是 | 资料完整度提醒，不阻塞结果返回 |

---

## 5. 接口详情

### 5.1 AI1 获取 AI 能力与就绪状态

- 路径：`GET /api/ai-coach/capabilities`
- 认证：是
- 作用：返回当前用户是否可使用 AI、资料是否满足模板生成最低要求，以及最近可分析已完成循环摘要

请求示例：

```http
GET /api/ai-coach/capabilities
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "aiEnabled": true,
    "accountTier": "invited_ai",
    "platformRole": "user",
    "templateGeneration": {
      "available": true,
      "ready": false,
      "missingRequiredFields": [
        "currentWeightKg"
      ],
      "allowedSceneTypes": [
        "gym",
        "home"
      ],
      "allowedGoalTypes": [
        "fat_loss",
        "muscle_gain",
        "health_maintenance"
      ],
      "minCycleLength": 1,
      "maxCycleLength": 7
    },
    "cycleSummary": {
      "available": true,
      "ready": true,
      "latestCompletedCycleRunId": 1201,
      "latestCompletedAt": "2026-07-31T09:20:10",
      "recommendedMissingFields": [
        "currentWeightKg"
      ]
    }
  }
}
```

实现约定：

- `available = false` 时，前端仍可展示入口，但不允许继续调用提交接口
- `templateGeneration.ready = false` 时，前端应优先跳转或引导补资料
- `cycleSummary.ready = false` 时，前端应禁用“分析本轮”动作或展示明确提示

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401

### 5.2 AI2 提交 AI 训练模板生成任务

- 路径：`POST /api/ai-coach/template-generations`
- 认证：是
- 作用：提交一条 AI 模板生成任务，不同步返回最终草稿内容

请求示例：

```http
POST /api/ai-coach/template-generations
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "clientRequestId": "8f1b7665-a2f9-4d3d-a7c8-577a5906b651",
  "sceneType": "gym",
  "goalType": "muscle_gain",
  "cycleLength": 4,
  "includeCardio": true
}
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "taskId": 9001,
    "taskType": "template_generation",
    "taskStatus": "pending",
    "createdAt": "2026-07-31T09:30:15",
    "pollAfterSeconds": 2
  }
}
```

业务规则：

- 当前用户必须具备 AI 权限
- 基础资料必须满足最低要求
- `cycleLength` 必须在 `1 ~ 7`
- `goalType` 和 `sceneType` 必须在封闭枚举内
- 后端应以当前系统动作库与模板结构规则约束最终生成结果

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `AI_FEATURE_NOT_AVAILABLE`：当前账号未开通 AI 功能，HTTP 403
- `AI_REQUIRED_PROFILE_MISSING`：缺少必要基础档案字段，HTTP 400
- `AI_REQUIRED_BODY_METRIC_MISSING`：缺少必要身体指标字段，HTTP 400
- `INVALID_ARGUMENT`：请求体参数格式非法，HTTP 400
- `AI_SERVICE_UNAVAILABLE`：AI 服务暂不可用，HTTP 503

### 5.3 AI3 查询 AI 训练模板生成任务结果

- 路径：`GET /api/ai-coach/template-generations/{taskId}`
- 认证：是
- 作用：轮询查询模板生成任务状态；成功时返回模板草稿预览和 AI 设计说明

请求示例：

```http
GET /api/ai-coach/template-generations/9001
Authorization: Bearer <accessToken>
```

成功响应示例 1：任务进行中

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "taskId": 9001,
    "taskType": "template_generation",
    "taskStatus": "running",
    "createdAt": "2026-07-31T09:30:15",
    "startedAt": "2026-07-31T09:30:16",
    "completedAt": null,
    "errorCode": null,
    "errorMessage": null,
    "result": null
  }
}
```

成功响应示例 2：任务成功

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "taskId": 9001,
    "taskType": "template_generation",
    "taskStatus": "succeeded",
    "createdAt": "2026-07-31T09:30:15",
    "startedAt": "2026-07-31T09:30:16",
    "completedAt": "2026-07-31T09:30:20",
    "errorCode": null,
    "errorMessage": null,
    "result": {
      "draftTemplate": {
        "templateId": 501,
        "templateName": "AI 生成模板 2026-07-31 09:30",
        "templateStatus": "draft",
        "cycleLength": 4,
        "days": []
      },
      "generationRationale": {
        "overallDesignSummary": "采用 4 天循环，兼顾训练与恢复。",
        "dayRationales": [],
        "keyExerciseRationales": [],
        "intensityRationale": {
          "basisType": "starting_recommendation",
          "summary": "当前重量为起始建议。"
        },
        "warnings": []
      }
    }
  }
}
```

成功响应示例 3：任务失败

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "taskId": 9001,
    "taskType": "template_generation",
    "taskStatus": "failed",
    "createdAt": "2026-07-31T09:30:15",
    "startedAt": "2026-07-31T09:30:16",
    "completedAt": "2026-07-31T09:30:18",
    "errorCode": "AI_OUTPUT_INVALID",
    "errorMessage": "ai output cannot be converted to a valid cycle template draft",
    "result": null
  }
}
```

实现约定：

- `draftTemplate` 与 `generationRationale` 必须分开返回
- 后端落库成功后，`draftTemplate.templateId` 必须可用于后续进入 `cycle_template` 详情页
- 若 AI 返回结果无法通过系统结构校验，任务应进入 `failed`，而不是返回半成功草稿

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `AI_TASK_NOT_FOUND`：任务不存在、任务类型不匹配，或不属于当前用户，HTTP 404

### 5.4 AI4 提交 AI 周期总结任务

- 路径：`POST /api/ai-coach/cycle-summaries`
- 认证：是
- 作用：提交一条针对已完成循环的 AI 周期总结任务

请求示例：

```http
POST /api/ai-coach/cycle-summaries
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "clientRequestId": "59dc7a31-df2f-44b1-a344-2f1cd99f16fc",
  "cycleRunId": 1201
}
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "taskId": 9101,
    "taskType": "cycle_summary",
    "taskStatus": "pending",
    "createdAt": "2026-07-31T09:40:10",
    "pollAfterSeconds": 2
  }
}
```

业务规则：

- 当前用户必须具备 AI 权限
- `cycleRunId` 必须属于当前用户
- `cycleRunId` 必须对应 `completed` 状态的循环
- 基础档案或身体指标不完整时允许继续，但要在结果中返回提醒

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `AI_FEATURE_NOT_AVAILABLE`：当前账号未开通 AI 功能，HTTP 403
- `AI_CYCLE_RUN_NOT_COMPLETED`：目标循环不是 `completed`，HTTP 409
- `RESOURCE_NOT_FOUND`：目标循环不存在或无权访问，HTTP 404
- `INVALID_ARGUMENT`：请求体参数格式非法，HTTP 400
- `AI_SERVICE_UNAVAILABLE`：AI 服务暂不可用，HTTP 503

### 5.5 AI5 查询 AI 周期总结任务结果

- 路径：`GET /api/ai-coach/cycle-summaries/{taskId}`
- 认证：是
- 作用：轮询查询周期总结任务状态；成功时返回结构化总结结果

请求示例：

```http
GET /api/ai-coach/cycle-summaries/9101
Authorization: Bearer <accessToken>
```

成功响应示例：任务成功

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "taskId": 9101,
    "taskType": "cycle_summary",
    "taskStatus": "succeeded",
    "createdAt": "2026-07-31T09:40:10",
    "startedAt": "2026-07-31T09:40:11",
    "completedAt": "2026-07-31T09:40:14",
    "errorCode": null,
    "errorMessage": null,
    "result": {
      "cycleRunId": 1201,
      "templateId": 301,
      "templateName": "四天上/下肢分化",
      "runNo": 3,
      "cycleLength": 4,
      "executionOverview": "本轮 4 个 Day 均完成打卡，其中 1 个动作出现部分完成。",
      "strengths": [
        "整体出勤稳定"
      ],
      "issues": [
        "腿部训练后段疲劳明显"
      ],
      "causeAnalysis": [
        "下肢日总量偏高"
      ],
      "nextCycleSuggestions": [
        "下肢日减少 1 个辅助动作"
      ],
      "risks": [
        "如膝部不适持续，应优先调整腿部训练动作选择"
      ],
      "dataCompletenessNotice": "当前身体指标资料不完整，建议补充后再次分析以获得更准确建议。"
    }
  }
}
```

实现约定：

- 当资料不完整但不阻塞分析时，提醒放在 `dataCompletenessNotice`
- 结果只返回文字建议，不直接返回新模板草稿

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `AI_TASK_NOT_FOUND`：任务不存在、任务类型不匹配，或不属于当前用户，HTTP 404

---

## 6. 推荐错误码

| 错误码 | HTTP 状态码 | 含义 |
|------|------|------|
| `UNAUTHORIZED` | 401 | 未登录或 token 无效 |
| `FORBIDDEN` | 403 | 已登录但无通用访问权限 |
| `AI_FEATURE_NOT_AVAILABLE` | 403 | 当前账号未开通 AI 功能 |
| `INVALID_ARGUMENT` | 400 | 请求参数格式非法 |
| `AI_REQUIRED_PROFILE_MISSING` | 400 | 缺少 AI 模板生成所需基础档案字段 |
| `AI_REQUIRED_BODY_METRIC_MISSING` | 400 | 缺少 AI 模板生成所需身体指标字段 |
| `AI_CYCLE_RUN_NOT_COMPLETED` | 409 | 待分析循环不是 `completed` 状态 |
| `AI_TASK_NOT_FOUND` | 404 | AI 任务不存在、类型不匹配或不属于当前用户 |
| `AI_OUTPUT_INVALID` | 500 | AI 返回内容无法通过系统结构校验 |
| `AI_SERVICE_TIMEOUT` | 504 | AI 服务超时 |
| `AI_SERVICE_UNAVAILABLE` | 503 | AI 服务暂不可用 |

说明：

- `AI_OUTPUT_INVALID` 是后端内部处理 AI 结果失败，不应归类为前端参数错误。
- `AI_TASK_NOT_FOUND` 统一兜底任务查询失败场景，避免暴露“该任务是否属于其他用户”的信息。

---

## 7. 联调约束

### 7.1 前端调用顺序建议

AI 模板生成页面建议调用顺序：

1. 进入页面先调 `AI1 /capabilities`
2. 若 `templateGeneration.available = false`，展示未开通提示
3. 若 `templateGeneration.ready = false`，展示缺失字段并跳转补录
4. 用户提交后调用 `AI2`
5. 轮询 `AI3` 直到终态
6. 成功后在结果页同时展示 `draftTemplate` 和 `generationRationale`

AI 周期总结页面建议调用顺序：

1. 进入页面先调 `AI1 /capabilities`
2. 若 `cycleSummary.available = false`，展示未开通提示
3. 若 `cycleSummary.ready = false`，提示当前无可分析已完成循环
4. 用户提交后调用 `AI4`
5. 轮询 `AI5` 直到终态

### 7.2 前端不得自行推断的字段

前端不得自行推断以下内容，必须以后端返回为准：

- `templateGeneration.ready`
- `cycleSummary.ready`
- `missingRequiredFields`
- `recommendedMissingFields`
- `taskStatus`
- `generationRationale.intensityRationale.basisType`

### 7.3 后端保存时的再次校验

即使前端已通过 `AI1` 做了前置判断，后端在 `AI2`、`AI4` 中仍必须再次校验：

- 当前用户是否有 AI 权限
- 当前资料是否满足最低要求
- `cycleRunId` 是否属于当前用户
- `cycleRunId` 是否已完成

### 7.4 模板结果与正式模板编辑的边界

`AI3` 返回的 `draftTemplate` 仅用于：

- 结果页展示
- 进入模板详情页前的预览

真正编辑或启用时：

- 前端应跳转到 `cycle_template` 模块
- 后续保存、启用、切换均按 `cycle_template` 模块文档执行

### 7.5 轮询间隔

建议前端轮询间隔遵循：

- 优先使用返回体中的 `pollAfterSeconds`
- 若后端未返回，则默认每 `2 ~ 3` 秒轮询一次

当前不建议前端小于 1 秒高频轮询。

---

## 8. 变更说明

### 8.1 相对现有文档的新增

本文件是全新模块接口文档，新增了：

- AI 能力状态查询
- AI 模板生成正式接口
- AI 周期总结正式接口
- AI 异步任务轮询语义

### 8.2 对旧文档的影响

`cycle_template_接口文档_v2.md` 中的：

- `C5 POST /api/cycle-templates/drafts/ai-generate`

应视为旧占位接口，不再作为 AI 模板生成正式入口。
### 8.3 2026-08-01 实现补充

#### 错误码语义补充

- `AI_SERVICE_TIMEOUT`
  - 表示后端在调用上游 AI 服务时发生超时
  - HTTP 状态保持为 `504`
- `AI_SERVICE_UNAVAILABLE`
  - 表示后端在调用上游 AI 服务时发生不可用类错误
  - HTTP 状态保持为 `503`
  - 可能原因包括：
    - 上游 HTTP `4xx`
    - 上游 HTTP `5xx`
    - 网络访问异常
    - 未拿到有效模型响应

#### 联调排障说明

需要明确：

- `AI_SERVICE_UNAVAILABLE` 不等于“本次请求没有真正调用到 AI”
- 在真实联调中，可能已经发生：
  - 多轮 tool calling
  - 部分模型调用成功
  - 最终在后续某一次模型调用阶段失败

因此联调排障时，不能只看前端错误提示，还应结合：

- `ai_task_records.status / error_code / error_message`
- `ai_task_records.tool_call_count / repair_attempt_count`
- `ai_task_tool_calls`
- 后端结构化日志

#### 当前默认 timeout

当前后端默认配置已调整为：

- `dailyforge.ai.timeout = PT120S`

该值用于覆盖模板生成这类“多轮 tool calling + 最终大 JSON 输出”的 MVP 场景。
