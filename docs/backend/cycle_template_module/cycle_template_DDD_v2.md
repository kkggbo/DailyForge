# DailyForge Cycle Template 模块详细设计文档（DDD）v2

> 版本：v2.0  
> 日期：2026-07-15  
> 模块归属：`backend` 单体应用  
> Java 包路径：`com.dailyforge.modules.plan`  
> 文档状态：待开发重构设计稿

---

## 一、文档说明

### 1.1 上游输入文档

- PRD：[cycle_template_PRD.md](/D:/Computer%20Science/DailyForge/docs/prd/cycle_template_PRD.md)
- v2 接口文档：[cycle_template_接口文档_v2.md](/D:/Computer%20Science/DailyForge/docs/interfaces/cycle_template_%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3_v2.md)
- 数据库改造清单：[动作参数模型数据库改造清单.md](/D:/Computer%20Science/DailyForge/docs/backend/cycle_template_module/%E5%8A%A8%E4%BD%9C%E5%8F%82%E6%95%B0%E6%A8%A1%E5%9E%8B%E6%95%B0%E6%8D%AE%E5%BA%93%E6%94%B9%E9%80%A0%E6%B8%85%E5%8D%95.md)
- 后端影响分析：[动作参数模型改造后端影响分析.md](/D:/Computer%20Science/DailyForge/docs/backend/cycle_template_module/%E5%8A%A8%E4%BD%9C%E5%8F%82%E6%95%B0%E6%A8%A1%E5%9E%8B%E6%94%B9%E9%80%A0%E5%90%8E%E7%AB%AF%E5%BD%B1%E5%93%8D%E5%88%86%E6%9E%90.md)
- 当前已实现 v1 DDD：[cycle_template_DDD.md](/D:/Computer%20Science/DailyForge/docs/backend/cycle_template_module/cycle_template_DDD.md)

### 1.2 本文档目标

本文档用于定义 `cycle_template` 模块在“动作参数模型重构”后的后端技术实现方案，重点说明：

- v2 三层结构数据模型如何落地
- Java 包结构、类职责、DTO/VO、Entity/Mapper 如何调整
- 11 个接口在 v2 下的实现逻辑
- 参数字典、结构类型、执行项和备注字段的领域规则
- 日志、异常码、Swagger、测试与事务设计

### 1.3 当前状态

当前仓库已有一版 v1 后端实现，核心特点是：

- 模板动作参数使用固定字段
- `cycle_day_exercises` 同时承担动作头和动作参数
- 版本快照结构为：
  - `days -> exercises -> fixed fields`

本次 v2 的目标结构升级为：

- `days -> exercises -> items -> metrics`

这不是局部改字段，而是一次后端中等规模重构。

---

## 二、设计决策总览

本轮已确认的决策如下：

1. 不再支持 `8~10 次` 这种范围型目标。
2. 动作参数模型采用三层结构：
   - 动作
   - 执行项
   - 执行项参数
3. `structureType` 来源于 `exercises.default_structure_type`。
4. `single_segment` 在 MVP 只允许 1 个执行项。
5. `metricKey` 为封闭字典，不支持用户自定义。
6. 当前 MVP 所有 `metricKey` 都视为数值型参数。
7. 前端请求不传 `metricUnit`，由后端根据 `metricKey` 推导；数据库也不存储该字段。
8. `note` 只保留在动作层和执行项层，不作为 metric。
9. `active` 模板更新语义固定为：
   - 只覆盖请求中提交的可编辑天
   - 未提交的未来天默认保留
10. 接受破坏式迁移，迁移前直接清空全部 `cycle_template` 相关业务数据。

---

## 三、数据库模型设计

## 3.1 表结构总览

v2 结构使用以下关系：

```text
cycle_templates
  -> cycle_template_versions
      -> cycle_template_days
          -> cycle_day_exercises
              -> cycle_day_exercise_items
                  -> cycle_day_exercise_item_metrics
```

## 3.2 表职责

