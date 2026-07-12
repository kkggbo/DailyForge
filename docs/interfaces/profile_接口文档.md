# DailyForge Profile 模块接口文档

## 1. 文档说明

本文档描述 DailyForge 后端 `profile` 模块在 MVP 阶段的接口契约。

- 模块归属：`backend` 单体应用
- Controller 内部基础路径：`/profile`
- 对外接口前缀：`/api/profile`
- 统一返回包装：`ApiResponse`
- 当前实现状态：接口契约设计阶段，尚未正式落地

统一成功响应结构：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

统一失败响应结构：

```json
{
  "code": "INVALID_ARGUMENT",
  "message": "request arguments are invalid",
  "data": null
}
```

---

## 2. 通用约定

### 2.1 路由约定

所有 `profile` 接口统一挂载在：

`/api/profile`

### 2.2 认证约定

`profile` 模块所有接口均需要 Bearer Token。

请求头格式：

```http
Authorization: Bearer <accessToken>
```

### 2.3 数据语义约定

#### 基础档案

基础档案对应 `user_profiles`，用于存储低频变化资料。

当前约定：

- 所有基础档案字段都允许为空
- 基础档案保存接口不因为缺少某个字段而失败
- `completion-summary` 只用于提示资料完备度，不作为保存门槛

#### 当前体重

当前体重不存储在 `user_profiles` 中，而是由 `user_current_body_metrics` 提供。

#### 当前身体状态快照

`user_current_body_metrics` 是 `body_metric_logs` 的读模型，用于保存“用户当前已知的最新身体状态”。

语义约定：

- `body_metric_logs` 保存历史真相
- `user_current_body_metrics` 保存当前摘要
- 快照中的不同字段允许来自不同日期的最近一次非空记录

#### 身体指标记录

身体指标历史表 `body_metric_logs` 采用逻辑删除：

- `is_del=0`：有效记录
- `is_del=1`：已逻辑删除

删除规则：

- 仅允许删除当前用户按 `record_date DESC, id DESC` 排序后的最近一条记录
- 如果最近一条记录已经被逻辑删除，不允许再次删除，也不能跳过它去删更早记录

---

## 3. 接口列表

| 编号 | 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|------|
| P1 | GET | `/api/profile/basic` | 是 | 获取当前用户基础档案 |
| P2 | PUT | `/api/profile/basic` | 是 | 更新当前用户基础档案 |
| P3 | GET | `/api/profile/body-metrics/current` | 是 | 获取当前用户身体状态快照 |
| P4 | GET | `/api/profile/body-metrics` | 是 | 获取当前用户身体指标记录列表 |
| P5 | POST | `/api/profile/body-metrics` | 是 | 新增一条身体指标记录 |
| P6 | DELETE | `/api/profile/body-metrics/latest` | 是 | 逻辑删除最近一条身体指标记录 |
| P7 | GET | `/api/profile/completion-summary` | 是 | 获取资料完善状态摘要与 AI 资料情况 |

---

## 4. 接口详情

### 4.1 获取基础档案

- 路径：`GET /api/profile/basic`
- 认证：是
- 作用：返回当前登录用户基础档案，以及当前体重与最近身体指标记录日期摘要

请求头：

```http
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "gender": "male",
    "birthDate": "1998-06-15",
    "heightCm": 178.00,
    "goalType": "fat_loss",
    "trainingLevel": "beginner",
    "injuryNotes": "左膝旧伤，深蹲注意控制重量",
    "currentWeightKg": 76.50,
    "latestBodyMetricRecordDate": "2026-07-12"
  }
}
```

字段说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `data.gender` | `string \| null` | 是 | 性别，`male/female` |
| `data.birthDate` | `string \| null` | 是 | 出生日期，`YYYY-MM-DD` |
| `data.heightCm` | `number \| null` | 是 | 身高，单位 `cm` |
| `data.goalType` | `string \| null` | 是 | 目标，`fat_loss/muscle_gain/health_maintenance` |
| `data.trainingLevel` | `string \| null` | 是 | 训练经验，`beginner/experienced` |
| `data.injuryNotes` | `string \| null` | 是 | 伤病或注意事项 |
| `data.currentWeightKg` | `number \| null` | 是 | 当前已知体重，来自快照 |
| `data.latestBodyMetricRecordDate` | `string \| null` | 是 | 最近一条未删除身体指标记录日期 |

实现逻辑：

1. 从登录态中获取当前 `userId`
2. 查询 `user_profiles`
3. 查询 `user_current_body_metrics`
4. 查询最近一条未删除身体指标记录日期
5. 组装后返回

### 4.2 更新基础档案

- 路径：`PUT /api/profile/basic`
- 认证：是
- 作用：更新当前登录用户基础档案

请求头：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

请求体：

