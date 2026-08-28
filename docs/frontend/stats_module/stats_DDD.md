# DailyForge Frontend 统计模块 详细设计

> 版本：v0.1
> 日期：2026-08-26
> 模块归属：`frontend/src/features/stats`
> 契约来源：`docs/interfaces/stats_接口文档.md`
> 关联 PRD：`docs/prd/stats_PRD.md`

---

## 1. 文档目标

本文档定义「训练统计 + 身体指标趋势」模块（`stats`）在前端的实现边界、目录结构、类型定义、页面/组件职责、图表接入、入口接线与文件规划，作为本轮前端开发与联调的技术基线。

本文档沿用 `docs/frontend/ai_coach_module/ai_coach_DDD.md` 的结构与风格：先定位与边界，再目录/路由/数据模型/API/组件，最后验收与文件清单。它是 `ai_coach` 之外的**独立新模块**，不改动既有模块行为。

已确认的交互细节（来自 PRD §5 与主控确认）：

1. **双入口**：AppShell 导航「统计」+ 控制台首页入口卡片（卡片展示总体值 + 趣味文案）。
2. **训练区时间范围**：近 30 天 / 近 90 天 / 今年 / 全部 + 自定义 from-to；**身体指标区时间范围独立**。
3. **动作筛选**：全部 / 仅力量 / 仅有氧 + 按名称搜索单个动作。
4. 动作列表按**出现次数降序**；卡片头部（名称 + 出现次数/总组数/总次数徽标 + 查看进阶），正文按类型展示指标 + 趣味文案，展开为进阶曲线。
5. 进阶曲线：力量默认「最大重量」，有氧默认「总距离」（可切换）。
6. 身体指标：指标切换（体重/体脂/BMI/骨骼肌/腰围等）+ 折线图。
7. 图表库：**新增 `recharts` 依赖**。
8. 空数据友好提示。

---

## 2. 模块定位与职责边界

### 2.1 定位

`stats` 是**只读数据展示模块**，承接用户「练了什么、练了多少、变化趋势」的可视化。它不产生业务数据，只聚合展示已有训练打卡与身体指标记录。

### 2.2 职责

`stats` 前端**负责**：

- 汇总 Hero（总体值 + 趣味文案）
- 训练区时间范围与动作筛选
- 动作统计列表（卡片、展开、进阶曲线）
- 身体指标区（指标切换 + 折线图）
- 控制台入口卡片（展示总体值 + 趣味文案，可跳转 `/stats`）
- 空数据 / 加载 / 错误态

`stats` 前端**不负责**：

- 任何训练 / 模板 / 身体指标的写入
- 导出、分享（本版不做，见 PRD §7）
- 训练频率柱状图（二期）
- 数据口径计算（出现次数/总组数/容量等全部由后端聚合，前端只透传时间范围与筛选参数）

### 2.3 依赖

- `auth`：登录态与 `accessToken`。
- `shared/api/http.ts`：统一 `request` 封装。
- `cycle-template` 的 `MetricKey` 语义参考（但身体指标键为独立集合，见 §5.5）。

---

## 3. 推荐目录结构

新增独立 feature 目录 `frontend/src/features/stats/**`，沿用 `api / components / lib / pages / types` 五层结构，与 `ai-coach`、`workout` 一致：

```text
src/features/stats
├─ api
│  └─ stats.ts
├─ components
│  ├─ SummaryHero.tsx
│  ├─ StatsFilterBar.tsx
│  ├─ ExerciseStatCard.tsx
│  ├─ ProgressionChart.tsx
│  ├─ BodyMetricsChart.tsx
│  └─ StatsEmptyState.tsx
├─ lib
│  ├─ stats-options.ts
│  ├─ stats-formatters.ts
│  └─ stats-mappers.ts
├─ pages
│  └─ StatsPage.tsx
└─ types
   └─ stats.ts
```

另：

