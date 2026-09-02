# DailyForge Frontend 饮食模块 详细设计

> 版本：v0.2
> 日期：2026-09-03
> 模块归属：`frontend/src/features/diet`
> 契约来源：`docs/interfaces/diet_接口文档.md`
> 关联 PRD：`docs/prd/diet_PRD.md`

> 变更记录：v0.2 —— 同步两点决策：① `missingFields` 现可含 `activityLevel`，资料不足提示缺失项含「活动量」，profile 编辑表单新增 activityLevel 下拉（5 档：久坐/轻度/中度/高强度/极高）；② `FoodItem` 增加 `source`/`sourceLabel`/`ownerNickname`，食物卡片/选择器展示来源标签（官方/用户 + 用户食物脱敏昵称）。

---

## 1. 文档目标

本文档定义「饮食日记」模块（`diet`）在前端的实现边界、目录结构、类型、页面/组件职责、路由与入口、与个人资料页的衔接、错误/空态/资料不足提示、手机适配与文件规划，作为本轮前端开发与联调的技术基线。

沿用 `docs/frontend/stats_module/stats_DDD.md` 的结构与风格（含图表 recharts 复用）。它是**独立新模块**，不改动既有 `workout` / `stats` / `profile` / `ai-coach` 模块行为。

已确认交互细节（来自 PRD）：

1. **AppShell 导航新增「饮食」入口**；个人资料页承载「活动量（activityLevel）」字段编辑。
2. 三个页面：日记首页 `/diet`（默认今天，可回看/补录）、食物库 `/diet/foods`、摄入统计 `/diet/stats`。
3. **不阻断策略**：资料不足（无每日目标）时，日记仍正常展示「已摄入」与各餐明细，仅隐藏/温和提示目标进度条，并引导补齐；食物库/统计均不受影响。
4. 手机适配：日记的添加入口与操作按钮在手机上可点可达。

---

## 2. 模块定位与职责边界

### 2.1 定位

`diet` 是「记录与展示」模块：让用户记录吃什么、看当天热量/宏量进度、管理食物库、看长期摄入趋势。它不产生训练数据，只消费用户资料（性别/生日/身高/体重/目标/活动量）做目标展示。

### 2.2 职责

`diet` 前端**负责**：

- 日记首页：日期导航、目标进度条（可缺失提示）、三餐/加餐明细、增删改记录。
- 添加记录：餐次 → 食物选择 → 克数 → 实时营养计算 → 保存。
- 食物库：搜索 + 过滤（最常/最近/收藏/全部）、食物详情、上传、收藏。
- 摄入统计页：每日热量折线、宏量占比、周均值、目标符合度（无目标时隐藏）。
- 每日目标查看与自定义覆盖（入口在日记页或资料页）。

`diet` 前端**不负责**：

- 营养基准/BMR/TDEE/宏量拆分计算（由后端 `GET /diet/summary`、`GET /diet/targets` 返回）。
- 资料数据存储（复用 `profile`）。
- 饮水记录、食谱/组合餐、饮食 AI 建议、分享、上传审核（PRD §10 本版不含）。

### 2.3 依赖

- `auth`：登录态与 `accessToken`。
- `profile`：`GoalType`、`activityLevel`（新增字段，见 §7）、资料缺失提示复用。
- `shared/api/http.ts`：统一 `request`。
- `recharts`：统计页图表（已在 stats 模块引入，复用，无需新依赖）。

---

## 3. 推荐目录结构

新增独立 feature 目录 `frontend/src/features/diet/**`，沿用 `api / components / lib / pages / types` 五层：

