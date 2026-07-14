# DailyForge Cycle Template 模块详细设计文档（DDD）

> 版本：v1.1  
> 更新时间：2026-07-14  
> 模块归属：`backend` 单体应用  
> Java 包路径：`com.dailyforge.modules.plan`

---

## 一、文档说明

### 1.1 上游输入

- PRD：[cycle_template_PRD.md](/D:/Computer%20Science/DailyForge/docs/prd/cycle_template_PRD.md)
- 接口文档：[cycle_template_接口文档.md](/D:/Computer%20Science/DailyForge/docs/interfaces/cycle_template_%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3.md)
- 数据库设计：[数据库设计.md](/D:/Computer%20Science/DailyForge/docs/%E6%95%B0%E6%8D%AE%E5%BA%93%E8%AE%BE%E8%AE%A1.md)
- 建表草案：[MySQL建表草案.md](/D:/Computer%20Science/DailyForge/docs/MySQL%E5%BB%BA%E8%A1%A8%E8%8D%89%E6%A1%88.md)

### 1.2 文档目标

本文档用于把 `cycle_template` 模块当前已经实现的后端设计收敛成一份可回查、可演进的技术文档，重点记录：

- 真实的数据库适配结论
- 已落地的 Java 分层与类职责
- 每个接口的实际实现策略
- 事务、并发、日志、错误码与 Swagger 约定
- 当前已知的 MVP 边界

### 1.3 当前实现状态

截至 2026-07-14，本模块已完成 MVP 后端实现，并已有单元测试与集成测试覆盖关键流程，包括：

- 正式模板列表、草稿列表、模板详情、当前激活模板摘要
- 创建草稿、更新草稿、更新正式模板、复制模板、启用模板、删除模板
- AI 草稿生成占位接口
- `cycleLength` 可空草稿支持
- `active` 模板运行中编辑限制

---

## 二、数据库适配结论

### 2.1 结论摘要

本模块已可建立在现有循环模板数据模型上实现，唯一必须补到仓库中的正式迁移是：

- `cycle_templates.cycle_length` 从 `NOT NULL` 改为 `NULL`

原因：

- 草稿模板允许只保存名称，不强制立即填写 `cycleLength`
- `POST /api/cycle-templates/drafts`
- `PUT /api/cycle-templates/drafts/{templateId}`

都依赖这一能力

### 2.2 必须落库的迁移

迁移脚本：

- `V4__cycle_template_schema_upgrade.sql`

迁移内容：

```sql
ALTER TABLE cycle_templates
    MODIFY COLUMN cycle_length TINYINT UNSIGNED NULL COMMENT '循环天数(1-7)';
```

### 2.3 无需改表但需要明确的字段映射

#### 1. 接口 `restSeconds` -> 表字段 `target_rest_seconds`

- DTO/VO 使用 `restSeconds`
- 表字段保持 `cycle_day_exercises.target_rest_seconds`

#### 2. 接口 `note` -> 表字段 `notes`

- DTO/VO 使用 `note`
- 表字段保持 `cycle_day_exercises.notes`

#### 3. 空白 `dayName` 的标准化

- 表字段 `cycle_template_days.day_name` 仍保持 `NOT NULL`
- 服务层统一把空白值标准化为 `Day {dayIndex}`

### 2.4 当前真实使用的状态值

`cycle_templates.status`：

- `draft`
- `active`
- `inactive`
- `deleted`

`cycle_runs.status`：

- `active`
- `completed`
- `archived`

说明：

- 当前代码在模板切换时把旧 run 改为 `completed`
- `archived` 仍保留给后续训练周期归档语义

### 2.5 当前真实使用的版本来源值

`cycle_template_versions.source_type` 当前代码已使用：

- `manual`
- `copy`
- `active_patch`

保留但 MVP 未使用：

- `ai_generated`

---

## 三、模块职责与边界

### 3.1 模块定位

`cycle_template` 模块负责维护用户的循环训练模板，并回答四个核心问题：

