# DailyForge 饮食模块详细设计文档（DDD）

> 版本：v0.1
> 日期：2026-09-03
> 文档状态：待开发实现设计稿
> 模块归属：`backend` 单体应用
> 目标 Java 包路径：`com.dailyforge.modules.diet`
> 上游契约：`docs/interfaces/diet_接口文档.md`、`docs/prd/diet_PRD.md`

---

## 一、文档说明

### 1.1 上游输入文档

- PRD：[diet_PRD.md](../../prd/diet_PRD.md)
- 接口文档：[diet_接口文档.md](../../interfaces/diet_接口文档.md)
- 数据库设计草案：[diet_数据库设计.md](./diet_数据库设计.md)
- 结构范本：[stats_DDD.md](../stats_module/stats_DDD.md)（只读聚合风格）、[auth_account_DDD.md](../auth_module/auth_account_DDD.md)（分层/代码结构风格）

### 1.2 本文档目标

本文档把 `diet` 模块收口为可直接指导后端实现、代码评审与后续重构的技术方案。重点明确：

- 模块定位与职责边界
- 分层结构（interfaces / application / domain / infrastructure）
- 新增组件：`DietQueryService` / `DietLogService` / `DietFoodService` / `DietTargetService` / `DietStatsService`
- 每日目标算法（Mifflin-St Jeor + activityLevel 系数 + 目标调整 + 宏量拆分 + 资料不足→null+missingFields）
- 食物 / 收藏 / 记录服务的业务逻辑
- 快照语义
- 统计聚合
- 异常与校验
- 验收标准与本轮改动文件清单

### 1.3 当前仓库事实

截至本文档编写时，仓库现状：

1. 统一基础设施已存在：`ApiResponse<T>`、`ErrorCode`、`BusinessException`、`GlobalExceptionHandler`、JWT 鉴权链路、SpringDoc/OpenAPI。
2. `com.dailyforge.modules.diet` 尚无占位包（需要新建）。
3. 依赖数据已具备：
   - `user_profiles`（`gender` / `birth_date` / `height_cm` / `goal_type`），**暂无 `activity_level`**（V11 新增）。
   - `user_current_body_metrics`（`current_weight_kg` 最新体重快照）。
   - `ProfileCompletionDomainService` 已能计算缺失字段（`gender/birthDate/heightCm/goalType/weightRecord`），饮食可复用其完整度思路。
4. 现有迁移最大编号 `V10__user_name_unique.sql` → **下一个迁移是 V11**。
5. 训练模块 `TrainingSession*` 已演示「快照」模式（`exercise_name_snapshot` 等），饮食记录快照可借鉴。

> 说明：本文档所有「新增」「待新增」均为待实现项，不表示仓库中已存在。

---

## 二、方案概述

### 2.1 模块定位

`diet` 是一个饮食日记模块：按用户个人数据计算每日热量/宏量目标，记录三餐与加餐摄入并实时展示进度，提供系统+用户共享的食物库、收藏与摄入统计趋势。

- 记录 / 收藏 / 目标 / 统计只作用于当前用户；食物库为**全局只读共享**（system + 所有 user 上传，本期不做审核）。
- **资料不足不阻断**：每日目标计算依赖资料，资料缺失时只隐藏/提示目标相关展示，记录/食物库/上传/收藏/统计照常可用。

### 2.2 本期交付范围

| 能力 | 说明 | 状态 |
|------|------|:---:|
| D1 每日总结 | `GET /diet/summary`：目标 + 各餐 + 合计 + 进度 | 待开发 |
| D2-D4 记录增删改 | `POST/PUT/DELETE /diet/logs` | 待开发 |
| D5-D7 食物库 | `GET /diet/foods`、`GET /diet/foods/{id}`、`POST /diet/foods` | 待开发 |
| D8 收藏 | `POST/DELETE /diet/favorites/{foodId}` | 待开发 |
| D9-D10 每日目标 | `GET/PUT /diet/targets` | 待开发 |
| D11 摄入统计 | `GET /diet/stats` | 待开发 |
| V11+ 数据 | `user_profiles.activity_level` + 新表 + 种子食物 | 待开发 |

### 2.3 明确不做（本版）

- 饮水记录、食谱/组合餐、饮食 AI 建议、分享。
- 用户上传食物审核。

---

## 三、核心业务规则与数据口径

