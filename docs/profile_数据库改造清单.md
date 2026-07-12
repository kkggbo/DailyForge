# DailyForge Profile 数据库改造清单
> 版本：v1.1  
> 日期：2026-07-12  
> 用途：供后端 / 数据库开发直接执行

---

## 1. 目标

本清单只收口 `profile` 模块当前已经确认的数据库改造点。

本次改造目标：

- 支撑基础档案存储
- 支撑身体指标历史记录
- 支撑个人页面“当前身体状态”快速回显
- 支撑 AI 场景快速读取当前可用体征摘要
- 支撑身体指标记录逻辑删除

---

## 2. 必做改造项

### 2.1 `user_profiles` 保持全字段可空

`user_profiles` 中与基础档案相关的字段都应允许为空：

- `gender`
- `birth_date`
- `height_cm`
- `goal_type`
- `training_level`
- `injury_notes`

说明：

- 这些字段不作为保存门槛
- 完整度判断交给应用层 `completion-summary`

### 2.2 `user_profiles` 补齐/确认 `goal_type`

需要确认 `user_profiles` 中存在：

- `goal_type`

建议枚举值：

- `fat_loss`
- `muscle_gain`
- `health_maintenance`

### 2.3 `user_profiles` 不存当前体重

确认 `user_profiles` 不单独维护当前体重字段。

原因：

- 当前体重不应与身体指标历史表双写
- 当前体重统一从 `user_current_body_metrics` 读取

### 2.4 `user_profiles.injury_notes` 扩容

建议：

- `injury_notes VARCHAR(500)` 调整为 `VARCHAR(1000)`

原因：

- 与接口文档对齐
- 伤病备注通常比普通短备注更容易变长

### 2.5 `body_metric_logs` 保持“按次追加”的历史真相语义

`body_metric_logs` 继续作为身体指标历史表使用，并遵守以下规则：

- 每次新增一条记录
- 不直接编辑历史记录
- 仅允许删除最近一条记录
- 每条记录只保存本次真实填写的字段
- 未填写字段保持 `NULL`
- 不自动复制上一条记录的旧值

### 2.6 `body_metric_logs` 引入逻辑删除

新增字段：

- `is_del TINYINT(1) NOT NULL DEFAULT 0`
- `deleted_at DATETIME(3) NULL`

说明：

- `is_del=0` 表示有效记录
- `is_del=1` 表示逻辑删除记录
- 删除最近一条记录时，不做物理删除，改为逻辑删除

### 2.7 `body_metric_logs` 放开体重非空要求

当前约定改为：

- `record_date` 必填
- `weight_kg` 可空
- 其他身体指标字段也可空

但应用层要保证：

- 一条记录至少有一个有效身体指标字段非空

说明：

- 这个约束不建议用单列 `NOT NULL` 表达
- 更适合交给应用层校验

### 2.8 `body_metric_logs.note` 扩容

建议：

- `note VARCHAR(500)` 调整为 `VARCHAR(1000)`

### 2.9 新增 `user_current_body_metrics`

新增用户当前身体状态快照表：

- `user_current_body_metrics`

建议字段：

- `user_id`
- `current_weight_kg`
- `current_body_fat_percent`
- `current_bmi`
- `current_skeletal_muscle_percent`
- `current_body_water_percent`
- `current_basal_metabolic_rate_kcal`
- `current_waist_cm`
- `current_hip_cm`
- `current_waist_hip_ratio`
- `current_body_age`
- `current_body_type`
- `updated_at`

约束建议：

- `user_id` 作为主键或唯一键
- 与 `users.id` 建立一对一关系

说明：

- 该表是读模型，不是历史真相表
- 不同字段允许来自不同日期的最近一次非空记录

---

## 3. 快照同步规则

### 3.1 新增身体指标记录时

当用户新增一条 `body_metric_logs` 记录后：

- 对本次填写的字段，覆盖 `user_current_body_metrics` 对应字段
- 对本次未填写字段，不覆盖旧快照值
- 更新 `updated_at`

### 3.2 逻辑删除最近一条记录时

当用户删除最近一条记录后：

- 不只更新 `is_del`
- 还需要基于剩余 `is_del=0` 的历史记录重算 `user_current_body_metrics`

### 3.3 读取时

以下场景优先读取 `user_current_body_metrics`：

- 个人信息页当前状态摘要
- AI 训练计划生成
- AI 饮食建议
- AI 总结 / 调整建议

以下场景读取 `body_metric_logs`：

- 历史列表
- 后续趋势统计
- 历史回溯分析

---

## 4. 删除规则

“最近一条记录”定义为：

- 按 `record_date DESC, id DESC` 排序后的第一条

删除限制：

1. 只能删除这一条
2. 如果这条记录已经 `is_del=1`，不允许再次删除
3. 不允许跳过它去删除更旧记录

这个规则需要数据库查询与应用层逻辑一起配合实现。

---

## 5. 索引建议

建议至少补充以下索引：

- `body_metric_logs(user_id, is_del, record_date, id)`
- `user_current_body_metrics(user_id)`

说明：

- `body_metric_logs` 索引用于按用户查询历史、定位最近一条未删除记录
- `user_current_body_metrics` 索引用于按用户快速读取当前状态

---

## 6. 与接口设计的对应关系

本次数据库改造将直接支撑以下接口：

- `GET /api/profile/basic`
- `PUT /api/profile/basic`
- `GET /api/profile/body-metrics/current`
- `GET /api/profile/body-metrics`
- `POST /api/profile/body-metrics`
- `DELETE /api/profile/body-metrics/latest`
- `GET /api/profile/completion-summary`

其中：

- 历史列表接口读 `body_metric_logs`，过滤 `is_del=0`
- 当前状态与摘要接口读 `user_current_body_metrics`
- 删除接口更新 `is_del/deleted_at`，并重算快照

---

## 7. 最终结论

本次 `profile` 模块数据库改造采用“三层模型 + 逻辑删除”：

1. `user_profiles`
说明：保存基础档案，字段可空

2. `body_metric_logs`
说明：保存历史真相，支持逻辑删除

3. `user_current_body_metrics`
说明：保存当前已知状态快照

这样做的收益：

- 历史数据真实可追溯
- 当前状态读取快
- 逻辑删除比物理删除更利于审计和纠错
- 不需要每次为 AI 或页面回显扫描全量历史
