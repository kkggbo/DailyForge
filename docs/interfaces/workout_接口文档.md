# DailyForge Workout 模块接口文档

> 版本：v1.0  
> 更新时间：2026-07-29  
> 模块归属：`backend` 单体应用，建议代码包为 `com.dailyforge.modules.workout`  
> 文档状态：接口契约设计阶段，尚未落代码

---

## 1. 文档范围

本文档定义 DailyForge `workout` 模块的 MVP 接口契约，覆盖：

- 训练工作台上下文与当前循环 Day 导航。
- 默认当前 Day 的自动 session 初始化。
- 当前循环内历史 Day、当前 Day、未来 Day 的查看。
- 进行中训练会话的完整保存。
- 训练日和休息日的完成打卡。
- 最近训练记录与训练详情。
- 循环结束后重新使用当前模板开启下一轮。
- AI 分析占位接口。

本文档以 [workout_PRD.md](../prd/workout_PRD.md) 为准；如与旧版 `cycle_template` 接口文档存在冲突，以本模块已确认的最新业务规则为准。

---

## 2. 通用约定

### 2.1 路由与鉴权

外部接口前缀：

- `/api/workouts`

后端 Controller 建议映射：

- `/workouts`

所有接口都要求登录态：

```http
Authorization: Bearer <accessToken>
```

