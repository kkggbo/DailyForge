# DailyForge 统计模块 详细设计文档（DDD）

> 版本：v0.1
> 日期：2026-08-26
> 文档状态：待开发实现设计稿
> 模块归属：`backend` 单体应用
> 目标 Java 包路径：`com.dailyforge.modules.stats`
> 上游契约：`docs/interfaces/stats_接口文档.md`、`docs/prd/stats_PRD.md`

---

## 一、文档说明

### 1.1 上游输入文档

- PRD：[stats_PRD.md](../../prd/stats_PRD.md)
- 接口文档：[stats_接口文档.md](../../interfaces/stats_接口文档.md)
- 现有 DDD 结构范本：[ai_coach_DDD.md](../ai_coach_module/ai_coach_DDD.md)

### 1.2 本文档目标

本文档把 `stats` 模块收口为可直接指导后端实现、代码评审与后续重构的技术方案。重点明确：

- 模块定位与职责边界（只读聚合，不写数据）
- 分层结构（interfaces / application / infrastructure / controller）
- 数据口径实现（排除 cancelled、有实际值判定、出现次数/总组数/总次数、力量 vs 有氧分支）
- 趣味等价物文案常量与生成逻辑
- 身体指标序列（按 record_date、同天取最新、metric 白名单）
- 性能策略（一次性内存聚合）与是否复用 `TrainingPerformanceAggregationService`
- 新增组件：`StatsQueryApplicationService`、Controller 三接口、请求/响应 VO
- 验收标准与本轮改动文件清单

### 1.3 当前仓库事实

截至本文档编写时，仓库现状：

1. 统一基础设施已存在：`ApiResponse<T>`、`ErrorCode`、`BusinessException`、`GlobalExceptionHandler`、JWT 鉴权链路（`AuthSecurityUtils.getCurrentUserId()`）、SpringDoc/OpenAPI。
2. `com.dailyforge.modules.stats` 已有占位 `package-info.java`，尚无业务实现。
3. 训练打卡数据已具备：
   - `training_sessions`（`user_id`、`status`、`session_type`、`started_at`、`completed_at`）
   - `training_session_exercises`（`session_id`、`exercise_id`、`exercise_name_snapshot`、`structure_type`）
   - `training_session_exercise_items`（`session_exercise_id`、`item_type`）
   - `training_session_exercise_item_metrics`（`session_exercise_item_id`、`metric_key`、`planned_value_number`、`actual_value_number`）
4. `TrainingPerformanceAggregationService` 已存在，封装了「按用户加载最近 N 个 completed workout 会话 → 按 sessionId 批量查动作/组/指标 → 内存聚合」的范式，可借鉴复用。
5. `MetricKey` 已作为度量键单一来源（`weight_kg`、`reps`、`duration_seconds`、`duration_minutes`、`distance_km`、`speed_kmh`、`pace_seconds_per_km` 等，含 `getUnit()`）。
6. `body_metric_logs` 已具备（`record_date`、`weight_kg`、`body_fat_percent`、`bmi`、`skeletal_muscle_percent`、`body_water_percent`、`basal_metabolic_rate_kcal`、`waist_cm`、`hip_cm`、`waist_hip_ratio`、`body_age`、`is_del`）。
7. `SystemExerciseLookupResult` / `SystemExerciseLookupService.loadActiveSystemExercisesByIds(...)` 可提供动作名称/类型。

> 说明：本文档所有「新增」「待新增」均为待实现项，不表示仓库中已存在。

---

## 二、方案概述

### 2.1 模块定位

`stats` 是一个**只读聚合模块**：聚合用户全部已打卡训练记录（不限周期/会话是否完成）与身体指标趋势，供前端折线图/卡片展示。它：

- 只读取数据，**不写入任何数据**（无 DDL、无状态变更）。
- 只返回当前登录用户自己的数据（数据归属强校验）。
- 纯查询聚合，无事务写入需求，天然幂等（同一请求返回稳定结果）。

### 2.2 本期交付范围

| 能力 | 说明 | 状态 |
|------|------|:---:|
| S1 训练统计汇总 | `GET /stats/summary`，总体值 + 按出现次数降序动作列表 | 待开发 |
| S2 单动作进阶 | `GET /stats/exercise/{exerciseId}`，单动作聚合详情 + 进阶序列 | 待开发 |
| S3 身体指标时间序列 | `GET /stats/body-metrics?metric=...`，折线图数据 | 待开发 |