### `cycle_templates`

职责：

- 模板主表
- 保存名称、周期长度、目标类型、状态、当前版本引用

说明：

- 结构重构后该表本身无需新增字段
- 仍需保留 `cycle_length` 可空能力

### `cycle_template_versions`

职责：

- 记录模板版本
- 作为三层动作结构快照的版本根

建议 `source_type` 实际使用值：

- `manual`
- `copy`
- `active_patch`
- 预留：`ai_generated`

### `cycle_template_days`

职责：

- 记录版本下的第几天
- 保存 `dayIndex`、`dayName`

### `cycle_day_exercises`

职责：

- 作为模板动作头表

保留字段：

- `id`
- `template_day_id`
- `exercise_id`
- `exercise_name_snapshot`
- `sort_order`
- `note`

新增字段：

- `structure_type`

删除字段：

- `target_sets`
- `target_reps_min`
- `target_reps_max`
- `target_weight_kg`
- `target_duration_seconds`
- `target_rest_seconds`
- `target_rpe`
- `target_extra_json`

### `cycle_day_exercise_items`

职责：

- 表示一个动作下的执行单元

字段：

- `id`
- `cycle_day_exercise_id`
- `item_index`
- `item_type`
- `item_name`
- `note`
- `sort_order`
- `created_at`
- `updated_at`

### `cycle_day_exercise_item_metrics`

职责：

- 表示一个执行项下的结构化参数集合

字段：

- `id`
- `exercise_item_id`
- `metric_key`
- `metric_value_number`
- `sort_order`
- `created_at`
- `updated_at`

说明：

- 不保留 `metric_value_text`
- 不保留 `metric_unit`

### `exercises`

新增字段：

- `default_structure_type`

作用：

- 系统动作搜索 / 详情接口返回默认结构
- 保存模板时校验 `structureType`

## 3.3 破坏式迁移前提

本轮不迁移旧模板结构数据。  
数据库变更前必须先清空：

- `user_active_cycles`
- `training_session_sets`
- `training_session_exercises`
- `training_sessions`
- `cycle_runs`
- `cycle_day_exercises`
- `cycle_template_days`
- `cycle_template_versions`
- `cycle_templates`

---

## 四、Java 包结构设计

## 4.1 目标包结构

```text
com.dailyforge.modules.plan
├─ application
│  ├─ assembler
│  │  └─ CycleTemplateAssembler.java
│  └─ service
│     ├─ CycleTemplateQueryApplicationService.java
│     ├─ CycleTemplateCommandApplicationService.java
│     ├─ CycleTemplateActivationApplicationService.java
│     ├─ CycleTemplateAiApplicationService.java
│     └─ PlanUserSupportService.java
├─ domain
│  ├─ model
│  │  ├─ StructureType.java
│  │  ├─ ItemType.java
│  │  └─ MetricKey.java
│  └─ service
│     ├─ CycleTemplatePolicyService.java
│     ├─ ExerciseStructurePolicyService.java
│     ├─ CycleTemplateVersionDomainService.java
│     └─ CycleActivationDomainService.java
├─ infrastructure
│  └─ persistence
│     ├─ entity
│     └─ mapper
└─ interfaces
   ├─ dto
   ├─ rest
   └─ vo
```

## 4.2 类职责

| 类名 | 职责 |
|------|------|
| `CycleTemplateController` | 对外暴露 11 个接口，负责参数接收、Swagger 注解、统一返回 |
| `CycleTemplateQueryApplicationService` | 查询正式列表、草稿列表、模板详情、当前激活摘要 |
| `CycleTemplateCommandApplicationService` | 创建草稿、更新草稿、更新正式模板、复制模板、删除模板 |
| `CycleTemplateActivationApplicationService` | 启用模板、关闭旧激活上下文、创建新 run、覆盖 active context |
| `CycleTemplateAiApplicationService` | AI 占位接口，直接返回未开发错误 |
| `PlanUserSupportService` | 获取并校验当前登录用户 |
| `CycleTemplatePolicyService` | 模板级规则、编辑范围规则、启用规则、删除规则 |
| `ExerciseStructurePolicyService` | 结构类型、执行项类型、metricKey、metricValueNumber、默认结构一致性校验 |
| `CycleTemplateVersionDomainService` | 保存 / 克隆 / 读取三层版本快照 |
| `CycleActivationDomainService` | 创建新 run、构造 active context |