统一响应体：

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
  "code": "WORKOUT_SESSION_STATUS_INVALID",
  "message": "workout session status is invalid",
  "data": null
}
```

### 2.2 时间格式

所有时间字段使用服务端现有 ISO-8601 LocalDateTime 格式：

```text
2026-07-29T20:15:30
```

前端不得自行假设时区或将缺失时间字段补成当前时间。

### 2.3 自动初始化不是有副作用的 GET

训练页不展示“开始训练”按钮，但不能因此让 `GET` 请求创建 session。

前端进入 `/workout` 时的推荐调用顺序：

1. 调用 `W1 GET /api/workouts/context`。
2. 当 `workspaceState = active` 时，读取 `defaultDayIndex`。
3. 自动调用 `W2 POST /api/workouts/current-day/session`。
4. 使用 W2 返回的当前 Day 详情直接渲染可编辑训练页。

说明：

- W2 是幂等命令，不对应用户可见的“开始训练”按钮。
- W2 仅初始化默认当前 Day。
- 用户切换查看历史 Day 或未来 Day 时，只调用 W3，不调用 W2。

### 2.4 页面状态与 Day 状态

`workspaceState` 用于决定训练页整体状态：

- `no_active_template`：没有激活模板。
- `active`：当前循环正在执行。
- `cycle_completed`：当前循环已完成，等待用户选择下一步。

`dayState` 用于当前循环内 Day 导航：

- `completed`：已完成打卡的历史 Day。
- `current`：当前最新未打卡 Day。
- `upcoming`：当前 Day 之后的待训练 Day。

`viewMode` 用于前端交互控制：

- `editable`：当前 Day，可保存或完成打卡。
- `readonly`：已完成记录，只读。
- `preview`：未来 Day 的计划预览，只读。

### 2.5 Session 状态

`sessionStatus` 仅使用以下值：

- `in_progress`：进行中，可保存和完成。
- `completed`：已完成打卡，只读。
- `cancelled`：因模板切换等原因取消，只读。

`sessionType` 仅使用以下值：

- `workout`：有动作的训练日。
- `rest_day`：无动作的休息日打卡。

### 2.6 动作状态和失败原因

动作状态 `exerciseStatus`：

- `completed`
- `partial_completed`
- `skipped`
- `failed`

失败 / 跳过原因 `failureReason`：

- `too_tired`
- `equipment_unavailable`
- `pain_or_discomfort`
- `time_not_enough`
- `plan_too_hard`
- `other`

约束：

- `exerciseStatus` 在进行中保存时允许为 `null`。
- 完成打卡时，每个动作必须存在合法的 `exerciseStatus`。
- `failureReason` 可为空；若传值，只允许使用上述枚举。
- `failureReason` 仅在 `exerciseStatus` 为 `partial_completed`、`skipped` 或 `failed` 时由前端展示；后端不强制该字段必填。
- `failureReason = other` 时，前端应提示用户在 `feedback` 中补充说明，但后端 MVP 不强制。

### 2.7 实际执行数据结构

`workout` 复用 `cycle_template v2` 的三层结构：

```text
动作 exercise -> 执行项 item -> 参数 metric
```

结构类型 `structureType`：

- `set_based`
- `single_segment`

执行项类型 `itemType`：

- `set`
- `segment`

参数键 `metricKey` 为封闭字典：

- `weight_kg`
- `reps`
- `duration_seconds`
- `duration_minutes`
- `distance_km`
- `speed_kmh`
- `pace_seconds_per_km`
- `incline_percent`
- `rest_seconds`
- `rpe`
- `intensity_level`

单位由后端依据 `metricKey` 返回，前端请求不传 `metricUnit`。

---

## 3. 接口列表

| 编号 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|:---:|------|
| W1 | GET | `/api/workouts/context` | 是 | 获取训练工作台上下文和 Day 导航摘要 |
| W2 | POST | `/api/workouts/current-day/session` | 是 | 自动创建或返回默认当前 Day 的 session |
| W3 | GET | `/api/workouts/days/{dayIndex}` | 是 | 查看当前循环指定 Day |
| W4 | PUT | `/api/workouts/sessions/{sessionId}` | 是 | 完整保存进行中训练会话 |
| W5 | POST | `/api/workouts/sessions/{sessionId}/complete` | 是 | 完成打卡并推进循环 |
| W6 | GET | `/api/workouts/sessions/{sessionId}` | 是 | 查看训练记录详情 |
| W7 | GET | `/api/workouts/recent` | 是 | 分页查询最近训练记录 |
| W8 | POST | `/api/workouts/cycles/current/restart` | 是 | 当前循环完成后，以当前模板开启新一轮 |
| W9 | POST | `/api/workouts/cycles/current/ai-analysis` | 是 | AI 循环分析占位接口 |

不提供用户手动取消 session 的公开接口。`cancelled` 由模板切换等跨模块业务流程触发。

---

## 4. 公共响应结构

### 4.1 Day 导航项 `WorkoutDayNavigationItem`

```json
{
  "dayIndex": 3,
  "dayName": "腿部训练",
  "isRestDay": false,
  "dayState": "current",
  "sessionId": 501,
  "sessionStatus": "in_progress"
}
```

| 字段 | 类型 | 可空 | 说明 |
|------|------|:---:|------|
| `dayIndex` | `number` | 否 | 当前循环中的 Day 序号，范围 `1 ~ cycleLength` |
| `dayName` | `string` | 否 | Day 名称快照或当前模板名称，空白时返回标准化的 `Day n` |
| `isRestDay` | `boolean` | 否 | 当前 Day 是否没有动作 |
| `dayState` | `string` | 否 | `completed` / `current` / `upcoming` |
| `sessionId` | `number` | 是 | 已创建 session 时返回；未来 Day 通常为 `null` |
| `sessionStatus` | `string` | 是 | 已创建 session 的状态；未来 Day 为 `null` |

### 4.2 训练动作 `WorkoutSessionExercise`

```json
{
  "sessionExerciseId": 7001,
  "sortOrder": 1,
  "exerciseId": 1001,
  "exerciseName": "Barbell Bench Press",
  "structureType": "set_based",
  "exerciseStatus": "partial_completed",
  "failureReason": "too_tired",
  "feedback": "三头提前力竭；下轮可减少热身俯卧撑次数",
  "items": [
    {
      "itemIndex": 1,
      "itemType": "set",
      "itemName": "第1组",
      "note": "工作组",
      "metrics": [
        {
          "sortOrder": 1,
          "metricKey": "weight_kg",
          "metricUnit": "kg",
          "plannedValueNumber": 60,
          "actualValueNumber": 50
        },
        {
          "sortOrder": 2,
          "metricKey": "reps",
          "metricUnit": "count",
          "plannedValueNumber": 8,
          "actualValueNumber": 6
        }
      ]
    }
  ]
}
```

字段说明：

| 字段 | 类型 | 可空 | 说明 |
|------|------|:---:|------|
| `sessionExerciseId` | `number` | 是 | 已初始化 session 时返回；未来 Day 计划预览可为 `null` |
| `sortOrder` | `number` | 否 | 动作在 Day 内排序 |
| `exerciseId` | `number` | 否 | 系统动作 ID |
| `exerciseName` | `string` | 否 | 动作名称快照 |
| `structureType` | `string` | 否 | `set_based` / `single_segment` |
| `exerciseStatus` | `string` | 是 | 动作完成状态；进行中未标记时为 `null` |
| `failureReason` | `string` | 是 | 失败 / 跳过原因 |
| `feedback` | `string` | 是 | 动作感受、失败补充或下轮调整备注，最大 500 字符 |
| `items` | `array` | 否 | 计划快照中的执行项结构 |
| `items[].itemIndex` | `number` | 否 | 执行项序号 |
| `items[].itemType` | `string` | 否 | `set` / `segment` |
| `items[].itemName` | `string` | 是 | 执行项名称 |
| `items[].note` | `string` | 是 | 计划执行项备注 |
| `items[].metrics` | `array` | 否 | 参数快照 |
| `metrics[].metricKey` | `string` | 否 | 封闭字典参数键 |
| `metrics[].metricUnit` | `string` | 否 | 后端推导出的展示单位 |
| `metrics[].plannedValueNumber` | `number` | 是 | 计划值；不填写计划参数时可为空 |
| `metrics[].actualValueNumber` | `number` | 是 | 实际值覆盖；未填写时为 `null`，编辑态按计划展示 |

### 4.3 Day 详情 `WorkoutDayDetail`

W2、W3 和 W5 中的 Day 详情统一使用以下语义：

```json
{
  "cycleRunId": 31,
  "runNo": 2,
  "templateId": 101,
  "templateName": "Push Pull Legs",
  "dayIndex": 3,
  "dayName": "腿部训练",
  "isRestDay": false,
  "dayState": "current",
  "viewMode": "editable",
  "canInitializeSession": true,
  "session": {
    "sessionId": 501,
    "sessionType": "workout",
    "sessionStatus": "in_progress",
    "startedAt": "2026-07-29T20:15:30",
    "completedAt": null,
    "notes": null,
    "exercises": []
  }
}
```

约束：

- `viewMode = editable` 时，`session` 必须存在且 `sessionStatus = in_progress`。
- `viewMode = readonly` 时，`session` 必须存在且状态为 `completed` 或 `cancelled`。
- `viewMode = preview` 时，`session` 为 `null`，计划动作通过顶层 `exercises` 返回。
- 为避免前端区分两套数组，响应统一在 `session.exercises` 存在时使用该字段；`session = null` 时使用顶层 `exercises`。

未来 Day 预览示例：

```json
{
  "cycleRunId": 31,
  "runNo": 2,
  "templateId": 101,
  "templateName": "Push Pull Legs",
  "dayIndex": 5,
  "dayName": "有氧",
  "isRestDay": false,
  "dayState": "upcoming",
  "viewMode": "preview",
  "canInitializeSession": false,
  "session": null,
  "exercises": [
    {
      "sessionExerciseId": null,
      "sortOrder": 1,
      "exerciseId": 2001,
      "exerciseName": "Treadmill Running",
      "structureType": "single_segment",
      "exerciseStatus": null,
      "failureReason": null,
      "feedback": null,
      "items": []
    }
  ]
}
```

---

## 5. 接口详情

### 5.1 W1 获取训练工作台上下文

- 路径：`GET /api/workouts/context`
- 作用：返回当前训练页整体状态、真实循环进度和 Day 导航摘要。
- 是否写库：否。

当存在激活且未完成的循环：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "workspaceState": "active",
    "templateId": 101,
    "templateName": "Push Pull Legs",
    "cycleRunId": 31,
    "runNo": 2,
    "cycleLength": 6,
    "currentDayIndex": 3,
    "defaultDayIndex": 3,
    "days": [
      {
        "dayIndex": 1,
        "dayName": "推",
        "isRestDay": false,
        "dayState": "completed",
        "sessionId": 490,
        "sessionStatus": "completed"
      },
      {
        "dayIndex": 3,
        "dayName": "腿部训练",
        "isRestDay": false,
        "dayState": "current",
        "sessionId": null,
        "sessionStatus": null
      },
      {
        "dayIndex": 4,
        "dayName": "休息",
        "isRestDay": true,
        "dayState": "upcoming",
        "sessionId": null,
        "sessionStatus": null
      }
    ]
  }
}
```