### 2.3 明确不做（本版）

- 训练频率柱状图（二期）。
- 导出 / 分享。
- 写入/刷新任何统计快照表（本版纯实时聚合，无物化）。

---

## 三、数据口径与状态

### 3.1 纳入范围（已确认，PRD §4）

- **会话**：该用户所有 `session_type='workout'` 的训练会话，**排除 `status='cancelled'`**；`in_progress` 与 `completed` 均纳入。
- **「打卡」判定**：一个动作只要任一执行项存在实际值（`actual_value_number != null`）即算练过；一个组（item）有实际值才算一组。
- **动作维度三个清晰量**：
  - `出现次数`：出现过该动作（有实际数据）的**不同会话数**
  - `总组数`：累计有实际值的组数
  - `总次数`：累计 reps
- **力量动作（set_based）**：另展示 `总容量`（Σ 重量×次数）、`平均重量`、`最大重量`、`平均次数`。
- **有氧动作（single_segment）**：另展示 `总时长`、`总距离`、`平均配速/速度`。
- **身体指标**：`body_metric_logs` 按 `record_date` 排序，同一天多条取最新，某天该指标为空则跳过。
- **趣味等价物**：静态常量（1 头大象≈5000kg、地球周长≈40075km 等），配置于后端常量。

### 3.2 会话过滤

实现时建议在 `TrainingSessionMapper` 新增查询方法（待新增），按 `user_id + session_type='workout' + status != 'cancelled'` 加载，并可选按 `started_at`/`completed_at` 时间范围过滤。等价实现：

```sql
SELECT * FROM training_sessions
WHERE user_id = #{userId}
  AND session_type = 'workout'
  AND status <> 'cancelled'
  -- AND started_at >= #{from} AND started_at <= #{to}   -- 可选时间过滤
ORDER BY started_at ASC, id ASC
```

> 说明：`from`/`to` 为可选参数，不传即不限。时间语义统一用会话 `started_at`（训练发生时刻）。

### 3.3 有实际值判定（打卡）

- 组（item）有实际值：`TrainingSessionExerciseItemMetricEntity.actualValueNumber != null`（该组任一 metric 有实际值）。
- 动作出现（打卡）判定：该动作在该会话至少有一组有实际值 → 计入该会话的「出现」，且 `出现次数` 用 `sessionId` 去重。

### 3.4 力量 vs 有氧指标分支

- `structureType == "set_based"`（力量）→ 计算 `totalVolumeKg / avgWeightKg / maxWeightKg / avgReps / setCount / repCount`；`duration/distance/avgSpeed` 置 null。
- `structureType == "single_segment"`（有氧）→ 计算 `totalDurationSeconds / totalDistanceKm / avgSpeedKmh`；`setCount / repCount / volume/weight` 置 null。
- 度量键取值复用 `MetricKey`：
  - 力量：`weight_kg`、`reps`、`rpe`、`rest_seconds`
  - 有氧：`duration_seconds`、`duration_minutes`、`distance_km`、`speed_kmh`、`pace_seconds_per_km`、`incline_percent`、`intensity_level`
- `totalDurationSeconds` 汇总：将 `duration_minutes` 换算为秒后并入 `duration_seconds`（避免口径分叉）；`totalDistanceKm` 汇总 `distance_km`。
- `avgSpeedKmh`：由 Σ距离 / Σ时长换算（有距离且有时长时），否则基于 `speed_kmh` 均值；若该动作无 `speed_kmh` 原始值，则用 总距离km / 总时长h 计算。

### 3.5 动作元数据来源

- 动作名称：优先 `TrainingSessionExerciseEntity.exerciseNameSnapshot`（打卡快照，保证历史名一致）；对 S2 按 exerciseId 查询时可回查 `SystemExerciseLookupService.loadActiveSystemExercisesByIds` 补充 `name`/`exerciseType`。
- `exerciseType` / `structureType`：`structureType` 来自打卡快照；`exerciseType`（strength/cardio）来自 `SystemExerciseLookupResult.exerciseType`。

---

## 四、API 设计

### 4.1 Base Path 与鉴权