---

## 五、DTO 设计

## 5.1 DTO 清单

| 类名 | 用途 |
|------|------|
| `CreateDraftCycleTemplateRequest` | C4 创建草稿 |
| `AiGenerateDraftCycleTemplateRequest` | C5 AI 占位接口 |
| `UpdateDraftCycleTemplateRequest` | C6 更新草稿 |
| `UpdateCycleTemplateRequest` | C7 更新正式模板 |
| `CopyCycleTemplateRequest` | C8 复制模板 |
| `ActivateCycleTemplateRequest` | C9 启用模板 |
| `CycleTemplateDayRequest` | 模板日请求体 |
| `CycleTemplateExerciseRequest` | 模板动作请求体 |
| `CycleTemplateItemRequest` | 执行项请求体 |
| `CycleTemplateMetricRequest` | 执行项参数请求体 |

## 5.2 DTO 字段详细设计

### `CycleTemplateMetricRequest`

| 字段名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `sortOrder` | `Integer` | 是 | 参数排序 |
| `metricKey` | `String` | 是 | 只能取封闭字典值 |
| `metricValueNumber` | `BigDecimal` | 是 | 数值参数值 |

Swagger 建议：

- `metricKey` 示例：`weight_kg`
- `metricValueNumber` 示例：`60`

### `CycleTemplateItemRequest`

| 字段名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `itemIndex` | `Integer` | 是 | 同一动作下唯一 |
| `itemType` | `String` | 是 | `set` / `segment` |
| `itemName` | `String` | 否 | 最大 64 字符 |
| `note` | `String` | 否 | 最大 500 字符 |
| `metrics` | `List<CycleTemplateMetricRequest>` | 是 | 至少 1 个 |

### `CycleTemplateExerciseRequest`

| 字段名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `sortOrder` | `Integer` | 是 | 当天动作排序 |
| `exerciseId` | `Long` | 是 | 系统动作 ID |
| `structureType` | `String` | 是 | 必须与动作默认结构一致 |
| `note` | `String` | 否 | 动作总体备注，最大 500 字符 |
| `items` | `List<CycleTemplateItemRequest>` | 是 | 至少 1 个 |

### `CycleTemplateDayRequest`

| 字段名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `dayIndex` | `Integer` | 是 | `1 ~ 7` |
| `dayName` | `String` | 否 | 空白时标准化为 `Day {dayIndex}` |
| `exercises` | `List<CycleTemplateExerciseRequest>` | 否 | 空数组表示休息日 |

### `CreateDraftCycleTemplateRequest`

| 字段名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `templateName` | `String` | 是 | 最大 128 字符 |
| `cycleLength` | `Integer` | 否 | 草稿阶段允许为空 |
| `goalType` | `String` | 否 | 最大 32 字符 |
| `days` | `List<CycleTemplateDayRequest>` | 否 | 不传或空数组都允许 |

说明：

- `UpdateDraftCycleTemplateRequest` 与其同构
- `UpdateCycleTemplateRequest` 与其基本同构，但 `active` 模板更新时要受额外约束

---

## 六、VO 设计

## 6.1 VO 清单