当没有激活模板：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "workspaceState": "no_active_template",
    "templateId": null,
    "templateName": null,
    "cycleRunId": null,
    "runNo": null,
    "cycleLength": null,
    "currentDayIndex": null,
    "defaultDayIndex": null,
    "days": []
  }
}
```

当当前循环已经完成：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "workspaceState": "cycle_completed",
    "templateId": 101,
    "templateName": "Push Pull Legs",
    "cycleRunId": 31,
    "runNo": 2,
    "cycleLength": 6,
    "currentDayIndex": null,
    "defaultDayIndex": null,
    "days": []
  }
}
```

实现规则：

- W1 不创建 session。
- `defaultDayIndex` 仅在 `workspaceState = active` 时返回，且等于真实 `currentDayIndex`。
- 前端进入、刷新或重新进入训练页时，应以 `defaultDayIndex` 作为初始 `selectedDayIndex`。
- 用户在页面内切换 Day 时，不得修改 `currentDayIndex`。

### 5.2 W2 自动初始化默认当前 Day session

- 路径：`POST /api/workouts/current-day/session`
- 作用：自动创建或返回默认当前 Day 的 `in_progress` session。
- 请求体：无。
- 幂等性：是。

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "sessionCreated": true,
    "day": {
      "cycleRunId": 31,
      "runNo": 2,
      "templateId": 101,
      "templateName": "Push Pull Legs",
      "dayIndex": 3,
      "dayName": "腿部训练",
      "isRestDay": false,
      "dayState": "current",
      "viewMode": "editable",
      "canInitializeSession": true,
      "session": {
        "sessionId": 501,
        "sessionType": "workout",
        "sessionStatus": "in_progress",
        "startedAt": "2026-07-29T20:15:30",
        "completedAt": null,
        "notes": null,
        "exercises": []
      }
    }
  }
}
```

实现规则：

- 仅允许初始化当前激活 `cycle_run` 的 `currentDayIndex`。
- 当前 Day 有动作时，创建 `sessionType = workout`；空白 Day 创建 `sessionType = rest_day`。
- 创建时复制模板、Day、动作、执行项和参数快照。
- 同一用户、同一 `cycleRunId`、同一 `dayIndex` 同时只允许存在一个 `in_progress` session。
- 如果已存在 `in_progress` session，返回它并令 `sessionCreated = false`。
- 不允许为已完成 Day 或未来 Day 创建 session。
- 前端仅在默认进入当前 Day 时自动调用本接口，不为 Day 导航中的未来 Day 调用。

常见失败：

- `WORKOUT_ACTIVE_CYCLE_NOT_FOUND`
- `WORKOUT_CYCLE_COMPLETED`
- `WORKOUT_CURRENT_DAY_SESSION_CONFLICT`

### 5.3 W3 查看当前循环指定 Day

- 路径：`GET /api/workouts/days/{dayIndex}`
- 作用：按 `dayIndex` 查看当前循环中的历史记录、当前打卡内容或未来计划预览。
- 是否写库：否。

路径参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `dayIndex` | `number` | 是 | 当前 `cycle_run` 内的 Day 序号，范围 `1 ~ cycleLength` |

实现规则：

- 已完成 Day：返回 `viewMode = readonly` 和对应 completed session。
- 当前 Day：若 session 已由 W2 创建，返回 `viewMode = editable` 和 `in_progress` session；若未创建，返回 `session = null` 和 `canInitializeSession = true`。
- 未来 Day：返回 `viewMode = preview`，返回计划快照，不创建 session。
- W3 不得因读取操作创建 session。
- 当前循环已完成时，不支持通过 W3 创建或编辑 session；历史详情应使用 W6。

当前 Day 未初始化示例：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "cycleRunId": 31,
    "runNo": 2,
    "templateId": 101,
    "templateName": "Push Pull Legs",
    "dayIndex": 3,
    "dayName": "腿部训练",
    "isRestDay": false,
    "dayState": "current",
    "viewMode": "editable",
    "canInitializeSession": true,
    "session": null,
    "exercises": []
  }
}
```

