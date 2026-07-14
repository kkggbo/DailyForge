# DailyForge Frontend Profile 模块详细设计

> 版本：v1.1  
> 日期：2026-07-14  
> 模块归属：`frontend/src/features/profile`

---

## 1. 模块目标

前端 `profile` 模块负责承接用户基础档案、身体指标记录，以及 AI 场景下的资料补录引导。

当前版本的设计目标：

1. 让用户可以在任意时间维护基础档案。
2. 让用户可以新增身体指标记录，并查看当前快照与历史记录。
3. 让首次登录用户通过轻量引导补充资料，但不把资料填写变成使用门槛。
4. 在 AI 相关场景前，给出明确的资料缺失提示与补录入口。

---

## 2. 设计输入

本文档基于以下文档与当前已落地代码整理：

- [profile_PRD.md](/D:/Computer%20Science/DailyForge/docs/prd/profile_PRD.md)
- [profile_页面实现.md](/D:/Computer%20Science/DailyForge/docs/frontend/profile_module/profile_页面实现.md)
- [profile_接口文档.md](/D:/Computer%20Science/DailyForge/docs/interfaces/profile_接口文档.md)

当前对接接口：

- `GET /api/profile/basic`
- `PUT /api/profile/basic`
- `GET /api/profile/body-metrics/current`
- `GET /api/profile/body-metrics`
- `POST /api/profile/body-metrics`
- `DELETE /api/profile/body-metrics/latest`
- `GET /api/profile/completion-summary`

---

## 3. 模块定位

`profile` 不是普通的“设置页”，而是 DailyForge 的用户画像输入层。

它与其他模块的关系：

- `auth`：依赖登录态和 Bearer Token。
- `home`：登录后用户可从应用主导航进入 `profile`。
- `ai`：依赖 `completion-summary` 判断资料是否满足 AI 场景输入条件。
- `stats`：后续身体趋势分析依赖身体指标历史。

---

## 4. 目录结构

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
│  ├─ profile-mappers.ts
│  └─ onboarding-storage.ts
├─ pages
│  ├─ ProfilePage.tsx
│  ├─ ProfileOnboardingPage.tsx
│  └─ ProfileAiCompletionPage.tsx
└─ types
   └─ profile.ts
