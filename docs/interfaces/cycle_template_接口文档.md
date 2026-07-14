# DailyForge Cycle Template 模块接口文档

> 版本：v1.1  
> 更新时间：2026-07-14  
> 模块归属：`backend` 单体应用，代码包为 `com.dailyforge.modules.plan`

---

## 1. 文档说明

本文档用于描述 `cycle_template` 模块当前已经落地的后端接口契约，供前后端联调、测试和后续模块衔接使用。

- 外部接口前缀：`/api/cycle-templates`
- 鉴权方式：Bearer Token
- 统一返回包装：`ApiResponse`
- 当前实现状态：已完成 MVP 后端实现

成功响应示例：

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
  "code": "CYCLE_TEMPLATE_NOT_FOUND",
  "message": "cycle template not found",
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
- 列表接口不会返回 `deleted`

### 2.3 `cycleLength` 语义

- 草稿阶段允许 `cycleLength = null`
- 正式启用前必须满足 `cycleLength` 在 `1 ~ 7`
- `active` 模板运行中不允许修改 `cycleLength`

### 2.4 模板日与动作语义

- `dayIndex` 取值范围固定为 `1 ~ 7`
- 当 `cycleLength` 非空时，所有 `dayIndex` 必须 `<= cycleLength`
- `dayName` 为空或空白时，后端会标准化为 `Day {dayIndex}`
- `exercises = []` 视为该天是休息日
- 当前版本只允许引用系统动作：
  - `exercises.owner_user_id IS NULL`
  - `exercises.is_active = 1`

### 2.5 保存与版本语义

- `POST /drafts`：即使未传 `days`，也会创建模板主记录和空版本
- `PUT /drafts/{templateId}`：每次保存都会创建新版本，并用请求中的 `days` 全量覆盖当前草稿内容
- `PUT /{templateId}`：
  - `inactive` 模板：按完整模板全量覆盖保存，生成新版本
  - `active` 模板：只允许改 `currentDayIndex` 及之后的天；保存时“保留锁定天 + 用请求中的可编辑天全量覆盖可编辑范围”，并同步更新当前运行引用版本

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

## 4. 接口详情

### 4.1 C1 获取正式模板列表

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
        "updatedAt": "2026-07-14T20:15:30"
      }
    ]
  }
}
```

实现逻辑：

- 只查询 `status IN ('active', 'inactive')`
- 按 `updatedAt DESC` 排序
- `currentDayIndex` 仅对当前激活模板返回值，其余为 `null`

### 4.2 C2 获取草稿模板列表

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
        "createdAt": "2026-07-14T18:00:00",
        "updatedAt": "2026-07-14T19:10:00"
      }
    ]
  }
}
```

实现逻辑：

- 只查询 `status = 'draft'`
- `configuredDayCount = 当前版本 cycle_template_days 行数`

### 4.3 C3 获取模板详情

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
    "updatedAt": "2026-07-14T20:15:30",
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
            "targetSets": 4,
            "targetRepsMin": 6,
            "targetRepsMax": 8,
            "targetWeightKg": 60.0,
            "targetDurationSeconds": null,
            "restSeconds": 180,
            "targetRpe": 8.0,
            "note": "Last set close to failure",
            "targetExtraJson": {
              "pace": "5:30",
              "incline": 6
            }
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