- 控制台入口卡片 `StatsEntryCard.tsx` 放在 `stats/components/`（统计 UI 归拢到 stats feature），由 `home` 的 `HomePage` 引用（`HomePage` 已存在跨 feature 引用 workout 的先例）。
- `app/router.tsx` 新增 `/stats` 受保护路由。
- `frontend/package.json` 新增 `recharts` 依赖。

---

## 4. 路由设计

受保护路由（挂在 `ProtectedOutlet` 下）：

```tsx
{
  path: "/stats",
  element: <StatsPage />
}
```

- `/stats` 为唯一统计入口，无子路由。
- AppShell 顶部导航新增 `NavLink to="/stats"`（文案「统计」），与「控制台 / 训练模板 / 训练工作台 / 个人资料」并列。

---

## 5. 数据模型设计（`types/stats.ts`）

> 全部字段名 / 类型 / 语义严格对齐 `docs/interfaces/stats_接口文档.md`，前端不自行推断聚合逻辑。

### 5.1 动作类型与筛选

```ts
export type ExerciseType = "strength" | "cardio";

// 前端筛选：全部 / 仅力量 / 仅有氧
export type ExerciseFilter = "all" | ExerciseType;
```

### 5.2 总体统计 `OverallStats`

```ts
export type OverallStats = {
  sessionCount: number;
  totalSets: number;
  totalReps: number;
  totalVolumeKg: number;
  totalDistanceKm: number;
  totalDurationMinutes: number;
  overviewCopy: string;
};
```

### 5.3 单动作统计 `ExerciseStat`

```ts
export type ExerciseStat = {
  exerciseId: number;
  name: string;
  exerciseType: ExerciseType;
  structureType: "set_based" | "single_segment";
  appearanceCount: number;
  setCount: number | null;
  repCount: number | null;
  totalVolumeKg: number | null;
  avgWeightKg: number | null;
  maxWeightKg: number | null;
  avgReps: number | null;
  totalDurationSeconds: number | null;
  totalDistanceKm: number | null;
  avgSpeedKmh: number | null;
  funCopy: string;
};
```

说明：

- `structureType = "set_based"` 时关注力量字段，`= "single_segment"` 时有氧字段；另一组为 `null`。
- 由 `structureType` / `exerciseType` 决定正文展示的指标集合。

### 5.4 汇总响应 `StatsSummary`

```ts
export type StatsSummary = {
  overall: OverallStats;
  exercises: ExerciseStat[];
};
```

### 5.5 进阶曲线（S2）

```ts
export type ExerciseProgressionPoint = {
  date: string;
  maxWeightKg: number | null;
  maxReps: number | null;
  totalVolumeKg: number | null;
  totalDurationSeconds: number | null;
  totalDistanceKm: number | null;
};

export type ExerciseProgression = ExerciseStat & {
  progression: ExerciseProgressionPoint[];
};
```

### 5.6 身体指标（S3）

```ts
export type BodyMetricKey =
  | "weight_kg"
  | "body_fat_percent"
  | "bmi"
  | "skeletal_muscle_percent"
  | "body_water_percent"
  | "basal_metabolic_rate_kcal"
  | "waist_cm"
  | "hip_cm"
  | "waist_hip_ratio"
  | "body_age";

export type BodyMetricPoint = {
  date: string;
  value: number;
};

export type BodyMetricsSeries = {
  metric: BodyMetricKey;
  unit: string;
  points: BodyMetricPoint[];
};
```

### 5.7 查询参数

```ts
// 通用时间范围（from/to，ISO-8601，可省）
export type StatsTimeRangeQuery = {
  from?: string;
  to?: string;
};
```

本地 UI 类型（`stats-options.ts`）：

```ts
export type TimeRangePreset = "30d" | "90d" | "year" | "all" | "custom";

export type TimeRangeSelection = {
  preset: TimeRangePreset;
  from?: string;
  to?: string;
};
```

---

## 6. API 层设计（`api/stats.ts`）

统一使用 `shared/api/http.ts` 的 `request`，全部接口需 `accessToken`。

