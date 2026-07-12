# DailyForge Frontend Profile 模块详细设计

> 版本：v1.0  
> 日期：2026-07-12  
> 模块归属：`frontend/src/features/profile`

---

## 1. 模块目标

前端 `profile` 模块用于承接用户基础资料、身体指标记录以及 AI 场景前置资料检查的全部交互流程。

当前模块需要覆盖三类目标：

1. 让用户可以随时维护基础档案。
2. 让用户可以追加身体指标记录，并查看当前状态与最近历史。
3. 在 AI 相关场景进入前，对资料是否足够做前端提示与补录引导。

---

## 2. 设计输入

本模块文档基于以下文档整理：

- [profile_PRD.md](/D:/Computer%20Science/DailyForge/docs/prd/profile_PRD.md)
- [profile_接口文档.md](/D:/Computer%20Science/DailyForge/docs/interfaces/profile_接口文档.md)
- [profile_DDD.md](/D:/Computer%20Science/DailyForge/docs/backend/profile_module/profile_DDD.md)

同时对齐当前后端真实实现：

- `GET /api/profile/basic`
- `PUT /api/profile/basic`
- `GET /api/profile/body-metrics/current`
- `GET /api/profile/body-metrics`
- `POST /api/profile/body-metrics`
- `DELETE /api/profile/body-metrics/latest`
- `GET /api/profile/completion-summary`

---

## 3. 前端模块定位

`profile` 模块不是一个单独的“设置页”，而是 DailyForge 用户画像输入层的一部分。

它与其他模块的关系如下：

- `auth`：依赖登录态与 Bearer Token
- `home`：可以从控制台进入 `profile`
- `ai`：依赖 `completion-summary` 判断资料是否足够
- `plan`：AI 生成计划前可能需要跳转到资料补录引导
- `stats`：后续趋势图和摘要统计会依赖身体指标历史

---

## 4. 推荐目录结构

建议前端按以下结构组织：

```text
src/features/profile
├─ api
│  └─ profile.ts
├─ components
│  ├─ ProfileTabNav.tsx
│  ├─ BasicProfileForm.tsx
│  ├─ BodyMetricSummaryCard.tsx
│  ├─ BodyMetricForm.tsx
│  ├─ BodyMetricHistoryList.tsx
│  ├─ CompletionSummaryBanner.tsx
│  └─ DeleteLatestMetricDialog.tsx
├─ lib
│  ├─ profile-enums.ts
│  ├─ profile-formatters.ts
│  └─ profile-mappers.ts
├─ pages
│  ├─ ProfilePage.tsx
│  ├─ ProfileOnboardingPage.tsx
│  └─ ProfileAiCompletionPage.tsx
└─ types
   └─ profile.ts
```

### 4.1 分层说明

#### `api`

负责：

- 封装后端 `profile` 接口
- 定义请求与响应类型
- 把后端契约整理成前端可直接消费的方法

#### `components`

负责：

- `profile` 模块内部复用 UI
- 避免 `ProfilePage` 过大
- 隔离“表单区”“摘要区”“历史区”等版块

#### `lib`

负责：

- 枚举项定义
- 中文标签映射
- 表单初始值与空值处理
- 前端展示格式化

#### `pages`

负责：

- 页面级路由入口
- 请求时机
- 页面整体布局
- 模块内状态编排

---

## 5. 页面与路由设计

建议本模块至少设计三个页面入口。

### 5.1 `ProfilePage`

建议路由：

`/profile`

作用：

- 正常个人信息管理页
- 包含“基础档案”和“身体指标”两个主要 Tab

### 5.2 `ProfileOnboardingPage`

建议路由：

`/profile/onboarding`

作用：

- 注册成功后的首次欢迎与资料引导页
- 采用两步流

### 5.3 `ProfileAiCompletionPage`

建议路由：

`/profile/ai-completion`

作用：

- 当用户进入 AI 计划、AI 饮食、AI 总结前资料不完整时，引导补录
- 页面结构可以与首次 onboarding 共用，但文案需要不同

---

## 6. 数据模型设计

前端建议直接按后端契约定义类型。

### 6.1 基础档案类型

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

### 6.2 基础档案更新请求

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

### 6.3 当前身体快照

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

### 6.4 身体指标历史项

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

### 6.5 身体指标分页结果

```ts
type BodyMetricsPageResponse = {
  page: number;
  pageSize: number;
  total: number;
  records: BodyMetricLogItemResponse[];
};
```

### 6.6 资料完成度摘要

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

## 7. API 层设计

建议新增：

- `src/features/profile/api/profile.ts`