### 3.1 营养基准

- 食物以 **每 100g 可食部分** 表示：`calories_kcal`、`protein_g`、`carbs_g`、`fat_g`。
- 录入/选择填 **克数**，摄入量 = `营养素 × 克数 / 100`。

### 3.2 膳食分类

`breakfast` / `lunch` / `dinner` / `snack`。

### 3.3 食物来源与展示标签

- `source=system`：种子数据。
- `source=user`：用户上传，`owner_user_id` 记录上传者，**对所有用户可见可搜索可用**。

**食物列表 / 详情需返回来源标签与脱敏昵称**：

- `sourceLabel`：人类可读来源标签——`source=system` → `官方`；`source=user` → `用户`。
- `ownerNickname`：`source=user` 时展示上传者的**脱敏昵称**；`source=system` 时为 `null`。
  - 数据来源：`foods.owner_user_id` 关联 `users.user_name`（`UserMapper.selectByUserName` / 按 id 批量查）。
  - 脱敏规则示例：保留首字符 + `**`，如「张三」→「张**」；单字符用户名 → 首字符 + `**`。昵称缺失时回退为空串或 `null`。
- 后端在 `DietFoodService` 查询食物并回查上传者昵称后，统一由 VO 组装（`source` / `sourceLabel` / `ownerNickname`）。

### 3.4 每日目标算法（Mifflin-St Jeor）

输入：性别、年龄（由 `birthDate` 算）、身高 `heightCm`、当前体重 `currentWeightKg`（`user_current_body_metrics` 最新值）、`goalType`、`activityLevel`（资料新增）。

**BMR**：

- 男：`10×体重kg + 6.25×身高cm − 5×年龄 + 5`
- 女：`10×体重kg + 6.25×身高cm − 5×年龄 − 161`

**TDEE = BMR × activityLevel 系数**：

| activityLevel | 系数 |
|------|------|
| sedentary | 1.2 |
| light | 1.375 |
| moderate | 1.55 |
| high | 1.725 |
| very_high | 1.9 |

> `activityLevel` 是目标计算的**必需项**（主控决策）：个人资料缺 `activity_level` 时，目标 `basis=null` 且 `missingFields` 包含 `activityLevel`（**不做缺省 sedentary 兜底**）。虽然 V11 迁移 `activity_level` 列允许 NULL，但业务上视为必需项之一。

**目标热量**：

- `fat_loss`：TDEE × 0.85
- `muscle_gain`：TDEE × 1.10
- `health_maintenance`：TDEE
- 下限保护：不低于 1200 kcal。

**宏量拆分（g，取整）**：

- 蛋白质 = 系数 × 体重（fat_loss 1.8 / muscle_gain 2.0 / maintenance 1.6）
- 脂肪 = 目标热量 × 25% ÷ 9
- 碳水 = (目标热量 − 蛋白质×4 − 脂肪×9) ÷ 4

**资料不足（性别/生日/身高/体重/目标/活动量缺一）**：

- `basis=null`、目标字段为 null、`missingFields` 列出缺失项（含 `activityLevel`）。
- **不产生无效目标**，不影响其它功能。

### 3.5 快照语义

每次添加/修改记录时，把当时的营养计算结果（每 100g → 克数折算后）**快照**存入 `diet_food_logs`（`food_name_snapshot` + `calories/protein/carbs/fat` 快照 + `quantity_grams`）。此后食物资料被修改不影响历史记录。

### 3.6 业务规则（PRD §6）

- 克数必须 > 0 且 ≤ 5000；一次记录属于一个餐次与一个日期。
- 记录保存营养素快照。
- 上传食物校验：名称非空（≤64），四项营养 ≥ 0 且非全 0；`is_active=true` 默认。
- 「最常食用」= 该用户按记录次数排序；「最近使用」= 按最近记录时间排序（都可跨餐次）。

---

## 四、API 设计

### 4.1 Base Path 与鉴权

- 外部访问前缀：`/api/diet`（Controller 映射 `/diet`）。
- 统一 `ApiResponse<T>` + Bearer Token 鉴权。
- 用户身份：`AuthSecurityUtils.getCurrentUserId()`（或复用 `PlanUserSupportService.requireActiveUserId()`）。

### 4.2 接口总览

