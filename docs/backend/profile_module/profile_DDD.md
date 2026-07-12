# DailyForge Profile 模块详细设计文档（DDD）
> 版本：v1.1 | 日期：2026-07-12 | 模块归属：`backend` 单体应用 `com.dailyforge.modules.profile`

---

## 一、文档说明

### 1.1 上游输入文档

本设计基于以下输入整理：

- PRD：[profile_PRD.md](/D:/Computer%20Science/DailyForge/docs/prd/profile_PRD.md)
- 接口文档：[profile_接口文档.md](/D:/Computer%20Science/DailyForge/docs/interfaces/profile_%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3.md)
- 数据库改造清单：[profile_数据库改造清单.md](/D:/Computer%20Science/DailyForge/docs/profile_%E6%95%B0%E6%8D%AE%E5%BA%93%E6%94%B9%E9%80%A0%E6%B8%85%E5%8D%95.md)

### 1.2 当前目标

本文件用于把 `profile` 模块收口成一套可实施的后端技术设计，重点明确：

- 模块边界
- 数据模型
- 逻辑删除与快照同步策略
- 包结构与类职责
- 错误码与鉴权行为
- 测试范围

### 1.3 当前实现状态

截至本文档编写时：

- `com.dailyforge.modules.profile` 包仅存在占位 `package-info.java`
- `auth` 模块已完成，可直接复用 JWT 鉴权、统一响应、全局异常和 Swagger 基础设施
- `user_profiles` 的 Entity / Mapper 目前仍在 `auth` 模块下，建议本次迁回 `profile` 模块

---

## 二、模块概述

### 2.1 模块定位

`profile` 模块负责维护用户基础档案与身体指标数据，为以下能力提供输入：

- AI 训练计划生成
- AI 饮食建议
- AI 周期总结与调整建议
- 个人信息页资料展示
- 后续统计模块中的身体变化趋势分析

### 2.2 本期交付范围

| 序号 | 功能 | 说明 | 状态 |
|------|------|------|:---:|
| 1 | 获取基础档案 | 返回 `user_profiles` + 当前体重摘要 | 待开发 |
| 2 | 更新基础档案 | 更新全部档案字段，字段均可选 | 待开发 |
| 3 | 获取当前身体状态快照 | 返回 `user_current_body_metrics` | 待开发 |
| 4 | 获取身体指标历史列表 | 分页查询 `body_metric_logs` | 待开发 |
| 5 | 新增身体指标记录 | 追加历史记录并同步快照 | 待开发 |
| 6 | 删除最近一条身体指标记录 | 逻辑删除最近一条记录并重算快照 | 待开发 |
| 7 | 获取资料完善摘要 | 支撑 AI 前置资料提示 | 待开发 |

### 2.3 明确不做

- 历史身体指标编辑
- 历史身体指标恢复接口
- 趋势图与复杂统计分析
- 可穿戴设备同步
- 医疗结论类自动分析
- 强制资料完成拦截

---

## 三、与现有代码的衔接

### 3.1 可复用基础设施

`profile` 模块直接复用当前项目已存在的共享能力：

- `ApiResponse`
- `ErrorCode`
- `BusinessException`
- `GlobalExceptionHandler`
- `SecurityConfig`
- `OpenApiConfig`
- `AuthSecurityUtils`
- `JwtAuthenticationFilter`
- `RestAuthenticationEntryPoint`
- `RestAccessDeniedHandler`

### 3.2 路由基线

当前项目真实路由约定是：

- 全局 `context-path`：`/api`
- Controller 内部资源路径：如 `auth` 模块使用 `/auth`

因此 `profile` 模块沿用：

- Controller 基础路径：`/profile`
- 外部访问路径：`/api/profile/...`

### 3.3 结构收口建议

建议在开发 `profile` 模块时同步做一次轻量重构：

1. 将 `UserProfileEntity` 和 `UserProfileMapper` 迁移到 `profile` 模块
2. `auth` 注册流程继续依赖该 Mapper 初始化空档案
3. 避免 `profile` 读写自己的主表时反向依赖 `auth`

---

## 四、关键设计决策

### 4.1 档案与身体指标分层