对外暴露方法如下：

| 方法 | 接口 | 作用 |
|------|------|------|
| `getBasicProfile` | `GET /api/profile/basic` | 获取基础档案 |
| `updateBasicProfile` | `PUT /api/profile/basic` | 保存基础档案 |
| `getCurrentBodyMetricSnapshot` | `GET /api/profile/body-metrics/current` | 获取当前身体状态摘要 |
| `getBodyMetricsPage` | `GET /api/profile/body-metrics` | 获取身体指标历史分页 |
| `createBodyMetric` | `POST /api/profile/body-metrics` | 新增一条身体指标记录 |
| `deleteLatestBodyMetric` | `DELETE /api/profile/body-metrics/latest` | 删除最新一条记录 |
| `getProfileCompletionSummary` | `GET /api/profile/completion-summary` | 获取资料完成度摘要 |

### 7.1 查询参数约定

`getBodyMetricsPage` 需要支持：

```ts
type BodyMetricPageQuery = {
  page?: number;
  pageSize?: number;
};
```

建议默认：

- `page = 1`
- `pageSize = 20`

### 7.2 请求封装建议

当前项目已有 `shared/api/http.ts`，但它只适合简单路径请求。  
如果继续沿用，建议为 `profile` 模块增加一个查询串拼接工具，例如：

```ts
getBodyMetricsPage(accessToken, { page, pageSize })
```

内部拼接成：

`/profile/body-metrics?page=1&pageSize=20`

### 7.3 错误码对接

前端需要重点识别的 profile 模块错误码：

- `INVALID_ARGUMENT`
- `BODY_METRIC_EMPTY_RECORD`
- `BODY_METRIC_NOT_FOUND`
- `BODY_METRIC_LATEST_ALREADY_DELETED`
- `UNAUTHORIZED`
- `TOKEN_INVALID`
- `TOKEN_EXPIRED`
- `FORBIDDEN`

建议前端保留 `code`，不要只显示 `message`。后续实现时建议扩展当前 `request` 封装，使其能向上抛出结构化错误对象。

---

## 8. 页面状态设计

### 8.1 `ProfilePage` 页面状态

建议由页面持有以下状态：

| 状态 | 作用 |
|------|------|
| `activeTab` | 当前是基础档案还是身体指标 |
| `basicProfile` | 基础档案详情 |
| `completionSummary` | 完成度摘要 |
| `snapshot` | 当前身体快照 |
| `bodyMetricPage` | 身体指标分页数据 |
| `isLoadingProfile` | 初次加载状态 |
| `isSavingBasicProfile` | 保存基础档案状态 |
| `isSubmittingMetric` | 新增身体指标状态 |
| `isDeletingLatestMetric` | 删除最新记录状态 |
| `page` / `pageSize` | 当前历史列表分页状态 |

### 8.2 基础档案表单状态

建议单独在 `BasicProfileForm` 内维护表单草稿，字段包括：

- `gender`
- `birthDate`
- `heightCm`
- `goalType`
- `trainingLevel`
- `injuryNotes`

另外维护：

- `dirty`
- `fieldErrors`
- `formError`

### 8.3 身体指标表单状态

建议单独在 `BodyMetricForm` 内维护：

- `recordDate`
- `weightKg`
- `bodyFatPercent`
- `bmi`
- `skeletalMusclePercent`
- `bodyWaterPercent`
- `basalMetabolicRateKcal`
- `waistCm`
- `hipCm`
- `waistHipRatio`
- `bodyAge`
- `bodyType`
- `note`

并维护：

- `fieldErrors`
- `formError`
- `isDirty`

---

## 9. 页面数据流设计

### 9.1 `ProfilePage` 初始化

页面进入时建议并行请求：

1. `GET /api/profile/basic`
2. `GET /api/profile/completion-summary`
3. `GET /api/profile/body-metrics/current`
4. `GET /api/profile/body-metrics?page=1&pageSize=20`

如果要控制首屏速度，也可以使用“两阶段加载”：

第一阶段：

- `basic`
- `completion-summary`
- `body-metrics/current`

第二阶段：

- `body-metrics`

### 9.2 保存基础档案后

保存成功后建议同步刷新：

1. `basicProfile`
2. `completionSummary`

快照不一定需要刷新，因为基础档案保存不会直接改变身体快照。

### 9.3 新增身体指标后

新增成功后建议同步刷新：

1. `snapshot`
2. `bodyMetricPage`
3. `completionSummary`
4. `basicProfile`

说明：

- `basicProfile.currentWeightKg`
- `basicProfile.latestBodyMetricRecordDate`