1. 用户有哪些草稿模板和正式模板
2. 当前激活的是哪个模板
3. 当前循环进行到第几天
4. 运行中的模板如何只修改未来安排而不破坏已完成天

### 3.2 本期已交付范围

| 功能 | 状态 | 说明 |
|------|:---:|------|
| 获取正式模板列表 | 已实现 | 只返回 `active`、`inactive` |
| 获取草稿模板列表 | 已实现 | 只返回 `draft` |
| 获取模板详情 | 已实现 | 返回详情、锁定态、是否可启用/删除 |
| 新建手动草稿 | 已实现 | 支持 `cycleLength=null` |
| AI 生成草稿 | 已实现占位 | 直接返回 `501 + CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED` |
| 更新草稿 | 已实现 | 每次保存创建新版本 |
| 更新正式模板 | 已实现 | 区分 `inactive` 全量覆盖和 `active` 未来覆盖 |
| 复制模板 | 已实现 | 统一复制为 `draft` |
| 启用模板 | 已实现 | 切换状态、关闭旧 run、创建新 run |
| 获取当前激活模板摘要 | 已实现 | 供训练打卡模块读取 |
| 删除模板 | 已实现 | 软删除 |

### 3.3 本期明确不做

- 真实 AI 模板生成
- AI 权益校验
- `ai_generation_records` 写入
- 运行中的 `active` 模板修改 `cycleLength`
- 用户自定义动作库接入模板
- 后端撤销草稿编辑历史

---

## 四、代码结构设计

### 4.1 包结构

```text
com.dailyforge.modules.plan
├─ application
│  ├─ assembler
│  │  └─ CycleTemplateAssembler
│  └─ service
│     ├─ CycleTemplateQueryApplicationService
│     ├─ CycleTemplateCommandApplicationService
│     ├─ CycleTemplateActivationApplicationService
│     ├─ CycleTemplateAiApplicationService
│     └─ PlanUserSupportService
├─ domain
│  └─ service
│     ├─ CycleTemplatePolicyService
│     ├─ CycleTemplateVersionDomainService
│     └─ CycleActivationDomainService
├─ infrastructure
│  └─ persistence
│     ├─ entity
│     └─ mapper
└─ interfaces
   ├─ dto
   ├─ rest
   └─ vo
```

### 4.2 类职责

| 类名 | 职责 |
|------|------|
| `CycleTemplateController` | 对外暴露 11 个接口，负责参数接收、Swagger 注解、统一返回 |
| `CycleTemplateQueryApplicationService` | 查询正式列表、草稿列表、模板详情、当前激活摘要 |
| `CycleTemplateCommandApplicationService` | 创建草稿、更新草稿、更新正式模板、复制模板、删除模板 |
| `CycleTemplateActivationApplicationService` | 启用模板、关闭旧激活上下文、创建新 run、覆盖 active context |
| `CycleTemplateAiApplicationService` | AI 接口占位实现，直接返回未开发错误 |
| `PlanUserSupportService` | 获取并校验当前登录用户 |
| `CycleTemplatePolicyService` | 参数规则、状态规则、编辑范围规则、启用规则 |
| `CycleTemplateVersionDomainService` | 版本号生成、版本快照读取、整版保存、锁定天复制 |
| `CycleActivationDomainService` | 生成新 run、构造当前 active context |

### 4.3 持久层实体

本模块当前使用以下实体：

- `CycleTemplateEntity`
- `CycleTemplateVersionEntity`
- `CycleTemplateDayEntity`
- `CycleDayExerciseEntity`
- `CycleRunEntity`
- `UserActiveCycleEntity`
- `ExerciseReadEntity`

### 4.4 Mapper

当前实现依赖：

- `CycleTemplateMapper`
- `CycleTemplateVersionMapper`
- `CycleTemplateDayMapper`
- `CycleDayExerciseMapper`
- `CycleRunMapper`
- `UserActiveCycleMapper`
- `ExerciseReadMapper`

用途：

- 模板主记录读写
- 版本记录维护
- 日与动作快照持久化
- 当前激活循环上下文读写
- 系统动作合法性校验

