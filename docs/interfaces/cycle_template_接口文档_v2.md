# DailyForge Cycle Template 模块接口文档 v2

> 版本：v2.0  
> 更新时间：2026-07-15  
> 模块归属：`backend` 单体应用，代码包为 `com.dailyforge.modules.plan`  
> 文档状态：接口契约设计阶段，尚未落代码

---

## 1. 文档说明

本文档描述 `cycle_template` 模块在“动作参数模型重构”后的 v2 接口契约。

相较 v1，核心变化是：

- 模板动作不再使用固定参数字段
- 动作改为三层结构：
  - 动作
  - 执行项
  - 执行项参数
- `cycle_template` 与未来 `training_session` 预对齐相同的数据语义

外部接口前缀仍为：

- `/api/cycle-templates`

统一返回包装仍为：

- `ApiResponse`

统一成功响应示例：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

统一失败响应示例：

```json
{
  "code": "CYCLE_TEMPLATE_METRIC_KEY_INVALID",
  "message": "cycle template metric key is invalid",
  "data": null
}
```

---

## 2. 通用约定

### 2.1 路由与鉴权

所有接口都要求登录态：

```http
Authorization: Bearer <accessToken>
```

### 2.2 模板状态语义

- `draft`：草稿模板
- `active`：当前激活模板
- `inactive`：当前未激活的正式模板
- `deleted`：软删除模板

约束：

- 同一用户同一时间只能有一个 `active` 模板
- `draft` 和 `inactive` 可同时存在多个
- 列表接口不返回 `deleted`

### 2.3 `cycleLength` 语义

- 草稿阶段允许 `cycleLength = null`
- 正式启用前必须满足 `cycleLength` 在 `1 ~ 7`
- `active` 模板运行中不允许修改 `cycleLength`

### 2.4 动作三层结构语义

每个模板动作由三层组成：

1. 动作 `exercise`
2. 执行项 `item`
3. 执行项参数 `metric`

结构示意：

```json
{
  "exerciseId": 1001,
  "structureType": "set_based",
  "items": [
    {
      "itemIndex": 1,
      "itemType": "set",
      "metrics": [
        {
          "metricKey": "weight_kg",
          "metricValueNumber": 60
        },
        {
          "metricKey": "reps",
          "metricValueNumber": 8
        }
      ]
    }
  ]
}
```

### 2.5 结构类型

MVP 只支持两种：

- `set_based`
- `single_segment`

说明：

- `set_based`
  - 适用于力量、器械、自重等按组训练动作
- `single_segment`
  - 适用于跑步、椭圆机、爬坡、骑行等持续型动作

### 2.6 执行项类型

MVP 只支持两种：

- `set`
- `segment`

约束：

- `set_based` 只能使用 `set`
- `single_segment` 只能使用 `segment`

### 2.7 参数字典

MVP 为封闭字典，仅允许以下 `metricKey`：

- `weight_kg`
- `reps`
- `duration_seconds`
- `distance_km`
- `speed_kmh`
- `pace_seconds_per_km`
- `incline_percent`
- `rest_seconds`
- `rpe`
- `intensity_level`

说明：

- 当前不支持用户自定义 metricKey
- 同一个执行项下同一个 `metricKey` 不允许重复
- 每个执行项至少要有 1 个 metric
- 当前 MVP 这批 `metricKey` 全部视为数值型参数
- 因此前端请求体中所有 metric 都必须传 `metricValueNumber`

### 2.7.1 参数单位规则

- 前端请求体不传 `metricUnit`
- 后端根据 `metricKey` 推导规范单位
- 数据库不存储 `metricUnit`
- 响应体可返回 `metricUnit`，仅用于前端展示

### 2.8 `structureType` 来源与限制

`structureType` 由动作元数据决定：

- 来自 `exercises.default_structure_type`

MVP 规则：