```text
src/features/diet
├─ api
│  └─ diet.ts
├─ components
│  ├─ NutrientProgressBar.tsx
│  ├─ MealSection.tsx
│  ├─ AddMealLogDialog.tsx
│  ├─ FoodPickerDialog.tsx
│  ├─ FoodDetailDialog.tsx
│  ├─ UploadFoodDialog.tsx
│  ├─ DietTargetCard.tsx
│  └─ DietMissingFieldsNotice.tsx
├─ lib
│  ├─ diet-enums.ts
│  ├─ diet-mappers.ts
│  └─ diet-validation.ts
├─ pages
│  ├─ DietDiaryPage.tsx
│  ├─ DietFoodsPage.tsx
│  └─ DietStatsPage.tsx
└─ types
   └─ diet.ts
```

说明：

- 食物选择/详情/上传/添加记录做成**弹窗组件**，供日记与食物库页复用（沿用 `ProfileCompletionModal` 的 `open/onClose` 受控弹窗模式）。
- 图表复用 `stats` 模块的 recharts 用法，但组件独立（避免跨模块耦合 UI 细节）。

---

## 4. 路由与入口

`router.tsx` 新增（受保护路由 `ProtectedOutlet` children）：

```tsx
{
  path: "/diet",
  element: <DietDiaryPage />
},
{
  path: "/diet/foods",
  element: <DietFoodsPage />
},
{
  path: "/diet/stats",
  element: <DietStatsPage />
}
```

`AppShell` 登录态导航新增「饮食」：

```tsx
<NavLink to="/diet" className={navLinkClass}>饮食</NavLink>
```

`/diet` 为日记首页；食物库与统计页从日记页顶部分段入口/链接进入（与 `/ai-coach/history` 类似的分段导航），也可在 AppShell 通过 `/diet` 进入后切换。

---

## 5. 数据模型设计（`types/diet.ts`）

> 全部字段对齐 `docs/interfaces/diet_接口文档.md`。

### 5.1 通用枚举

```ts
export type MealType = "breakfast" | "lunch" | "dinner" | "snack";
export type FoodCategory =
  | "staple" | "meat_egg" | "vegetable" | "fruit" | "dairy"
  | "nut_bean" | "drink" | "other";
export type ActivityLevel =
  | "sedentary" | "light" | "moderate" | "high" | "very_high";
export type FoodSource = "system" | "user";
export type DietTargetBasis = "auto" | "custom";
```

### 5.2 每日目标

```ts
export type DietTarget = {
  basis: DietTargetBasis | null;
  caloriesKcal: number | null;
  proteinG: number | null;
  carbsG: number | null;
  fatG: number | null;
};

// GET /diet/targets：额外带缺失字段
export type DietTargetResponse = DietTarget & {
  missingFields: string[];
};
```

### 5.3 记录项与当日总结

```ts
export type NutrientValues = {
  caloriesKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
};

export type MealLogItem = NutrientValues & {
  logId: number;
  foodId: number;
  foodName: string;
  grams: number;
};

export type DaySummary = {
  date: string;
  target: DietTarget | null;
  meals: Record<MealType, MealLogItem[]>;
  totals: NutrientValues;
  progress: {
    caloriesPct: number;
    proteinPct: number;
    carbsPct: number;
    fatPct: number;
  } | null; // target 为 null 时为 null
};
```

### 5.4 食物

```ts
export type FoodItem = {
  foodId: number;
  name: string;
  category: FoodCategory | null;
  source: FoodSource;
  // 来源展示：官方 / 用户（后端返回）；用户食物带上传者脱敏昵称
  sourceLabel: string;
  ownerNickname: string | null;
  caloriesKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  favorited: boolean;
};

export type FoodListResponse = {
  foods: FoodItem[];
};

export type FoodQuery = {
  keyword?: string;
  filter?: "all" | "recent" | "frequent" | "favorite";
};
```

说明：

- `source` / `sourceLabel`：`system` → 「官方」，`user` → 「用户」（标签文案放 `diet-enums.ts`）。
- `ownerNickname`：用户食物显示上传者的**脱敏昵称**（如「张**」）；系统食物为 `null`。展示规则见 §8.5 食物详情/§8.10 食物来源标签。

### 5.5 请求体