常见失败：

- `WORKOUT_ACTIVE_CYCLE_NOT_FOUND`
- `WORKOUT_CYCLE_COMPLETED`
- `WORKOUT_DAY_OUT_OF_RANGE`

### 5.4 W4 保存进行中训练会话

- 路径：`PUT /api/workouts/sessions/{sessionId}`
- 作用：手动保存当前页面中的完整训练填写内容。
- 更新语义：全量覆盖当前 session 的可编辑填写部分，不是 patch。

路径参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `sessionId` | `number` | 是 | 当前用户的进行中训练会话 ID |

请求体：

```json
{
  "notes": "下次腿举重量可以小幅增加",
  "exercises": [
    {
      "sessionExerciseId": 7001,
      "exerciseStatus": "partial_completed",
      "failureReason": "too_tired",
      "feedback": "三头提前力竭；下轮减少热身俯卧撑次数",
      "items": [
        {
          "itemIndex": 1,
          "metrics": [
            {
              "metricKey": "weight_kg",
              "actualValueNumber": 50
            },
            {
              "metricKey": "reps",
              "actualValueNumber": 6
            }
          ]
        }
      ]
    }
  ]
}
```

请求字段说明：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `notes` | `string \| null` | 否 | 训练整体感受与备注，最大 1000 字符；`null` 表示清空 |
| `exercises` | `array` | 是 | 当前 session 的全部动作；休息日必须传空数组 |
| `exercises[].sessionExerciseId` | `number` | 是 | session 内动作 ID，必须属于当前 session |
| `exercises[].exerciseStatus` | `string \| null` | 否 | 保存时可为 `null`，完成时不允许为 `null` |
| `exercises[].failureReason` | `string \| null` | 否 | 失败 / 跳过原因 |
| `exercises[].feedback` | `string \| null` | 否 | 动作感受、失败补充或调整备注，最大 500 字符 |
| `exercises[].items` | `array` | 是 | 必须覆盖该动作计划快照中的全部执行项 |
| `items[].itemIndex` | `number` | 是 | 快照执行项序号 |
| `items[].metrics` | `array` | 是 | 必须覆盖该执行项计划快照中的全部参数键 |
| `metrics[].metricKey` | `string` | 是 | 快照参数键，不允许新增、删除或改名 |
| `metrics[].actualValueNumber` | `number \| null` | 是 | 实际参数覆盖；`null` 表示未填写，完成状态会按计划值处理 |