- 前端在选动作时应带出默认结构
- 请求体仍需显式传 `structureType`
- 后端会校验其必须与动作默认结构一致
- 不允许用户把某个动作随意改成另一种结构类型

### 2.9 `single_segment` 限制

MVP 阶段：

- `single_segment` 只能有 1 个执行项
- 该执行项的 `itemIndex` 固定建议为 `1`

### 2.10 备注字段语义

保留两层备注：

- `exercise.note`
  - 整个动作的总体说明
- `item.note`
  - 某一组/某一段的单独说明

不把 `note` 作为 metric。

### 2.11 保存与版本语义

- `POST /drafts`
  - 即使未传 `days`，也会创建模板主记录和空版本
- `PUT /drafts/{templateId}`
  - 每次保存都创建新版本
  - 请求中的 `days` 是草稿完整内容
- `PUT /{templateId}`
  - `inactive`：按完整模板全量覆盖保存，生成新版本
  - `active`：只允许改 `currentDayIndex` 及之后的天，并同步更新当前运行引用版本

---

## 3. 接口列表

| 编号 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|------|
| C1 | GET | `/api/cycle-templates/formal` | 是 | 获取正式模板列表 |
| C2 | GET | `/api/cycle-templates/drafts` | 是 | 获取草稿模板列表 |
| C3 | GET | `/api/cycle-templates/{templateId}` | 是 | 获取模板详情 |
| C4 | POST | `/api/cycle-templates/drafts` | 是 | 新建手动草稿模板 |
| C5 | POST | `/api/cycle-templates/drafts/ai-generate` | 是 | AI 生成草稿模板占位接口 |
| C6 | PUT | `/api/cycle-templates/drafts/{templateId}` | 是 | 更新草稿模板 |
| C7 | PUT | `/api/cycle-templates/{templateId}` | 是 | 更新正式模板 |
| C8 | POST | `/api/cycle-templates/{templateId}/copy` | 是 | 复制模板为草稿 |
| C9 | POST | `/api/cycle-templates/{templateId}/activate` | 是 | 确认启用模板 |
| C10 | GET | `/api/cycle-templates/active/current` | 是 | 获取当前激活模板与运行摘要 |
| C11 | DELETE | `/api/cycle-templates/{templateId}` | 是 | 删除模板 |

---

## 4. 公共数据结构

## 4.1 动作请求体

```json
{
  "sortOrder": 1,
  "exerciseId": 1001,
  "structureType": "set_based",
  "note": "主动作",
  "items": [
    {
      "itemIndex": 1,
      "itemType": "set",
      "itemName": "第1组",
      "note": "热身组",
      "metrics": [
        {
          "sortOrder": 1,
          "metricKey": "weight_kg",
          "metricValueNumber": 40
        },
        {
          "sortOrder": 2,
          "metricKey": "reps",
          "metricValueNumber": 12
        }
      ]
    }
  ]
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `sortOrder` | `number` | 是 | 当天动作排序，需唯一 |
| `exerciseId` | `number` | 是 | 系统动作 ID |
| `structureType` | `string` | 是 | `set_based` / `single_segment`，必须与动作默认结构一致 |
| `note` | `string \| null` | 否 | 动作总体备注，最大 500 字符 |
| `items` | `array` | 是 | 执行项列表，至少 1 个 |
| `items[].itemIndex` | `number` | 是 | 执行项序号，同一动作下唯一 |
| `items[].itemType` | `string` | 是 | `set` / `segment` |
| `items[].itemName` | `string \| null` | 否 | 显示名称，最大 64 字符 |
| `items[].note` | `string \| null` | 否 | 执行项备注，最大 500 字符 |
| `items[].metrics` | `array` | 是 | 参数列表，至少 1 个 |
| `items[].metrics[].sortOrder` | `number` | 是 | 参数排序 |
| `items[].metrics[].metricKey` | `string` | 是 | 必须在封闭字典中 |
| `items[].metrics[].metricValueNumber` | `number` | 是 | 数值参数值；当前 MVP 所有 metricKey 都必须使用该字段 |

## 4.2 动作响应体

```json
{
  "sortOrder": 1,
  "exerciseId": 1001,
  "exerciseName": "Barbell Bench Press",
  "structureType": "set_based",
  "note": "主动作",
  "items": [
    {
      "itemIndex": 1,
      "itemType": "set",
      "itemName": "第1组",
      "note": "热身组",
      "metrics": [
        {
          "sortOrder": 1,
          "metricKey": "weight_kg",
          "metricValueNumber": 40,
          "metricUnit": "kg"
        }
      ]
    }
  ]
}
```

与请求体相比，新增：

- `exerciseName`
- `items[].metrics[].metricUnit`

说明：

- `metricUnit` 不来自前端请求
- 由后端按 `metricKey` 推导后返回

---

## 5. 接口详情

### 5.1 C1 获取正式模板列表

- 路径：`GET /api/cycle-templates/formal`
- 作用：返回当前用户全部 `active` 与 `inactive` 模板

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "activeTemplateId": 101,
    "records": [
      {
        "templateId": 101,
        "templateName": "Push Pull Legs",
        "cycleLength": 6,
        "goalType": "muscle_gain",
        "status": "active",
        "isActive": true,
        "currentDayIndex": 3,
        "updatedAt": "2026-07-15T20:15:30"
      }
    ]
  }
}
```

