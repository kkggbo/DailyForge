# DailyForge Frontend Profile API 对接说明

> 版本：v1.1  
> 日期：2026-08-23  
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

## 3. 前端 API 文件现状

`profile.ts` 已导出以下函数：

```ts
getBasicProfile(accessToken)
updateBasicProfile(accessToken, payload)
getCurrentBodyMetricSnapshot(accessToken)
getBodyMetricsPage(accessToken, query)
createBodyMetric(accessToken, payload)
deleteLatestBodyMetric(accessToken)
getProfileCompletionSummary(accessToken)
```

> 说明：`getProfileCompletionSummary` 仍保留，但个人资料总览 / 编辑 / 历史三页已不再调用它（完成度 banner 已移除），当前主要由 AI 补录等场景使用。

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

## 6. 前端基础设施现状

`profile` 对接所需的两点基础设施已落地（不再是「建议」）：

- `shared/api/http.ts` 已支持 query 参数拼接。
- `shared/api/http.ts` 已抛出结构化 `ApiRequestError`（含 `code` / `status`）。

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