```ts
export type CreateMealLogPayload = {
  date: string; // yyyy-MM-dd
  mealType: MealType;
  foodId: number;
  grams: number;
};

export type UpdateMealLogPayload = {
  grams: number;
  mealType: MealType;
  date: string;
};

export type UploadFoodPayload = {
  name: string;
  category?: FoodCategory | null;
  caloriesKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
};

export type SetDietTargetPayload = {
  caloriesKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  clear?: boolean;
};
```

### 5.6 摄入统计

```ts
export type DietStats = {
  dailyCalories: Array<{ date: string; caloriesKcal: number }>;
  macroShare: { proteinPct: number; carbsPct: number; fatPct: number };
  weeklyAverage: Array<{
    weekStart: string;
    caloriesKcal: number;
    proteinG: number;
    carbsG: number;
    fatG: number;
  }>;
  goalAdherence: {
    daysWithinTarget: number;
    daysLogged: number;
    ratePct: number;
  } | null; // 无目标时为 null
};
```

---

## 6. API 层设计（`api/diet.ts`）

统一用 `request`，均需 `accessToken`。

| 方法 | 接口 | 作用 |
| --- | --- | --- |
| `getDaySummary(accessToken, date)` | `GET /diet/summary?date=` | 某日总结 |
| `createMealLog(accessToken, payload)` | `POST /diet/logs` | 添加记录 |
| `updateMealLog(accessToken, logId, payload)` | `PUT /diet/logs/{logId}` | 修改记录 |
| `deleteMealLog(accessToken, logId)` | `DELETE /diet/logs/{logId}` | 删除记录 |
| `searchFoods(accessToken, query)` | `GET /diet/foods` | 食物搜索/过滤 |
| `getFoodDetail(accessToken, foodId)` | `GET /diet/foods/{foodId}` | 食物详情 |
| `uploadFood(accessToken, payload)` | `POST /diet/foods` | 上传食物 |
| `addFavorite(accessToken, foodId)` | `POST /diet/favorites/{foodId}` | 收藏 |
| `removeFavorite(accessToken, foodId)` | `DELETE /diet/favorites/{foodId}` | 取消收藏 |
| `getDietTargets(accessToken)` | `GET /diet/targets` | 查询当前目标 |
| `setDietTarget(accessToken, payload)` | `PUT /diet/targets` | 自定义/清除目标 |
| `getDietStats(accessToken, query)` | `GET /diet/stats?from=&to=` | 摄入统计 |

`date` / `from` / `to` 为 `yyyy-MM-dd` 字符串，直接作为 query 传入（可复用 `stats-options` 的日期工具约定）。

---

## 7. 与个人资料页的衔接