| 编号 | 方法 | 路径 | 作用 |
|------|------|------|------|
| D1 | GET | `/diet/summary?date=` | 每日总结（目标+各餐+合计+进度） |
| D2 | POST | `/diet/logs` | 添加记录 |
| D3 | PUT | `/diet/logs/{logId}` | 修改记录 |
| D4 | DELETE | `/diet/logs/{logId}` | 删除记录 |
| D5 | GET | `/diet/foods?keyword=&filter=` | 食物搜索 |
| D6 | GET | `/diet/foods/{foodId}` | 食物详情 |
| D7 | POST | `/diet/foods` | 上传食物（全局共享） |
| D8 | POST/DELETE | `/diet/favorites/{foodId}` | 收藏/取消收藏 |
| D9 | GET | `/diet/targets` | 查询每日目标 |
| D10 | PUT | `/diet/targets` | 自定义/清除自定义目标 |
| D11 | GET | `/diet/stats?from=&to=` | 摄入统计 |

### 4.3 各接口实现逻辑（服务方法见 §六）

- **D1**：`DietQueryService.getDailySummary(userId, date)` → 目标 + 当日记录分组 + 合计 + 进度。
- **D2**：`DietLogService.addLog(userId, req)` → 校验克数/餐次/日期 → 加载食物 → 折算快照 → 入库。
- **D3**：`DietLogService.updateLog(userId, logId, req)` → 校验归属 → 按最新食物资料重算快照 → 更新。
- **D4**：`DietLogService.deleteLog(userId, logId)`。
- **D5**：`DietFoodService.searchFoods(userId, keyword, filter)`。
- **D6**：`DietFoodService.getFoodDetail(userId, foodId)`。
- **D7**：`DietFoodService.uploadFood(userId, req)`。
- **D8**：`DietFoodService.addFavorite(userId, foodId)` / `removeFavorite`。
- **D9**：`DietTargetService.getTarget(userId)`。
- **D10**：`DietTargetService.overrideTarget(userId, req)` / `clearTarget`。
- **D11**：`DietStatsService.getStats(userId, from, to)`。

---

## 五、每日目标算法实现（domain）

### 5.1 目标领域模型

新增 `DietTargetService`（应用层）承载每日目标查询与自定义覆盖；核心纯计算放 `DietTargetDomainService`（可单测）。

```java
// 计算输入
record TargetInput(
    String gender, LocalDate birthDate, BigDecimal heightCm,
    BigDecimal currentWeightKg, String goalType, String activityLevel) {}

// 计算输出
record DietTarget(
    String basis,            // auto / custom / null
    Integer caloriesKcal, Integer proteinG, Integer carbsG, Integer fatG,
    List<String> missingFields) {}
```

### 5.2 算法步骤（纯函数）

1. 校验缺失字段：`gender/birthDate/heightCm/currentWeightKg/goalType/activityLevel` 缺任一 → `missingFields` 列出（含 `activityLevel`）、目标字段 null、`basis=null`。
2. 计算年龄（由 birthDate，不足算整岁）。
3. `BMR`（分性别）。
4. `TDEE = BMR × activityCoefficient(activityLevel)`（`activityLevel` 为必需项，缺失已在步骤 1 拦截；无 sedentary 缺省兜底）。
5. `targetCalories`（按 goalType 乘系数，下限 1200）。
6. 宏量拆分（见 §3.4）。

**数值精度**：`BigDecimal`，最终 kcal / g 取整；宏量拆分配平保证碳水 = 目标 − 蛋白×4 − 脂肪×9 不为负（若为负则碳水置 0 并给 warning，作为边界兜底）。

### 5.3 自定义覆盖

- 自定义目标存 `user_diet_targets`（见数据库设计）：`basis=custom` 时读取覆盖值；`clear=true` 时删除覆盖回自动。
- `getTarget`：有自定义 → 返回 custom 值；无自定义 → 走自动计算（资料不足则 null + missingFields）。

---

## 六、代码结构设计

### 6.1 目标包结构