```json
{
  "gender": "male",
  "birthDate": "1998-06-15",
  "heightCm": 178.00,
  "goalType": "fat_loss",
  "trainingLevel": "beginner",
  "injuryNotes": "左膝旧伤，深蹲注意控制重量"
}
```

请求参数说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `gender` | `string \| null` | 否 | 性别，`male/female` |
| `birthDate` | `string \| null` | 否 | 出生日期，`YYYY-MM-DD` |
| `heightCm` | `number \| null` | 否 | 身高，单位 `cm` |
| `goalType` | `string \| null` | 否 | 目标，`fat_loss/muscle_gain/health_maintenance` |
| `trainingLevel` | `string \| null` | 否 | 训练经验，`beginner/experienced` |
| `injuryNotes` | `string \| null` | 否 | 伤病或注意事项，建议最大 1000 字符 |

成功响应：

返回结构与 `GET /api/profile/basic` 一致。

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `TOKEN_INVALID` / `TOKEN_EXPIRED`：Token 无效或过期，HTTP 401
- `USER_NOT_FOUND`：用户不存在，HTTP 404
- `ACCOUNT_DISABLED`：账户被禁用，HTTP 403
- `INVALID_ARGUMENT`：字段格式、枚举值或数值范围不合法，HTTP 400

实现逻辑：

1. 从登录态中获取当前 `userId`
2. 对传入字段做格式与枚举校验
3. 更新当前用户的 `user_profiles`
4. 返回最新基础档案信息

说明：

- 所有字段都可选
- 接口不再因为“基础资料缺失”而拒绝保存

### 4.3 获取当前身体状态快照

- 路径：`GET /api/profile/body-metrics/current`
- 认证：是
- 作用：获取当前登录用户的当前身体状态摘要

请求头：

```http
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "currentWeightKg": 76.50,
    "currentBodyFatPercent": 18.20,
    "currentBmi": 24.10,
    "currentSkeletalMusclePercent": 39.80,
    "currentBodyWaterPercent": 56.10,
    "currentBasalMetabolicRateKcal": 1680,
    "currentWaistCm": 82.00,
    "currentHipCm": 96.00,
    "currentWaistHipRatio": 0.85,
    "currentBodyAge": 24,
    "currentBodyType": "healthy",
    "updatedAt": "2026-07-12T20:15:30.000+08:00"
  }
}
```

实现逻辑：

1. 获取当前 `userId`
2. 查询 `user_current_body_metrics`
3. 返回快照

说明：

- 快照字段可能来自不同日期的最近一次非空记录

### 4.4 获取身体指标记录列表

- 路径：`GET /api/profile/body-metrics`
- 认证：是
- 作用：获取当前用户未删除的身体指标记录，按日期倒序返回

请求头：

```http
Authorization: Bearer <accessToken>
```