| 方法 | 接口 | 作用 |
| --- | --- | --- |
| `getStatsSummary(accessToken, query?)` | `GET /api/stats/summary` | 总体值 + 按出现次数降序的动作列表 |
| `getExerciseProgression(accessToken, exerciseId, query?)` | `GET /api/stats/exercise/{exerciseId}` | 单动作聚合 + 进阶序列 |
| `getBodyMetrics(accessToken, metric, query?)` | `GET /api/stats/body-metrics` | 身体指标时间序列 |

```ts
export function getStatsSummary(accessToken: string, query: StatsTimeRangeQuery = {}) {
  return request<StatsSummary>("/stats/summary", {
    accessToken,
    query: toStatsQuery(query)
  });
}

export function getExerciseProgression(
  accessToken: string,
  exerciseId: number,
  query: StatsTimeRangeQuery = {}
) {
  return request<ExerciseProgression>(`/stats/exercise/${exerciseId}`, {
    accessToken,
    query: toStatsQuery(query)
  });
}

export function getBodyMetrics(
  accessToken: string,
  metric: BodyMetricKey,
  query: StatsTimeRangeQuery = {}
) {
  return request<BodyMetricsSeries>("/stats/body-metrics", {
    accessToken,
    query: { metric, ...toStatsQuery(query) }
  });
}
```

约束：

- `from` / `to` 只在有值时写入 query（避免把空串传给后端）。
- 首页入口卡片复用 `getStatsSummary`，不需要额外接口。

---

## 7. 本地状态与页面数据流

`StatsPage` 页面级 `useState` 管理，不引入全局状态库（与项目现状一致）。

### 7.1 训练区

- `trainingSummary: StatsSummary | null`
- `trainingRange: TimeRangeSelection`（默认 `all`）
- `exerciseFilter: ExerciseFilter`（默认 `all`）
- `exerciseSearch: string`
- `isLoadingSummary` / `summaryError`
- 展开态 `expandedExerciseId: number | null` 及对应的 `progression: ExerciseProgression | null`

### 7.2 身体指标区

- `metric: BodyMetricKey`（默认 `weight_kg`）
- `metricRange: TimeRangeSelection`（独立，默认 `all`）
- `metricsSeries: BodyMetricsSeries | null`
- `isLoadingMetrics` / `metricsError`

### 7.3 数据流

1. 进入页面 → `getStatsSummary(accessToken, {})`（默认全部时间）→ 渲染 Hero 与动作列表。
2. 训练区时间范围 / 动作筛选 / 搜索变化 → 重新拉取 `getStatsSummary`（动作筛选与搜索在前端本地过滤 `exercises`；时间范围作为 query 重新请求）。
3. 展开某动作 → `getExerciseProgression(accessToken, exerciseId, trainingRange)`，缓存到展开卡片。
4. 身体指标区 `metric` / `metricRange` 变化 → `getBodyMetrics(accessToken, metric, metricRange)`。
5. 控制台入口卡片独立调用 `getStatsSummary`，展示 `overall`。

> 时间范围变化会重置展开态与已缓存进阶数据（因数据范围变化）。

---

## 8. 组件边界设计

### 8.1 `StatsPage`（页面）

职责：编排训练区 + 身体指标区两大块；持有上述状态；处理加载/错误/空态；组合子组件。

不负责：具体指标计算（来自接口）、图表绘制细节。

### 8.2 `SummaryHero`

职责：接收 `OverallStats`，展示总体值 + `overviewCopy` 趣味文案（1 条总览 + 1 条趣味）。`overviewCopy` 由后端生成并直接展示，前端不拼装等价物换算。

不负责：发请求。

### 8.3 `StatsFilterBar`

职责：

- 训练区时间范围（近 30 天 / 近 90 天 / 今年 / 全部 + 自定义 from-to）。
- 动作筛选（全部 / 仅力量 / 仅有氧）。
- 按名称搜索单个动作。
- 通过 `onChange` 向页面抛出筛选结果；时间范围与动作搜索/类型分离开关，避免重复请求（时间范围触发请求，类型/搜索本地过滤）。