实现逻辑：

- 只查询 `status IN ('active', 'inactive')`
- 按 `updatedAt DESC` 排序
- `currentDayIndex` 仅对当前激活模板返回值

### 5.2 C2 获取草稿模板列表

- 路径：`GET /api/cycle-templates/drafts`
- 作用：返回当前用户全部 `draft` 模板

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "records": [
      {
        "templateId": 201,
        "templateName": "五分化尝试版",
        "cycleLength": 5,
        "configuredDayCount": 3,
        "createdAt": "2026-07-15T18:00:00",
        "updatedAt": "2026-07-15T19:10:00"
      }
    ]
  }
}
```

实现逻辑：

- 只查询 `status = 'draft'`
- `configuredDayCount` 继续定义为“当前版本已配置的 day 数”

### 5.3 C3 获取模板详情

- 路径：`GET /api/cycle-templates/{templateId}`
- 作用：返回模板详情页与编辑页所需完整结构

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "templateId": 101,
    "templateName": "Push Pull Legs",
    "goalType": "muscle_gain",
    "status": "active",
    "cycleLength": 6,
    "isActive": true,
    "currentDayIndex": 3,
    "editableFromDayIndex": 3,
    "canActivate": false,
    "canDelete": false,
    "createdAt": "2026-07-01T20:00:00",
    "updatedAt": "2026-07-15T20:15:30",
    "days": [
      {
        "dayIndex": 1,
        "dayName": "Push",
        "isRestDay": false,
        "isLocked": true,
        "exercises": [
          {
            "sortOrder": 1,
            "exerciseId": 1001,
            "exerciseName": "Barbell Bench Press",
            "structureType": "set_based",
            "note": "主动作",
            "items": [
              {
                "itemIndex": 1,
                "itemType": "set",
                "itemName": "第1组",
                "note": "热身组",
                "metrics": [
                  {
                    "sortOrder": 1,
                    "metricKey": "weight_kg",
                    "metricValueNumber": 40,
                    "metricUnit": "kg"
                  },
                  {
                    "sortOrder": 2,
                    "metricKey": "reps",
                    "metricValueNumber": 12,
                    "metricUnit": "count"
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
```

实现逻辑：

- `draft` / `inactive`：`editableFromDayIndex = 1`
- `active`：`editableFromDayIndex = currentDayIndex`
- `isLocked = true` 表示该天不允许编辑
- `canActivate` 取决于：
  - 模板状态是否为 `draft` 或 `inactive`
  - `templateName` 非空
  - `currentVersionId` 非空
  - `cycleLength` 在 `1 ~ 7`
  - 所有动作结构合法