---

## 五、接口实现设计

### 5.1 C1 获取正式模板列表

实现策略：

- 登录态取 `userId`
- 查询 `status IN ('active', 'inactive')`
- 按 `updated_at DESC` 返回
- 结合 `user_active_cycles` 补 `activeTemplateId` 和 `currentDayIndex`

### 5.2 C2 获取草稿模板列表

实现策略：

- 查询 `status='draft'`
- `configuredDayCount = count(cycle_template_days where template_version_id = current_version_id)`

### 5.3 C3 获取模板详情

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

锁定规则：

- `draft` / `inactive`：全部可编辑
- `active`：`dayIndex < currentDayIndex` 视为锁定

### 5.4 C4 新建手动草稿

实现策略：

1. 校验 `templateName`
2. 校验草稿 `cycleLength` 可为空
3. 校验 `days` 与 `exerciseId`
4. 插入 `cycle_templates(status='draft')`
5. 创建 `version_no=1`
6. 保存 `days`
7. 回写 `current_version_id`

说明：

- 即使 `days` 为空，也会创建空版本

### 5.5 C5 AI 生成草稿

MVP 当前真实行为：

- 接口保留
- 完成登录态校验
- 记录 debug 日志
- 直接抛出 `CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED`

明确不做：

- 不调用 AI
- 不校验 AI 权限
- 不写 `ai_generation_records`
- 不创建模板或版本

### 5.6 C6 更新草稿

实现策略：

1. 加锁读取模板
2. 只允许 `draft`
3. 校验请求参数
4. 创建新版本
5. 用请求中的 `days` 全量保存新版本内容
6. 更新 `current_version_id`

说明：

- 请求中的 `days` 是草稿最新完整内容
- `days` 为空或不传，会得到空版本

### 5.7 C7 更新正式模板

#### 1. 更新 `inactive`

实现策略：

- 允许改 `templateName`、`goalType`、`cycleLength`
- 若请求 `cycleLength` 为空，则沿用原值
- `days` 按完整模板内容覆盖
- 创建新版本，更新 `current_version_id`

#### 2. 更新 `active`

实现策略：

1. 读取并锁定 `user_active_cycles`
2. 校验：
   - 不允许修改 `cycleLength`
   - 不允许提交锁定天
3. 创建新版本
4. 从旧版本复制锁定天
5. 用请求中的可编辑天覆盖可编辑范围
6. 更新：
   - `cycle_templates.current_version_id`
   - `user_active_cycles.template_version_id`
   - `cycle_runs.template_version_id`

关键语义：

- 这是“未来可编辑范围全量覆盖”
- 若请求未提交某个可编辑天，该天不会自动从旧版本继承

### 5.8 C8 复制模板

实现策略：

- 来源允许为 `draft`、`inactive`、`active`
- 新模板统一落为 `draft`
- 复制当前版本快照
- 新版本 `source_type='copy'`

### 5.9 C9 启用模板

实现策略：

1. 锁定目标模板
2. 只允许 `draft` 或 `inactive`
3. 校验最小可启用条件：
   - `templateName` 非空
   - `currentVersionId` 非空
   - `cycleLength` 在 `1 ~ 7`
4. 锁定 `user_active_cycles`
5. 若已有其他激活模板且 `confirmSwitch=false`，报错
6. 关闭旧模板与旧 run
7. 目标模板置为 `active`
8. 创建新的 `cycle_runs`
9. upsert `user_active_cycles`

切换结果：

- 旧模板状态：`inactive`
- 旧 run 状态：`completed`
- 新模板状态：`active`
- 新 `currentDayIndex = 1`

### 5.10 C10 获取当前激活模板摘要

实现策略：

- 从 `user_active_cycles` 读取：
  - 当前模板 ID
  - 当前版本 ID
  - 当前 run ID
  - 当前天索引
- 关联模板主表、模板日表、run 表
- 返回训练入口所需最小摘要

### 5.11 C11 删除模板

实现策略：

