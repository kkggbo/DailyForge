# DailyForge Exercise 模块详细设计文档（DDD）

> 版本：v1.0 | 日期：2026-07-15 | 模块归属：`backend` 单体应用 `com.dailyforge.modules.exercise`

---

## 一、文档说明

### 1.1 上游输入文档

本设计基于以下输入整理：

- 接口文档：[exercise_接口文档.md](/D:/Computer%20Science/DailyForge/docs/interfaces/exercise_%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3.md)
- 现有数据库结构：
  - [V1__init_schema.sql](/D:/Computer%20Science/DailyForge/backend/src/main/resources/db/migration/V1__init_schema.sql)
  - [V2__seed_base_data.sql](/D:/Computer%20Science/DailyForge/backend/src/main/resources/db/migration/V2__seed_base_data.sql)
  - [V5__cycle_template_structure_v2.sql](/D:/Computer%20Science/DailyForge/backend/src/main/resources/db/migration/V5__cycle_template_structure_v2.sql)
- 现有代码中与动作读取相关的最小实现：
  - `com.dailyforge.modules.plan.infrastructure.persistence.entity.ExerciseReadEntity`
  - `com.dailyforge.modules.plan.infrastructure.persistence.mapper.ExerciseReadMapper`

### 1.2 当前目标

本文档用于把 `exercise` 模块收口成一套可实施的后端查询设计，重点明确：

- 模块边界与与 `plan` 的职责拆分
- 数据来源与查询策略
- 列表 / 详情两个接口的真实实现形态
- DTO / VO / Entity / Mapper 设计
- 错误码、日志、Swagger、测试范围

### 1.3 当前实现状态

截至本文档编写时：

- 项目中尚未存在独立的 `com.dailyforge.modules.exercise` 模块
- `plan` 模块内存在一个仅用于模板保存校验的最小动作读取能力：
  - `ExerciseReadEntity`
  - `ExerciseReadMapper`
- 当前动作相关数据库主表和关系表已经具备：
  - `exercises`
  - `exercise_muscles`
  - `exercise_equipments`
  - `muscles`
  - `equipments`
- `V5__cycle_template_structure_v2.sql` 已经给 `exercises` 增加 `default_structure_type`

因此，本模块是“从已有表结构上新增独立查询能力”，不是从零设计数据模型。

---

## 二、模块概述

### 2.1 模块定位

`exercise` 模块负责提供“系统动作查询能力”，作为以下模块的统一动作数据源：

- `cycle_template`：模板编辑页的动作搜索、选择、回显
- 后续 `training_session`：训练打卡页的动作详情展示
- 后续 AI 计划生成：动作引用与动作元数据过滤

本模块当前只做读能力，不做写能力。

### 2.2 本期交付范围

| 序号 | 功能 | 说明 | 状态 |
|------|------|------|:---:|
| 1 | 系统动作列表查询 | 支持关键字、类型、结构、场景、肌肉、分页过滤 | 待开发 |
| 2 | 系统动作详情查询 | 返回动作基础信息、热量参考、肌肉和器械详情 | 待开发 |

### 2.3 明确不做

- 用户自定义动作 CRUD
- 系统动作后台管理
- 收藏、评论、课程关联
- 可穿戴设备动作同步
- 动作排序推荐、热度推荐、搜索纠错

### 2.4 与现有 `plan` 模块的关系

当前 `plan` 模块内部已经自带一个最小动作读取 Mapper，仅用于：

- 校验 `exerciseId` 是否存在
- 校验动作是否为系统动作
- 读取 `defaultStructureType`

本 DDD 的设计决策是：

1. 正式开发 `exercise` 模块时，新模块成为动作读取能力的唯一归属模块
2. 现有 `plan` 模块中的 `ExerciseReadEntity / Mapper` 视为临时技术债
3. 后续实施 `exercise` 模块时，建议把这两个类迁移或收拢到 `exercise.infrastructure.persistence`
4. `plan` 后续改为依赖 `exercise` 模块的内部查询支持服务，而不再直接占有动作读取 Mapper

