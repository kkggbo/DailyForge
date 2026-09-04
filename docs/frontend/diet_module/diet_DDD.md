# DailyForge Frontend 饮食模块 详细设计

> 版本：v0.3
> 日期：2026-09-05
> 模块归属：`frontend/src/features/diet`
> 契约来源：`docs/interfaces/diet_接口文档.md`
> 关联 PRD：`docs/prd/diet_PRD.md`

> 变更记录：
> - v0.3（2026-09-05）：对齐当前实现——独立「食物库」页与 `/diet/foods` 路由删除，食物库浏览/搜索/收藏/上传收敛进添加流程（AddMealFlowDialog）第一步；日记页改为「今日摄入 IntakeSummaryCard + 四餐卡片 + 展开明细」；添加改为单层弹窗两步（分页选食物→填克数）；每日目标并入今日摄入卡；类型/API 支持分页（page/pageSize/hasMore）；补充 profile activityLevel 编辑与个人资料摘要「?」说明。
> - v0.2：`missingFields` 含 activityLevel；FoodItem 来源标签 sourceLabel/ownerNickname。

---

## 1. 文档目标

本文档描述「饮食日记」模块在前端的**当前实现基线**（含「食物库入口收敛 + 日记交互重构」），作为后续开发与联调的依据。仅描述已落地的代码结构，不写尚未实现的设想。

沿用 `docs/frontend/stats_module/stats_DDD.md` 的结构与风格（含 recharts 复用）。仅描述与实现一致的页面、组件与类型。

---

## 2. 模块定位与职责边界

### 2.1 定位

`diet` 是「记录与展示」模块：记录每餐吃什么、看当天热量/宏量摄入与目标进度、浏览/搜索/收藏/上传食物、看摄入统计趋势。营养换算与每日目标计算全部由后端完成，前端只填克数并展示返回的合计/进度/快照。

### 2.2 职责

`diet` 前端**负责**：

- 日记页：日期导航、今日摄入（目标进度或纯实际摄入）、四餐卡片与展开明细、添加/编辑/删除记录。
- 添加流程：选食物（分页滚动、搜索与 filter、收藏、上传入口）→ 填克数 → 保存。
- 上传食物（日记页头部按钮 + 添加流程第一步内）。
- 摄入统计页：时间范围 + 每日热量折线 + 宏量占比饼图 + 周均值 + 目标符合度。
- 资料不足（无目标）的补齐提示（missingFields 精确缺失项）。

`diet` 前端**不负责**：

- 营养基准/BMR/TDEE/宏量拆分（后端 `GET /diet/summary`、`GET /diet/targets` 返回）。
- 资料数据存储（复用 `profile`；活动量编辑在个人资料侧）。
- 饮水记录、食谱、饮食 AI 建议、分享、上传审核。

### 2.3 依赖

- `auth`：登录态与 `accessToken`。
- `profile`：`ActivityLevel` 类型与活动量编辑/说明（见 §3）。
- `shared/api/http.ts`：统一 `request`。
- `recharts`：摄入统计页图表。

---

## 3. 与个人资料页的衔接（activityLevel）

- `profile/types/profile.ts` 新增 `ActivityLevel`（sedentary/light/moderate/high/very_high），`ProfileBasicResponse` / `UpdateProfileBasicPayload` / `BasicProfileFormValues` 均含 `activityLevel`。
- 编辑：`profile/components/BasicProfileForm.tsx` 新增「活动量」下拉（5 档中文：久坐/轻度/中度/高强度/极高），选项在 `profile/lib/profile-enums.ts` 的 `activityLevelOptions`；保存经 `UpdateProfileBasicPayload.activityLevel` 提交。
- 展示：`profile/components/BasicProfileSummaryCard.tsx` 基础档案增加「活动量」行，未填显示 `--`；该行带「?」按钮 → `ActivityLevelHelpDialog`（`createPortal` 弹窗）列出 5 档各自「含义 + 例子」，说明「活动量用于估算每日总能量消耗，直接影响每日目标自动计算」。
- 缺失项文案：`diet/lib/diet-enums.ts` 的 `dietMissingFieldLabels` 含 `activityLevel → "活动量"`；`DietMissingFieldsNotice` 依此展示补齐项。