- 外部访问前缀：`/api/stats`（Controller 映射 `/stats`）。
- 统一返回 `ApiResponse<T>`，统一 Bearer Token 鉴权。
- 用户身份：`AuthSecurityUtils.getCurrentUserId()`（可复用 `PlanUserSupportService.requireActiveUserId()` 校验账号 active）。

### 4.2 接口总览

| 编号 | 方法 | 路径 | 作用 |
|------|------|------|------|
| S1 | GET | `/stats/summary` | 总体值 + 按出现次数降序动作列表 |
| S2 | GET | `/stats/exercise/{exerciseId}` | 单动作聚合详情 + 进阶序列 |
| S3 | GET | `/stats/body-metrics` | 身体指标时间序列 |

### 4.3 S1 训练统计汇总

**请求参数**：`from` / `to`（可选，ISO-8601 日期或日期时间）。

**核心实现步骤**：
1. `requireActiveUserId()`。
2. 解析 `from`/`to`（非法格式 → `INVALID_ARGUMENT`）。
3. 加载范围内 workout 会话（排除 cancelled）+ 动作/组/指标，一次性内存加载。
4. 按 `exerciseId` 分组做内存聚合（见 §五）。
5. 计算 `overall`（sessionCount / totalSets / totalReps / totalVolumeKg / totalDistanceKm / totalDurationMinutes）。
6. 生成 `overall.overviewCopy` 与每个动作的 `funCopy`（见 §六）。
7. `exercises` 按 `appearanceCount` 降序（并列按 `exerciseId` 升序稳定）。

**事务边界**：只读，无事务要求。

### 4.4 S2 单动作进阶

**请求**：`exerciseId`（路径参数）+ 通用 `from`/`to`。

**核心实现步骤**：
1. `requireActiveUserId()`。
2. 加载范围内 workout 会话数据（复用 S1 的加载/聚合逻辑，仅保留该 `exerciseId`）。
3. 若该动作在范围内无任何数据 → 校验 `exerciseId` 是否存在于系统动作库：
   - 存在但无数据 → 返回空聚合详情（`appearanceCount=0`，空 `progression`）。
   - 不存在 → `RESOURCE_NOT_FOUND`。
4. 计算动作顶部聚合字段（与 S1 中该动作一致）。
5. `progression`：按「天」（动作发生的 `started_at` 日期）分组，力量动作聚合 `maxWeightKg / maxReps / totalVolumeKg`，有氧动作聚合 `totalDurationSeconds / totalDistanceKm`，按日期升序。

**事务边界**：只读。

### 4.5 S3 身体指标时间序列

**请求参数**：
- `metric`（必填）：白名单见下。
- `from` / `to`（可选）：按 `record_date` 过滤。

**可选 metric 白名单**（与 `body_metric_logs` 字段一致）：

```
weight_kg、body_fat_percent、bmi、skeletal_muscle_percent、body_water_percent、
basal_metabolic_rate_kcal、waist_cm、hip_cm、waist_hip_ratio、body_age
```

**核心实现步骤**：
1. `requireActiveUserId()`。
2. 校验 `metric` 在白名单内，否则 → `INVALID_ARGUMENT`。
3. `BodyMetricLogMapper` 新增查询（待新增）：`SELECT * FROM body_metric_logs WHERE user_id = #{userId} AND is_del = 0 [AND record_date >= from] [AND record_date <= to] ORDER BY record_date ASC, id DESC`。
4. 同一天多条取最新：遍历时按 `record_date` 分组，取该日 `id` 最大（最新写入）一条；该日目标字段为 null 则跳过。
5. 组装 `points`（date + value）升序。
6. `unit` 从映射表取（见 §五 身体指标）。

**事务边界**：只读。

---

## 五、聚合实现细节

### 5.1 复用与扩展策略

**决策：不直接改动 `TrainingPerformanceAggregationService` 以适配 stats，而是新建 `stats` 模块自己的聚合服务**，但借鉴其「批量加载 + 内存聚合」范式。理由：

- `TrainingPerformanceAggregationService.aggregateRecentCompletedWorkout` 面向 **completed + 最近 N 个** 会话，为 AI 上下文服务，口径与 stats 不同（stats 需 in_progress + completed、时间范围可配、无 limit）。
- 改动它会引入回归风险，且职责混淆。