- 只允许删除 `draft` 或 `inactive`
- 执行软删除：`status='deleted'`
- 不删版本、日、动作快照、run 记录

---

## 六、DTO / VO 清单

### 6.1 DTO

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

### 6.2 VO

| 类名 | 用途 |
|------|------|
| `FormalCycleTemplateListResponse` | C1 返回体 |
| `FormalCycleTemplateSummary` | 正式模板卡片 |
| `DraftCycleTemplateListResponse` | C2 返回体 |
| `DraftCycleTemplateSummary` | 草稿模板卡片 |
| `CycleTemplateDetailResponse` | C3 返回体 |
| `CycleTemplateDayResponse` | 模板日详情 |
| `CycleTemplateExerciseResponse` | 模板动作详情 |
| `CreateDraftCycleTemplateResponse` | C4/C6/C7 返回体 |
| `CopyCycleTemplateResponse` | C8 返回体 |
| `ActivateCycleTemplateResponse` | C9 返回体 |
| `CurrentActiveCycleTemplateResponse` | C10 返回体 |
| `DeleteCycleTemplateResponse` | C11 返回体 |

### 6.3 DTO/VO 关键字段约定

- `templateName`：必填，最大 128 字符
- `cycleLength`：
  - 草稿 DTO 可空
  - 正式启用前必须为 `1 ~ 7`
- `days[].dayIndex`：必填，`1 ~ 7`
- `days[].exercises[].sortOrder`：同一天内唯一
- `days[].exercises[].exerciseId`：只能是系统动作
- `targetExtraJson`：`JsonNode`

---

## 七、业务规则与领域规则

### 7.1 草稿规则

- 草稿允许 `cycleLength=null`
- 草稿允许 `days` 为空
- 草稿保存始终创建新版本

### 7.2 正式模板规则

- `inactive` 可整体编辑
- `active` 只能编辑当前天及之后
- `active` 运行中不允许改 `cycleLength`

### 7.3 启用规则

- 只能启用 `draft` 或 `inactive`
- 若已有其他激活模板，必须显式确认切换

### 7.4 删除规则

- `draft` 可删
- `inactive` 可删
- `active` 不可删

### 7.5 动作规则

- 只允许系统动作
- 不允许引用停用动作
- 同一天内 `sortOrder` 不能重复

### 7.6 版本规则

- 每次保存都创建新版本
- `active` 模板更新采用“锁定天复制 + 可编辑天覆盖”
- 历史版本内容不做原地修改

---

## 八、错误码设计

当前模块已使用或扩展的错误码：

- `CYCLE_TEMPLATE_NOT_FOUND`
- `CYCLE_TEMPLATE_ACTIVE_NOT_FOUND`
- `CYCLE_TEMPLATE_CYCLE_LENGTH_INVALID`
- `CYCLE_TEMPLATE_DAY_OUT_OF_RANGE`
- `CYCLE_TEMPLATE_EXERCISE_NOT_FOUND`
- `CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED`
- `CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED`
- `CYCLE_TEMPLATE_EDIT_FORBIDDEN`
- `CYCLE_TEMPLATE_DELETE_FORBIDDEN`
- `CYCLE_TEMPLATE_STATUS_INVALID`
- `CYCLE_TEMPLATE_ACTIVATE_INVALID`
- `CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED`

通用复用：

- `UNAUTHORIZED`
- `INVALID_ARGUMENT`
- `USER_NOT_FOUND`
- `ACCOUNT_DISABLED`

---

## 九、事务与并发设计

### 9.1 事务边界

| 方法 | 是否事务 | 说明 |
|------|------|------|
| 获取正式模板列表 | 否 | 只读 |
| 获取草稿模板列表 | 否 | 只读 |
| 获取模板详情 | 否 | 只读 |
| 创建草稿 | 是 | 模板主表 + 版本 + 日 + 动作 |
| AI 草稿生成 | 否 | 占位接口，直接抛错 |
| 更新草稿 | 是 | 新版本保存 |
| 更新正式模板 | 是 | 新版本保存及 active 上下文更新 |
| 复制模板 | 是 | 主表复制 + 版本复制 |
| 启用模板 | 是 | 状态切换 + run 切换 + active context 覆盖 |
| 删除模板 | 是 | 软删除 |