- `user_profiles` 新增 `activityLevel` 字段（后端迁移 V11）。`profile` 类型 `ProfileBasicResponse` / `UpdateProfileBasicPayload` 需新增 `activityLevel: ActivityLevel | null`，个人资料编辑表单扩展「活动量」下拉，5 档中文标签：久坐 / 轻度 / 中度 / 高强度 / 极高（对应 `sedentary/light/moderate/high/very_high`；标签映射放 `diet-enums.ts`，profile 侧消费）。
- 每日目标计算的完整资料依赖：性别/出生日期/身高/体重(身体指标)/目标/**活动量(activityLevel)**。资料不足时由后端 `DietTargetResponse.missingFields` 返回缺失项，**该列表可能包含 `activityLevel`**；前端「资料不足」提示需把 `activityLevel` 也映射为「活动量」中文（缺失项文案集中放 `diet-enums.ts`），并引导补齐（复用 `AiCoachMissingFieldsNotice` 的视觉模式，跳转资料补录）。
- **不阻断**：日记页即使 `target` 为 null 仍正常展示已摄入与各餐，进度条区显示温和提示。

---

## 8. 页面与组件职责

### 8.1 `DietDiaryPage`（日记首页 `/diet`）

职责：日期导航（默认今天，可回看/补录历史）、拉取 `DaySummary`、渲染目标/进度与各餐、承载添加记录弹窗入口与删除。

- 页面级状态：`selectedDate`、`summary`、`isLoading`、`error`、`activeAddMeal`（当前添加餐次）、`dialogOpen`。
- 添加/编辑/删除后重新拉取 summary 实时刷新进度。

### 8.2 `DietFoodsPage`（食物库 `/diet/foods`）

职责：搜索（关键字）+ 过滤（最常/最近/收藏/全部）、食物列表、详情、上传、收藏。

- 页面级状态：`keyword`、`filter`、`foods`、`isLoading`、`error`。
- 食物列表项展示**来源标签**（`sourceLabel` 官方/用户 + `ownerNickname` 脱敏昵称）。
- 收藏/上传成功后刷新列表。

### 8.3 `DietStatsPage`（摄入统计 `/diet/stats`）

职责：时间范围（复用 stats 预设）→ `getDietStats` → 渲染每日热量折线、宏量占比、周均值、目标符合度。

- `goalAdherence` 为 null 时隐藏该块（不展示）。
- recharts：每日热量用 `LineChart`（或 `AreaChart`）；宏量占比用 `PieChart`（recharts）。

### 8.4 `AddMealLogDialog`（添加记录弹窗）

职责：三选流程——选择餐次（若从日记页传入则跳过）→ `FoodPickerDialog` 选食物 → 填克数（快捷 100g / 半份 / 上次用量）→ 展示实时营养计算 → 保存 `createMealLog` → 回调刷新。

### 8.5 `FoodPickerDialog`（食物选择器）

职责：食物搜索 + 过滤（最常/最近/收藏/全部），列表选择；回调 `FoodItem`。可含「未找到？上传新食物」入口跳上传。

- 食物卡片/选择列表展示**来源标签**：`sourceLabel`（官方/用户）+ 用户食物的 `ownerNickname`（脱敏昵称，如「张**」）。

### 8.6 `FoodDetailDialog` / `UploadFoodDialog`

- `FoodDetailDialog`：展示每 100g 营养与收藏按钮；同步展示来源标签（`sourceLabel` + `ownerNickname`）。
- `UploadFoodDialog`：表单（名称/分类/每100g四营养）+ `diet-validation` 校验 → `uploadFood`。

### 8.7 `NutrientProgressBar`

职责：单个营养素「已摄入 / 目标」+ 进度条（热量、蛋白、碳水、脂肪各一个）。target 为 null 时由父级不渲染此区或显示提示。

### 8.8 `MealSection`

职责：单个餐次（早餐/午餐/晚餐/加餐）分组：标题 + 该餐记录列表（食物名/克数/热量/宏量）+ 每项「编辑克数/删除」+ 「添加」按钮（触发 AddMealLogDialog）。

### 8.9 `DietTargetCard`

职责：展示当前目标（auto/custom，标注「自定义」）与「修改/清除自定义目标」入口。无目标（资料不足）时显示补齐提示。

### 8.10 `DietMissingFieldsNotice`

职责：把 `missingFields` 映射为中文 + 「去补齐资料」按钮，跳资料补录（复用 `buildProfileAiCompletionPath` 风格）。

- `missingFields` 可能包含 `gender/birthDate/heightCm/currentWeightKg/goalType` 与 **`activityLevel`（活动量）**；缺失项中文文案（含「活动量」）集中放 `diet-enums.ts`，此组件只消费映射。

---

## 9. 页面数据流

### 9.1 日记页

1. `selectedDate` 变化 → `getDaySummary(accessToken, date)` → `summary`。
2. `summary.target === null`：显示「已摄入 + 各餐」；进度条区显示温和补齐提示（`DietMissingFieldsNotice` 或纯提示），不渲染进度条。
3. 添加记录：`AddMealLogDialog` → `FoodPickerDialog` 选食物 → 填克数 → `createMealLog` → 关弹窗 → 重拉 summary。
4. 删除/编辑 → 对应接口 → 重拉 summary。

### 9.2 食物库页

1. `keyword` / `filter` 变化（可叠加）→ `searchFoods` → 列表。
2. 收藏/取消 → `addFavorite`/`removeFavorite` → 刷新。
3. 上传 → `UploadFoodDialog` → `uploadFood` → 刷新（可提示新食物已可用）。

### 9.3 统计页

1. `range`（近 7/30/90 天、今年、自定义）→ `getDietStats` → 渲染四个图/块。
2. `goalAdherence === null` → 隐藏目标符合度块。

---

## 10. 本地状态设计

页面级 `useState`（无全局状态库），与 `ai-coach` / `stats` 一致。弹窗组件用受控 `open / onClose` + 回调。无新增状态管理库。

---

## 11. 前端约束与规则

1. **信任后端口径**：营养计算、目标、宏量占比、目标符合度全部来自后端；前端只填克数并展示返回的合计/快照，不做营养素换算。
2. **快照语义**：记录里的是保存时的营养快照，前端修改克数走 `updateMealLog` 由后端按最新食物资料重算，前端不自行重算。
3. **资料不足不阻断**：目标相关展示（进度条、目标符合度、目标卡）在无目标时隐藏/提示；记录、食物库、上传、收藏、摄入趋势始终可用。
4. **克数校验**：>0 且 ≤5000；前端先校验，后端兜底（`DIET_LOG_INVALID`）。
5. **手机适配**：日记添加入口与每餐操作按钮为可点区域；列表/弹窗在小屏下可用（弹窗全屏或底部抽屉式）。
6. **食物共享**：上传后全局可用，但前端不显示「作者」；收藏按当前用户。

---

## 12. 错误处理设计

沿用 `ApiRequestError`。在 `lib/diet-mappers.ts`（或 enums）提供 `getDietErrorMessage` 中文映射：

| 错误码 | 提示 |
| --- | --- |
| `FOOD_NOT_FOUND` | 该食物不存在或已被下架 |
| `FOOD_UPLOAD_INVALID` | 上传食物信息不合法（名称/营养） |
| `DIET_LOG_INVALID` | 记录参数不合法，请检查克数与餐次 |
| `DIET_TARGET_INVALID` | 目标值不合法 |
| `RESOURCE_NOT_FOUND` | 记录/食物不存在或无权访问 |
| `UNAUTHORIZED` | 登录已失效，请重新登录 |
| `INVALID_ARGUMENT` | 提交内容不合法，请检查后重试 |

资料不足不属于错误，走正常数据流（target 为 null + missingFields）。

---

## 13. 空态与资料不足提示

- 某日无记录 → 该餐次空列表文案「暂无记录」。
- 无目标（资料不足）→ 进度条区提示「补充身高/体重/活动量等资料后可查看目标进度」+ 去补齐（缺失项含 `activityLevel` 时列出「活动量」）。
- 食物搜索无结果 → 「没有找到匹配的食物」+ 提供上传入口。
- 统计无数据 → 折线/占比区空态提示。

---

## 14. 手机适配

- 页面主体沿用全局 `px` 容器与 `sm/md/lg` 断点（与 `ai-coach`/`stats` 一致）。
- 日记添加入口在手机端固定/明显可达；弹窗在小屏下采用全宽/底部上滑。
- 每餐记录项操作按钮（编辑/删除）使用足够点击区。

---

## 15. 验收标准

1. `/diet` 可访问；AppShell 导航出现「饮食」。
2. 日记页默认今天：展示目标进度条与三餐/加餐明细；添加/编辑/删除记录后进度实时刷新。
3. 资料不足时日记仍可记录，仅隐藏目标进度并提示补齐；食物库/统计不受影响。
4. 日期导航可回看/补录历史日期。
5. 食物库搜索 + 过滤（最常/最近/收藏/全部）正确；上传后全局可搜；收藏可用。
6. 统计页：每日热量折线、宏量占比、周均值、目标符合度（无目标隐藏）正确（recharts）。
7. 手机端可正常操作。
8. 前端 `pnpm test` 通过；契约联调校验通过。

---

## 16. 本轮改动文件清单

### 新增

| 文件 | 说明 |
| --- | --- |
| `frontend/src/features/diet/types/diet.ts` | 全部饮食类型 |
| `frontend/src/features/diet/api/diet.ts` | 12 个接口封装 |
| `frontend/src/features/diet/lib/diet-enums.ts` | 餐次/分类/活动量/过滤/来源标签与缺失项(含活动量)中文映射 |
| `frontend/src/features/diet/lib/diet-mappers.ts` | 日期/格式化、`getDietErrorMessage` |
| `frontend/src/features/diet/lib/diet-validation.ts` | 克数/上传食物校验 |
| `frontend/src/features/diet/components/NutrientProgressBar.tsx` | 营养素进度条 |
| `frontend/src/features/diet/components/MealSection.tsx` | 单餐分组 |
| `frontend/src/features/diet/components/AddMealLogDialog.tsx` | 添加记录弹窗 |
| `frontend/src/features/diet/components/FoodPickerDialog.tsx` | 食物选择器 |
| `frontend/src/features/diet/components/FoodDetailDialog.tsx` | 食物详情 |
| `frontend/src/features/diet/components/UploadFoodDialog.tsx` | 上传食物 |
| `frontend/src/features/diet/components/DietTargetCard.tsx` | 目标卡/自定义覆盖 |
| `frontend/src/features/diet/components/DietMissingFieldsNotice.tsx` | 资料不足提示 |
| `frontend/src/features/diet/pages/DietDiaryPage.tsx` | 日记首页 |
| `frontend/src/features/diet/pages/DietFoodsPage.tsx` | 食物库页 |
| `frontend/src/features/diet/pages/DietStatsPage.tsx` | 摄入统计页 |

### 修改

| 文件 | 改动 |
| --- | --- |
| `frontend/src/app/router.tsx` | 新增 `/diet`、`/diet/foods`、`/diet/stats` |
| `frontend/src/app/layout/AppShell.tsx` | 登录态导航新增「饮食」 |
| `frontend/src/features/profile/types/profile.ts` | `ProfileBasicResponse`/`UpdateProfileBasicPayload` 增 `activityLevel` |
| `frontend/src/features/profile`（编辑表单相关） | 基础资料表单增「活动量」下拉 |

### 不改动

- `backend/**`、`db/**`。
- 既有 `workout` / `stats` / `ai-coach` 行为（图表仅复用 recharts，不引依赖）。

---

## 17. 风险与未完成项

| 风险 / 缺口 | 等级 | 说明与对策 |
| --- | --- | --- |
| `activityLevel` 为后端新增资料字段 | 低 | 前端 profile 类型与编辑表单需同步扩展；`ProfileBasicResponse` 目前无该字段，需后端返回后对齐 |
| 统计页宏量占比需 PieChart | 低 | recharts 已可用；本期新增 PieChart/AreaChart 用法 |
| 上传食物/收藏为写入 | 低 | 权限仅本人；食物库全局共享（只读列表不含作者） |
| 自定义目标入口位置待定 | 低 | 可在日记页 `DietTargetCard` 或个人资料页；实现时定 |
| 日期/补录交互 | 低 | 复用 stats 的日期处理约定（`yyyy-MM-dd`） |

---

## 18. 设计结论

`diet` 是独立记录与展示模块，沿用 `api/components/lib/pages/types` 结构：

1. **三个页面**（日记/食物库/统计）+ AppShell「饮食」入口。
2. **目标计算/营养换算全由后端**，前端只填克数并展示返回快照与进度。
3. **不阻断策略**贯穿：资料不足只隐藏/提示目标相关，记录/食物库/统计始终可用。
4. 图表复用 `recharts`（已在 stats 引入）；新增 `DietStatsPage` 的折线/饼图。
5. 食物库弹窗组件（选择/详情/上传）跨页复用。

如后续进入实现阶段，本文档可直接作为 `diet` 前端开发与联调的执行基线。