**建议**：在 `stats` 模块新增 `StatsQueryApplicationService`（或拆 `StatsAggregationService` + `StatsQueryApplicationService`），内部封装一套独立的加载/聚合逻辑。后续若发现两处聚合逻辑高度重叠，可再抽取共享工具（预留扩展点）。

### 5.2 一次性内存聚合（性能策略）

- 个人数据量小（单用户训练记录有限），采用**一次性加载该用户范围内全部所需行 → 内存聚合**，避免多次往返。
- 加载流程（复用 `TrainingSessionMapper` / `TrainingSessionExerciseMapper` / `TrainingSessionExerciseItemMapper` / `TrainingSessionExerciseItemMetricMapper` 现有的 `selectBySessionIds` / `selectBySessionExerciseIds` / `selectBySessionExerciseItemIds` 批量查询）：
  1. 查范围内的 workout 会话（排除 cancelled）。
  2. `exerciseMapper.selectBySessionIds(sessionIds)`。
  3. `itemMapper.selectBySessionExerciseIds(exerciseEntityIds)`。
  4. `metricMapper.selectBySessionExerciseItemIds(itemIds)`。
  5. 建 `sessionId → sessions`、`exerciseEntityId → items`、`itemId → metrics` 的内存索引。
- 不引入 N+1，也不做 SQL 端聚合（保持 SQL 简单，聚合放内存）。

### 5.3 动作聚合计算

对每个 `exerciseId`：

- `appearanceCount`：包含该动作且有实际数据的 `sessionId` 去重数。
- `setCount`：有实际值的 item 数。
- `repCount`：Σ `reps` 实际值。
- 力量（set_based）：
  - `totalVolumeKg`：Σ (`weight_kg` 实际值 × `reps` 实际值)，按 item 内匹配。
  - `avgWeightKg`：Σ重量 / 重量实际值条数。
  - `maxWeightKg`：max(`weight_kg` 实际值)。
  - `avgReps`：Σ次数 / 次数实际值条数。
- 有氧（single_segment）：
  - `totalDurationSeconds`：Σ (`duration_seconds` + `duration_minutes`×60)。
  - `totalDistanceKm`：Σ `distance_km`。
  - `avgSpeedKmh`：优先 Σ`speed_kmh`/条数；否则 总距离km / 总时长h。

### 5.4 总体值汇总（overall）

- `sessionCount`：纳入的会话数。
- `totalSets`：跨所有动作的有实际值 item 数。
- `totalReps`：跨所有动作 Σ reps。
- `totalVolumeKg`：跨所有力量动作 Σ (重量×次数)。
- `totalDistanceKm`：跨所有有氧动作 Σ distance_km。
- `totalDurationMinutes`：跨所有动作 Σ 实际时长，换算为分钟（`duration_seconds/60 + duration_minutes`）。

### 5.5 身体指标 unit 映射

| metric 值 | unit |
|------|------|
| `weight_kg` | kg |
| `body_fat_percent` | % |
| `bmi` | — |
| `skeletal_muscle_percent` | % |
| `body_water_percent` | % |
| `basal_metabolic_rate_kcal` | kcal |
| `waist_cm` | cm |
| `hip_cm` | cm |
| `waist_hip_ratio` | — |
| `body_age` | level |

（unit 通过常量映射，不依赖数据库。）

---

## 六、趣味等价物文案

### 6.1 常量定义

在 `stats` 模块新增 `StatsFunConstants`（或内嵌于服务常量），集中定义静态等价物常量：

```java
public final class StatsFunConstants {
    public static final BigDecimal ELEPHANT_WEIGHT_KG = new BigDecimal("5000");  // 1 头成年大象 ≈ 5000kg
    public static final BigDecimal EARTH_CIRCUMFERENCE_KM = new BigDecimal("40075"); // 地球周长 ≈ 40075km
    // 可按需扩展：汽车重量、楼层高度、马拉松距离(42.195km) 等
}
```

> 这些常量是「后端配置于常量的趣味等价物」（PRD §4），未来可改配置化。

### 6.2 生成逻辑

`StatsFunCopyGenerator`（建议新建，纯函数式、可单测）提供：