- `user_profiles` 保存低频变化资料
- `body_metric_logs` 保存历史真实记录
- `user_current_body_metrics` 保存“当前已知状态快照”

明确不在 `user_profiles` 冗余当前体重。

### 4.2 基础档案字段规则

基础档案字段全部可选，不作为保存门槛：

- `gender`
- `birthDate`
- `heightCm`
- `goalType`
- `trainingLevel`
- `injuryNotes`

这意味着：

- `PUT /profile/basic` 不因为缺少某个字段而失败
- `completion-summary` 只负责提示资料完备度
- AI 相关接口未来可根据完备度决定是否提示或阻断，但 `profile` 自身不阻断保存

### 4.3 身体指标录入规则

- 新增历史记录采用“只追加，不修改”
- `recordDate` 仍必填
- 所有身体指标值字段都可选，包括 `weightKg`
- 但一条身体指标记录必须至少包含一个有效指标值
- 每条记录只保存本次真实填写的字段
- 不自动复制上一条记录的旧值

“至少一个有效指标值”包括：

- `weightKg`
- `bodyFatPercent`
- `bmi`
- `skeletalMusclePercent`
- `bodyWaterPercent`
- `basalMetabolicRateKcal`
- `waistCm`
- `hipCm`
- `waistHipRatio`
- `bodyAge`
- `bodyType`

`note` 不计入“有效指标值”，避免出现只有备注、没有任何身体数据的空记录。

### 4.4 删除规则

- `body_metric_logs` 改为逻辑删除，不做物理删除
- 新增字段 `is_del`
- 建议同时补充 `deleted_at`
- 仅允许删除当前用户按 `record_date DESC, id DESC` 排序后的最近一条记录
- 如果最近一条记录已经被逻辑删除，不允许再次删除，也不能跳过它去删更早记录
- 删除完成后必须重算快照

### 4.5 AI 资料完备性规则

MVP 阶段，`aiPlanReady`、`aiNutritionReady`、`aiSummaryReady` 使用同一套最小资料规则：

- `gender`
- `birthDate`
- `heightCm`
- `goalType`
- 至少存在一条未删除且带有效体重的记录，或当前快照中存在 `currentWeightKg`

### 4.6 快照语义决策

当前快照语义保持为：

- 新增记录时，仅用本次实际填写的字段覆盖 `user_current_body_metrics`
- 本次未填写字段不覆盖旧快照
- 删除最近一条记录时，基于剩余未删除历史全量重算快照

这表示当前设计更接近“最后一次提交后的当前已知状态”。

---

## 五、数据设计

### 5.1 涉及表

| 表名 | 用途 |
|------|------|
| `users` | 账户主表，用于校验登录用户是否存在且启用 |
| `user_profiles` | 基础档案主表 |
| `body_metric_logs` | 身体指标历史真相表 |
| `user_current_body_metrics` | 当前身体状态快照表 |

### 5.2 `user_profiles`

建议由 `profile` 模块正式接管。

关键字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `user_id` | `BIGINT` | 用户 ID，唯一 |
| `gender` | `VARCHAR(16)` | 性别，`male/female`，可空 |
| `birth_date` | `DATE` | 出生日期，可空 |
| `height_cm` | `DECIMAL(5,2)` | 身高，可空 |
| `goal_type` | `VARCHAR(32)` | 目标，可空 |
| `training_level` | `VARCHAR(32)` | 训练水平，可空 |
| `injury_notes` | `VARCHAR(1000)` | 伤病或注意事项，可空 |
| `created_at` | `DATETIME(3)` | 创建时间 |
| `updated_at` | `DATETIME(3)` | 更新时间 |

与当前 DDL 的差异建议：

- `goal_type` 必须确认已经实际落表
- `injury_notes` 建议长度从 `500` 提升到 `1000`
- 不增加任何“必填”数据库约束

### 5.3 `body_metric_logs`

该表继续作为历史明细表，但增加逻辑删除语义。