| 类名 | 用途 |
|------|------|
| `FormalCycleTemplateListResponse` | C1 返回体 |
| `FormalCycleTemplateSummary` | 正式模板卡片 |
| `DraftCycleTemplateListResponse` | C2 返回体 |
| `DraftCycleTemplateSummary` | 草稿模板卡片 |
| `CycleTemplateDetailResponse` | C3 返回体 |
| `CycleTemplateDayResponse` | 模板日详情 |
| `CycleTemplateExerciseResponse` | 模板动作详情 |
| `CycleTemplateItemResponse` | 执行项详情 |
| `CycleTemplateMetricResponse` | 参数详情 |
| `CreateDraftCycleTemplateResponse` | C4/C6/C7 返回体 |
| `CopyCycleTemplateResponse` | C8 返回体 |
| `ActivateCycleTemplateResponse` | C9 返回体 |
| `CurrentActiveCycleTemplateResponse` | C10 返回体 |
| `DeleteCycleTemplateResponse` | C11 返回体 |

## 6.2 VO 字段详细设计

### `CycleTemplateMetricResponse`

| 字段名 | 类型 | 说明 |
|------|------|------|
| `sortOrder` | `Integer` | 参数排序 |
| `metricKey` | `String` | 参数键 |
| `metricValueNumber` | `BigDecimal` | 参数值 |
| `metricUnit` | `String` | 后端推导的规范单位 |

### `CycleTemplateItemResponse`

| 字段名 | 类型 | 说明 |
|------|------|------|
| `itemIndex` | `Integer` | 执行项序号 |
| `itemType` | `String` | `set` / `segment` |
| `itemName` | `String` | 显示名称 |
| `note` | `String` | 执行项备注 |
| `metrics` | `List<CycleTemplateMetricResponse>` | 参数列表 |

### `CycleTemplateExerciseResponse`

| 字段名 | 类型 | 说明 |
|------|------|------|
| `sortOrder` | `Integer` | 动作排序 |
| `exerciseId` | `Long` | 动作 ID |
| `exerciseName` | `String` | 动作名称快照 |
| `structureType` | `String` | 结构类型 |
| `note` | `String` | 动作总体备注 |
| `items` | `List<CycleTemplateItemResponse>` | 执行项列表 |

说明：

- 仅响应体返回 `metricUnit`
- 响应体不返回 `defaultStructureType`，那是系统动作接口的职责

---

## 七、Entity / Mapper 设计

## 7.1 Entity 清单

保留：

- `CycleTemplateEntity`
- `CycleTemplateVersionEntity`
- `CycleTemplateDayEntity`
- `CycleDayExerciseEntity`
- `CycleRunEntity`
- `UserActiveCycleEntity`
- `ExerciseReadEntity`

新增：

- `CycleDayExerciseItemEntity`
- `CycleDayExerciseItemMetricEntity`

## 7.2 `CycleDayExerciseEntity`

重构后字段：

- `id`
- `templateDayId`
- `exerciseId`
- `exerciseNameSnapshot`
- `structureType`
- `note`
- `sortOrder`

删除字段映射：

- `targetSets`
- `targetRepsMin`
- `targetRepsMax`
- `targetWeightKg`
- `targetDurationSeconds`
- `targetRestSeconds`
- `targetRpe`
- `targetExtraJson`

## 7.3 `ExerciseReadEntity`

需要扩展字段：

- `exerciseType`
- `movementType`
- `defaultUnit`
- `defaultStructureType`

## 7.4 Mapper 清单

保留：

- `CycleTemplateMapper`
- `CycleTemplateVersionMapper`
- `CycleTemplateDayMapper`
- `CycleDayExerciseMapper`
- `CycleRunMapper`
- `UserActiveCycleMapper`
- `ExerciseReadMapper`

新增：

- `CycleDayExerciseItemMapper`
- `CycleDayExerciseItemMetricMapper`

## 7.5 建议的自定义 Mapper 方法

### `CycleDayExerciseMapper`

- `selectByTemplateDayId(Long templateDayId)`
- `deleteByTemplateDayId(Long templateDayId)`

### `CycleDayExerciseItemMapper`

- `selectByExerciseId(Long cycleDayExerciseId)`
- `deleteByExerciseId(Long cycleDayExerciseId)`

### `CycleDayExerciseItemMetricMapper`