---

## 三、关键设计决策

### 3.1 路由决策

当前项目真实路由基线为：

- 全局 `server.servlet.context-path: /api`

因此本模块采用：

- Controller 基础路径：`/exercises`
- 外部访问路径：
  - `GET /api/exercises/system`
  - `GET /api/exercises/system/{exerciseId}`

### 3.2 鉴权决策

当前接口文档要求登录态访问，且这与现有项目安全配置一致。

因此本模块：

- 默认要求 JWT 登录态
- 不新增匿名放行路径
- `SecurityConfig` 无需修改

### 3.3 数据来源决策

本模块只返回系统动作，统一以以下条件约束：

- `owner_user_id IS NULL`
- `is_active = 1`

也就是说：

- 不返回用户自定义动作
- 不返回已禁用动作

### 3.4 `defaultStructureType` 决策

`defaultStructureType` 是本模块对 `cycle_template` 最重要的输出字段之一。

设计要求：

- 来自 `exercises.default_structure_type`
- MVP 仅支持：
  - `set_based`
  - `single_segment`
- 前端选动作后，必须以它初始化模板动作结构
- 前端不能自行猜测动作属于哪种结构

### 3.5 列表查询策略决策

动作列表接口涉及：

- 多个过滤条件
- 多表关联
- 分页
- 返回轻量化聚合字段

为了避免 join 后分页失真、重复行和结果集爆炸，列表查询采用“两段式查询”：

1. 第一段：
   - 只查符合过滤条件的动作主键分页结果
   - 产出 `exerciseId` 列表和 `total`
2. 第二段：
   - 按 `exerciseId IN (...)` 批量查询动作基础信息
   - 批量查询肌肉关系
   - 批量查询器械关系
   - 在应用层聚合成 VO

这是本模块的固定实现策略，不采用“一条超大 join SQL 直接分页”的方案。

### 3.6 详情查询策略决策

详情查询只查单个动作，因此不需要分页优化，采用：

- 主表单行查询
- 肌肉关系表批量读取
- 器械关系表批量读取
- 应用层组装成完整详情返回

### 3.7 关系字段暴露决策

接口文档中列表和详情对肌肉、器械的返回粒度不同，因此固定如下：

- 列表接口：
  - `primaryMuscles`：`string[]`
  - `secondaryMuscles`：`string[]`
  - `equipmentNames`：`string[]`
- 详情接口：
  - `primaryMuscles`：对象数组
  - `secondaryMuscles`：对象数组
  - `equipments`：对象数组

当前版本不对外返回：

- `exercise_equipments.requirement_type`

即使数据库里存在 `requirement_type`，本轮接口也不暴露，避免过早扩展契约。

### 3.8 搜索与过滤决策

#### `keyword`

- 仅对 `exercises.name` 做模糊匹配
- MVP 不做拼音、别名、全文索引、错别字纠正

#### `exerciseType`

- 精确匹配 `exercises.exercise_type`

#### `movementType`

- 精确匹配 `exercises.movement_type`

#### `structureType`

- 精确匹配 `exercises.default_structure_type`

#### `sceneType`

- 通过 `exercise_equipments -> equipments.scene_type` 过滤
- 语义为“该动作关联至少一个该场景器械”

#### `muscleId`

- 通过 `exercise_muscles.muscle_id` 过滤
- 同时匹配 `primary` 与 `secondary`

### 3.9 排序决策

列表接口默认排序固定为：

1. `exercises.name ASC`
2. `exercises.id ASC`

本轮不增加可配置排序字段。

### 3.10 错误码决策

本模块需要新增一个领域错误码：

- `EXERCISE_NOT_FOUND`

语义：

- 目标动作不存在
- 或不是系统动作
- 或已禁用

其他参数类错误继续复用：

- `INVALID_ARGUMENT`
- `UNAUTHORIZED`