关键字段建议：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `BIGINT` | 主键 |
| `user_id` | `BIGINT` | 所属用户 |
| `record_date` | `DATE` | 记录日期，必填 |
| `weight_kg` | `DECIMAL(6,2)` | 体重，可空 |
| `body_fat_percent` | `DECIMAL(5,2)` | 体脂率，可空 |
| `bmi` | `DECIMAL(5,2)` | BMI，可空 |
| `skeletal_muscle_percent` | `DECIMAL(5,2)` | 骨骼肌率，可空 |
| `body_water_percent` | `DECIMAL(5,2)` | 身体水分，可空 |
| `basal_metabolic_rate_kcal` | `DECIMAL(8,2)` | 基础代谢，可空 |
| `waist_cm` | `DECIMAL(6,2)` | 腰围，可空 |
| `hip_cm` | `DECIMAL(6,2)` | 臀围，可空 |
| `waist_hip_ratio` | `DECIMAL(5,2)` | 腰臀比，可空 |
| `body_age` | `SMALLINT` | 身体年龄，可空 |
| `body_type` | `VARCHAR(32)` | 体型，可空 |
| `data_source` | `VARCHAR(32)` | 数据来源，可空 |
| `note` | `VARCHAR(1000)` | 备注，可空 |
| `is_del` | `TINYINT(1)` | 逻辑删除标记，`0/1` |
| `deleted_at` | `DATETIME(3)` | 逻辑删除时间，可空 |
| `created_at` | `DATETIME(3)` | 创建时间 |

排序规则统一为：

- `record_date DESC, id DESC`

“最近一条”也按这套规则定义。

### 5.4 `user_current_body_metrics`

新增快照表，最小建议字段如下：

| 字段 | 类型 | 说明 |
|------|------|------|
| `user_id` | `BIGINT` | 用户 ID，主键或唯一键 |
| `current_weight_kg` | `DECIMAL(6,2)` | 当前体重 |
| `current_body_fat_percent` | `DECIMAL(5,2)` | 当前体脂率 |
| `current_bmi` | `DECIMAL(5,2)` | 当前 BMI |
| `current_skeletal_muscle_percent` | `DECIMAL(5,2)` | 当前骨骼肌率 |
| `current_body_water_percent` | `DECIMAL(5,2)` | 当前身体水分 |
| `current_basal_metabolic_rate_kcal` | `DECIMAL(8,2)` | 当前基础代谢 |
| `current_waist_cm` | `DECIMAL(6,2)` | 当前腰围 |
| `current_hip_cm` | `DECIMAL(6,2)` | 当前臀围 |
| `current_waist_hip_ratio` | `DECIMAL(5,2)` | 当前腰臀比 |
| `current_body_age` | `SMALLINT` | 当前身体年龄 |
| `current_body_type` | `VARCHAR(32)` | 当前体型 |
| `updated_at` | `DATETIME(3)` | 最近快照更新时间 |

### 5.5 索引建议

- `body_metric_logs(user_id, is_del, record_date, id)`
- `user_current_body_metrics(user_id)`

---

## 六、快照同步设计

### 6.1 新增身体指标记录

新增一条 `body_metric_logs` 后：

1. 插入历史记录，`is_del=0`
2. 读取或初始化 `user_current_body_metrics`
3. 对本次请求中“有填写”的指标字段执行覆盖
4. 对本次未填写字段保持原值
5. 更新 `updated_at`

实现建议：

- 用显式字段判断“本次有填写”
- 不要用简单 Bean 拷贝覆盖快照

### 6.2 删除最近一条身体指标记录

删除流程建议：

1. 查询当前用户按 `record_date DESC, id DESC` 的最近一条记录
2. 如果不存在，返回 `BODY_METRIC_NOT_FOUND`
3. 如果该记录 `is_del=1`，返回 `BODY_METRIC_LATEST_ALREADY_DELETED`
4. 将该记录更新为 `is_del=1`，写入 `deleted_at`
5. 基于剩余 `is_del=0` 的历史记录重算快照

### 6.3 重算快照算法

重算快照时：

1. 查询该用户所有 `is_del=0` 的历史记录
2. 按 `record_date DESC, id DESC`
3. 对每个快照字段取“第一条非空值”
4. 若所有未删除记录都不存在，删除快照行

---

## 七、API 设计映射

### 7.1 接口总览