### 9.2 并发控制

当前实现使用的关键加锁点：

- `selectByIdAndUserIdForUpdate`
- `selectByUserIdForUpdate`

主要保护场景：

- 同一模板被并发编辑
- 同一用户并发切换激活模板
- `active` 模板更新与切换同时发生

---

## 十、日志、注释与 Swagger

### 10.1 Debug 日志

当前设计要求关键链路输出 debug 日志：

- 查询正式/草稿列表：`userId`、数量
- 创建草稿：`userId`、`templateId`、`versionNo`、`cycleLength`
- 更新草稿：`userId`、`templateId`、`versionNo`
- 更新 `active` 模板：`userId`、`templateId`、旧版本、新版本、`editableFromDayIndex`
- 启用模板：`userId`、旧模板、新模板、旧 run、新 run
- 删除模板：`userId`、`templateId`
- AI 占位接口：`userId`、`useProfileData`、`cycleLength`

禁止输出：

- access token
- 完整 AI prompt 原文
- 过长 `targetExtraJson`

### 10.2 注释要求

当前代码已按以下约定落地：

- Controller 接口方法使用 Javadoc
- Application Service 公开方法使用 Javadoc
- 核心领域服务方法使用简短说明
- 复杂流程如“锁定天复制 + 可编辑天覆盖”保留代码注释

### 10.3 Swagger 约定

- Controller 使用：
  - `@Tag`
  - `@Operation`
  - `@ApiResponses`
  - `@SecurityRequirement(name = "bearerAuth")`
- DTO / VO 使用 `@Schema`
- `targetExtraJson` 提供 JSON 示例

---

## 十一、测试设计与当前覆盖

### 11.1 单元测试

已覆盖：

- `CycleTemplatePolicyServiceTest`
  - 草稿允许 `cycleLength=null`
  - 正式模板不允许 `cycleLength=null`
  - `active` 模板不允许改 `cycleLength`
  - 锁定天不允许修改

### 11.2 集成测试

已覆盖：

- 正式模板列表只返回 `active/inactive`
- 创建草稿支持 `cycleLength=null`
- 更新草稿创建新版本
- 模板切换时未确认会被拒绝
- 模板切换成功后创建新 run 并覆盖 active context
- `active` 模板更新时修改 `cycleLength` 被拒绝
- AI 接口返回 `501`
- 所有受保护接口未登录返回 `401`

### 11.3 当前验证结果

本轮实现已通过：

- `CycleTemplatePolicyServiceTest`
- `CycleTemplateIntegrationTest`
- 全量 `mvn test`

---

## 十二、后续演进建议

### 12.1 AI 接入前置点

后续真正接入 AI 时建议补齐：

- AI 权益校验
- `ai_generation_records` 落库
- prompt 摘要与生成参数追踪
- 生成失败重试与超时策略

### 12.2 与训练打卡模块的后续联动

后续建议让训练打卡模块承担：

- `currentDayIndex` 推进
- 周期完成后的 run 归档
- 以 `training_sessions` 为准锁定已打卡天

### 12.3 可继续增强的规则

- `active` 模板编辑时真正按已打卡天锁定，而不是仅按 `currentDayIndex`
- 提供历史版本浏览与回滚能力
- 接入用户自定义动作库

---

## 十三、结论

当前 `cycle_template` 模块已经以“模板主表 + 版本快照 + 当前激活上下文”的模型完成 MVP 落地，关键实现口径如下：

- 草稿支持 `cycleLength=null`
- 每次保存创建新版本
- `active` 模板只允许影响当前天及未来天
- 模板切换会关闭旧 run、创建新 run、重置到 `Day 1`
- AI 接口当前仅作为 501 占位

本模块后续最重要的演进方向不是改数据模型，而是继续补齐训练打卡联动和 AI 能力。