- `selectByItemId(Long exerciseItemId)`
- `deleteByItemId(Long exerciseItemId)`

### `ExerciseReadMapper`

- `selectByIds(List<Long> ids)`
- `selectById(Long id)`

要求读取字段包含：

- `id`
- `owner_user_id`
- `name`
- `is_active`
- `default_structure_type`

---

## 八、领域规则设计

## 8.1 模板级规则

- 草稿允许 `cycleLength = null`
- 正式启用前必须 `cycleLength in 1..7`
- `active` 运行中不允许修改 `cycleLength`

## 8.2 动作级规则

- 只允许系统动作
- 只允许启用动作
- `structureType` 必须与 `exercises.default_structure_type` 一致
- 同一天内 `sortOrder` 唯一

## 8.3 执行项规则

### `set_based`

- `items` 至少 1 个
- 所有 `itemType` 必须为 `set`

### `single_segment`

- `items` 必须且只能有 1 个
- `itemType` 必须为 `segment`

### 共同规则

- 同一动作下 `itemIndex` 唯一

## 8.4 参数规则

- `metricKey` 必须来自封闭字典
- 同一 item 下不允许重复 `metricKey`
- `metricValueNumber` 必填
- 当前不允许文本型参数

## 8.5 `active` 更新规则

- 请求只允许修改 `currentDayIndex` 及之后的天
- 锁定天不可修改
- 只替换请求中提交的可编辑天
- 未提交的未来天默认保留原内容

---

## 九、版本快照设计

## 9.1 v2 快照结构

`CycleTemplateVersionDomainService` 内部快照建议升级为：

```text
VersionSnapshot
  -> DaySnapshot
      -> ExerciseSnapshot
          -> ItemSnapshot
              -> MetricSnapshot
```

## 9.2 建议快照对象

- `VersionSnapshot`
- `DaySnapshot`
- `ExerciseSnapshot`
- `ItemSnapshot`
- `MetricSnapshot`

## 9.3 快照对象字段建议

### `ExerciseSnapshot`

- `sortOrder`
- `exerciseId`
- `exerciseNameSnapshot`
- `structureType`
- `note`
- `items`

### `ItemSnapshot`

- `itemIndex`
- `itemType`
- `itemName`
- `note`
- `metrics`

### `MetricSnapshot`

- `sortOrder`
- `metricKey`
- `metricValueNumber`

## 9.4 版本服务职责

### 保存新版本

- 保存 day
- 保存 exercise
- 保存 item
- 保存 metric

### 克隆版本

- 克隆锁定天
- 克隆未来天
- 克隆 item 和 metric

### 读取版本

- 读取三层嵌套结构
- 查询时补 `metricUnit`

---

## 十、接口实现设计

## 10.1 C1 获取正式模板列表

实现策略：

- 登录态取 `userId`
- 查询 `status IN ('active', 'inactive')`
- 按 `updated_at DESC` 返回
- 结合 `user_active_cycles` 补：
  - `activeTemplateId`
  - `currentDayIndex`

## 10.2 C2 获取草稿模板列表

实现策略：

- 查询 `status='draft'`
- `configuredDayCount = 当前版本已配置的 day 数`

## 10.3 C3 获取模板详情

实现策略：

- 查询模板主记录与当前版本
- 若状态为 `active`，再读取 `user_active_cycles`
- 组装：
  - `currentDayIndex`
  - `editableFromDayIndex`
  - `canActivate`
  - `canDelete`
  - `days[].isLocked`
  - `days[].isRestDay`
  - 三层动作结构
- 返回时按 `metricKey` 推导 `metricUnit`

## 10.4 C4 创建草稿模板

实现步骤：

1. 校验模板头字段
2. 校验 day 列表
3. 校验 exercise 列表
4. 校验 item 列表
5. 校验 metric 列表
6. 插入 `cycle_templates(status='draft')`
7. 创建 `version_no = 1`
8. 保存完整三层结构
9. 回写 `current_version_id`

