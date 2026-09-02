# DailyForge 饮食模块 接口文档

> 版本：v0.1
> 日期：2026-09-03
> 状态：待评审
> 关联 PRD：`docs/prd/diet_PRD.md`
> 模块名称：`diet`

---

## 1. 文档范围

定义饮食模块的只读/写入接口：每日总结与进度、饮食记录增删改、食物库（搜索/上传/收藏）、每日目标、摄入统计。所有接口需鉴权（`Authorization: Bearer <token>`）；除食物库为全局共享外，记录/收藏/目标/统计只作用于当前用户。统一响应体 `ApiResponse<T>`。

通用枚举：

- 餐次 `mealType`：`breakfast` / `lunch` / `dinner` / `snack`
- 食物分类 `category`：`staple`（主食）/ `meat_egg`（肉蛋水产）/ `vegetable`（蔬菜）/ `fruit`（水果）/ `dairy`（奶制品）/ `nut_bean`（坚果豆类）/ `drink`（饮品）/ `other`（其它）
- 活动量 `activityLevel`（资料字段）：`sedentary` / `light` / `moderate` / `high` / `very_high`

---

## 2. 接口列表

| 编号 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| D1 | GET | `/api/diet/summary?date=` | 某日总结（目标+各餐+合计+进度） |
| D2 | POST | `/api/diet/logs` | 添加饮食记录 |
| D3 | PUT | `/api/diet/logs/{logId}` | 修改记录（克数/餐次/日期） |
| D4 | DELETE | `/api/diet/logs/{logId}` | 删除记录 |
| D5 | GET | `/api/diet/foods?keyword=&filter=` | 食物搜索（最常/最近/收藏/全部） |
| D6 | GET | `/api/diet/foods/{foodId}` | 食物详情 |
| D7 | POST | `/api/diet/foods` | 上传食物（全局共享） |
| D8 | POST/DELETE | `/api/diet/favorites/{foodId}` | 收藏 / 取消收藏 |
| D9 | GET | `/api/diet/targets` | 查询当前每日目标 |
| D10 | PUT | `/api/diet/targets` | 自定义/清除自定义目标 |
| D11 | GET | `/api/diet/stats?from=&to=` | 摄入统计趋势 |

---

## 3. 接口详情

### 3.1 D1 每日总结 `GET /api/diet/summary?date=`

**参数**：`date`（`yyyy-MM-dd`，缺省=今天）。

**响应 `data`**：

```json
{
  "date": "2026-09-03",
  "target": {
    "basis": "auto",
    "caloriesKcal": 2200,
    "proteinG": 150,
    "carbsG": 250,
    "fatG": 73
  },
  "meals": {
    "breakfast": [],
    "lunch": [
      { "logId": 1, "foodId": 100, "foodName": "鸡胸肉", "grams": 200,
        "caloriesKcal": 330, "proteinG": 62, "carbsG": 0, "fatG": 7 }
    ],
    "dinner": [],
    "snack": []
  },
  "totals": { "caloriesKcal": 330, "proteinG": 62, "carbsG": 0, "fatG": 7 },
  "progress": { "caloriesPct": 15, "proteinPct": 41, "carbsPct": 0, "fatPct": 10 }
}
```

字段说明：

- `target`：当日目标；**资料不足或无自定义目标时可能为 `null`**（见 §3.4）。`basis`：`auto` / `custom` / `null`。
- `meals`：按餐次分组，`items` 为已快照的记录（含克数与营养）。
- `totals`：当日各营养素合计。
- `progress`：`已摄入/目标 × 100`，四舍五入取整；`target` 为 null 时 `progress` 为 null（前端显示补齐提示）。

### 3.2 D2 添加记录 / D3 修改 / D4 删除

**D2 `POST /api/diet/logs`** 请求体：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `date` | string | 是 | `yyyy-MM-dd` |
| `mealType` | string | 是 | 餐次枚举 |
| `foodId` | number | 是 | 食物 ID |
| `grams` | number | 是 | 克数，>0，≤ 5000 |

**成功响应**：`ApiResponse<D2Log>`，`D2Log` 为该条记录（含快照营养，与 D1 中 item 结构一致）。

**D3 `PUT /api/diet/logs/{logId}`**：可更新 `grams`、`mealType`、`date`；后端按最新食物资料重算快照。

**D4 `DELETE /api/diet/logs/{logId}`**：删除本人记录。

错误：`UNAUTHORIZED`(401)、`RESOURCE_NOT_FOUND`(404，非本人或不存在)、`INVALID_ARGUMENT`(400，克数非法/餐次非法/日期格式)。

### 3.3 D5 食物搜索 / D6 详情 / D7 上传 / D8 收藏

**D5 `GET /api/diet/foods?keyword=&filter=`**