```text
com.dailyforge.modules.diet
├─ application
│  └─ service
│     ├─ DietQueryService.java        # D1 每日总结编排
│     ├─ DietLogService.java          # D2-D4 记录增删改
│     ├─ DietFoodService.java         # D5-D8 食物库/收藏/上传
│     ├─ DietTargetService.java       # D9-D10 每日目标（读+覆盖）
│     └─ DietStatsService.java        # D11 摄入统计聚合
├─ domain
│  └─ service
│     ├─ DietTargetDomainService.java # 纯算法（Mifflin-St Jeor）
│     ├─ DietLogDomainService.java    # 记录折算/校验/快照
│     └─ DietFoodPolicyService.java   # 上传/收藏/餐次/克数规则
├─ infrastructure
│  └─ persistence
│     ├─ entity
│     │  ├─ FoodEntity.java
│     │  ├─ UserFoodFavoriteEntity.java
│     │  ├─ DietFoodLogEntity.java
│     │  └─ UserDietTargetEntity.java
│     └─ mapper
│        ├─ FoodMapper.java
│        ├─ UserFoodFavoriteMapper.java
│        ├─ DietFoodLogMapper.java
│        └─ UserDietTargetMapper.java
└─ interfaces
   ├─ dto
   │  ├─ CreateDietLogRequest.java
   │  ├─ UpdateDietLogRequest.java
   │  ├─ UploadFoodRequest.java
   │  ├─ OverrideTargetRequest.java
   │  └─ DietQuery.java            # date/keyword/filter/from/to 参数对象
   ├─ rest
   │  └─ DietController.java
   └─ vo
      ├─ DietSummaryResponse.java
      ├─ DietMealGroupResponse.java
      ├─ DietLogItemResponse.java
      ├─ DietTotalsResponse.java
      ├─ DietProgressResponse.java
      ├─ FoodSearchResponse.java
      ├─ FoodItemResponse.java
      ├─ DietTargetResponse.java
      └─ DietStatsResponse.java
```

### 6.2 核心类职责

| 类名 | 职责 |
|------|------|
| `DietController` | 暴露 D1-D11，参数接收、Swagger 注解、`ApiResponse<T>` 返回 |
| `DietQueryService` | D1 编排：目标 + 记录分组 + 合计 + 进度 |
| `DietLogService` | D2-D4：记录增删改，加载食物 + 折算 + 快照入库 |
| `DietFoodService` | D5-D8：搜索/详情/上传/收藏 |
| `DietTargetService` | D9-D10：目标读 + 自定义覆盖读写 |
| `DietStatsService` | D11：每日热量/宏量占比/周均值/目标符合度 |
| `DietTargetDomainService` | 纯算法计算每日目标（可单测） |
| `DietLogDomainService` | 折算、克数校验、快照生成 |
| `DietFoodPolicyService` | 上传/收藏/餐次/克数规则 |

### 6.3 Mapper 与数据访问

- `FoodMapper`：按名称模糊搜索（keyword）、按来源/收藏/最近/最常过滤、`selectByIdAndActive`。
- `DietFoodLogMapper`：按 `userId + recordDate` 查当日记录、按 `userId + date range` 查统计、按 `foodId` 统计最常/最近。
- `UserFoodFavoriteMapper`：按 `userId+foodId` 查/增/删（唯一键）。
- `UserDietTargetMapper`：按 `userId` 查/覆盖。
- 数据访问复用现有 `UserProfileMapper`、`UserCurrentBodyMetricsMapper`（读取资料/体重）；「最常/最近使用」需在 `DietFoodLogMapper` 做 group by 聚合查询。
- **食物来源昵称回查**：`DietFoodService` 对 `source=user` 的食物批量回查上传者昵称（`owner_user_id` → `users.user_name`），脱敏后组装 `ownerNickname`；`system` 食物不查询（`ownerNickname=null`）。

### 6.4 DTO / VO 清单

按接口文档字段逐一对应（见 §4 与数据库设计表）。核心 VO：

- `DietSummaryResponse`：`date / target(DietTargetResponse|null) / meals(Map<mealType,List<DietLogItemResponse>>) / totals / progress(null)`。
- `DietLogItemResponse`：`logId / foodId / foodName / grams / caloriesKcal / proteinG / carbsG / fatG`。
- `DietTargetResponse`：`basis / caloriesKcal / proteinG / carbsG / fatG / missingFields`。
- `FoodItemResponse` / `FoodSearchResponse`（D5/D6 食物项）：`foodId / name / category / source / sourceLabel / ownerNickname / caloriesKcal / proteinG / carbsG / fatG / favorited`。
  - `source`：`system` / `user`。
  - `sourceLabel`：`官方` / `用户`（后端映射）。
  - `ownerNickname`：`source=user` 时为上传者脱敏昵称（首字符 + `**`）；`source=system` 时为 `null`。