## 10.5 C5 AI 占位接口

当前保持：

- 登录态校验
- 请求体验证
- 直接抛出 `CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED`

## 10.6 C6 更新草稿模板

实现步骤：

1. 锁定模板
2. 只允许 `draft`
3. 校验完整请求结构
4. 创建新版本
5. 用请求中的完整 `days` 覆盖新版本
6. 更新 `current_version_id`

## 10.7 C7 更新正式模板

### 更新 `inactive`

- 允许完整更新模板头和三层结构
- 创建新版本，更新 `current_version_id`

### 更新 `active`

实现步骤：

1. 读取并锁定 `user_active_cycles`
2. 校验：
   - 不允许修改 `cycleLength`
   - 不允许提交锁定天
3. 创建新版本
4. 复制旧版本所有锁定天
5. 对请求里提交的可编辑天执行替换
6. 未提交的未来天默认保留原内容
7. 更新：
   - `cycle_templates.current_version_id`
   - `user_active_cycles.template_version_id`
   - `cycle_runs.template_version_id`

## 10.8 C8 复制模板

实现策略：

- 来源允许 `draft` / `inactive` / `active`
- 复制结果统一落为 `draft`
- 复制完整三层版本结构

## 10.9 C9 启用模板

实现步骤：

1. 锁定目标模板
2. 只允许 `draft` / `inactive`
3. 校验模板最小启用条件
4. 校验三层结构合法
5. 必要时关闭旧 active 模板和旧 run
6. 创建新 run
7. upsert `user_active_cycles`

## 10.10 C10 获取当前激活模板摘要

实现策略：

- 从 `user_active_cycles` 读取当前模板、版本、run 和 dayIndex
- 关联模板日表读取 `currentDayName`

## 10.11 C11 删除模板

实现策略：

- 只允许删除 `draft` / `inactive`
- 执行软删除：`status='deleted'`

---

## 十一、错误码设计

保留 v1 错误码，并新增：

- `CYCLE_TEMPLATE_STRUCTURE_TYPE_INVALID`
- `CYCLE_TEMPLATE_ITEM_INVALID`
- `CYCLE_TEMPLATE_ITEM_COUNT_INVALID`
- `CYCLE_TEMPLATE_METRIC_KEY_INVALID`
- `CYCLE_TEMPLATE_METRIC_DUPLICATE`
- `CYCLE_TEMPLATE_METRIC_VALUE_INVALID`

语义：

- `STRUCTURE_TYPE_INVALID`
  - 结构类型非法或与动作默认结构不一致
- `ITEM_INVALID`
  - 执行项结构非法
- `ITEM_COUNT_INVALID`
  - `single_segment` 的 item 数量不等于 1
- `METRIC_KEY_INVALID`
  - 参数 key 不在封闭字典中
- `METRIC_DUPLICATE`
  - 同一 item 下重复 metricKey
- `METRIC_VALUE_INVALID`
  - 未传 `metricValueNumber` 或值类型非法

---

## 十二、日志、Swagger 与注释规范

## 12.1 Debug 日志

建议关键链路输出以下 debug 日志：

- 查询正式/草稿列表：`userId`、返回数量
- 创建草稿：`userId`、`templateId`、`versionNo`、`cycleLength`
- 更新草稿：`userId`、`templateId`、`versionNo`
- 更新 `active` 模板：`userId`、`templateId`、旧版本、新版本、提交的 `dayIndex` 列表
- 启用模板：`userId`、旧模板、新模板、旧 run、新 run
- 删除模板：`userId`、`templateId`
- AI 占位接口：`userId`、`useProfileData`、`cycleLength`

禁止输出：

- access token
- 完整 AI prompt 原文

## 12.2 Swagger 注解要求

Controller：

- `@Tag`
- `@Operation`
- `@ApiResponses`
- `@SecurityRequirement(name = "bearerAuth")`

DTO / VO：