---

## 四、包结构设计

### 4.1 目标包结构

```text
com.dailyforge.modules.exercise
├─ application
│  ├─ assembler
│  │  └─ ExerciseAssembler.java
│  └─ service
│     └─ ExerciseQueryApplicationService.java
├─ domain
│  └─ service
│     └─ ExerciseQueryPolicyService.java
├─ infrastructure
│  └─ persistence
│     ├─ entity
│     │  ├─ ExerciseEntity.java
│     │  ├─ ExerciseMuscleRelationEntity.java
│     │  ├─ ExerciseEquipmentRelationEntity.java
│     │  ├─ MuscleEntity.java
│     │  └─ EquipmentEntity.java
│     └─ mapper
│        ├─ ExerciseQueryMapper.java
│        ├─ ExerciseMuscleQueryMapper.java
│        └─ ExerciseEquipmentQueryMapper.java
└─ interfaces
   ├─ dto
   │  └─ ExerciseSystemListQuery.java
   ├─ rest
   │  └─ ExerciseController.java
   └─ vo
      ├─ ExerciseSystemListItemResponse.java
      ├─ ExerciseSystemListResponse.java
      ├─ ExerciseSystemDetailResponse.java
      ├─ ExerciseMuscleResponse.java
      └─ ExerciseEquipmentResponse.java
```

### 4.2 类职责

| 类名 | 职责 |
|------|------|
| `ExerciseController` | 对外暴露 2 个查询接口，负责参数接收、Swagger 注解、统一返回 |
| `ExerciseQueryApplicationService` | 承接系统动作列表与详情查询、分页与聚合 |
| `ExerciseQueryPolicyService` | 查询参数默认值、分页边界、过滤值合法性校验 |
| `ExerciseAssembler` | Entity / 聚合结果转 VO |
| `ExerciseQueryMapper` | 动作主表分页 ID 查询、基础信息批量查询、详情单行查询 |
| `ExerciseMuscleQueryMapper` | 动作与肌肉关系批量读取 |
| `ExerciseEquipmentQueryMapper` | 动作与器械关系批量读取 |

### 4.3 与 `plan` 的收口建议

后续实现时建议：

1. 把 `plan` 下的最小 `ExerciseReadEntity / Mapper` 删除或迁移
2. `plan` 改为复用：
   - `exercise` 模块的内部查询实体
   - 或新建一个包内可见的 `ExerciseLookupSupportService`

本 DDD 推荐目标是“动作读取逻辑只在一个模块维护”。

---

## 五、数据设计

### 5.1 涉及表

| 表名 | 用途 |
|------|------|
| `exercises` | 动作主表 |
| `exercise_muscles` | 动作与肌肉关系 |
| `muscles` | 肌肉基础信息 |
| `exercise_equipments` | 动作与器械关系 |
| `equipments` | 器械基础信息 |

### 5.2 关键字段说明

#### `exercises`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `BIGINT` | 动作主键 |
| `owner_user_id` | `BIGINT \| NULL` | `NULL` 表示系统动作 |
| `name` | `VARCHAR(128)` | 动作名称 |
| `exercise_type` | `VARCHAR(32)` | 动作类型 |
| `movement_type` | `VARCHAR(32) \| NULL` | 动作模式 |
| `video_url` | `VARCHAR(500) \| NULL` | 示范视频 |
| `default_unit` | `VARCHAR(32)` | 默认单位 |
| `default_structure_type` | `VARCHAR(32)` | 默认结构类型，依赖 `V5` |
| `calorie_burn_reference` | `DECIMAL(8,2) \| NULL` | 热量消耗参考值 |
| `calorie_reference_unit` | `VARCHAR(32) \| NULL` | 热量参考单位 |
| `is_active` | `TINYINT(1)` | 启用状态 |

#### `exercise_muscles`