---

## 4. 目录结构（实际）

```text
src/features/diet
├─ api
│  └─ diet.ts
├─ components
│  ├─ IntakeSummaryCard.tsx    # 今日摄入卡（目标+摄入合并）
│  ├─ AddMealFlowDialog.tsx    # 单层两步添加流程（选食物→填克数）
│  ├─ UploadFoodDialog.tsx     # 上传食物
│  ├─ FoodSourceTag.tsx        # 食物来源标签（官方/用户·昵称）
│  ├─ MealDetailPanel.tsx      # 展开餐段明细
│  ├─ MealRowItem.tsx          # 单条记录（编辑克数/删除）
│  ├─ NutrientProgressBar.tsx  # 营养素进度条
│  └─ DietMissingFieldsNotice.tsx # 资料补齐提示
├─ lib
│  ├─ diet-enums.ts
│  └─ diet-formatters.ts
├─ pages
│  ├─ DietDiaryPage.tsx
│  └─ DietStatsPage.tsx
└─ types
   └─ diet.ts
```

说明：

- 无独立「食物库」页；食物浏览/搜索/收藏/上传全部并入 `AddMealFlowDialog` 第一步「选择食物」。
- 「四餐卡片」（MealCard）为 `DietDiaryPage` 内联小组件，无独立文件。

---

## 5. 路由与入口

`router.tsx` 实际路由（受保护）：

```tsx
{ path: "/diet", element: <DietDiaryPage /> }
{ path: "/diet/stats", element: <DietStatsPage /> }
```

- 已移除 `/diet/foods` 路由与 `DietFoodsPage` 引用（无独立食物库入口）。
- `AppShell` 登录态导航含「饮食」→ `/diet`。
- 日记页 header：`上传食物` 按钮（开 `UploadFoodDialog`）+ `摄入统计` 链接（→ `/diet/stats`）。无「食物库」链接。

---

## 6. 数据模型设计（`types/diet.ts`）

> 对齐接口文档，仅列与实现相关的核心类型。

```ts
type MealType = "breakfast" | "lunch" | "dinner" | "snack";
type FoodCategory = "staple"|"meat_egg"|"vegetable"|"fruit"|"dairy"|"nut_bean"|"drink"|"other";
type FoodSource = "system" | "user";
type DietTargetBasis = "auto" | "custom";
type NutrientValues = { caloriesKcal:number; proteinG:number; carbsG:number; fatG:number };
```

- `DietTarget = { basis: DietTargetBasis|null; caloriesKcal|null; proteinG|null; carbsG|null; fatG|null }`。
- `DaySummary = { date; target:DietTarget|null; meals:Record<MealType,MealLogItem[]>; totals:NutrientValues; progress:{caloriesPct;proteinPct;carbsPct;fatPct}|null }`。
- `MealLogItem = NutrientValues & { logId; foodId; foodName; grams }`。
- `FoodItem = { foodId; name; category; source; sourceLabel; ownerNickname; caloriesKcal; proteinG; carbsG; fatG; favorited }`。
- **分页**：`FoodListResponse = { foods: FoodItem[]; hasMore:boolean }`；`FoodQuery = { keyword?; filter?; page?; pageSize? }`（page from 1，pageSize 默认 20）。
- `SetDietTargetPayload` 为判别联合：`{clear:true}` 或 `{clear?:false; caloriesKcal; proteinG; carbsG; fatG}`（清除只发 `{clear:true}`，避免四营养字段被校验拒绝）。
- `DietStats = { dailyCalories; macroShare; weeklyAverage; goalAdherence|null }`。

---

## 7. API 层设计（`api/diet.ts`）

均用 `request` + `accessToken`，接口对齐 D1~D11：

`getDaySummary`、`createMealLog`、`updateMealLog`、`deleteMealLog`、`searchFoods`、`getFoodDetail`、`uploadFood`、`addFavorite`、`removeFavorite`、`getDietTargets`、`setDietTarget`、`getDietStats`。

关键点：