- `DietStatsResponse`：`dailyCalories / macroShare / weeklyAverage / goalAdherence|null`。

### 6.5 Swagger / OpenAPI 注解约定

- Controller：`@Tag(name = "Diet")`、`@Operation`、`@ApiResponses`、`@SecurityRequirement(name = "bearerAuth")`（D1-D11 均需登录，D5-D7 食物库虽是共享但接口仍登录）。
- VO/DTO 统一 `@Schema`，对 `mealType/category/activityLevel/source/basis` 给枚举说明。

### 6.6 Debug 日志设计

- 允许：`userId`、`date`、`logId`、`foodId`、`filter`、`from/to`、目标结果规模。
- 禁止：明文个人信息原文全量、原始营养明细全量（仅在 debug 级别、短摘要）。

---

## 七、异常与校验

### 7.1 复用错误码

- `UNAUTHORIZED`（401）、`INVALID_ARGUMENT`（400）、`RESOURCE_NOT_FOUND`（404）、`FORBIDDEN`（403）。

### 7.2 建议新增错误码

| 错误码 | HTTP 状态 | 含义 |
|------|------|------|
| `FOOD_NOT_FOUND` | 404 | 食物不存在 |
| `FOOD_UPLOAD_INVALID` | 400 | 上传食物字段非法/全 0 |
| `DIET_LOG_INVALID` | 400 | 记录参数非法 |
| `DIET_TARGET_INVALID` | 400 | 目标值非法 |

### 7.3 校验位置

- 克数/餐次/日期：`DietLogDomainService` + DTO 注解（克数 >0 ≤5000、mealType 枚举）。
- 上传食物：`DietFoodPolicyService`（名称非空 ≤64、四项 ≥0 且非全 0）。
- 自定义目标：`DietTargetService`（四项 >0；`clear=true` 时忽略数值）。
- 记录归属：`DietLogService`/`DietFoodService` 校验 `userId` 归属，越权 → `RESOURCE_NOT_FOUND`。

---

## 八、快照与一致性

### 8.1 事务边界

| 方法 | 是否事务 | 说明 |
|------|------|------|
| `addLog` | 否/是 | 单表插入（快照已算好） |
| `updateLog` | 否 | 单表更新 |
| `addFavorite` | 否 | 单表插入（幂等：存在则跳过） |
| `uploadFood` | 否 | 单表插入 |
| `overrideTarget` | 否 | upsert user_diet_targets |
| `getDailySummary` / `getStats` | 否 | 只读 |

### 8.2 一致性

- 记录快照在写入时刻确定，之后食物修改不影响历史。
- 收藏/记录归属强校验（当前用户）。
- 无分布式事务需求。

---

## 九、验收标准

1. 目标：按资料自动算出的热量/宏量合理；资料缺失时提示补齐；自定义覆盖生效并可回退自动。
2. 日记：记录早/午/晚/加餐食物与克数，营养自动计算，进度实时更新；可编辑/删除；可回看/补录历史日期。
3. 食物库：搜索与过滤（最常/最近/收藏）正确；上传后所有用户可搜到；收藏可用。
4. 统计：每日热量折线、宏量占比、周均值、目标符合度正确。
5. 资料不足不阻断记录/食物库/统计。
6. `mvn test` 通过；契约联调校验通过。

---

## 十、本轮改动文件清单（预期）

> 以下为按本文档落地实现后预计新增/修改的文件（本轮仅产出文档，不落地代码）。

### 新增（Java）

- `com.dailyforge.modules.diet` 包全部类（§6.1 所列 Service / DomainService / Entity / Mapper / DTO / VO / Controller）。

### 新增（资源）

- `backend/src/main/resources/db/migration/V11__diet_schema.sql`（见数据库设计文档）。
- 种子食物数据（可并入 V11 或单独 V12 seed，实现时定）。

### 修改（backend/**）

- `user_profiles` 对应 `UserProfileEntity.java` + profile 更新 DTO/组装处（新增 `activityLevel`）。
- `com.dailyforge.common.ErrorCode.java`（新增 4 个错误码，见 §7.2）。
- `backend/src/test/resources/schema-auth.sql`（新增 user_profiles.activity_level 与 diet 表，测试 H2 用）。

> 以上均为文档设计预期，非本轮实际改动。