### 8.4 `ExerciseStatCard`

职责：

- 头部：动作名 + `出现次数 / 总组数 / 总次数` 徽标 + 「查看进阶 / 收起」展开按钮。
- 正文：按 `structureType` 展示力量字段（总容量/平均重量/最大重量/平均次数）或有氧字段（总时长/总距离/平均配速），并展示 `funCopy`。
- 展开：渲染 `ProgressionChart`；展开中拉取进阶数据时显示加载，失败显示重试。

### 8.5 `ProgressionChart`

职责：封装 recharts `LineChart`，渲染单动作进阶序列。

- 力量动作：默认「最大重量」；可切换 最大重量 / 最大次数 / 总容量。
- 有氧动作：默认「总距离」；可切换 总时长 / 总距离。
- 输入：`points: ExerciseProgressionPoint[]`、`structureType`、`unit?`。
- 空 `points` → 展示空态。

### 8.6 `BodyMetricsChart`

职责：

- 指标切换（`BodyMetricKey` 下拉，含中文标签）。
- 独立时间范围。
- 封装 recharts `LineChart`（可选 `AreaChart`）渲染 `points`，展示单位 `unit`。
- 空 `points` → 展示空态。

### 8.7 `StatsEntryCard`（首页入口卡片）

职责：控制台首页入口卡片；自取 `getStatsSummary`，展示总体值（训练场数 / 总容量 / 总里程等）+ `overviewCopy`，点击跳转 `/stats`。

- 自带加载 / 空数据 / 错误态（失败可折叠为纯入口，不影响首页主体）。

### 8.8 `StatsEmptyState`

职责：统一的空数据提示（无训练记录 / 时间范围内无数据 / 无身体指标），可复用。

---

## 9. recharts 图表接入

### 9.1 依赖

在 `frontend/package.json` 的 `dependencies` 新增：

```json
"recharts": "^2.x"
```

（具体版本以安装时的最新稳定为准；dev 环境无需额外配置，recharts 为纯组件库。）

### 9.2 类型选择

| 用途 | 组件 | 说明 |
| --- | --- | --- |
| 单动作进阶 | `LineChart` | 力量/有氧均用折线，多指标可叠加 `Line` |
| 身体指标 | `LineChart`（或 `AreaChart`） | 单一指标单线，Area 更突出趋势；本版默认 LineChart |

### 9.3 接入约定

- 统一用 `<ResponsiveContainer width="100%" height={...}>` 包裹，保证响应式。
- `<XAxis dataKey="date" />`、`<YAxis />`、`<Tooltip />`（展示日期/数值）、`<Line>` 用 `dataKey` 指向选中的指标字段。
- 时间 x 轴建议按序排列（接口已按日期升序返回），`date` 可格式化为 `MM-DD` 或 `YYYY-MM-DD` 展示。
- 数值格式化（kg / km / 次 / 分钟）集中在 `stats-formatters.ts`，`Tooltip formatter` 复用。
- 自定义 tooltip / 空态：`points.length === 0` 时直接渲染 `StatsEmptyState`，不渲染 `LineChart`。
- 图表配色沿用项目琥珀/中性色（如 `#fbbf24`），通过 `stroke` 属性控制。

---

## 10. 页面入口接入

### 10.1 AppShell 导航

`frontend/src/app/layout/AppShell.tsx` 的登录态 `nav` 区，在「训练工作台」与「个人资料」之间（或按现有顺序合适位置）新增：

```tsx
<NavLink to="/stats" className={navLinkClass}>
  统计
</NavLink>
```

### 10.2 控制台首页入口卡片

`frontend/src/features/home/pages/HomePage.tsx` 引入 `StatsEntryCard`（来自 `stats/components`），在 Welcome/AI 卡之后、训练卡片之前渲染：

```tsx
<StatsEntryCard />
```

`StatsEntryCard` 自行拉取 `getStatsSummary`，内部处理加载/空/错误，不影响首页主体数据流。

### 10.3 路由