### 5.4 C4 新建手动草稿模板

- 路径：`POST /api/cycle-templates/drafts`
- 作用：创建一个新的草稿模板

请求体：

```json
{
  "templateName": "新模板",
  "cycleLength": 5,
  "goalType": "fat_loss",
  "days": [
    {
      "dayIndex": 1,
      "dayName": "Push",
      "exercises": [
        {
          "sortOrder": 1,
          "exerciseId": 1001,
          "structureType": "set_based",
          "note": "主动作",
          "items": [
            {
              "itemIndex": 1,
              "itemType": "set",
              "itemName": "第1组",
              "note": "热身组",
              "metrics": [
                {
                  "sortOrder": 1,
                  "metricKey": "weight_kg",
                  "metricValueNumber": 40
                },
                {
                  "sortOrder": 2,
                  "metricKey": "reps",
                  "metricValueNumber": 12
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

请求字段说明：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `templateName` | `string` | 是 | 非空，最大 128 字符 |
| `cycleLength` | `number \| null` | 否 | 草稿阶段允许为空；非空时范围为 `1 ~ 7` |
| `goalType` | `string \| null` | 否 | 最大 32 字符 |
| `days` | `array \| null` | 否 | 不传或空数组都允许 |
| `days[].dayIndex` | `number` | 是 | 范围 `1 ~ 7` |
| `days[].dayName` | `string \| null` | 否 | 最大 64 字符；空白时后端标准化 |
| `days[].exercises` | `array \| null` | 否 | 空数组表示休息日 |

动作结构字段说明见“4. 公共数据结构”。

关键校验规则：

- `structureType` 必须与动作默认结构一致
- `set_based`
  - `items` 至少 1 个
  - `items[].itemType` 必须为 `set`
- `single_segment`
  - `items` 必须且只能有 1 个
  - `items[0].itemType` 必须为 `segment`
- 每个 item 至少 1 个 metric
- 同一个 item 下不允许重复 `metricKey`
- `metricKey` 必须来自封闭字典
- 每个 metric 必须传 `metricValueNumber`

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "templateId": 201,
    "status": "draft"
  }
}
```

实现逻辑：

- 插入 `cycle_templates(status='draft')`
- 生成 `version_no = 1`
- 即使 `days` 为空，也会创建空版本并回写 `current_version_id`

### 5.5 C5 AI 生成草稿模板

- 路径：`POST /api/cycle-templates/drafts/ai-generate`
- 作用：MVP 占位接口，当前不做真实 AI 生成

请求体：

```json
{
  "goalType": "muscle_gain",
  "cycleLength": 5,
  "prompt": "Create a 5-day muscle gain split",
  "useProfileData": true
}
```

当前实现行为：

- 要求登录态
- 校验请求参数格式
- 直接返回 `CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED`
- HTTP 状态码为 `501`
- 不校验 AI 权益
- 不写入任何模板、版本、AI 调用记录

### 5.6 C6 更新草稿模板

- 路径：`PUT /api/cycle-templates/drafts/{templateId}`
- 作用：保存草稿模板最新内容

请求体结构与 `POST /drafts` 相同。

实现逻辑：

- 只允许更新 `draft`
- 每次保存都会创建新版本
- 请求中的 `days` 被视为当前草稿完整内容
- 未出现在 `days` 中的天会从新版本中消失
- `items` 与 `metrics` 也按完整覆盖处理，不做增量 patch

### 5.7 C7 更新正式模板

- 路径：`PUT /api/cycle-templates/{templateId}`
- 作用：更新 `inactive` 或 `active` 模板

请求体结构与 `POST /drafts` 相同。

实现逻辑：

- `inactive`
  - 允许修改 `templateName`、`goalType`、`cycleLength`
  - `days` / `exercises` / `items` / `metrics` 按完整模板全量覆盖
  - 生成新版本并更新 `current_version_id`