| 字段 | 类型 | 说明 |
|------|------|------|
| `exercise_id` | `BIGINT` | 动作 ID |
| `muscle_id` | `BIGINT` | 肌肉 ID |
| `relation_type` | `VARCHAR(32)` | `primary` / `secondary` |
| `sort_order` | `INT` | 排序值 |

#### `muscles`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `BIGINT` | 肌肉主键 |
| `name` | `VARCHAR(64)` | 肌肉名称 |
| `code` | `VARCHAR(64)` | 肌肉编码 |
| `parent_id` | `BIGINT \| NULL` | 上级肌肉 |
| `muscle_level` | `VARCHAR(32)` | 层级 |
| `is_active` | `TINYINT(1)` | 是否启用 |

#### `exercise_equipments`

| 字段 | 类型 | 说明 |
|------|------|------|
| `exercise_id` | `BIGINT` | 动作 ID |
| `equipment_id` | `BIGINT` | 器械 ID |
| `requirement_type` | `VARCHAR(32)` | `required/optional/alternative`，本轮不对外暴露 |
| `sort_order` | `INT` | 排序值 |

#### `equipments`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `BIGINT` | 器械主键 |
| `name` | `VARCHAR(64)` | 器械名称 |
| `scene_type` | `VARCHAR(32)` | 场景类型，如 `home/gym/both` |
| `is_active` | `TINYINT(1)` | 是否启用 |

### 5.3 实施前置条件

本模块依赖数据库已具备：

- `exercises.default_structure_type`

因此真实环境在开发或联调前必须已经执行：

- [V5__cycle_template_structure_v2.sql](/D:/Computer%20Science/DailyForge/backend/src/main/resources/db/migration/V5__cycle_template_structure_v2.sql)

否则：

- `defaultStructureType` 无法返回
- `cycle_template` 与 `exercise` 的契约会失效

---

## 六、接口设计

### 6.1 DTO 清单

| 类名 | 用途 |
|------|------|
| `ExerciseSystemListQuery` | E1 列表查询参数承载体 |

#### `ExerciseSystemListQuery`

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `keyword` | `String` | 否 | 动作名称关键字，模糊匹配 |
| `exerciseType` | `String` | 否 | 动作类型过滤 |
| `movementType` | `String` | 否 | 动作模式过滤 |
| `structureType` | `String` | 否 | `set_based` / `single_segment` |
| `sceneType` | `String` | 否 | 器械场景过滤 |
| `muscleId` | `Long` | 否 | 肌肉过滤 |
| `page` | `Integer` | 否 | 页码，默认 `1` |
| `pageSize` | `Integer` | 否 | 每页大小，默认 `20`，最大 `100` |

### 6.2 VO 清单

| 类名 | 用途 |
|------|------|
| `ExerciseSystemListResponse` | E1 返回体 |
| `ExerciseSystemListItemResponse` | 列表项 |
| `ExerciseSystemDetailResponse` | E2 返回体 |
| `ExerciseMuscleResponse` | 肌肉对象 |
| `ExerciseEquipmentResponse` | 器械对象 |

#### `ExerciseSystemListResponse`

| 字段名 | 类型 | 说明 |
|------|------|------|
| `page` | `Integer` | 当前页 |
| `pageSize` | `Integer` | 每页大小 |
| `total` | `Long` | 总条数 |
| `records` | `List<ExerciseSystemListItemResponse>` | 列表记录 |

#### `ExerciseSystemListItemResponse`

| 字段名 | 类型 | 说明 |
|------|------|------|
| `exerciseId` | `Long` | 动作 ID |
| `exerciseName` | `String` | 动作名称 |
| `exerciseType` | `String` | 动作类型 |
| `movementType` | `String` | 动作模式 |
| `defaultUnit` | `String` | 默认单位 |
| `defaultStructureType` | `String` | 默认结构类型 |
| `videoUrl` | `String \| null` | 视频地址 |
| `primaryMuscles` | `List<String>` | 主要肌肉名称 |
| `secondaryMuscles` | `List<String>` | 次要肌肉名称 |
| `equipmentNames` | `List<String>` | 器械名称 |