实现规则：

- 仅允许保存当前用户自己的 `in_progress` session。
- 请求中的 `exercises`、`items`、`metrics` 采用全量覆盖语义。
- 保存时允许动作状态和实际参数尚未填写；实际参数为 `null` 时不覆盖计划值。
- 不允许新增计划外动作、执行项或参数键。
- 不允许修改计划快照、动作顺序、结构类型或单位。
- 后端必须再次校验 `sessionExerciseId`、`itemIndex`、`metricKey` 均属于 session 快照。

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "sessionId": 501,
    "sessionStatus": "in_progress",
    "savedAt": "2026-07-29T20:30:00"
  }
}
```

常见失败：

- `WORKOUT_SESSION_NOT_FOUND`
- `WORKOUT_SESSION_EDIT_FORBIDDEN`
- `WORKOUT_SESSION_STATUS_INVALID`
- `WORKOUT_SESSION_EXERCISE_INVALID`
- `WORKOUT_ITEM_INVALID`
- `WORKOUT_METRIC_INVALID`
- `WORKOUT_METRIC_VALUE_INVALID`

### 5.5 W5 完成训练 / 休息日打卡

- 路径：`POST /api/workouts/sessions/{sessionId}/complete`
- 作用：保存当前页面填写内容、完成当前 Day 打卡并推进循环。
- 请求体：与 W4 完全相同。

完成校验：

- `sessionStatus` 必须为 `in_progress`。
- 训练日的每个动作都必须存在合法 `exerciseStatus`。
- `completed`、`partial_completed`、`skipped`、`failed` 均可以完成打卡。
- `skipped`、`failed`、`partial_completed` 不强制填写失败原因或实际参数。
- `completed` 动作未填写实际参数时，完成接口按计划值写入实际值。
- `completed` 动作未填写实际参数时，完成接口按计划值写入实际值。
- 休息日 `exercises` 必须是空数组，不执行动作状态校验。

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "sessionId": 501,
    "sessionStatus": "completed",
    "completedAt": "2026-07-29T21:10:00",
    "completedDayIndex": 3,
    "cycleRunId": 31,
    "cycleRunStatus": "active",
    "nextCurrentDayIndex": 4,
    "nextDay": {
      "dayIndex": 4,
      "dayName": "休息",
      "isRestDay": true
    },
    "completedDay": {
      "cycleRunId": 31,
      "runNo": 2,
      "templateId": 101,
      "templateName": "Push Pull Legs",
      "dayIndex": 3,
      "dayName": "腿部训练",
      "isRestDay": false,
      "dayState": "completed",
      "viewMode": "readonly",
      "canInitializeSession": false,
      "session": {
        "sessionId": 501,
        "sessionType": "workout",
        "sessionStatus": "completed",
        "startedAt": "2026-07-29T20:15:30",
        "completedAt": "2026-07-29T21:10:00",
        "notes": "下次腿举重量可以小幅增加",
        "exercises": []
      }
    }
  }
}
```