- `searchFoods(token, query)` 透传分页：`filter` 默认 `all`，`page ?? 1`，`pageSize ?? 20`。
- `setDietTarget` 支持 `{clear:true}`（清自定义）或四营养自定义覆盖。
- `getFoodDetail` 保留为契约方法；UI 中食物详情字段由选食物列表项直接提供。

---

## 8. 组件职责（实际）

### 8.1 `DietDiaryPage`（/diet）

- 日期状态默认今天；顶栏「‹ 前一天 / 日期 / 回到今天」，**今天时隐藏「后一天」**（不能看未来）。
- `load(date)` → `getDaySummary`；当 `summary.target?.basis` 为空时额外 `getDietTargets` 取精确 `missingFields`（含 activityLevel），失败回落通用文案。
- 渲染顺序：`IntakeSummaryCard`（今日摄入）→ 四餐卡片一行 → 展开的 `MealDetailPanel`（若某餐被展开）。
- 内联 `MealCard`：餐次名 + 右上圆形「+」（添加该餐，`stopPropagation` 阻止冒泡展开）+ 该餐合计总热量大值 + 蛋白/碳水/脂肪小值。
- 增删改：`AddMealFlowDialog` 保存后 `createMealLog` 并 reload；`MealRowItem` 编辑克数走 `updateMealLog`；删除走 `deleteMealLog`；自定义目标 `setDietTarget`。

### 8.2 `IntakeSummaryCard`（今日摄入卡，合并目标+摄入）

- props：`totals`、`target`、`progress`、`missingFields`、`onSaveTarget`、`onClearTarget`。
- **有可用目标**（`target.basis` 为 auto/custom）：
  - 总热量单独一行大值 + 进度条；
  - 蛋白/碳水/脂肪三格均分同行，各显示 current/target + 进度条；
  - 「自定义目标」按钮 → 展开 4 输入保存；basis=custom 时显示「恢复自动」（`{clear:true}`）。
- **无可用目标**（basis 为 null / target null）：只显示四大营养素实际摄入（总热量大值；蛋白/碳水/脂肪同行，名称与数值分行并带单位），不显示目标值/进度条；仍保留「自定义目标」按钮（设定成功后 reload → basis=custom 切到有目标视图）；卡内用 `DietMissingFieldsNotice` 提示补齐资料（精确 missingFields）+「去补齐资料」。

### 8.3 `AddMealFlowDialog`（单层弹窗两步）

- props：`open`、`date`、`mealType`（由所点餐卡决定）、`onClose`、`onSaved`。
- 标题带目标餐次（如「添加到 早餐」）；**无餐次下拉**，餐次自动确定并随提交传给 `createMealLog`。
- 第一步「选择食物」：搜索框 + filter chips（全部/最近/最常/收藏）+ 食物列表。
  - 分页：`searchFoods(page, pageSize=20)`；滚动容器接近底部自动加载下一页并追加，`hasMore=false` 停止；切换 keyword/filter 重置第一页；加载中轻量提示。
  - 每行：食物名 + 每100g 热量 + `FoodSourceTag`（官方/用户·昵称）+ ★/☆ 快速收藏（`addFavorite`/`removeFavorite`，切换 `stopPropagation` 不选中该行）。
  - 右上「上传食物」→ 开 `UploadFoodDialog`，上传成功刷新当前列表（page=1）。
- 第二步「添加记录」：显示已选食物 + 「更换」回第一步；克数输入（1..5000，快捷 100/150/200g）；按克数实时估算营养；保存 `createMealLog({date, mealType, foodId, grams})` → `onSaved` 刷新 summary 并关闭。

### 8.4 `UploadFoodDialog`

- 名称（非空、≤64）+ 分类（可选，foodCategoryOptions）+ 每 100g 四营养（均 ≥0 且非全 0）→ `uploadFood` → `onSaved`。

### 8.5 `FoodSourceTag`

- 展示来源：system →「官方」；user →「用户 · 脱敏昵称」（`ownerNickname`）。供食物列表行内复用。

### 8.6 `MealDetailPanel` / `MealRowItem`

- `MealRowItem`：单条记录（食物名/克数/热量/宏量 + 编辑克数/删除），内联克数编辑（1..5000 校验）。
- `MealDetailPanel`：某餐展开明细列表；空则「暂无记录」。