| 编号 | 方法 | 路径 | 鉴权 | 作用 |
|------|------|------|:---:|------|
| P1 | GET | `/api/profile/basic` | 是 | 获取基础档案 |
| P2 | PUT | `/api/profile/basic` | 是 | 更新基础档案 |
| P3 | GET | `/api/profile/body-metrics/current` | 是 | 获取当前身体状态快照 |
| P4 | GET | `/api/profile/body-metrics` | 是 | 获取身体指标历史列表 |
| P5 | POST | `/api/profile/body-metrics` | 是 | 新增身体指标记录 |
| P6 | DELETE | `/api/profile/body-metrics/latest` | 是 | 删除最近一条身体指标记录 |
| P7 | GET | `/api/profile/completion-summary` | 是 | 获取资料完备度摘要 |

### 7.2 P1 获取基础档案

数据来源：

- `user_profiles`
- `user_current_body_metrics.current_weight_kg`
- 最近一条未删除历史记录的 `record_date`

### 7.3 P2 更新基础档案

写入表：

- `user_profiles`

校验点：

- 字段格式合法
- 枚举值合法
- `heightCm` 合法范围
- `injuryNotes` 长度限制

不校验“必填字段完整性”。

### 7.4 P3 获取当前身体状态快照

数据来源：

- `user_current_body_metrics`

该接口返回的是“当前已知快照”，不承诺所有字段来自同一条历史记录。

### 7.5 P4 获取身体指标历史列表

数据来源：

- `body_metric_logs`

查询规则：

- 仅查询 `is_del=0`
- 按 `record_date DESC, id DESC`
- 默认分页 `page=1, pageSize=20`
- `pageSize` 最大建议 `100`

### 7.6 P5 新增身体指标记录

写入与副作用：

- 插入 `body_metric_logs`
- 更新 `user_current_body_metrics`

关键规则：

- `recordDate` 必填
- 至少一个指标字段非空
- `note` 不算指标字段

### 7.7 P6 删除最近一条身体指标记录

写入与副作用：

- 逻辑删除一条 `body_metric_logs`
- 全量重算 `user_current_body_metrics`

关键规则：

- 只允许删除按 `record_date DESC, id DESC` 排序后的第一条
- 若该记录已经逻辑删除，不允许再次删除

### 7.8 P7 获取资料完备摘要

数据来源：

- `user_profiles`
- `user_current_body_metrics`
- 或“是否存在至少一条未删除体重记录”的布尔查询

返回的 `ready` 字段仅代表“资料是否足够支撑 AI 场景”，不是 `profile` 保存的前置门槛。

---

## 八、包结构设计

### 8.1 推荐结构

```text
com.dailyforge.modules.profile
├── application
│   ├── assembler
│   │   └── ProfileAssembler.java
│   └── service
│       ├── ProfileApplicationService.java
│       └── BodyMetricApplicationService.java
├── domain
│   └── service
│       ├── BasicProfilePolicyService.java
│       ├── ProfileCompletionPolicyService.java
│       └── BodyMetricSnapshotDomainService.java
├── infrastructure
│   └── persistence
│       ├── entity
│       │   ├── UserProfileEntity.java
│       │   ├── BodyMetricLogEntity.java
│       │   └── UserCurrentBodyMetricsEntity.java
│       └── mapper
│           ├── UserProfileMapper.java
│           ├── BodyMetricLogMapper.java
│           └── UserCurrentBodyMetricsMapper.java
└── interfaces
    ├── dto
    │   ├── UpdateBasicProfileRequest.java
    │   ├── CreateBodyMetricRequest.java
    │   └── BodyMetricPageQuery.java
    ├── rest
    │   └── ProfileController.java
    └── vo
        ├── BasicProfileResponse.java
        ├── CurrentBodyMetricsResponse.java
        ├── BodyMetricRecordItem.java
        ├── BodyMetricPageResponse.java
        ├── DeleteLatestBodyMetricResponse.java
        └── ProfileCompletionSummaryResponse.java
```

### 8.2 类职责