当完成周期最后一天时：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "sessionId": 506,
    "sessionStatus": "completed",
    "completedAt": "2026-07-29T21:10:00",
    "completedDayIndex": 6,
    "cycleRunId": 31,
    "cycleRunStatus": "completed",
    "nextCurrentDayIndex": null,
    "nextDay": null,
    "completedDay": {}
  }
}
```

前端联调约束：

- W5 成功后，前端必须保留当前 `selectedDayIndex = completedDayIndex`。
- 前端使用 `completedDay` 切换页面为只读完成态，不得自动跳转到 `nextCurrentDayIndex`。
- 用户手动点击 Day 导航时，才请求 W3 查看其他 Day。
- 用户刷新或重新进入训练页后，重新调用 W1，并使用新的 `defaultDayIndex`；最后一天完成时则进入 `cycle_completed` 状态。

常见失败：

- `WORKOUT_SESSION_NOT_FOUND`
- `WORKOUT_SESSION_STATUS_INVALID`
- `WORKOUT_SESSION_COMPLETE_FORBIDDEN`
- `WORKOUT_EXERCISE_STATUS_REQUIRED`
- `WORKOUT_SESSION_EXERCISE_INVALID`
- `WORKOUT_ITEM_INVALID`
- `WORKOUT_METRIC_INVALID`

### 5.6 W6 查看训练记录详情

- 路径：`GET /api/workouts/sessions/{sessionId}`
- 作用：查看当前用户的一条训练记录详情。
- 支持状态：`in_progress`、`completed`、`cancelled`。

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "sessionId": 490,
    "sessionType": "workout",
    "sessionStatus": "completed",
    "cycleRunId": 31,
    "runNo": 2,
    "templateId": 101,
    "templateName": "Push Pull Legs",
    "dayIndex": 1,
    "dayName": "推",
    "startedAt": "2026-07-27T19:00:00",
    "completedAt": "2026-07-27T20:10:00",
    "notes": null,
    "exercises": []
  }
}
```

实现规则：

- 只允许读取当前用户自己的 session。
- `completed` 和 `cancelled` 永远只读。
- `in_progress` 可被训练工作台继续编辑，但详情接口本身不改变状态。

### 5.7 W7 查询最近训练记录