- `active`
  - 不允许修改 `cycleLength`
  - 只允许提交 `dayIndex >= currentDayIndex` 的天
  - 保存时会：
    - 保留锁定天原内容
    - 只替换请求中提交的可编辑天
    - 未提交的未来天默认保留原内容
    - 生成新版本
    - 更新 `cycle_templates.current_version_id`
    - 更新 `user_active_cycles.template_version_id`
    - 更新 `cycle_runs.template_version_id`

常见失败：

- `CYCLE_TEMPLATE_EDIT_FORBIDDEN`
- `CYCLE_TEMPLATE_DAY_OUT_OF_RANGE`
- `CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED`
- `CYCLE_TEMPLATE_STRUCTURE_TYPE_INVALID`
- `CYCLE_TEMPLATE_ITEM_INVALID`
- `CYCLE_TEMPLATE_METRIC_KEY_INVALID`

### 5.8 C8 复制模板为草稿

- 路径：`POST /api/cycle-templates/{templateId}/copy`
- 作用：把任意未删除模板复制为新的草稿模板

请求体：

```json
{
  "templateName": "Push Pull Legs - Copy"
}
```

实现逻辑：

- 可复制来源：`draft`、`inactive`、`active`
- 复制结果统一为 `draft`
- 复制主表基础信息以及完整三层动作结构

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "templateId": 301,
    "status": "draft"
  }
}
```

### 5.9 C9 确认启用模板

- 路径：`POST /api/cycle-templates/{templateId}/activate`
- 作用：把 `draft` 或 `inactive` 模板切换为当前激活模板

请求体：

```json
{
  "confirmSwitch": true
}
```

说明：

- 请求体可省略，省略时等价于 `{"confirmSwitch": false}`
- 若当前已有其他 `active` 模板，必须传 `confirmSwitch = true`

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "templateId": 101,
    "status": "active",
    "currentDayIndex": 1,
    "previousActiveTemplateId": 88
  }
}
```

实现逻辑：

1. 校验目标模板状态必须为 `draft` 或 `inactive`
2. 校验模板满足最小启用条件
3. 校验模板中所有动作结构合法
4. 若已有旧激活模板：
   - 旧模板状态改为 `inactive`
   - 旧 `cycle_run` 状态改为 `completed`
5. 目标模板状态改为 `active`
6. 创建新的 `cycle_runs`
7. upsert `user_active_cycles`
8. `currentDayIndex` 重置为 `1`

### 5.10 C10 获取当前激活模板与运行摘要

- 路径：`GET /api/cycle-templates/active/current`
- 作用：为训练打卡模块和模板首页提供当前上下文

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "templateId": 101,
    "templateName": "Push Pull Legs",
    "cycleLength": 6,
    "currentDayIndex": 3,
    "currentDayName": "Legs",
    "editableFromDayIndex": 3,
    "startedAt": "2026-07-15T08:00:00"
  }
}
```

失败：

- `CYCLE_TEMPLATE_ACTIVE_NOT_FOUND`，HTTP 404

### 5.11 C11 删除模板

- 路径：`DELETE /api/cycle-templates/{templateId}`
- 作用：软删除模板

实现逻辑：

- 只允许删除 `draft` 或 `inactive`
- `active` 不允许直接删除
- 删除方式为更新 `status = 'deleted'`
- 不删除历史版本、历史 run 和未来训练记录

响应体：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "templateId": 102,
    "status": "deleted"
  }
}
```

---

## 6. 错误码

在保留 v1 错误码基础上，v2 建议新增以下错误码：