#### `ExerciseSystemDetailResponse`

| 字段名 | 类型 | 说明 |
|------|------|------|
| `exerciseId` | `Long` | 动作 ID |
| `exerciseName` | `String` | 动作名称 |
| `exerciseType` | `String` | 动作类型 |
| `movementType` | `String` | 动作模式 |
| `defaultUnit` | `String` | 默认单位 |
| `defaultStructureType` | `String` | 默认结构类型 |
| `videoUrl` | `String \| null` | 视频地址 |
| `calorieBurnReference` | `BigDecimal \| null` | 热量消耗参考值 |
| `calorieReferenceUnit` | `String \| null` | 热量参考单位 |
| `primaryMuscles` | `List<ExerciseMuscleResponse>` | 主要肌肉列表 |
| `secondaryMuscles` | `List<ExerciseMuscleResponse>` | 次要肌肉列表 |
| `equipments` | `List<ExerciseEquipmentResponse>` | 器械列表 |

#### `ExerciseMuscleResponse`

| 字段名 | 类型 | 说明 |
|------|------|------|
| `muscleId` | `Long` | 肌肉 ID |
| `muscleName` | `String` | 肌肉名称 |
| `muscleCode` | `String` | 肌肉编码 |
| `relationType` | `String` | `primary` / `secondary` |

#### `ExerciseEquipmentResponse`

| 字段名 | 类型 | 说明 |
|------|------|------|
| `equipmentId` | `Long` | 器械 ID |
| `equipmentName` | `String` | 器械名称 |
| `sceneType` | `String` | 器械场景 |

### 6.3 E1 `GET /api/exercises/system`

#### 路径与方法

- 方法：`GET`
- 路径：`/api/exercises/system`

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `keyword` | `string` | 否 | 按 `name` 模糊匹配 |
| `exerciseType` | `string` | 否 | 精确过滤 |
| `movementType` | `string` | 否 | 精确过滤 |
| `structureType` | `string` | 否 | 精确过滤 |
| `sceneType` | `string` | 否 | 关联器械场景过滤 |
| `muscleId` | `number` | 否 | 关联肌肉过滤 |
| `page` | `number` | 否 | 默认 `1` |
| `pageSize` | `number` | 否 | 默认 `20`，最大 `100` |

#### 实现逻辑

1. 校验分页参数与过滤参数格式
2. 构造系统动作基础过滤：
   - `owner_user_id IS NULL`
   - `is_active = 1`
3. 根据可选参数拼接过滤条件
4. 第一段 SQL 查分页 ID 与总数
5. 第二段按 ID 批量查询：
   - 动作基础信息
   - 肌肉名称聚合
   - 器械名称聚合
6. 应用层按 `exerciseId` 聚合成列表记录
7. 返回分页结果

#### 查询设计约束

- `sceneType` 过滤时仅要求“存在至少一个该场景器械”
- `muscleId` 过滤时同时匹配 primary / secondary
- 不做空结果报错，空列表正常返回

### 6.4 E2 `GET /api/exercises/system/{exerciseId}`

#### 路径与方法

- 方法：`GET`
- 路径：`/api/exercises/system/{exerciseId}`

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `exerciseId` | `number` | 是 | 系统动作 ID |

#### 实现逻辑

1. 根据 `exerciseId` 查询动作主表
2. 校验：
   - 记录存在
   - `owner_user_id IS NULL`
   - `is_active = 1`
3. 批量读取该动作的肌肉关系与器械关系
4. 按 `relationType` 拆分 primary / secondary
5. 组装详情返回体

#### 失败逻辑

以下场景统一抛：

- `EXERCISE_NOT_FOUND`

包括：

- 动作不存在
- 动作不是系统动作
- 动作已禁用

---

## 七、持久层与查询设计

### 7.1 Entity 清单