- 统一加 `@Schema`
- 对以下字段给出示例和枚举说明：
  - `structureType`
  - `itemType`
  - `metricKey`
  - `cycleLength`

## 12.3 注释要求

- Controller 方法写 Javadoc
- Application Service 公开方法写 Javadoc
- 快照克隆逻辑写短注释
- 结构校验复杂分支写短注释

---

## 十三、事务与并发设计

## 13.1 事务边界

| 方法 | 是否事务 | 说明 |
|------|------|------|
| 获取正式模板列表 | 否 | 只读 |
| 获取草稿模板列表 | 否 | 只读 |
| 获取模板详情 | 否 | 只读 |
| 创建草稿 | 是 | 模板主表 + 版本 + day + exercise + item + metric |
| AI 占位接口 | 否 | 直接返回未开发错误 |
| 更新草稿 | 是 | 新版本完整保存 |
| 更新正式模板 | 是 | 新版本保存及 active 上下文更新 |
| 复制模板 | 是 | 主表复制 + 三层快照复制 |
| 启用模板 | 是 | 状态切换 + run 切换 + active context 覆盖 |
| 删除模板 | 是 | 软删除 |

## 13.2 并发控制

继续复用：

- `selectByIdAndUserIdForUpdate`
- `selectByUserIdForUpdate`

重点保护：

- 同一模板被并发编辑
- 同一用户并发切换激活模板
- `active` 模板更新与切换同时发生

---

## 十四、测试设计

## 14.1 单元测试

建议至少新增：

- `ExerciseStructurePolicyServiceTest`
  - `set_based + set` 合法
  - `single_segment + 2 items` 非法
  - 重复 `metricKey` 非法
  - 缺少 `metricValueNumber` 非法
- `CycleTemplatePolicyServiceTest`
  - `active` 模板不允许改 `cycleLength`
  - 锁定天不允许修改
  - 未提交未来天默认保留不由 policy 层报错

## 14.2 集成测试

建议至少覆盖：

- 创建草稿成功并落三层结构
- `set_based` 保存多 item 成功
- `single_segment` 保存 2 个 item 失败
- 重复 `metricKey` 保存失败
- 获取详情返回三层结构和 `metricUnit`
- 更新草稿创建新版本
- 更新 `active` 模板仅替换已提交未来天
- 未提交未来天保留原内容
- 复制模板会复制 item / metric
- 启用模板会校验三层结构合法
- 未登录访问所有受保护接口返回 401

## 14.3 受影响测试文件

必须同步更新：

- `schema-auth.sql`
- `CycleTemplateIntegrationTest`
- `CycleTemplatePolicyServiceTest`

---

## 十五、依赖接口协作约定

## 15.1 系统动作搜索 / 详情接口

前端在模板编辑器里选择动作时，至少依赖以下字段：

- `exerciseId`
- `exerciseName`
- `defaultStructureType`

本模块不自行推断动作默认结构，应以该接口返回为准。

## 15.2 与训练打卡模块的后续协作

后续 `training_session` 建议复用相同结构语义：

- 动作
- 执行项
- 执行项参数

否则未来计划快照与打卡记录之间会出现映射断层。

---

## 十六、实施顺序建议

建议按以下顺序推进：

1. 完成正式 SQL 迁移脚本
2. 重构 Entity / Mapper
3. 重构 DTO / VO
4. 重构领域校验服务
5. 重构版本快照读写
6. 重构查询与写接口
7. 回归测试

---

## 十七、结论

`cycle_template` v2 的本质不是“把固定字段换个壳”，而是把模板动作模型正式升级为结构化三层快照模型。

本轮最重要的技术原则是：

- 结构类型显式化
- metricKey 封闭化
- metricUnit 后端推导化
- `active` 模板未来天局部覆盖化
- 旧数据破坏式清理

只要后续代码实现严格沿这份 DDD 收口，`cycle_template` 就能为未来 `training_session`、统计和 AI 功能打下更稳的模型基础。