| 错误码 | HTTP 状态码 | 含义 |
|------|------|------|
| `UNAUTHORIZED` | 401 | 未登录或 token 无效 |
| `INVALID_ARGUMENT` | 400 | 通用参数格式非法 |
| `CYCLE_TEMPLATE_NOT_FOUND` | 404 | 模板不存在或已删除 |
| `CYCLE_TEMPLATE_ACTIVE_NOT_FOUND` | 404 | 当前没有激活模板 |
| `CYCLE_TEMPLATE_CYCLE_LENGTH_INVALID` | 400 | `cycleLength` 非法 |
| `CYCLE_TEMPLATE_DAY_OUT_OF_RANGE` | 400 | `dayIndex` 超出 `cycleLength` |
| `CYCLE_TEMPLATE_EXERCISE_NOT_FOUND` | 404 | 动作不存在 |
| `CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED` | 400 | 只能使用系统动作 |
| `CYCLE_TEMPLATE_STRUCTURE_TYPE_INVALID` | 400 | `structureType` 非法或与动作默认结构不一致 |
| `CYCLE_TEMPLATE_ITEM_INVALID` | 400 | 执行项结构非法 |
| `CYCLE_TEMPLATE_ITEM_COUNT_INVALID` | 400 | 执行项数量非法，例如 `single_segment` 不等于 1 个 item |
| `CYCLE_TEMPLATE_METRIC_KEY_INVALID` | 400 | 参数 key 不在封闭字典中 |
| `CYCLE_TEMPLATE_METRIC_DUPLICATE` | 400 | 同一 item 下重复 `metricKey` |
| `CYCLE_TEMPLATE_METRIC_VALUE_INVALID` | 400 | 参数值非法，例如未传 `metricValueNumber` 或传入非数值 |
| `CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED` | 409 | 切换激活模板前需要二次确认 |
| `CYCLE_TEMPLATE_EDIT_FORBIDDEN` | 409 | 当前模板或模板日不允许修改 |
| `CYCLE_TEMPLATE_DELETE_FORBIDDEN` | 409 | 当前模板不允许删除 |
| `CYCLE_TEMPLATE_STATUS_INVALID` | 409 | 当前状态下不允许执行该操作 |
| `CYCLE_TEMPLATE_ACTIVATE_INVALID` | 400 | 模板不满足启用最小条件 |
| `CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED` | 501 | AI 生成功能暂未实现 |

---

## 7. 与训练打卡模块的衔接约定

- `GET /api/cycle-templates/active/current` 仍然是训练打卡读取当前上下文的基础接口
- `GET /api/cycle-templates/{templateId}` 返回的三层动作结构，应作为未来训练打卡计划快照的参考结构
- `PUT /api/cycle-templates/{templateId}` 对 `active` 模板的修改只影响当前天及未来天
- `currentDayIndex` 的自动推进不在本模块内完成，后续由训练打卡模块负责
- 未来 `training_session` 模块应尽量复用同一结构语义：
  - 动作
  - 执行项
  - 执行项参数

## 7.1 依赖接口约定

为了让前端在“选择系统动作”时正确初始化模板动作结构，系统动作搜索/详情接口至少需要返回以下字段：

- `exerciseId`
- `exerciseName`
- `defaultStructureType`

最小响应示例：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "records": [
      {
        "exerciseId": 1001,
        "exerciseName": "Barbell Bench Press",
        "defaultStructureType": "set_based"
      },
      {
        "exerciseId": 2001,
        "exerciseName": "Treadmill Running",
        "defaultStructureType": "single_segment"
      }
    ]
  }
}
```

说明：

- `cycle_template` 前端编辑器不应自行推断 `structureType`
- 应以系统动作接口返回的 `defaultStructureType` 为准

---

## 8. 备注

- 当前代码包名仍使用 `com.dailyforge.modules.plan`，但业务模块名称按 `cycle_template` 维护文档
- `structureType` 虽然出现在请求体中，但产品层面不是用户可自由编辑字段
- AI 生成草稿接口后续真正接入时，建议单独补充：
  - 权益校验
  - prompt / response 记录
  - `ai_generation_records` 落库