- `keyword`：可选，名称模糊匹配。
- `filter`：`all`（默认）/ `recent`（最近使用，按当前用户记录时间）/ `frequent`（最常食用，按记录次数）/ `favorite`（我的收藏）。
- 响应 `data`：

```json
{
  "foods": [
    { "foodId": 100, "name": "鸡胸肉", "category": "meat_egg", "source": "system",
      "sourceLabel": "官方", "ownerNickname": null,
      "caloriesKcal": 165, "proteinG": 31, "carbsG": 0, "fatG": 3.6, "favorited": true },
    { "foodId": 301, "name": "自制鸡胸", "category": "meat_egg", "source": "user",
      "sourceLabel": "用户", "ownerNickname": "张**",
      "caloriesKcal": 160, "proteinG": 30, "carbsG": 1, "fatG": 3, "favorited": false }
  ]
}
```

字段说明：

- `source`：`system`（官方）/ `user`（用户上传）。
- `sourceLabel`：展示用中文标签（`官方` / `用户`）。
- `ownerNickname`：`source=user` 时为上传者**脱敏昵称**（如 `张**`），`system` 为 null。
- `recent` / `frequent` / `favorite` 基于当前用户数据；无记录时回退为空（或全量按名称排序）。

**D6 `GET /api/diet/foods/{foodId}`**：单条食物详情（含每 100g 营养与 `favorited`）。

**D7 `POST /api/diet/foods`**（上传，全局共享）请求体：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | string | 是 | ≤64 |
| `category` | string | 否 | 分类枚举 |
| `caloriesKcal` / `proteinG` / `carbsG` / `fatG` | number | 是 | 每 100g；均 ≥0 且非全 0 |

成功响应：`ApiResponse<D6 结构>`（新食物）。

**D8** `POST /api/diet/favorites/{foodId}`（收藏）、`DELETE /api/diet/favorites/{foodId}`（取消）——幂等。

### 3.4 D9/D10 每日目标

**D9 `GET /api/diet/targets`** 响应 `data`：

```json
{
  "basis": "auto",
  "caloriesKcal": 2200,
  "proteinG": 150,
  "carbsG": 250,
  "fatG": 73,
  "missingFields": []
}
```

- `basis`：`auto` / `custom` / `null`。
- **资料不足（性别/生日/身高/体重/目标/活动量缺一）**：`basis=null`、目标字段为 null、`missingFields` 列出缺失项（`gender/birthDate/heightCm/currentWeightKg/goalType/activityLevel`）。前端据此提示补齐（复用资料完整度）。

**D10 `PUT /api/diet/targets`**：自定义目标（`basis=custom`）请求体：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `caloriesKcal` / `proteinG` / `carbsG` / `fatG` | number | 是 | 均 >0 |
| `clear` | boolean | 否 | `true` 时清除自定义、回到自动 |

### 3.5 D11 摄入统计 `GET /api/diet/stats?from=&to=`

**响应 `data`**：

```json
{
  "dailyCalories": [ { "date": "2026-09-01", "caloriesKcal": 2100 } ],
  "macroShare": { "proteinPct": 25, "carbsPct": 50, "fatPct": 25 },
  "weeklyAverage": [
    { "weekStart": "2026-08-31", "caloriesKcal": 2050, "proteinG": 120, "carbsG": 200, "fatG": 60 }
  ],
  "goalAdherence": { "daysWithinTarget": 18, "daysLogged": 25, "ratePct": 72 }
}
```

- `goalAdherence`：仅在用户**有目标**时返回；无目标为 `null`。`daysWithinTarget`：当日热量在目标 ±10% 内的天数（以当日记录数为准）。
- `from` / `to` 缺省为近 7 天。

---

## 4. 错误码（新增）

| 错误码 | 说明 |
| --- | --- |
| `FOOD_NOT_FOUND` | 食物不存在（404） |
| `FOOD_UPLOAD_INVALID` | 上传食物字段非法/全 0（400） |
| `DIET_LOG_INVALID` | 记录参数非法（400） |
| `DIET_TARGET_INVALID` | 目标值非法（400） |

---

## 5. 前端调用顺序建议

1. 日记页：`GET /diet/summary?date=` 渲染目标/进度/各餐；资料不足时按 `target.missingFields` 提示补齐。
2. 添加：打开食物选择 → `GET /diet/foods?keyword&filter` → 选食物 → 填克数 → `POST /diet/logs` → 刷新 summary。
3. 修改/删除记录 → 刷新 summary。
4. 食物库页：搜索/上传（`POST /diet/foods`）/收藏。
5. 统计页：`GET /diet/stats?from&to`。
6. 目标页（个人资料或日记入口）：`GET /diet/targets`、`PUT /diet/targets`。

---

## 6. 变更说明

- 新增 `diet` 模块全部接口；`user_profiles` 新增 `activity_level` 字段（个人资料更新接口扩展该字段）。