- `buildOverviewCopy(sessionCount, totalVolumeKg, totalDistanceKm)`：
  - 总览：`"你从开始运动到现在累计训练 N 场、总容量 X kg、总里程 Y km。"`
  - 趣味（叠加一条）：若 `totalDistanceKm` 可换算 → `"总里程相当于绕地球 Z 圈。"`（`Z = totalDistanceKm / EARTH_CIRCUMFERENCE_KM`，保留 2 位小数，> 0 时才展示）。
- `buildExerciseFunCopy(exerciseName, exerciseType, repCount, totalVolumeKg, totalDistanceKm)`：
  - 力量：`"你已经{动作名} {repCount} 次，总容量 {X}kg，相当于 {N} 头成年大象。"`（`N = totalVolumeKg / ELEPHANT_WEIGHT_KG`，保留 2 位小数）。
  - 有氧：`"你已经累计{动作名} {Y}km，相当于绕地球 {Z} 圈。"`

**数值格式化**：重量/距离等用 `BigDecimal` 保留 1~2 位小数，去尾随零；0 值不生成换算趣味句。

**可测试性**：copy 生成是纯函数（入参为聚合数值），独立成类便于单元测试。

---

## 七、安全设计

### 7.1 认证链路

`stats` 三接口自然走现有 JWT 鉴权链路（`SecurityConfig` 已拦截，无需改放行规则）。

### 7.2 身份获取

统一通过 `AuthSecurityUtils.getCurrentUserId()`；Controller 不解析 JWT Claims，不接受前端传 userId。推荐复用 `PlanUserSupportService.requireActiveUserId()`（校验账号 active）。

### 7.3 数据归属

所有查询均以 `userId` 为强约束（`training_sessions.user_id`、`body_metric_logs.user_id`），确保只返回当前用户数据。

### 7.4 日志脱敏

- 允许记录：`userId`、`exerciseId`、`from/to`、聚合结果规模（动作数/会话数/点位数）。
- 禁止记录：完整原始训练备注、完整身体指标原始记录、其他用户数据。

---

## 八、错误码设计

### 8.1 复用现有错误码

- `UNAUTHORIZED`（401）
- `RESOURCE_NOT_FOUND`（404，exerciseId 不存在）
- `INVALID_ARGUMENT`（400，metric 不在白名单 / from/to 格式非法）

### 8.2 无需新增错误码

本模块为纯只读聚合，无需新增 ErrorCode。

---

## 九、Java 代码结构设计

### 9.1 目标包结构

```text
com.dailyforge.modules.stats
├─ application
│  └─ service
│     ├─ StatsQueryApplicationService.java      # 对外用例编排（S1/S2/S3）
│     ├─ StatsAggregationService.java           # 训练数据加载 + 内存聚合（复用范式）
│     └─ StatsFunCopyGenerator.java             # 趣味文案纯函数（可单测）
├─ infrastructure
│  └─ persistence
│     └─ mapper
│        └─ StatsQueryMapper.java               # 待新增：会话/动作/组/指标按范围查询、body_metric 序列
├─ interfaces
│  ├─ rest
│  │  └─ StatsController.java                    # 待新增：S1/S2/S3
│  └─ vo
│     ├─ StatsSummaryResponse.java               # overall + exercises
│     ├─ StatsOverallResponse.java
│     ├─ StatsExerciseAggregateResponse.java     # 单个动作聚合（S1 项 / S2 顶部复用）
│     ├─ StatsExerciseDetailResponse.java        # S2 顶部 + progression
│     ├─ StatsProgressionPointResponse.java
│     └─ BodyMetricSeriesResponse.java           # S3 metric + unit + points
```

> 说明：`stats` 模块无需 DTO 请求体（三接口均为 GET 查询），因此只有 VO；若采用 MyBatis 注解 SQL 可放在 `StatsQueryMapper`，或复用现有 `TrainingSession*Mapper` + 新增少量方法。`StatsAggregationService` 也可直接注入现有四个 `TrainingSession*Mapper` + `BodyMetricLogMapper` + `SystemExerciseLookupService`，避免新增 Mapper。**推荐后者（复用现有 Mapper）**，仅当需要复杂 SQL 时才新增 `StatsQueryMapper`。

### 9.2 核心类职责

