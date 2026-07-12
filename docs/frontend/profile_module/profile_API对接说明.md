# DailyForge Frontend Profile API 对接说明

> 版本：v1.0  
> 日期：2026-07-12  
> 模块归属：`frontend/src/features/profile/api`

---

## 1. 文档目标

本文档专门用于指导前端 `profile` 模块的 API 层实现，重点说明：

- 实际请求方法
- 请求体和响应体
- 查询参数
- 错误码处理
- 前端类型设计建议

---

## 2. 接口清单

### 2.1 获取基础档案

- 方法：`GET`
- 路径：`/api/profile/basic`
- 鉴权：需要 Bearer Token

成功响应 `data`：

```ts
type ProfileBasicResponse = {
  gender: "male" | "female" | null;
  birthDate: string | null;
  heightCm: number | null;
  goalType: "fat_loss" | "muscle_gain" | "health_maintenance" | null;
  trainingLevel: "beginner" | "experienced" | null;
  injuryNotes: string | null;
  currentWeightKg: number | null;
  latestBodyMetricRecordDate: string | null;
};
```

### 2.2 更新基础档案

- 方法：`PUT`
- 路径：`/api/profile/basic`
- 鉴权：需要 Bearer Token

请求体：

```ts
type UpdateProfileBasicPayload = {
  gender: "male" | "female" | null;
  birthDate: string | null;
  heightCm: number | null;
  goalType: "fat_loss" | "muscle_gain" | "health_maintenance" | null;
  trainingLevel: "beginner" | "experienced" | null;
  injuryNotes: string | null;
};
```

成功响应类型与基础档案读取结果一致，只是后端 VO 名称为 `ProfileBasicUpdateResponse`。

### 2.3 获取当前身体快照

- 方法：`GET`
- 路径：`/api/profile/body-metrics/current`
- 鉴权：需要 Bearer Token

成功响应：

```ts
type BodyMetricSnapshotResponse = {
  currentWeightKg: number | null;
  currentBodyFatPercent: number | null;
  currentBmi: number | null;
  currentSkeletalMusclePercent: number | null;
  currentBodyWaterPercent: number | null;
  currentBasalMetabolicRateKcal: number | null;
  currentWaistCm: number | null;
  currentHipCm: number | null;
  currentWaistHipRatio: number | null;
  currentBodyAge: number | null;
  currentBodyType: string | null;
  updatedAt: string | null;
};
```

### 2.4 获取身体指标历史分页

- 方法：`GET`
- 路径：`/api/profile/body-metrics`
- 鉴权：需要 Bearer Token

查询参数：

```ts
type BodyMetricPageQuery = {
  page?: number;
  pageSize?: number;
};
```

成功响应：

```ts
type BodyMetricsPageResponse = {
  page: number;
  pageSize: number;
  total: number;
  records: BodyMetricLogItemResponse[];
};
```

### 2.5 新增身体指标记录

- 方法：`POST`
- 路径：`/api/profile/body-metrics`
- 鉴权：需要 Bearer Token

请求体：

```ts
type CreateBodyMetricPayload = {
  recordDate: string;
  weightKg: number | null;
  bodyFatPercent: number | null;
  bmi: number | null;
  skeletalMusclePercent: number | null;
  bodyWaterPercent: number | null;
  basalMetabolicRateKcal: number | null;
  waistCm: number | null;
  hipCm: number | null;
  waistHipRatio: number | null;
  bodyAge: number | null;
  bodyType: string | null;
  note: string | null;
};
```

成功响应：

```ts
type BodyMetricLogItemResponse = {
  id: number;
  recordDate: string;
  weightKg: number | null;
  bodyFatPercent: number | null;
  bmi: number | null;
  skeletalMusclePercent: number | null;
  bodyWaterPercent: number | null;
  basalMetabolicRateKcal: number | null;
  waistCm: number | null;
  hipCm: number | null;
  waistHipRatio: number | null;
  bodyAge: number | null;
  bodyType: string | null;
  note: string | null;
  isLatest: boolean;
};
```

### 2.6 删除最新一条身体指标记录

- 方法：`DELETE`
- 路径：`/api/profile/body-metrics/latest`
- 鉴权：需要 Bearer Token

成功响应：

```ts
type DeleteLatestBodyMetricResponse = {
  deletedId: number;
  deletedRecordDate: string;
  deletedWeightKg: number | null;
};
```

### 2.7 获取资料完成度摘要

- 方法：`GET`
- 路径：`/api/profile/completion-summary`
- 鉴权：需要 Bearer Token

成功响应：

```ts
type ProfileCompletionSummaryResponse = {
  basicProfileReady: boolean;
  hasWeightRecord: boolean;
  currentWeightKg: number | null;
  missingBasicProfileFields: string[];
  aiPlanReady: boolean;
  aiPlanMissingFields: string[];
  aiNutritionReady: boolean;
  aiNutritionMissingFields: string[];
  aiSummaryReady: boolean;
  aiSummaryMissingFields: string[];
};
```

---

## 3. 前端 API 文件建议

建议在 `profile.ts` 中导出：

```ts
getBasicProfile(accessToken)
updateBasicProfile(accessToken, payload)
getCurrentBodyMetricSnapshot(accessToken)
getBodyMetricsPage(accessToken, query)
createBodyMetric(accessToken, payload)
deleteLatestBodyMetric(accessToken)
getProfileCompletionSummary(accessToken)
```

---

## 4. 建议的错误处理策略

### 4.1 基础档案保存

重点识别：

- `INVALID_ARGUMENT`
- `UNAUTHORIZED`
- `FORBIDDEN`

### 4.2 身体指标新增

重点识别：

- `INVALID_ARGUMENT`
- `BODY_METRIC_EMPTY_RECORD`

前端建议把：

- `BODY_METRIC_EMPTY_RECORD`

转成更友好的文案：

“请至少填写一个身体指标，不能只提交备注。”

### 4.3 删除最新记录

重点识别：

- `BODY_METRIC_NOT_FOUND`
- `BODY_METRIC_LATEST_ALREADY_DELETED`

前端收到这两个错误时应主动刷新列表与摘要，避免用户看到脏状态。

---

## 5. 字段展示映射建议

后端返回的 `missing...Fields` 是英文字段名，前端需要自行映射成中文。

建议映射表：

```ts
gender -> 性别
birthDate -> 出生日期
heightCm -> 身高
goalType -> 训练目标
weightKg -> 体重
```

如果后端未来增加字段，前端也要同步扩充这份映射。

---

## 6. 当前前端基础设施的改造建议

为了顺畅对接 `profile`，建议在真正写代码前对现有基础设施做两点增强。

### 6.1 `shared/api/http.ts` 支持 query 参数

目前 `request` 只适合静态路径。  
`profile` 会首次用到分页查询，因此建议新增：

- query 参数拼接工具

### 6.2 `shared/api/http.ts` 返回结构化错误

当前只抛 `Error(message)`，不保留 `code`。  
而 `profile` 模块有明确业务错误码，建议升级为：

```ts
type ApiRequestError = Error & {
  code?: string;
  status?: number;
};
```

这样页面才能做真正可控的错误分支。

---

## 7. 前端联调顺序建议

建议联调顺序如下：

1. 先通 `GET /basic` 与 `PUT /basic`
2. 再通 `GET /completion-summary`
3. 再通 `GET /body-metrics/current`
4. 再通 `GET /body-metrics`
5. 再通 `POST /body-metrics`
6. 最后通 `DELETE /body-metrics/latest`

原因：

- 基础档案和完成度摘要最容易先看到业务闭环
- 身体指标是最复杂的一段，放后面更稳