### 8.7 `NutrientProgressBar`

- 单营养素 current/target + 进度条；`target===null` 时只显示当前值、不渲染进度条轨道。

### 8.8 `DietMissingFieldsNotice`

- 将 `missingFields` 映射为中文（含 activityLevel）并给出「去补齐资料」链接（/profile/edit）；`targetText` 可定制（默认「每日目标进度」，统计页用「目标符合度」）。

---

## 9. 页面数据流

### 9.1 日记页

1. 初始今天 → `getDaySummary(date)`。
2. 无目标（basis 空）→ 额外 `getDietTargets` 取 `missingFields` → 传给 IntakeSummaryCard 内补齐提示。
3. 四餐卡片合计：`sumMeal(items)`（累加热量/蛋白/碳水/脂肪）。
4. 点餐卡「+」→ `AddMealFlowDialog`（mealType=该餐）→ 保存后 reload。
5. 编辑/删除记录 → `updateMealLog`/`deleteMealLog` → reload。
6. 自定义目标保存 / 恢复自动 → `setDietTarget` → reload（basis 变化自动切换今日摄入视图）。

### 9.2 统计页

1. 时间范围（近 7/30/90 天/今年/自定义）→ `getDietStats(from,to)`。
2. `goalAdherence` 为 null（无目标）→ 隐藏达标数据、改显示 `DietMissingFieldsNotice`（精确 missingFields 取 `getDietTargets`）；非 null 时显示达标统计（目标内天数/有记录天数/符合率）。

---

## 10. 本地状态设计

页面级 `useState`，无全局状态库。弹窗受控 `open/onClose` + 回调。`AddMealFlowDialog` 内部维护 keyword/filter/page/hasMore/foods/滚动加载态与「已选食物/克数」第二步状态。

---

## 11. 前端约束与规则

1. **信任后端口径**：营养换算、目标、进度、统计均由后端返回；前端只填克数并展示快照/合计，不做营养素换算。
2. **资料不足不阻断**：无目标时只隐藏目标/进度条展示（NutrientProgressBar 不画轨道 + 补齐提示），记录/选食物/收藏/上传/统计始终可用；无资料用户也可直接自定义目标，两种引导并存不互相遮盖。
3. **清除目标只发 `{clear:true}`**，不带 0 值营养字段。
4. **克数校验**：1..5000，前端先校验，后端兜底（`DIET_LOG_INVALID`）。
5. **分页**：选食物列表默认 20 条/页、滚动加载、`hasMore=false` 停止，避免一次渲染全部导致卡顿。
6. **食物来源只读共享**：上传后全局可用，列表仅显示来源标签与脱敏昵称，不显示其它作者信息；收藏按当前用户。
7. **手机适配**：四餐卡片响应式（窄屏压缩字号、sm 起多列）；日记添加入口（各餐卡「+」）与操作按钮可点可达；弹窗全屏可滚。

---

## 12. 错误处理设计

沿用 `ApiRequestError` + `diet-formatters.getDietErrorMessage`，映射：

`FOOD_NOT_FOUND`（该食物不存在或已不可用）、`FOOD_UPLOAD_INVALID`、`DIET_LOG_INVALID`、`DIET_TARGET_INVALID`、`RESOURCE_NOT_FOUND`、`UNAUTHORIZED`、`INVALID_ARGUMENT`。

资料不足不属于错误，走正常数据流（target null + missingFields 提示）。

---

## 13. 空态与资料不足提示

- 某餐无记录 → 展开明细面板「暂无记录」。
- 无目标 → 今日摄入卡隐藏进度条，`DietMissingFieldsNotice` 列精确缺失项（含 activityLevel）并引导补齐；仍可自定义目标。
- 食物搜索无结果 → 「没有找到匹配的食物」。
- 统计无数据 → 各区块空态文案；目标符合度无目标时显示补齐提示。

---

## 14. 手机适配

- 页面容器沿用全局断点；四餐卡片小屏适配（grid，窄屏压缩字号）。
- 添加流程弹窗全宽/全屏可滚；食物列表滚动分页在手机上同样工作。

---

## 15. 验收标准