| 类名 | 新增 | 职责 |
|------|:---:|------|
| `StatsController` | 待新增 | 暴露 S1/S2/S3，参数接收、Swagger 注解、`ApiResponse<T>` 返回 |
| `StatsQueryApplicationService` | 待新增 | 编排三接口用例：鉴权、参数校验、调用聚合、组装 VO |
| `StatsAggregationService` | 待新增 | 加载 workout 会话/动作/组/指标 + 内存聚合（复用现有 Mapper 批量查询范式） |
| `StatsFunCopyGenerator` | 待新增 | 总体/动作趣味文案纯函数（含常量） |
| `StatsQueryMapper` | 可选 | 需要复杂 SQL 时才新增；否则复用现有 Mapper |

### 9.3 VO 清单

- `StatsSummaryResponse`：`overall` + `exercises`（List）。
- `StatsOverallResponse`：`sessionCount / totalSets / totalReps / totalVolumeKg / totalDistanceKm / totalDurationMinutes / overviewCopy`。
- `StatsExerciseAggregateResponse`：`exerciseId / name / exerciseType / structureType / appearanceCount / setCount / repCount / totalVolumeKg / avgWeightKg / maxWeightKg / avgReps / totalDurationSeconds / totalDistanceKm / avgSpeedKmh / funCopy`。
- `StatsExerciseDetailResponse`：顶部聚合字段（复用 `StatsExerciseAggregateResponse` 字段）+ `progression`。
- `StatsProgressionPointResponse`：`date / maxWeightKg / maxReps / totalVolumeKg / totalDurationSeconds / totalDistanceKm`。
- `BodyMetricSeriesResponse`：`metric / unit / points`；`points` 元素为 `date + value`。

### 9.4 Swagger / OpenAPI 注解约定

- Controller：`@Tag(name = "Stats")`、`@Operation`、`@ApiResponses`、`@SecurityRequirement(name = "bearerAuth")`。
- VO：统一 `@Schema`，对 `metric`、`structureType`、`date`、`value` 给示例。

### 9.5 Debug 日志设计

- 允许：`userId`、`exerciseId`、`metric`、`from/to`、返回点位数/动作数。
- 禁止：完整身体指标原始记录、完整训练备注。

---

## 十、事务、一致性与幂等

### 10.1 事务边界

| 方法 | 是否事务 | 说明 |
|------|:---:|------|
| `getSummary` | 否 | 只读 |
| `getExerciseDetail` | 否 | 只读 |
| `getBodyMetrics` | 否 | 只读 |

### 10.2 幂等与一致性

- 纯只读聚合，天然幂等；同一 `from/to` 返回稳定结果（数据不变前提下）。
- 无并发写入点；若训练数据在统计读取过程中被修改，读到的是某个快照时刻的数据，可接受。
- 无需分布式锁 / 物化缓存（个人数据量小，实时聚合即可）。

---

## 十一、配置与扩展点

### 11.1 新增配置项

- `StatsFunConstants` 中的趣味等价物常量为硬编码常量（本版）。若需运营可调，可后续改 `dailyforge.stats.fun-equivalents.*` 配置（预留扩展点）。

### 11.2 外部依赖

- 复用现有 `TrainingSession*Mapper`、`BodyMetricLogMapper`、`SystemExerciseLookupService`、`MetricKey`。无新增外部依赖。
- 前端需新增 `recharts`（前端范围，非本 DDD 重点）。

### 11.3 后续扩展点

- 训练频率柱状图（每周/每月打卡次数）。
- 统计快照物化（数据量大时）。
- 更多趣味等价物配置化。
- 导出 / 分享。

---

## 十二、测试设计

### 12.1 单元测试重点

- `StatsAggregationServiceTest`：
  - 排除 cancelled 会话；纳入 in_progress 与 completed。
  - 动作「出现次数」按 sessionId 去重。
  - 力量 vs 有氧字段分支正确（一组字段为 null，另一组非 null）。
  - `totalVolumeKg` 只按力量动作计算。
  - 有氧 `duration_minutes` 正确换算并入秒。
  - 时间范围过滤生效。
- `StatsFunCopyGeneratorTest`：
  - 力量文案换算（大象头数）、有氧文案换算（绕地球圈数）。
  - 0 值不生成换算趣味句；格式化正确。
