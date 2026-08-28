# DailyForge 统计模块 接口文档

> 版本：v0.1
> 日期：2026-08-26
> 状态：待评审
> 关联 PRD：`docs/prd/stats_PRD.md`
> 模块名称：`stats`

---

## 1. 文档范围

定义统计模块的三个只读聚合接口。所有接口需鉴权（`Authorization: Bearer <token>`），且只返回当前登录用户自己的数据。统一响应体 `ApiResponse<T>`。

数据口径：统计用户**打卡过的所有训练记录**（排除 `cancelled` 会话），动作只要有任一执行项实际值即纳入。有氧动作若未记录 `distance_km`，后端会按「时长 × 配速（`speed_kmh` 或 `pace_seconds_per_km` 推算）」补算距离。

---

## 2. 接口列表

| 编号 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| S1 | GET | `/api/stats/summary` | 总体值 + 按出现次数降序的动作列表 |
| S2 | GET | `/api/stats/exercise/{exerciseId}` | 单动作聚合详情 + 进阶序列 |
| S3 | GET | `/api/stats/body-metrics` | 身体指标时间序列（折线图数据） |

通用查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `from` | string | 否 | 起始时间，ISO-8601 日期或日期时间 |
| `to` | string | 否 | 结束时间，ISO-8601 日期或日期时间 |

不传即不限（全部时间）。

---

## 3. 接口详情

### 3.1 S1 训练统计汇总 `GET /api/stats/summary`

**响应 `data`**：

```json
{
  "overall": {
    "sessionCount": 12,
    "totalSets": 200,
    "totalReps": 1500,
    "totalVolumeKg": 12345.5,
    "totalDistanceKm": 88.5,
    "totalDurationMinutes": 720,
    "overviewCopy": "你从开始运动到现在累计训练 12 场、总容量 12345.5kg、总里程 88.5km。"
  },
  "exercises": [
    {
      "exerciseId": 1001,
      "name": "卧推",
      "exerciseType": "strength",
      "structureType": "set_based",
      "appearanceCount": 5,
      "setCount": 20,
      "repCount": 150,
      "totalVolumeKg": 8000.5,
      "avgWeightKg": 60.2,
      "maxWeightKg": 80.0,
      "avgReps": 7.5,
      "totalDurationSeconds": null,
      "totalDistanceKm": null,
      "avgSpeedKmh": null,
      "funCopy": "你已经卧推 150 次，总容量 8000.5kg，相当于 X 头成年大象。"
    }
  ]
}
```

字段说明：

- `overall`：跨所有动作汇总。
  - `sessionCount`：纳入的**有实际打卡数据**的会话数（训练场数）。
  - `totalSets` / `totalReps`：总组数 / 总次数。
  - `totalVolumeKg`：总容量（力量动作 Σ 重量×次数）。
  - `totalDistanceKm`：总里程（有氧动作 Σ 距离）。
  - `totalDurationMinutes`：总时长（**所有动作**（力量+有氧）实际时长合计，`duration_minutes` 换算并入秒后再转分钟，统一口径）。
  - `overviewCopy`：总览趣味文案（含「相当于绕地球 X 圈」等等价换算）。
- `exercises`：按 `appearanceCount` 降序。
  - `appearanceCount`：出现次数（有实际数据的会话数）。
  - `setCount` / `repCount`：总组数 / 总次数（仅 set_based，有氧为 null）。
  - 力量（set_based）：`totalVolumeKg`、`avgWeightKg`、`maxWeightKg`、`avgReps`。
  - 有氧（single_segment）：`totalDistanceKm`、`avgSpeedKmh`。
  - `totalDurationSeconds`：**无论类型，只要该动作记录了实际时长就展示**（`duration_minutes` 换算并入秒）；未记录则为 null。
  - `funCopy`：该动作的趣味文案。

> `set_based` 关注力量字段（`totalVolumeKg/avgWeightKg/maxWeightKg/avgReps`），`single_segment` 关注 `totalDistanceKm/avgSpeedKmh`；`totalDurationSeconds` 为通用字段，有实际时长即展示，否则 null。

### 3.2 S2 单动作进阶 `GET /api/stats/exercise/{exerciseId}`

**请求**：`exerciseId`（动作 ID）；查询参数同通用 `from`/`to`。

**响应 `data`**：

```json
{
  "exerciseId": 1001,
  "name": "卧推",
  "exerciseType": "strength",
  "structureType": "set_based",
  "appearanceCount": 5,
  "setCount": 20,
  "repCount": 150,
  "totalVolumeKg": 8000.5,
  "avgWeightKg": 60.2,
  "maxWeightKg": 80.0,
  "avgReps": 7.5,
  "progression": [
    {
      "date": "2026-07-01",
      "maxWeightKg": 60.0,
      "maxReps": 10,
      "totalVolumeKg": 1200.0,
      "totalDurationSeconds": null,
      "totalDistanceKm": null
    }
  ]
}
```

字段说明：

- 顶部字段与 S1 中该动作一致。
- `progression`：按日期升序。力量动作按「天」聚合 `maxWeightKg` / `maxReps` / `totalVolumeKg`；有氧动作聚合 `totalDurationSeconds` / `totalDistanceKm`，另一组为 null。时长统一以秒为单位（`duration_minutes` 换算并入秒）。

### 3.3 S3 身体指标时间序列 `GET /api/stats/body-metrics`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `metric` | string | 是 | 指标键，见下 |
| `from` / `to` | string | 否 | 时间范围（按 `record_date`） |

可选 `metric` 值（与 `body_metric_logs` 字段对应）：

`weight_kg`、`body_fat_percent`、`bmi`、`skeletal_muscle_percent`、`body_water_percent`、`basal_metabolic_rate_kcal`、`waist_cm`、`hip_cm`、`waist_hip_ratio`、`body_age`

**响应 `data`**：

```json
{
  "metric": "weight_kg",
  "unit": "kg",
  "points": [
    { "date": "2026-07-01", "value": 75.5 },
    { "date": "2026-07-15", "value": 74.8 }
  ]
}
```

字段说明：

- `points`：按日期升序；同一天多条记录取最新一条；某天该指标为空则跳过。
- `unit`：指标单位（kg / % / cm / kcal / level 等）。

---

## 4. 错误码

- `UNAUTHORIZED`：未登录，HTTP 401。
- `RESOURCE_NOT_FOUND`：exerciseId 不存在，HTTP 404。
- `INVALID_ARGUMENT`：`metric` 不在可选集合内，HTTP 400。

---

## 5. 前端调用顺序建议

1. 页面加载 → `GET /stats/summary`（默认全部时间）→ 渲染 Hero 总体值/文案 + 动作列表。
2. 时间范围 / 动作筛选变化 → 重新 `GET /stats/summary`。
3. 展开某动作进阶 → `GET /stats/exercise/{id}`（可选带时间范围）。
4. 身体指标区 → `GET /stats/body-metrics?metric=...`（独立时间范围）。
5. 控制台首页入口卡片 → 复用 `GET /stats/summary` 的 `overall`（`sessionCount`、`totalVolumeKg`、`overviewCopy` 等）。

---

## 6. 变更说明

- 新增 `stats` 模块三接口；不修改既有接口。
- 复用 `MetricKey`（度量键语义）与既有训练/指标查询能力。