查询参数：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `page` | `number` | 否 | 页码，默认 `1` |
| `pageSize` | `number` | 否 | 每页数量，默认 `20`，最大 `100` |

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "page": 1,
    "pageSize": 10,
    "total": 2,
    "records": [
      {
        "id": 12,
        "recordDate": "2026-07-12",
        "weightKg": 76.50,
        "bodyFatPercent": 18.20,
        "bmi": 24.10,
        "skeletalMusclePercent": 39.80,
        "bodyWaterPercent": 56.10,
        "basalMetabolicRateKcal": 1680,
        "waistCm": 82.00,
        "hipCm": 96.00,
        "waistHipRatio": 0.85,
        "bodyAge": 24,
        "bodyType": "healthy",
        "note": "健身房体测",
        "isLatest": true
      }
    ]
  }
}
```

实现逻辑：

1. 获取当前 `userId`
2. 查询 `is_del=0` 的记录
3. 按 `record_date desc, id desc` 排序
4. 返回分页结果
5. 标记全局最新一条记录为 `isLatest=true`

### 4.5 新增身体指标记录

- 路径：`POST /api/profile/body-metrics`
- 认证：是
- 作用：为当前登录用户新增一条身体指标记录

请求头：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

请求体：

```json
{
  "recordDate": "2026-07-12",
  "weightKg": 76.50,
  "bodyFatPercent": 18.20,
  "bmi": 24.10,
  "skeletalMusclePercent": 39.80,
  "bodyWaterPercent": 56.10,
  "basalMetabolicRateKcal": 1680,
  "waistCm": 82.00,
  "hipCm": 96.00,
  "waistHipRatio": 0.85,
  "bodyAge": 24,
  "bodyType": "healthy",
  "note": "健身房体测"
}
```

请求参数说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `recordDate` | `string` | 是 | 记录日期，`YYYY-MM-DD` |
| `weightKg` | `number \| null` | 否 | 体重 |
| `bodyFatPercent` | `number \| null` | 否 | 体脂率 |
| `bmi` | `number \| null` | 否 | BMI |
| `skeletalMusclePercent` | `number \| null` | 否 | 骨骼肌率 |
| `bodyWaterPercent` | `number \| null` | 否 | 身体水分 |
| `basalMetabolicRateKcal` | `number \| null` | 否 | 基础代谢 |
| `waistCm` | `number \| null` | 否 | 腰围 |
| `hipCm` | `number \| null` | 否 | 臀围 |
| `waistHipRatio` | `number \| null` | 否 | 腰臀比 |
| `bodyAge` | `number \| null` | 否 | 身体年龄 |
| `bodyType` | `string \| null` | 否 | 体型 |
| `note` | `string \| null` | 否 | 备注 |

成功响应：

返回新创建记录，结构与列表项一致。

失败场景：

- `INVALID_ARGUMENT`：参数格式非法，HTTP 400
- `BODY_METRIC_EMPTY_RECORD`：没有任何有效指标值，HTTP 400

实现逻辑：

1. 获取当前 `userId`
2. 校验 `recordDate`
3. 校验至少一个指标字段非空
4. 插入新 `body_metric_logs` 记录，`is_del=0`
5. 仅用本次填写字段覆盖 `user_current_body_metrics`
6. 返回新记录

说明：

- `note` 不计入“有效指标值”
- 未填写字段保持为空，不自动复制旧值

### 4.6 删除最近一条身体指标记录

- 路径：`DELETE /api/profile/body-metrics/latest`
- 认证：是
- 作用：逻辑删除当前用户最近一条身体指标记录

请求头：

```http
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "deletedId": 12,
    "deletedRecordDate": "2026-07-12",
    "deletedWeightKg": 76.50
  }
}
```

失败场景：

- `BODY_METRIC_NOT_FOUND`：没有任何记录，HTTP 404
- `BODY_METRIC_LATEST_ALREADY_DELETED`：最近一条记录已经被逻辑删除，HTTP 409

实现逻辑：

1. 获取当前 `userId`
2. 查询该用户按 `record_date desc, id desc` 的最近一条记录
3. 如果不存在则失败
4. 如果 `is_del=1` 则不允许再次删除
5. 将该记录更新为逻辑删除
6. 基于剩余 `is_del=0` 记录重算快照
7. 返回被删除记录摘要

### 4.7 获取资料完善状态摘要

- 路径：`GET /api/profile/completion-summary`
- 认证：是
- 作用：返回资料完整度摘要与 AI 资料情况

请求头：

```http
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "basicProfileReady": true,
    "hasWeightRecord": true,
    "currentWeightKg": 76.50,
    "missingBasicProfileFields": [],
    "aiPlanReady": true,
    "aiPlanMissingFields": [],
    "aiNutritionReady": true,
    "aiNutritionMissingFields": [],
    "aiSummaryReady": true,
    "aiSummaryMissingFields": []
  }
}
```

说明：

- 这里的 `ready` 仅代表“资料是否足够支撑 AI 场景”
- 不代表 `profile` 模块保存时必须达到该状态

---

## 5. 鉴权与错误响应说明

### 5.1 401 响应

以下情况会返回 401：

- 未携带 access token 访问受保护接口
- token 非法
- token 过期
- token 类型错误

### 5.2 403 响应

以下情况会返回 403：

- 用户状态不是 `active`
- 已通过认证但没有权限访问受限资源

---

## 6. 推荐错误码

除通用安全错误码外，`profile` 模块建议新增：

| 错误码 | HTTP 状态 | 含义 |
|------|------|------|
| `BODY_METRIC_EMPTY_RECORD` | 400 | 身体指标记录没有任何有效指标值 |
| `BODY_METRIC_NOT_FOUND` | 404 | 身体指标记录不存在 |
| `BODY_METRIC_LATEST_ALREADY_DELETED` | 409 | 最近一条记录已被逻辑删除 |

---

## 7. 相关数据库改造建议

为支持当前接口契约，建议数据库侧补充：

### 7.1 `user_profiles`

- 保持资料字段可空
- `injury_notes` 长度提升到 `1000`

### 7.2 `body_metric_logs`

- 新增 `is_del`
- 建议新增 `deleted_at`
- `note` 长度提升到 `1000`
- 查询历史列表时默认过滤 `is_del=0`

### 7.3 `user_current_body_metrics`

新增快照表用于：

- 个人信息页当前状态回显
- AI 快速读取当前已知身体状态

---

## 8. Swagger 与调试约定

- `profile` 模块所有接口都应使用 `@SecurityRequirement(name = "bearerAuth")`
- DTO/VO 应补齐 `@Schema`
- 日志中不输出完整 access token
- 删除最近一条身体指标记录时应记录操作用户和被删除记录 ID

---

## 9. 当前设计备注

- `profile` 完全可选，不阻断普通功能使用
- AI 场景使用 `completion-summary` 做前置资料提示
- `body_metric_logs` 第一版不支持编辑，只支持新增和逻辑删除最近一条
- 当前状态一律来自 `user_current_body_metrics`，不在 `user_profiles` 中冗余存储
