# DailyForge 饮食模块 数据库设计文档

> 版本：v0.1
> 日期：2026-09-03
> 文档状态：DDL 草案（待评审）
> 关联 DDD：`diet_DDD.md`
> 说明：本文档为**表结构与迁移草案**，仅设计，不落代码。

---

## 一、迁移版本规划

现有迁移最大编号 `V10__user_name_unique.sql`，故饮食模块从 **V11** 开始。

| 迁移文件 | 内容 | 说明 |
|------|------|------|
| `V11__diet_schema.sql` | `user_profiles` 增 `activity_level` + 新增 4 张表 + 索引 | 建表与字段 |
| `V12__diet_seed_foods.sql` | 系统基础食物种子数据 | 推荐单独文件，便于 seed 与 schema 分离 |

执行顺序：按版本号升序，先 V11 后 V12。当前项目 Flyway 运行时默认关闭，需手动按序执行（沿用 `docs/agent协作规范.md` 与既有 V7/V8/V9/V10 处理方式）。

---

## 二、`user_profiles` 增字段（V11）

```sql
ALTER TABLE user_profiles
    ADD COLUMN activity_level VARCHAR(32) NULL
        COMMENT '日常活动量(sedentary/light/moderate/high/very_high)'
        AFTER goal_type;
```

- `NULL`：列允许 NULL；但业务上 `activity_level` 是目标计算的**必需项**之一（主控决策）——缺失时目标 `basis=null` 且 `missingFields` 含 `activityLevel`，**不做缺省 sedentary 兜底**（见 DDD §3.4）。
- 枚举值：`sedentary/light/moderate/high/very_high`。

> 需要同步：`UserProfileEntity` 增 `activityLevel`；`UpdateProfileBasicRequest` 增 `activityLevel`（`@Pattern` 校验枚举）；profile 更新组装处回填；profile 完整度/缺失字段清单需将 `activityLevel` 纳入（饮食目标判断用）。

---

## 三、新表 DDL 草案

### 3.1 `foods`（食物库，系统 + 用户上传共享）

```sql
CREATE TABLE foods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL COMMENT '食物名称',
    category VARCHAR(32) NULL COMMENT '分类(staple/meat_egg/vegetable/fruit/dairy/nut_bean/drink/other)',
    calories_kcal DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每100g热量(kcal)',
    protein_g DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每100g蛋白质(g)',
    carbs_g DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每100g碳水(g)',
    fat_g DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '每100g脂肪(g)',
    source VARCHAR(16) NOT NULL DEFAULT 'system' COMMENT 'system/user',
    owner_user_id BIGINT NULL COMMENT '用户上传时的归属用户(system为NULL)',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可用(1=是 0=否)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_foods_name (name),
    KEY idx_foods_source_active (source, is_active)
) COMMENT='食物库：每100g可食部分营养';
```

**约束/说明**：

- `name` 允许重名（不同用户可上传同名），无唯一约束。
- `owner_user_id` 仅 `source=user` 使用；`system` 行为 NULL。不做审核。
- 四项营养 ≥ 0 且非全 0 由服务层校验（`FOOD_UPLOAD_INVALID`）。
- **来源昵称**：`foods` 表**不冗余存昵称**，`ownerNickname` 由服务层在查询时按 `owner_user_id` 回查 `users.user_name` 并脱敏（保留首字符 + `**`，如「张**」）后组装；`system` 食物 `ownerNickname=null`。`sourceLabel` 由 `source` 映射（`官方` / `用户`）。

### 3.2 `user_food_favorites`（用户收藏）

```sql
CREATE TABLE user_food_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_food (user_id, food_id),
    KEY idx_user_food_favorites_user (user_id)
) COMMENT='用户食物收藏';
```

**唯一约束**：`user_id + food_id` 防重复收藏（幂等）。

### 3.3 `diet_food_logs`（饮食记录，含营养快照）

```sql
CREATE TABLE diet_food_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL COMMENT '来源食物id',
    food_name_snapshot VARCHAR(64) NOT NULL COMMENT '食物名快照',
    meal_type VARCHAR(16) NOT NULL COMMENT 'breakfast/lunch/dinner/snack',
    record_date DATE NOT NULL COMMENT '记录日期',
    quantity_grams DECIMAL(10,2) NOT NULL COMMENT '克数',
    calories_kcal DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '热量快照(kcal)',
    protein_g DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '蛋白质快照(g)',
    carbs_g DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '碳水快照(g)',
    fat_g DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '脂肪快照(g)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_diet_logs_user_date (user_id, record_date),
    KEY idx_diet_logs_user_food (user_id, food_id),
    KEY idx_diet_logs_user_meal_date (user_id, meal_type, record_date)
) COMMENT='饮食记录：营养素按添加时快照';
```

**说明**：

- 快照字段：`food_name_snapshot` + 四项营养，记录写入时按 `每100g × quantity_grams / 100` 折算后存入，食物资料后续修改不影响历史。
- 克数 >0 ≤5000 由服务层校验。
- 索引支撑：按用户+日期查当日总结、按用户+foodId 统计最常/最近使用、按用户+餐次+日期分组。
- `food_id` 不设外键（食物可能被停用但历史记录保留；`food_name_snapshot` 保证可读）。