- 路径：`GET /api/workouts/recent`
- 作用：分页获取当前用户最近训练记录，用于训练页简要历史和后续统计模块复用。

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|------|------|
| `page` | `number` | 否 | `1` | 从 1 开始 |
| `pageSize` | `number` | 否 | `20` | 最大 `50` |
| `sessionStatus` | `string` | 否 | 不过滤 | `completed` / `cancelled` / `in_progress` |

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "page": 1,
    "pageSize": 20,
    "total": 2,
    "records": [
      {
        "sessionId": 501,
        "sessionType": "workout",
        "sessionStatus": "completed",
        "templateId": 101,
        "templateName": "Push Pull Legs",
        "cycleRunId": 31,
        "runNo": 2,
        "dayIndex": 3,
        "dayName": "腿部训练",
        "startedAt": "2026-07-29T20:15:30",
        "completedAt": "2026-07-29T21:10:00"
      }
    ]
  }
}
```

排序规则：

- 优先按 `completedAt DESC`。
- `completedAt` 为空的进行中记录按 `startedAt DESC`。

### 5.8 W8 使用当前模板开启新一轮循环

- 路径：`POST /api/workouts/cycles/current/restart`
- 作用：仅在当前 cycle run 已完成时，使用同一 active 模板创建新一轮循环。
- 请求体：无。

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "templateId": 101,
    "templateName": "Push Pull Legs",
    "cycleRunId": 32,
    "runNo": 3,
    "cycleRunStatus": "active",
    "currentDayIndex": 1
  }
}
```

实现规则：

- 只允许当前用户执行。
- 当前 active 模板必须存在。
- 当前 `cycle_run.status` 必须为 `completed`。
- 创建新 `cycle_run`，`runNo` 在当前用户和模板维度递增。
- 更新 `user_active_cycles.current_run_id`、`template_version_id` 和 `current_day_index = 1`。
- 前端收到成功响应后，应重新调用 W1，再自动调用 W2 初始化 Day 1 session。

常见失败：

- `WORKOUT_ACTIVE_CYCLE_NOT_FOUND`
- `WORKOUT_CYCLE_RESTART_FORBIDDEN`

### 5.9 W9 AI 循环分析占位接口

- 路径：`POST /api/workouts/cycles/current/ai-analysis`
- 作用：为循环结束页的 AI 分析入口预留契约。
- 请求体：无。

当前 MVP 行为：

- 校验登录态。
- 校验存在当前已完成 cycle run。
- 直接返回 `WORKOUT_AI_NOT_IMPLEMENTED`。
- HTTP 状态码为 `501`。
- 不调用真实 AI。
- 不写入 AI 生成记录。

选择其他模板不是 workout API：

- 前端跳转 `/cycle-templates`。
- 用户按 `cycle_template` 模块的启用规则完成模板切换。

---

## 6. 推荐错误码

以下为本模块建议新增到后端 `ErrorCode` 的错误码：

| 错误码 | HTTP 状态码 | 含义 |
|------|:---:|------|
| `WORKOUT_ACTIVE_CYCLE_NOT_FOUND` | 409 | 没有可执行的当前循环 |
| `WORKOUT_CYCLE_COMPLETED` | 409 | 当前循环已完成，需先选择下一步 |
| `WORKOUT_CYCLE_RESTART_FORBIDDEN` | 409 | 当前循环未完成，不允许重新开启下一轮 |
| `WORKOUT_DAY_OUT_OF_RANGE` | 400 | `dayIndex` 不在当前循环范围内 |
| `WORKOUT_CURRENT_DAY_SESSION_CONFLICT` | 409 | 当前 Day session 初始化发生并发冲突 |
| `WORKOUT_SESSION_NOT_FOUND` | 404 | session 不存在或不属于当前用户 |
| `WORKOUT_SESSION_STATUS_INVALID` | 409 | 当前 session 状态不允许该操作 |
| `WORKOUT_SESSION_EDIT_FORBIDDEN` | 409 | session 不允许保存，例如已完成或已取消 |
| `WORKOUT_SESSION_COMPLETE_FORBIDDEN` | 409 | session 不属于当前真实 Day 或循环状态不允许完成 |
| `WORKOUT_SESSION_EXERCISE_INVALID` | 400 | 请求动作不属于当前 session 快照，或出现重复 / 缺失动作 |
| `WORKOUT_EXERCISE_STATUS_REQUIRED` | 400 | 完成训练日时存在未标记状态的动作 |
| `WORKOUT_EXERCISE_STATUS_INVALID` | 400 | 动作状态不在允许枚举内 |
| `WORKOUT_FAILURE_REASON_INVALID` | 400 | 失败原因不在允许枚举内 |
| `WORKOUT_ITEM_INVALID` | 400 | 执行项不属于当前动作快照，或出现重复 / 缺失执行项 |
| `WORKOUT_METRIC_INVALID` | 400 | 参数键不属于当前执行项快照，或出现重复 / 缺失参数 |
| `WORKOUT_METRIC_VALUE_INVALID` | 400 | 实际参数值格式或范围非法 |
| `WORKOUT_AI_ANALYSIS_COMPLETED_CYCLE_REQUIRED` | 409 | 当前循环未完成，不能请求 AI 循环分析 |
| `WORKOUT_AI_NOT_IMPLEMENTED` | 501 | AI 循环分析暂未实现 |