```

---

## 5. 当前页面与路由

### 5.1 `ProfilePage`

路由：`/profile`

作用：

- 正常的个人资料管理主页面。
- 包含 `基础档案` 和 `身体指标` 两个 Tab。

### 5.2 `ProfileOnboardingPage`

路由：`/profile/onboarding`

作用：

- 首次登录后的资料引导页。
- 为轻量引导，不是强制门槛。

### 5.3 `ProfileAiCompletionPage`

路由：`/profile/ai-completion`

作用：

- 在 AI 场景前做资料补录。
- 支持 `scene` 和 `redirect` 查询参数。

---

## 6. 关键数据模型

### 6.1 基础档案

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

### 6.2 当前身体指标快照

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

### 6.3 身体指标历史

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

---

## 7. API 层设计

`src/features/profile/api/profile.ts` 对外暴露：

| 方法 | 接口 | 作用 |
|------|------|------|
| `getBasicProfile` | `GET /api/profile/basic` | 获取基础档案 |
| `updateBasicProfile` | `PUT /api/profile/basic` | 更新基础档案 |
| `getCurrentBodyMetricSnapshot` | `GET /api/profile/body-metrics/current` | 获取当前快照 |
| `getBodyMetricsPage` | `GET /api/profile/body-metrics` | 获取身体指标分页 |
| `createBodyMetric` | `POST /api/profile/body-metrics` | 新增身体指标记录 |
| `deleteLatestBodyMetric` | `DELETE /api/profile/body-metrics/latest` | 删除最新记录 |
| `getProfileCompletionSummary` | `GET /api/profile/completion-summary` | 获取资料完成度摘要 |

分页默认值：

- `page = 1`
- `pageSize = 20`

---

## 8. 共享表单设计

### 8.1 `BasicProfileForm`

当前表单行为：

- 生日默认值为 `2000-01-01`。
- 出生日期继续使用原生 `input[type="date"]`。
- 下拉框使用深色背景方案，保证暗色主题下可读。
- 身高仅允许输入整数，范围 `1 ~ 300`。
- 身高输入隐藏浏览器默认 spinner。
- 支持注入 `secondaryAction` 和 `actionsAlign`，用于 onboarding 和普通资料页复用不同按钮布局。

前端校验：

- `heightCm`：必须为 `1 ~ 300` 的整数。
- `injuryNotes.length <= 1000`

### 8.2 `BodyMetricForm`

当前表单行为：

- 页面上不显示 `recordDate` 输入框。
- 提交时自动把 `recordDate` 写为用户本地当天日期。
- 小数字段步进统一为 `0.1`。
- `bodyAge` 步进为 `1`。
- 备注栏只有在任一指标字段有值时才显示。
- 支持 `allowEmptySubmit`，只在 onboarding 第二步启用。
- 支持 `initialValue`，用于把当前快照映射到表单默认值。
- 支持 `showClearAction`，在资料页展示“清空”按钮。

当前指标字段：

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

前端校验：

- 普通模式下至少填写一个指标字段。
- onboarding 第二步允许空提交直接完成引导。
- `bodyFatPercent <= 100`
- `bodyWaterPercent <= 100`
- `bodyAge` 必须为 `0 ~ 150` 的整数
- `bodyType.length <= 32`
- `note.length <= 1000`

---

## 9. 页面状态与数据流

### 9.1 `ProfilePage` 初始加载

并行请求：

1. `GET /api/profile/basic`
2. `GET /api/profile/completion-summary`
3. `GET /api/profile/body-metrics/current`
4. `GET /api/profile/body-metrics?page=1&pageSize=20`

### 9.2 基础档案保存后

刷新：

1. `basicProfile`
2. `completionSummary`

### 9.3 身体指标新增后

刷新：

1. `basicProfile`
2. `completionSummary`
3. `snapshot`
4. `history`

### 9.4 删除最新记录后

刷新：

1. `basicProfile`
2. `completionSummary`
3. `snapshot`
4. `history`

---

## 10. `ProfilePage` 当前实现规则

### 10.1 顶部信息区

显示：

- 页面标题
- 资料说明文案
- `ProfileTabNav`
- `CompletionSummaryBanner`

### 10.2 基础档案 Tab

使用 `BasicProfileForm`。

提交按钮文案：

- `保存基础档案`

### 10.3 身体指标 Tab

由三部分组成：

1. `BodyMetricSummaryCard`
2. `BodyMetricForm`
3. `BodyMetricHistoryList`

当前实现新增的细节：

- 表单默认回填当前快照中的指标值，减少重复输入。
- 用户可通过“清空”按钮改为从空表单重新填写。
- 回填只发生在 `/profile` 资料页，不影响 onboarding 和 AI 补录页。

### 10.4 删除最新记录交互

使用 `DeleteLatestMetricDialog` 二次确认。

当前实现规则：

- 删除失败时，不只写页面级错误。
- 弹窗内部也会显示错误提示，避免被遮罩层挡住。
- 对以下错误码做了中文业务提示映射：
  - `BODY_METRIC_LATEST_ALREADY_DELETED`
  - `BODY_METRIC_NOT_FOUND`

---

## 11. `ProfileOnboardingPage` 当前实现规则

当前为两步流程：

1. 欢迎说明 + 基础档案
2. 身体指标录入

第一步按钮区：

- 不再提供“下一步，暂不保存”
- 只保留：
  - `跳过并进入应用`
  - `保存并继续`

布局规则：

- 两个按钮右对齐
- `跳过并进入应用` 在左
- `保存并继续` 在右

第二步规则：

- 提供 `上一步`
- 提供 `跳过并进入应用`
- 允许身体指标全空直接完成引导

---

## 12. `ProfileAiCompletionPage` 当前实现规则

页面目标：

- 在 `ai-plan` / `ai-nutrition` / `ai-summary` 场景下补齐资料。

顶部展示：

- 当前场景标题和说明
- 当前场景仍缺少的关键字段列表
- 当前场景是否已满足输入条件

交互规则：

- 第一步可“稍后补充”或切到第二步先看身体指标
- 第二步可返回上一步或“稍后补充”
- 完成后优先跳转 `redirect`
- 如果没有 `redirect`，回到 `/profile`

注意：

- AI 补录页仍保留“至少填写一个指标字段”校验。
- 允许空提交只在 onboarding 第二步启用。

---

## 13. 枚举与映射

统一定义在 `profile-enums.ts`：

- `gender`
  - `male -> 男`
  - `female -> 女`
- `goalType`
  - `fat_loss -> 减脂`
  - `muscle_gain -> 增肌`
  - `health_maintenance -> 保持健康 / 维持状态`
- `trainingLevel`
  - `beginner -> 新手`
  - `experienced -> 有经验`

字段标签映射同时用于：

- 完成度缺失字段中文显示
- AI 补录页面缺失项展示

---

## 14. 错误处理

前端通过 `ApiRequestError` 识别结构化错误。

当前重点错误码：

- `INVALID_ARGUMENT`
- `BODY_METRIC_EMPTY_RECORD`
- `BODY_METRIC_NOT_FOUND`
- `BODY_METRIC_LATEST_ALREADY_DELETED`
- `UNAUTHORIZED`
- `TOKEN_INVALID`
- `TOKEN_EXPIRED`
- `FORBIDDEN`

当前特殊处理：

- 删除最新记录失败时，弹窗内直接展示业务文案。

---

## 15. 与注册登录链路的衔接

虽然不属于 `profile` 目录本身，但当前实现与 `profile` 引导强相关：

- 注册成功后，前端会立即复用现有登录接口自动登录。
- 自动登录成功后进入 `/app`。
- 用户首次登录时，如果本地未记录 onboarding 已完成，则继续进入 `/profile/onboarding`。

因此，`profile` onboarding 的真实入口已经从“注册后跳登录页”调整为“注册成功后自动登录，再进入首次引导”。

---

## 16. 当前版本开发结论

当前 `profile` 模块已经从最初的“基础 CRUD”收敛成一套更完整的资料录入体验：

1. onboarding 不再打断主流程，但体验更顺滑。
2. 资料页身体指标录入支持复用上次数据并手动清空。
3. 删除最新记录的失败提示已经回到当前操作上下文。
4. AI 补录、首次引导、普通资料页三条入口共用一套主要表单组件，但各自保留必要的行为差异。