都依赖身体指标记录，因此也要同步刷新。

### 9.4 删除最新身体指标后

删除成功后建议同步刷新：

1. `snapshot`
2. `bodyMetricPage`
3. `completionSummary`
4. `basicProfile`

---

## 10. 交互规则设计

### 10.1 基础档案 Tab

前端应体现以下规则：

- 字段允许为空
- 不要把“AI 需要这些字段”误写成“必须填写”
- 重点是提示，而不是阻塞

建议在表单顶部放一条说明：

“这些资料会帮助系统生成更贴合你的训练与饮食建议，当前不强制填写完整。”

### 10.2 身体指标 Tab

当前页面建议分三块：

1. 当前身体状态摘要
2. 身体指标录入表单
3. 最近历史记录

### 10.3 新增身体指标规则

前端要提前提示两件事：

1. `recordDate` 必填
2. 至少填写一个有效身体指标，只有备注不能提交

即使后端也会校验，前端仍然应该在提交前做一次轻量校验，降低往返成本。

### 10.4 删除最新一条记录

前端必须有二次确认，建议文案类似：

“只允许删除最新一条身体指标记录。删除后将重新计算当前身体状态摘要，是否继续？”

如果后端返回：

- `BODY_METRIC_NOT_FOUND`
- `BODY_METRIC_LATEST_ALREADY_DELETED`

前端应展示明确提示，并刷新列表，避免页面继续停留在旧状态。

---

## 11. Onboarding 与 AI 补录流设计

### 11.1 `ProfileOnboardingPage`

建议做成两步流程：

1. 欢迎说明 + 基础档案
2. 身体指标录入

页面需要支持：

- 下一步
- 上一步
- 跳过并进入应用
- 保存并继续

### 11.2 `ProfileAiCompletionPage`

此页面建议与 onboarding 共用大部分布局与表单组件，但差异在于：

- 标题更强调“为生成更准确结果补充资料”
- 顶部展示当前缺失字段清单
- 页面完成后返回触发来源页

### 11.3 AI 场景缺失字段展示

建议把后端返回的：

- `aiPlanMissingFields`
- `aiNutritionMissingFields`
- `aiSummaryMissingFields`

映射成中文标签，例如：

- `gender` -> `性别`
- `birthDate` -> `出生日期`
- `heightCm` -> `身高`
- `goalType` -> `训练目标`
- `weightKg` -> `体重`

---

## 12. 枚举与展示映射建议

建议在 `profile-enums.ts` 中统一定义。

### 12.1 性别

```ts
male   -> 男
female -> 女
```

### 12.2 训练目标

```ts
fat_loss            -> 减脂
muscle_gain         -> 增肌
health_maintenance  -> 维持体态 / 保持健康
```

### 12.3 训练经验

```ts
beginner    -> 新手
experienced -> 有经验
```

### 12.4 体型

当前后端 `bodyType` 是字符串，并未做枚举约束。前端第一版建议先按自由文本输入，不强行下拉枚举化，以免与后端实际值不一致。

---

## 13. 表单校验建议

### 13.1 基础档案表单

前端建议校验：

- `gender` 只能是 `male | female`
- `heightCm > 0 且 <= 300`
- `injuryNotes.length <= 1000`

### 13.2 身体指标表单

前端建议校验：

- `recordDate` 必填
- 所有数字字段不得小于 0
- `bodyFatPercent <= 100`
- `bodyWaterPercent <= 100`
- `bodyAge <= 150`
- `bodyType.length <= 32`
- `note.length <= 1000`
- 至少一个有效指标字段非空

---

## 14. 推荐开发顺序

前端建议按以下顺序开发：

1. 先完成 `profile.ts` API 层与类型定义
2. 搭建 `ProfilePage` 页面骨架和 Tab
3. 实现基础档案读取与保存
4. 实现当前身体快照展示
5. 实现身体指标新增表单
6. 实现历史记录列表与分页
7. 实现删除最新记录
8. 实现 `completion-summary` 展示
9. 最后再实现 onboarding 与 AI 补录流程

---

## 15. 当前前端实现注意点

结合项目现状，建议特别注意：

1. 继续沿用模块化结构，不要把 profile 逻辑塞进 `app` 或 `home`。
2. 当前 `shared/api/http.ts` 需要增强 query 参数和结构化错误支持。
3. 当前项目没有 React Query，profile 第一版可以先用页面级请求编排，但如果数据联动变复杂，应尽快引入数据请求缓存层。
4. 当前前端文案已经清洗过乱码，新模块新增文件时要继续保持 UTF-8。