通用错误码继续复用：

- `UNAUTHORIZED`
- `INVALID_ARGUMENT`
- `INTERNAL_SERVER_ERROR`

---

## 7. 前后端联调约束

### 7.1 前端不得自行推断的字段

以下字段必须完全以接口响应为准：

- `workspaceState`
- `currentDayIndex`
- `defaultDayIndex`
- `dayState`
- `viewMode`
- `canInitializeSession`
- `sessionStatus`
- `sessionType`
- `metricUnit`
- `cycleRunStatus`

特别说明：

- 前端不能仅凭 `dayIndex < currentDayIndex` 自行推断当前页面是否可编辑；必须以 `viewMode` 为准。
- 前端不能在 W5 成功后自行将页面跳到 `nextCurrentDayIndex`。
- 前端不能为未来 Day 调用 W2。

### 7.2 自动初始化约束

- W2 由页面加载逻辑自动调用，不对应用户显式点击。
- W2 必须可安全重试；重复调用只能返回同一个 `in_progress` session。
- W2 成功后，前端不需要再调用 W3 获取同一个默认 Day。
- 如果 W2 返回 `WORKOUT_CYCLE_COMPLETED`，前端应重新调用 W1 并展示循环结束页。

### 7.3 保存与完成约束

- W4 和 W5 请求体采用相同的全量保存结构。
- W5 必须在一个事务中完成“保存实际记录 + 修改 session 状态 + 推进 cycle run / currentDayIndex”。
- W5 返回的是刚完成 Day 的详情，目的是让前端原地显示完成态。
- 前端完成打卡后如果继续编辑本地状态，不得再次调用 W4；已完成 session 只能只读。

### 7.4 与 Cycle Template 的跨模块约束

- `cycle_template` 对 active 模板未执行 Day 的修改，会更新当前运行引用的模板版本。
- 当前 Day 如果已通过 W2 创建 `in_progress` session，用户确认保存 active 模板后，系统会同步刷新为最新模板快照。
- 刷新会覆盖该 session 中的整体备注、动作状态、失败原因、动作反馈和实际参数；前端必须在调用模板保存接口前显示明确的二次确认提示，并在 C7 请求体中传 `confirmOverwriteCurrentSession = true`。
- 已完成或已取消的 session 必须使用创建/完成时快照，不受后续模板修改影响。
- 用户中途启用其他模板时：
  - 旧 `cycle_run` 必须更新为 `cancelled`，而不是 `completed`。
  - 旧 `in_progress` session 必须更新为 `cancelled`。
  - 旧 `completed` session 保留不变。
- 该规则与现有 `cycle_template_接口文档_v2.md` 中“旧 run 标记为 `completed`”的旧描述冲突；实现前必须同步修正该文档和对应激活服务。

---

## 8. 变更说明

本接口文档相对最初“点击开始训练”的草案，明确改为：

- 训练页不展示“开始训练”按钮。
- 使用 W1 + 前端自动调用 W2 实现默认当前 Day 的自动 session 初始化。
- 新增 W3 支持当前循环内 Day 浏览。
- 完成打卡后，W5 返回刚完成 Day 的详情；前端原地停留，不自动跳转。
- 刷新或重新进入时，前端才按 W1 的 `defaultDayIndex` 切换到最新未打卡 Day。