- `BodyMetricSeriesBuilderTest`：
  - 同一天多条取最新；该日指标为 null 跳过；按日期升序。

### 12.2 集成测试重点

- S1 空数据返回空 overall + 空 exercises。
- S1 汇总总体值正确、动作按出现次数降序。
- S2 返回聚合详情 + 按天升序 progression；exerciseId 不存在返回 404。
- S3 metric 不在白名单返回 400；正常返回 points + unit。
- 越权：不返回其他用户数据。
- 未登录访问返回 401。

---

## 十三、实施顺序建议

1. 新增 `StatsController` 三接口骨架。
2. 新增 `StatsAggregationService`（加载 + 内存聚合）。
3. 新增 `StatsFunCopyGenerator`。
4. 新增 `StatsQueryApplicationService` 编排。
5. 新增 VO 清单。
6. 补 Swagger、日志、测试。

---

## 十四、当前发现的实现层冲突与缺口

1. **`TrainingPerformanceAggregationService` 口径与 stats 不同**：它面向 completed + 最近 N 个（AI 上下文），stats 需 in_progress+completed、可配时间范围、无 limit。为避免回归，本模块**独立实现聚合**，但复用其「批量加载 + 内存聚合」范式。若后续需统一，可抽取共享聚合工具。
2. **`sessionCount` 口径**：PRD 中 `overall.sessionCount` 是「训练场数」（纳入会话数）。建议用「该时间范围内非 cancelled 的 workout 会话数」；注意与「有实际数据的会话数」区分（前者含无实际值的会话）。
3. **`selectRecentCompletedWorkoutByUserId` 现仅查 completed**：stats 需 in_progress + completed，故不直接复用该方法，需另加载（新增查询或复用 `selectByUserId` 类能力）。
4. **`duration` 口径分叉**：有 `duration_seconds` 与 `duration_minutes` 两种键，需统一换算为秒后再汇总，避免重复/遗漏。

---

## 十五、验收标准

1. 统计覆盖所有已打卡训练（含未完成周期），排除 cancelled。
2. 动作列表按出现次数降序，三个清晰量（出现次数/总组数/总次数）正确。
3. 力量与有氧动作展示各自正确指标。
4. 时间范围与动作筛选生效；身体指标时间范围独立。
5. Hero 总体值 + 趣味文案正确展示；控制台入口卡片复用 `overall`。
6. 单动作进阶曲线与身体指标折线图正确渲染，空数据有友好提示。
7. 后端 `mvn test`、前端 `pnpm test` 通过；契约联调校验通过。

---

## 十六、本轮改动文件清单（预期）

> 以下为按本文档落地实现后预计新增的文件（本轮仅产出 DDD 文档，不落地代码）。

### 新增

- `backend/src/main/java/com/dailyforge/modules/stats/interfaces/rest/StatsController.java`
- `backend/src/main/java/com/dailyforge/modules/stats/interfaces/vo/StatsSummaryResponse.java`
- `backend/src/main/java/com/dailyforge/modules/stats/interfaces/vo/StatsOverallResponse.java`
- `backend/src/main/java/com/dailyforge/modules/stats/interfaces/vo/StatsExerciseAggregateResponse.java`
- `backend/src/main/java/com/dailyforge/modules/stats/interfaces/vo/StatsExerciseDetailResponse.java`
- `backend/src/main/java/com/dailyforge/modules/stats/interfaces/vo/StatsProgressionPointResponse.java`
- `backend/src/main/java/com/dailyforge/modules/stats/interfaces/vo/BodyMetricSeriesResponse.java`
- `backend/src/main/java/com/dailyforge/modules/stats/application/service/StatsQueryApplicationService.java`
- `backend/src/main/java/com/dailyforge/modules/stats/application/service/StatsAggregationService.java`
- `backend/src/main/java/com/dailyforge/modules/stats/application/service/StatsFunCopyGenerator.java`
- `backend/src/test/java/com/dailyforge/modules/stats/...`（对应单测/集成测试）

### 可选（仅当需要复杂 SQL）

- `backend/src/main/java/com/dailyforge/modules/stats/infrastructure/persistence/mapper/StatsQueryMapper.java`

> 无现有文件被修改（`com.dailyforge.modules.stats` 目前仅有占位 `package-info.java`）。