| 类名 | 对应表 | 说明 |
|------|------|------|
| `ExerciseEntity` | `exercises` | 动作主表基础读取 |
| `ExerciseMuscleRelationEntity` | `exercise_muscles + muscles` | 动作肌肉关系投影 |
| `ExerciseEquipmentRelationEntity` | `exercise_equipments + equipments` | 动作器械关系投影 |

### 7.2 Mapper 设计

#### `ExerciseQueryMapper`

建议提供方法：

- `selectSystemExercisePageIds(query)`
- `countSystemExercises(query)`
- `selectSystemExercisesByIds(List<Long> exerciseIds)`
- `selectSystemExerciseDetailById(Long exerciseId)`

#### `ExerciseMuscleQueryMapper`

建议提供方法：

- `selectByExerciseIds(List<Long> exerciseIds)`
- `selectByExerciseId(Long exerciseId)`

#### `ExerciseEquipmentQueryMapper`

建议提供方法：

- `selectByExerciseIds(List<Long> exerciseIds)`
- `selectByExerciseId(Long exerciseId)`

### 7.3 SQL 策略约束

#### 列表主查询

列表主查询只负责取 ID，不直接拼完整返回对象。

过滤条件建议：

- `keyword`：`name LIKE CONCAT('%', ?, '%')`
- `exerciseType`：`exercise_type = ?`
- `movementType`：`movement_type = ?`
- `structureType`：`default_structure_type = ?`
- `sceneType`：`EXISTS (...)` 关联 `exercise_equipments` 与 `equipments`
- `muscleId`：`EXISTS (...)` 关联 `exercise_muscles`

#### 详情查询

详情查询不做分页，允许直接按单主键读取。

### 7.4 聚合规则

列表接口聚合规则：

- `primaryMuscles`：按 `relation_type='primary'` 聚合名称数组
- `secondaryMuscles`：按 `relation_type='secondary'` 聚合名称数组
- `equipmentNames`：按器械名称聚合，按关系 `sort_order` 排序

详情接口聚合规则：

- 肌肉对象返回 `muscleId / muscleName / muscleCode / relationType`
- 器械对象返回 `equipmentId / equipmentName / sceneType`

---

## 八、领域规则

### 8.1 查询参数规则

`ExerciseQueryPolicyService` 负责：

- `page` 默认值为 `1`
- `pageSize` 默认值为 `20`
- `pageSize` 最大值限制为 `100`
- 空白字符串参数按 `null` 处理
- `exerciseId`、`muscleId`、`page`、`pageSize` 小于 1 时抛 `INVALID_ARGUMENT`

### 8.2 可见性规则

模块级固定规则：

- 只能看到系统动作
- 只能看到启用动作

### 8.3 结构类型规则

当前 `defaultStructureType` 仅允许：

- `set_based`
- `single_segment`

如果数据库中出现其他脏值：

- 列表和详情仍可原样返回数据库值
- 但应在 debug 日志中记录异常结构值

说明：

- 本模块是查询模块，不在这里承担结构值修复职责
- 真正的强校验仍在 `plan` 保存链路执行

---

## 九、错误码设计

建议新增：

| 错误码 | HTTP 状态码 | 含义 |
|------|------|------|
| `EXERCISE_NOT_FOUND` | 404 | 动作不存在、不是系统动作或已禁用 |

继续复用：

| 错误码 | HTTP 状态码 | 含义 |
|------|------|------|
| `UNAUTHORIZED` | 401 | 未登录或 token 无效 |
| `INVALID_ARGUMENT` | 400 | 查询参数非法 |

---

## 十、Swagger 与注释规范

### 10.1 Controller 注解

`ExerciseController` 统一补齐：

- `@Tag(name = "Exercise")`
- `@Operation`
- `@ApiResponses`
- `@SecurityRequirement(name = "bearerAuth")`

### 10.2 DTO / VO 注解

所有 DTO / VO 类和字段补：

- `@Schema(description = "...")`

重点字段示例值必须补充：