| 类名 | 职责 |
|------|------|
| `ProfileController` | 暴露 7 个接口，统一补 Swagger 和鉴权注解 |
| `ProfileApplicationService` | 编排基础档案读取、更新、资料完备性摘要 |
| `BodyMetricApplicationService` | 编排身体指标新增、删除、列表、快照查询 |
| `ProfileAssembler` | DTO/Entity/VO 映射 |
| `BasicProfilePolicyService` | 基础档案格式校验与规范化 |
| `ProfileCompletionPolicyService` | 计算资料缺失字段与 AI ready 状态 |
| `BodyMetricSnapshotDomainService` | 处理快照覆盖与快照重算算法 |
| `UserProfileMapper` | `user_profiles` 数据访问 |
| `BodyMetricLogMapper` | `body_metric_logs` 数据访问 |
| `UserCurrentBodyMetricsMapper` | `user_current_body_metrics` 数据访问 |

---

## 九、DTO / VO 设计

### 9.1 Request DTO

| 类名 | 作用 | 字段 |
|------|------|------|
| `UpdateBasicProfileRequest` | 更新基础档案 | `gender`、`birthDate`、`heightCm`、`goalType`、`trainingLevel`、`injuryNotes` |
| `CreateBodyMetricRequest` | 新增身体指标记录 | `recordDate`、`weightKg`、`bodyFatPercent`、`bmi`、`skeletalMusclePercent`、`bodyWaterPercent`、`basalMetabolicRateKcal`、`waistCm`、`hipCm`、`waistHipRatio`、`bodyAge`、`bodyType`、`note` |
| `BodyMetricPageQuery` | 历史列表分页查询 | `page`、`pageSize` |

### 9.2 Response VO

| 类名 | 作用 |
|------|------|
| `BasicProfileResponse` | 基础档案展示与回写响应 |
| `CurrentBodyMetricsResponse` | 当前身体状态快照 |
| `BodyMetricRecordItem` | 历史列表单条记录 |
| `BodyMetricPageResponse` | 历史分页结果 |
| `DeleteLatestBodyMetricResponse` | 删除最近一条记录结果 |
| `ProfileCompletionSummaryResponse` | 资料完备度摘要 |

### 9.3 MapStruct 使用建议

建议继续使用 `MapStruct` 处理：

- `UserProfileEntity -> BasicProfileResponse` 的基础字段映射
- `UserCurrentBodyMetricsEntity -> CurrentBodyMetricsResponse`
- `BodyMetricLogEntity -> BodyMetricRecordItem`

以下场景更适合手写组装：

- `BasicProfileResponse` 需要拼入当前体重与最近记录日期
- `ProfileCompletionSummaryResponse` 需要计算多个布尔值和缺失字段列表
- 快照覆盖更新不能使用普通 Bean 拷贝

---

## 十、错误码设计

### 10.1 复用现有错误码

- `UNAUTHORIZED`
- `FORBIDDEN`
- `USER_NOT_FOUND`
- `ACCOUNT_DISABLED`
- `INVALID_ARGUMENT`
- `TOKEN_INVALID`
- `TOKEN_EXPIRED`
- `TOKEN_TYPE_MISMATCH`

### 10.2 建议新增错误码

| 错误码 | HTTP 状态 | 含义 |
|------|------|------|
| `BODY_METRIC_EMPTY_RECORD` | 400 | 身体指标记录没有任何有效指标值 |
| `BODY_METRIC_NOT_FOUND` | 404 | 没有任何可操作的身体指标记录 |
| `BODY_METRIC_LATEST_ALREADY_DELETED` | 409 | 最近一条记录已经逻辑删除，不能再次删除 |

### 10.3 取消的旧规则

以下旧规则不再建议保留：

- `PROFILE_REQUIRED_FIELDS_MISSING`
- `BODY_METRIC_WEIGHT_REQUIRED`

原因：

- 基础档案字段不再有保存门槛
- 体重不再是身体指标保存门槛

---

## 十一、鉴权与 Swagger 设计

### 11.1 鉴权策略

`profile` 模块所有接口都要求登录态。

因此：

- `SecurityConfig` 无需为 `profile` 增加匿名放行路径
- 所有接口都走 JWT 过滤器
- Service 层依旧要校验用户存在且状态为 `active`

### 11.2 Swagger 注解要求

`ProfileController` 建议统一补齐：

- `@Tag(name = "Profile")`
- `@Operation`
- `@ApiResponses`
- 所有接口加 `@SecurityRequirement(name = "bearerAuth")`