`frontend/src/app/router.tsx` 在 `ProtectedOutlet` children 新增 `/stats` 路由（见 §4）。

---

## 11. 错误处理设计

沿用 `ApiRequestError`（`shared/api/http.ts`）。重点错误码：

- `UNAUTHORIZED`（401）
- `RESOURCE_NOT_FOUND`（404，exerciseId 不存在）
- `INVALID_ARGUMENT`（400，metric 不在可选集合内）

处理方式：

- 训练区 / 身体指标区各自持有 error 态，用中文文案 + 重试按钮。
- 单动作进阶拉取失败 → 卡片内展示错误 + 重试，不影响其他卡片。
- 首页入口卡片失败 → 折叠为纯入口（不阻塞首页）。

建议在 `lib/stats-formatters.ts`（或 `stats-mappers.ts`）提供 `getStatsErrorMessage(error, fallback)`，把 `ApiRequestError` 映射为中文文案，与 `ai-coach` 的 `getAiCoachErrorMessage` 风格一致。

---

## 12. 前端约束与规则

1. **信任后端口径**：出现次数 / 总组数 / 总次数 / 容量等聚合值直接展示，前端不做二次计算；`overviewCopy` / `funCopy` 后端已生成，前端原样展示。
2. **只读边界**：`stats` 模块所有操作只读，无任何写请求。
3. **时间范围语义**：训练区与身体指标区时间范围独立存储、独立请求；不共享同一 `TimeRangeSelection`。
4. **筛选分层**：时间范围 → 后端请求；动作类型 / 名称搜索 → 前端本地过滤（避免无谓请求）。
5. **字段可空**：力量/有氧字段互斥为 `null`，展示时按 `structureType` 取对应字段，避免渲染 `null` 为「0」误导。
6. **身体指标键独立**：`BodyMetricKey` 不直接复用 cycle-template 的 `MetricKey`（两集合不同），单独定义。

---

## 13. 页面状态设计

`StatsPage` 需显式覆盖：

- `loading summary` / `summary error` / `empty summary`
- 动作筛选（类型 + 搜索）为空 → 空态
- 单卡片：`idle / expanding / loading progression / error` / 展开（含曲线）
- `loading body metrics` / `error` / `empty`
- `loading` 与 `empty` 由真实接口返回驱动，不用前端猜测

---

## 14. 实现顺序建议

1. `package.json` 新增 `recharts` 依赖。
2. `types/stats.ts`（类型先行，契约对齐接口文档）。
3. `api/stats.ts`（三个接口封装 + query 归一化）。
4. `lib/`（`stats-options.ts` 预设/选项、`stats-formatters.ts` 格式化、`stats-mappers.ts` 错误文案）。
5. 图表组件 `ProgressionChart` / `BodyMetricsChart`（先验证 recharts 接入）。
6. `SummaryHero` / `StatsFilterBar` / `ExerciseStatCard` / `StatsEmptyState`。
7. `StatsPage` 组合 + `router.tsx` 路由 + `AppShell` 导航。
8. `StatsEntryCard` + `HomePage` 接入。
9. 前端测试补充 + 契约联调校验。

原因：先稳住类型与 API，再验证 recharts，最后拼装页面与入口，与 `ai-coach_DDD.md` 的顺序策略一致。

---

## 15. 验收标准

1. `/stats` 页可访问；AppShell 导航出现「统计」，控制台首页出现统计入口卡片。
2. Hero 展示总体值（训练场数 / 总容量 / 总里程 / 总时长）+ `overviewCopy`；首页卡片同样展示。
3. 动作列表按 `appearanceCount` 降序；头部展示出现次数 / 总组数 / 总次数徽标。
4. 力量动作展示力量字段、有氧动作展示有氧字段，并展示 `funCopy`。
5. 训练区时间范围（近 30 天 / 近 90 天 / 今年 / 全部 / 自定义）切换后刷新汇总与列表；动作类型筛选 + 名称搜索生效。
6. 身体指标区指标切换 + 独立时间范围生效。
7. 单动作展开后 `ProgressionChart` 渲染（力量默认「最大重量」、有氧默认「总距离」，可切换）；身体指标折线图渲染（recharts）。
8. 空数据有友好提示；加载 / 错误态齐全。
9. 前端 `pnpm test` 通过；契约联调校验通过（与接口文档字段一致）。