### 4.4 C4 新建手动草稿模板

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
      "exercises": []
    }
  ]
}
```

请求字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `templateName` | `string` | 是 | 非空，最大 128 字符 |
| `cycleLength` | `number \| null` | 否 | 草稿阶段允许为空；非空时范围为 `1 ~ 7` |
| `goalType` | `string \| null` | 否 | 最大 32 字符 |
| `days` | `array \| null` | 否 | 不传或空数组都允许 |
| `days[].dayIndex` | `number` | 是 | 范围 `1 ~ 7` |
| `days[].dayName` | `string \| null` | 否 | 最大 64 字符；空白时后端标准化 |
| `days[].exercises` | `array \| null` | 否 | 空数组表示休息日 |
| `days[].exercises[].sortOrder` | `number` | 是 | 同一天内必须唯一 |
| `days[].exercises[].exerciseId` | `number` | 是 | 只能是系统动作 ID |
| `days[].exercises[].targetSets` | `number \| null` | 否 | 1 ~ 100 |
| `days[].exercises[].targetRepsMin` | `number \| null` | 否 | 1 ~ 1000 |
| `days[].exercises[].targetRepsMax` | `number \| null` | 否 | 1 ~ 1000 |
| `days[].exercises[].targetWeightKg` | `number \| null` | 否 | 0.00 ~ 9999.99 |
| `days[].exercises[].targetDurationSeconds` | `number \| null` | 否 | 1 ~ 86400 |
| `days[].exercises[].restSeconds` | `number \| null` | 否 | 0 ~ 86400 |
| `days[].exercises[].targetRpe` | `number \| null` | 否 | 0.00 ~ 10.00 |
| `days[].exercises[].note` | `string \| null` | 否 | 最大 500 字符 |
| `days[].exercises[].targetExtraJson` | `object \| null` | 否 | 特殊目标扩展 JSON |

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

### 4.5 C5 AI 生成草稿模板

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

失败示例：

```json
{
  "code": "CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED",
  "message": "ai-generated cycle template is not implemented yet",
  "data": null
}
```

### 4.6 C6 更新草稿模板

- 路径：`PUT /api/cycle-templates/drafts/{templateId}`
- 作用：保存草稿模板最新内容

请求体结构与 `POST /drafts` 相同。

实现逻辑：

- 只允许更新 `draft`
- 每次保存都会创建新版本
- 请求中的 `days` 被视为“当前草稿的完整已配置天集合”
- 未出现在 `days` 中的天会从新版本中消失
- 如果 `days` 为空或不传，会生成空版本

### 4.7 C7 更新正式模板

- 路径：`PUT /api/cycle-templates/{templateId}`
- 作用：更新 `inactive` 或 `active` 模板

请求体：

```json
{
  "templateName": "Push Pull Legs",
  "goalType": "muscle_gain",
  "cycleLength": 6,
  "days": [
    {
      "dayIndex": 3,
      "dayName": "Legs",
      "exercises": []
    }
  ]
}
```

实现逻辑：

- `inactive`：
  - 允许修改 `templateName`、`goalType`、`cycleLength`
  - `days` 按完整模板全量覆盖
  - 生成新版本并更新 `current_version_id`
- `active`：
  - 不允许修改 `cycleLength`
  - 只允许提交 `dayIndex >= currentDayIndex` 的天
  - 保存时会：
    - 保留锁定天原内容
    - 用请求中的天全量覆盖可编辑范围
    - 生成新版本
    - 更新 `cycle_templates.current_version_id`
    - 更新 `user_active_cycles.template_version_id`
    - 更新 `cycle_runs.template_version_id`

常见失败：

- `CYCLE_TEMPLATE_EDIT_FORBIDDEN`
- `CYCLE_TEMPLATE_DAY_OUT_OF_RANGE`
- `CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED`

### 4.8 C8 复制模板为草稿

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
- 复制主表基础信息与当前版本内容

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

### 4.9 C9 确认启用模板

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
3. 若已有旧激活模板：
   - 旧模板状态改为 `inactive`
   - 旧 `cycle_run` 状态改为 `completed`
4. 目标模板状态改为 `active`
5. 创建新的 `cycle_runs`
6. upsert `user_active_cycles`
7. `currentDayIndex` 重置为 `1`

### 4.10 C10 获取当前激活模板与运行摘要

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
    "startedAt": "2026-07-10T08:00:00"
  }
}
```

失败：

- `CYCLE_TEMPLATE_ACTIVE_NOT_FOUND`，HTTP 404

### 4.11 C11 删除模板

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

## 5. 错误码

| 错误码 | HTTP 状态码 | 含义 |
|------|------|------|
| `UNAUTHORIZED` | 401 | 未登录或 token 无效 |
| `INVALID_ARGUMENT` | 400 | 参数格式非法，如重复 `dayIndex`、重复 `sortOrder` |
| `CYCLE_TEMPLATE_NOT_FOUND` | 404 | 模板不存在或已删除 |
| `CYCLE_TEMPLATE_ACTIVE_NOT_FOUND` | 404 | 当前没有激活模板 |
| `CYCLE_TEMPLATE_CYCLE_LENGTH_INVALID` | 400 | `cycleLength` 非法 |
| `CYCLE_TEMPLATE_DAY_OUT_OF_RANGE` | 400 | `dayIndex` 超出 `cycleLength` |
| `CYCLE_TEMPLATE_EXERCISE_NOT_FOUND` | 404 | 动作不存在 |
| `CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED` | 400 | 只能使用系统动作 |
| `CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED` | 409 | 切换激活模板前需要二次确认 |
| `CYCLE_TEMPLATE_EDIT_FORBIDDEN` | 409 | 当前模板或模板日不允许修改 |
| `CYCLE_TEMPLATE_DELETE_FORBIDDEN` | 409 | 当前模板不允许删除 |
| `CYCLE_TEMPLATE_STATUS_INVALID` | 409 | 当前状态下不允许执行该操作 |
| `CYCLE_TEMPLATE_ACTIVATE_INVALID` | 400 | 模板不满足启用最小条件 |
| `CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED` | 501 | AI 生成功能暂未实现 |

---

## 6. 与训练打卡模块的衔接约定

- `GET /api/cycle-templates/active/current` 是训练打卡读取当前上下文的基础接口
- `PUT /api/cycle-templates/{templateId}` 对 `active` 模板的修改只影响当前天及未来天
- `currentDayIndex` 的自动推进不在本模块内完成，后续由训练打卡模块负责
- 当前模块已经为后续训练打卡预留了：
  - `cycle_runs`
  - `user_active_cycles`
  - `training_sessions`
  - `cycle_runs.template_version_id`

---

## 7. 备注

- 当前代码包名使用 `com.dailyforge.modules.plan`，但业务模块名称仍按 `cycle_template` 维护文档
- AI 生成草稿接口已经保留好对外契约，后续真正接 AI 时建议单独补充：
  - 权益校验
  - prompt / response 记录
  - `ai_generation_records` 落库