DTO / VO 要补：

- `@Schema(description = "...")`
- 对 `recordDate`、分页参数、身体指标字段补示例值

---

## 十二、事务设计

### 12.1 事务边界

| 方法 | 是否事务 | 说明 |
|------|------|------|
| 获取基础档案 | 否 | 只读 |
| 更新基础档案 | 是 | 更新 `user_profiles` |
| 获取当前身体状态快照 | 否 | 只读 |
| 获取身体指标历史列表 | 否 | 只读 |
| 新增身体指标记录 | 是 | 插入历史记录并更新快照 |
| 删除最近一条身体指标记录 | 是 | 逻辑删除历史记录并重算快照 |
| 获取资料完备摘要 | 否 | 只读 |

### 12.2 并发考虑

MVP 阶段建议做轻量并发控制：

- 新增身体指标记录：单事务内完成“插入历史 + 更新快照”
- 删除最近一条：单事务内完成“查最近一条 + 逻辑删除 + 重算快照”

后续如果担心同一用户并发写入导致快照错乱，可再增加按 `user_id` 的行锁策略。

---

## 十三、测试设计

### 13.1 单元测试

建议至少覆盖：

- `BasicProfilePolicyServiceTest`
  - 全字段为空时允许保存
  - 枚举值校验
- `ProfileCompletionPolicyServiceTest`
  - 基础档案为空
  - 仅缺体重
  - AI ready 全通过
- `BodyMetricSnapshotDomainServiceTest`
  - 仅覆盖本次填写字段
  - 删除后按剩余历史重算
  - 无剩余历史时删除快照

### 13.2 集成测试

建议至少覆盖：

- `GET /api/profile/basic` 成功返回档案与当前体重
- `PUT /api/profile/basic` 在全字段可空情况下仍可保存
- `GET /api/profile/body-metrics/current` 返回快照
- `GET /api/profile/body-metrics` 仅返回 `is_del=0` 记录
- `POST /api/profile/body-metrics` 成功新增并更新快照
- 新增时所有指标字段都为空返回 `BODY_METRIC_EMPTY_RECORD`
- `DELETE /api/profile/body-metrics/latest` 成功逻辑删除并重算快照
- 最近一条已删除时返回 `BODY_METRIC_LATEST_ALREADY_DELETED`
- 无历史记录时删除返回 `BODY_METRIC_NOT_FOUND`
- `GET /api/profile/completion-summary` 正确返回缺失字段
- 全部接口未登录访问返回 401

---

## 十四、开发顺序建议

建议按下面顺序开发：

1. 先完成数据库迁移设计
2. 迁移 `UserProfileEntity` / `UserProfileMapper` 到 `profile` 模块
3. 完成 `GET/PUT /profile/basic`
4. 完成 `user_current_body_metrics` 持久层
5. 完成 `POST /profile/body-metrics`
6. 完成 `GET /profile/body-metrics/current`
7. 完成 `GET /profile/body-metrics`
8. 完成 `DELETE /profile/body-metrics/latest`
9. 完成 `GET /profile/completion-summary`
10. 补测试与文档同步

---

## 十五、需要你重点审查的点

这版修订后最值得你确认的是下面 4 件事：

1. 基础档案字段全部可选，这是否就是你最终想要的资料策略。
2. `body_metric_logs` 是否确认采用逻辑删除，并补 `deleted_at`。
3. “最近一条记录”的定义是否固定为 `record_date DESC, id DESC`。
4. 快照是否继续保持“按最后一次提交覆盖已填写字段”的语义。

---

## 十六、结论

在当前代码基础上，`profile` 模块最合适的实现方式仍然是：

- 沿用 `auth` 已建立的鉴权与统一响应基础设施
- 继续使用 MyBatis-Plus + MapStruct 的轻量分层
- 以 `user_profiles + body_metric_logs + user_current_body_metrics` 形成“档案 + 历史 + 快照”三层模型
- 用 `completion-summary` 把“资料是否足够支撑 AI”收口为提示能力，而不是保存门槛

这套设计可以先支撑 MVP，同时给后续 `ai`、`plan`、`stats` 留出演进空间。