1. 日记页默认今天；今天隐藏「后一天」，不能看未来日期。
2. 今日摄入卡：有目标时展示目标进度（总热量突出 + 三宏量同行）；无目标时只展示实际摄入 + 补齐提示 + 自定义目标按钮。
3. 自定义目标可保存（basis=custom）并可自动切到有目标视图；custom 下可「恢复自动」。
4. 四餐卡片均分一行、点卡在本行下方展开明细（一次一个），每餐右上「+」添加该餐。
5. 添加流程为单层两步（选食物分页滚动→填克数→保存），餐次由所点卡片决定、无餐次下拉。
6. 食物来源标签（官方/用户·昵称）与收藏（★）在列表中可用；上传入口在日记页头与添加流程内。
7. 统计页各图/块正确；无目标时目标符合度显示补齐提示。
8. 前端 `pnpm test`、契约联调通过。

---

## 16. 本轮改动文件清单（对齐 v0.3 实现）

### 新增

| 文件 | 说明 |
| --- | --- |
| `components/IntakeSummaryCard.tsx` | 今日摄入卡（目标+摄入合并） |
| `components/AddMealFlowDialog.tsx` | 单层两步添加流程（分页选食物+填克数） |
| `components/MealDetailPanel.tsx` | 展开餐段明细 |
| `components/MealRowItem.tsx` | 单条记录（编辑/删除） |
| `components/FoodSourceTag.tsx` | 来源标签（从旧 FoodPickerDialog 抽出） |

### 修改

| 文件 | 改动 |
| --- | --- |
| `pages/DietDiaryPage.tsx` | 重构：日期限制、IntakeSummaryCard、四餐卡片+展开、上传入口 |
| `types/diet.ts` | `FoodListResponse` 增 `hasMore`；`FoodQuery` 增 `page/pageSize` |
| `api/diet.ts` | `searchFoods` 透传分页 |
| `components/NutrientProgressBar.tsx` | target 为 null 不画进度条轨道 |
| `app/router.tsx` | 移除 `/diet/foods` 路由与 `DietFoodsPage` import |

### 删除（自模块移除）

`pages/DietFoodsPage.tsx`、`components/DietTargetCard.tsx`、`components/AddMealLogDialog.tsx`、`components/FoodPickerDialog.tsx`、`components/FoodDetailDialog.tsx`、`components/MealSection.tsx`。

### 相关（profile 侧，供活动量）

`profile/types/profile.ts`（ActivityLevel + 字段）、`profile/lib/profile-enums.ts`（activityLevelOptions + 文案）、`profile/lib/profile-mappers.ts`、`profile/components/BasicProfileForm.tsx`（活动量下拉）、`profile/components/BasicProfileSummaryCard.tsx`（活动量行 + 「?」说明弹窗 ActivityLevelHelpDialog）。

---

## 17. 风险与未完成项

- 后端 D5 分页契约（page from 1、pageSize 默认 20、`hasMore`）由并行 agent 落地，前端已按此约定对接，需联调确认字段一致。
- `getFoodDetail` 目前 UI 未被单独调用（列表项已含全部字段），保留为契约方法。
- 食物浏览/搜索/收藏/上传仅存在于添加流程第一步，用户需进入「添加」才能浏览食物——符合「入口收敛」决策，如需单独入口可在日记页增设浏览按钮（非本版）。

---

## 18. 设计结论

`diet` 是独立记录与展示模块，当前实现要点：

1. **路由收敛**为两页：`/diet`（日记首页）+ `/diet/stats`（摄入统计）；独立食物库页删除，浏览/搜索/收藏/上传并入添加流程。
2. **日记页**：日期受限（不能看未来）、`IntakeSummaryCard`（目标与摄入合并、无目标降级）、四餐卡片 + 展开明细、内联 MealCard。
3. **添加**为单层弹窗两步（分页选食物 → 填克数），餐次自动。
4. **目标/营养换算全由后端**，前端只填克数并展示快照与进度；不阻断策略贯穿。
5. **资料不足**：精确 `missingFields`（含 activityLevel）经 `getDietTargets` 获取并中文提示，个人资料侧提供活动量编辑与「?」说明。

本文档与当前 `diet` 前端代码一致，可作为联调与后续维护基线。