- `exerciseId`
- `exerciseName`
- `defaultStructureType`
- `exerciseType`
- `movementType`
- `sceneType`

### 10.3 方法注释

要求：

- Controller 接口方法写 Javadoc
- Application Service 公开方法写 Javadoc
- 自定义 Mapper 方法写简短注释说明查询用途
- 两段式分页聚合代码前增加短注释，解释为何先查 ID 再聚合

---

## 十一、日志设计

### 11.1 Debug 日志点位

#### `ExerciseController`

- 进入列表查询时记录：
  - `userId`
  - `page`
  - `pageSize`
  - 过滤条件是否存在
- 进入详情查询时记录：
  - `userId`
  - `exerciseId`

#### `ExerciseQueryApplicationService`

- 列表查询完成后记录：
  - `userId`
  - `page`
  - `pageSize`
  - `total`
  - `recordCount`
- 详情查询完成后记录：
  - `userId`
  - `exerciseId`
  - `primaryMuscleCount`
  - `secondaryMuscleCount`
  - `equipmentCount`

#### `ExerciseQueryPolicyService`

- 查询参数标准化后记录：
  - 最终 `page`
  - 最终 `pageSize`
  - 是否命中过滤条件

### 11.2 日志安全要求

禁止输出：

- 完整 JWT
- 大段 SQL 参数明细之外的敏感隐私

动作查询模块本身不涉及强敏感数据，日志风险较低。

---

## 十二、测试设计

### 12.1 单元测试

建议新增：

- `ExerciseQueryPolicyServiceTest`

覆盖：

- `page/pageSize` 默认值
- `page < 1` 非法
- `pageSize < 1` 非法
- `pageSize > 100` 非法
- 空白过滤值标准化

### 12.2 集成测试

建议新增：

- `ExerciseIntegrationTest`

至少覆盖：

1. `GET /api/exercises/system` 默认分页成功
2. `keyword` 过滤生效
3. `exerciseType` 过滤生效
4. `movementType` 过滤生效
5. `structureType` 过滤生效
6. `sceneType` 过滤生效
7. `muscleId` 过滤生效
8. 列表接口不返回用户自定义动作
9. 列表接口不返回已禁用动作
10. 详情接口成功返回完整结构
11. 详情接口对非系统动作返回 `EXERCISE_NOT_FOUND`
12. 详情接口对禁用动作返回 `EXERCISE_NOT_FOUND`
13. 未登录访问任一接口返回 401

### 12.3 测试基座要求

当前 `backend/src/test/resources/schema-auth.sql` 未来若承接本模块测试，必须保证包含：

- `muscles`
- `equipments`
- `exercises.default_structure_type`
- `exercise_muscles`
- `exercise_equipments`

如果当前测试基座缺少这些表或字段，实施时需要同步补齐。

---

## 十三、实施顺序建议

推荐实施顺序：

1. 新建 `com.dailyforge.modules.exercise` 包结构
2. 增加 `EXERCISE_NOT_FOUND` 错误码
3. 落 Entity / Mapper / 查询 SQL
4. 落 `ExerciseQueryPolicyService`
5. 落 `ExerciseQueryApplicationService`
6. 落 `ExerciseController`
7. 补 Swagger、Javadoc、Debug 日志
8. 补集成测试与测试数据
9. 视需要收拢 `plan` 中的 `ExerciseReadEntity / Mapper`

---

## 十四、当前默认假设

- 当前没有单独的 `exercise_PRD.md`，因此本设计完全以接口文档和现有代码为准。
- `exercise` 模块当前只负责系统动作查询，不承担模板结构校验职责。
- 当前数据库已执行 `V5`，因此 `default_structure_type` 可直接读取。
- 本轮不改变 `plan` 对外接口，只为后续动作查询模块独立化打基础。
- 列表接口优先保证 `cycle_template` 联调需要；如后续产品收口为最小字段集，也只收缩返回字段，不改变查询边界。