### 3.4 自定义目标存储方案

**结论：采用独立表 `user_diet_targets`**（PRD §7「实现时定」，决策为覆盖表，而非 profile 字段）。理由：

- 自定义目标与个人基本资料语义不同，放 profile 会污染资料表且无时间维度。
- 覆盖表天然支持「清除回退自动」（删除行即回 auto）。
- 后续如需历史/多目标扩展更灵活。

```sql
CREATE TABLE user_diet_targets (
    user_id BIGINT PRIMARY KEY COMMENT '用户id(1对1)',
    calories_kcal DECIMAL(10,2) NOT NULL,
    protein_g DECIMAL(10,2) NOT NULL,
    carbs_g DECIMAL(10,2) NOT NULL,
    fat_g DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='用户自定义每日目标覆盖';
```

- 一行 = 一个用户的自定义覆盖；存在即有 custom 目标，删除 = 回自动。
- 数值必须 >0（服务层校验 `DIET_TARGET_INVALID`）。

---

## 四、索引设计汇总

| 表 | 索引 | 目的 |
|------|------|------|
| `foods` | `idx_foods_name(name)` | 关键字名称模糊搜索 |
| `foods` | `idx_foods_source_active(source,is_active)` | 过滤 system/全部、仅 active |
| `user_food_favorites` | `uk_user_food(user_id,food_id)` | 幂等收藏 + 查收藏 |
| `user_food_favorites` | `idx_user_food_favorites_user(user_id)` | 按用户取收藏列表 |
| `diet_food_logs` | `idx_diet_logs_user_date(user_id,record_date)` | 按用户+日期查当日总结/统计 |
| `diet_food_logs` | `idx_diet_logs_user_food(user_id,food_id)` | 统计某食物最常/最近使用 |
| `diet_food_logs` | `idx_diet_logs_user_meal_date(user_id,meal_type,record_date)` | 按餐次分组读取 |
| `user_diet_targets` | 主键 `user_id` | 单用户 upsert |

**「最常食用 / 最近使用」实现**：在 `diet_food_logs` 上按 `food_id` 聚合：

```sql
-- 最常（记录次数降序）
SELECT food_id, COUNT(*) AS cnt FROM diet_food_logs
WHERE user_id = ? GROUP BY food_id ORDER BY cnt DESC LIMIT n;

-- 最近（最近记录时间）
SELECT food_id, MAX(record_date) AS latest FROM diet_food_logs
WHERE user_id = ? GROUP BY food_id ORDER BY latest DESC LIMIT n;
```

返回 food_id 集合后回查 `foods` 取营养，并补 `favorited`。

---

## 五、种子食物数据方案

### 5.1 数量建议

- **起步建议 30~50 条**常见食物（主食/肉蛋水产/蔬菜/水果/奶制品/坚果饮品），覆盖常用场景。
- 每条填：name、category、每 100g 四营养、`source=system`、`is_active=1`。

### 5.2 示例种子（INSERT 草案示意）

```sql
INSERT INTO foods (name, category, calories_kcal, protein_g, carbs_g, fat_g, source) VALUES
('米饭',   'staple',     116, 2.6, 25.9, 0.3, 'system'),
('鸡胸肉', 'meat_egg',   165, 31.0, 0.0,  3.6, 'system'),
('西兰花', 'vegetable',   34, 2.8, 6.6,  0.4, 'system'),
('苹果',   'fruit',       52, 0.3, 13.8, 0.2, 'system'),
('牛奶',   'dairy',       61, 3.2, 4.9,  3.2, 'system'),
-- ... 共约 30-50 条
;
```

- 数据清单需实现前补充并校对（PRD §12 依赖）。
- 放 `V12__diet_seed_foods.sql`，与 schema 分离便于更新。

---

## 六、测试 H2 schema 同步

`backend/src/test/resources/schema-auth.sql` 需在实现时同步：

- `user_profiles` 增 `activity_level VARCHAR(32) NULL`。
- 新增 `foods` / `user_food_favorites` / `diet_food_logs` / `user_diet_targets` 的 CREATE TABLE（与上方 DDL 对齐，字段用 H2 兼容类型 `TINYINT/DATE/DECIMAL/TIMESTAMP`）。

---

## 七、风险 / 待确认

1. **食物 `name` 无唯一约束**：允许不同来源重名；需确认前端「同名去重」策略（实现时定）。
2. **`foods.food_id` 在 log 中不做外键**：保留历史快照可读；停用食物不影响历史。
3. **种子清单数量**：PRD 建议几十条即可起步，实现前需提供具体营养清单。
4. **自定义目标存 `user_diet_targets`**（覆盖表）——已给出结论；若主控更倾向 profile 字段需另行调整（本文档默认覆盖表）。
5. `activity_level` 是目标计算的**必需项**（主控决策，已同步至 DDD §3.4）：缺失时目标 `basis=null` 且 `missingFields` 含 `activityLevel`，无 sedentary 兜底；`user_profiles.activity_level` 列虽为 NULL，业务上按必需项处理。