---

## 16. 本轮改动文件清单

### 新增文件

| 文件 | 说明 |
| --- | --- |
| `frontend/src/features/stats/types/stats.ts` | 全部统计类型 |
| `frontend/src/features/stats/api/stats.ts` | 三个接口封装 |
| `frontend/src/features/stats/lib/stats-options.ts` | 时间预设、指标选项、动作筛选选项、中文标签 |
| `frontend/src/features/stats/lib/stats-formatters.ts` | 数值/日期格式化、`getStatsErrorMessage` |
| `frontend/src/features/stats/lib/stats-mappers.ts` | 本地过滤（类型/搜索）、图表数据映射 |
| `frontend/src/features/stats/components/SummaryHero.tsx` | 汇总 Hero |
| `frontend/src/features/stats/components/StatsFilterBar.tsx` | 筛选栏 |
| `frontend/src/features/stats/components/ExerciseStatCard.tsx` | 动作卡片（含展开） |
| `frontend/src/features/stats/components/ProgressionChart.tsx` | 单动作进阶曲线 |
| `frontend/src/features/stats/components/BodyMetricsChart.tsx` | 身体指标折线图 + 指标切换 |
| `frontend/src/features/stats/components/StatsEmptyState.tsx` | 统一空态 |
| `frontend/src/features/stats/components/StatsEntryCard.tsx` | 首页入口卡片 |
| `frontend/src/features/stats/pages/StatsPage.tsx` | 统计页 |

### 修改文件

| 文件 | 改动 |
| --- | --- |
| `frontend/package.json` | `dependencies` 新增 `recharts` |
| `frontend/src/app/router.tsx` | 新增 `/stats` 受保护路由 + import |
| `frontend/src/app/layout/AppShell.tsx` | 登录态导航新增「统计」`NavLink` |
| `frontend/src/features/home/pages/HomePage.tsx` | 引入并渲染 `StatsEntryCard` |

### 不改动

- `backend/**`、`db/**`（统计只读，后端接口由后端角色实现）。
- 既有 `ai-coach` / `workout` / `cycle-template` 模块行为。

---

## 17. 风险与未完成项

| 风险 / 缺口 | 等级 | 说明与对策 |
| --- | --- | --- |
| `recharts` 依赖新增需网络安装 | 低 | 需在开发环境执行 `pnpm add recharts`；文档仅记录依赖，CI 安装即可 |
| 身体指标键集合独立于 `MetricKey` | 低 | `BodyMetricKey` 单独定义，避免误用 cycle-template 的 `MetricKey` |
| 动作筛选分层（后端时间 vs 前端类型/搜索）需与接口对账 | 低 | 接口 `summary` 仅按时间范围过滤；类型/名称搜索在前端过滤 `exercises`，联调时确认过滤字段 |
| 首页入口卡片额外一次 `summary` 请求 | 低 | 可接受；失败时折叠为纯入口不影响首页 |

---

## 18. 设计结论

`stats` 是一个独立的只读展示模块，采用与既有 feature 一致的 `api / components / lib / pages / types` 结构：

1. **三接口**（summary / exercise 进阶 / body-metrics）驱动页面，前端只透传时间范围，聚合口径全由后端负责。
2. **双入口**：AppShell 导航 + 首页 `StatsEntryCard`。
3. **训练区 / 身体指标区时间范围独立**；动作类型与名称搜索在前端本地过滤。
4. **recharts** 新增依赖，`LineChart` 承载进阶曲线与身体指标折线，空态统一处理。
5. **可空字段**与**只读边界**贯穿实现，避免误导展示与误写。

如后续进入实现阶段，本文档可直接作为 `stats` 前端开发与联调的执行基线。
