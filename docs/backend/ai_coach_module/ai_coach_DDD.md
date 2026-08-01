# DailyForge AI Coach 模块详细设计文档（DDD）

> 版本：v1.0  
> 日期：2026-07-31  
> 模块归属：`backend` 单体应用  
> 目标 Java 包路径：`com.dailyforge.modules.aicoach`  
> 文档状态：待开发实现设计稿

---

## 一、文档说明
### 1.1 上游输入文档

- PRD：[ai_coach_PRD.md](/D:/Computer%20Science/DailyForge/docs/prd/ai_coach_PRD.md)
- 接口文档：[ai_coach_接口文档.md](/D:/Computer%20Science/DailyForge/docs/interfaces/ai_coach_%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3.md)
- AI 接入设计：[AI接入与提示词上下文设计.md](/D:/Computer%20Science/DailyForge/docs/backend/ai_coach_module/AI%E6%8E%A5%E5%85%A5%E4%B8%8E%E6%8F%90%E7%A4%BA%E8%AF%8D%E4%B8%8A%E4%B8%8B%E6%96%87%E8%AE%BE%E8%AE%A1.md)
- 数据库改造清单：[ai_coach_数据库改造清单.md](/D:/Computer%20Science/DailyForge/docs/backend/ai_coach_module/ai_coach_%E6%95%B0%E6%8D%AE%E5%BA%93%E6%94%B9%E9%80%A0%E6%B8%85%E5%8D%95.md)
- 数据库迁移：[V7__ai_coach_schema_upgrade.sql](/D:/Computer%20Science/DailyForge/backend/src/main/resources/db/migration/V7__ai_coach_schema_upgrade.sql)

### 1.2 本文档目标

本文档用于把 `ai_coach` 模块收口为一套可直接指导后端实现、代码评审和后续重构的技术方案，重点明确：

- 模块边界与一期范围
- AI 任务模型、状态机与落库结构
- Controller / Application / Domain / Infrastructure 分层职责
- Tool calling、Prompt、JSON 修复与结果落库流程
- 权限、错误码、事务边界、幂等与并发控制
- Swagger、日志、配置项与测试策略

### 1.3 当前仓库事实

截至本文档编写时，仓库现状如下：

1. `backend` 已有统一基础设施：
   - `ApiResponse`
   - `ErrorCode`
   - `BusinessException`
   - `GlobalExceptionHandler`
   - JWT 鉴权链路与 Spring Security 配置
   - SpringDoc/OpenAPI 基础配置
2. `backend/src/main/java/com/dailyforge/modules/ai` 目前只有占位 `package-info.java`，尚无正式 AI 模块实现。
3. PRD、接口文档、AI 接入设计和数据库改造方案已经完成确认。
4. `V7__ai_coach_schema_upgrade.sql` 已创建，但当前项目运行时 Flyway 仍处于关闭状态，数据库迁移需手动执行。
5. `pom.xml` 当前尚未引入 Spring AI 相关依赖，AI 模块需要在实现阶段补齐。

---

## 二、方案概览
### 2.1 模块定位

`ai_coach` 是 DailyForge 的结构化 AI 能力模块，不提供开放式长对话，而是围绕两个固定场景工作：

1. `template_generation`
   - 根据用户档案、身体指标和本次生成条件，生成一个 `cycle template draft`
2. `cycle_summary`
   - 根据已完成的 `cycle_run` 与训练记录，生成结构化复盘总结

### 2.2 一期交付范围

| 能力 | 说明 | 状态 |
|------|------|:---:|
| AI1 获取 AI 能力状态 | 返回当前用户 AI 权限、资料完整度、可用场景摘要 | 待开发 |
| AI2 提交模板生成任务 | 创建 `template_generation` 异步任务 | 待开发 |
| AI3 查询模板生成任务 | 轮询任务状态并返回草稿模板与设计说明 | 待开发 |
| AI4 提交周期总结任务 | 创建 `cycle_summary` 异步任务 | 待开发 |
| AI5 查询周期总结任务 | 轮询任务状态并返回结构化总结 | 待开发 |
| AI 任务持久化 | 落库 `ai_task_records` 与 `ai_task_tool_calls` | 待开发 |
| AI 模板来源追溯 | 写入 `cycle_template_versions.source_task_id` | 待开发 |

### 2.3 明确不做

- 长对话式 AI 教练
- AI 饮食计划
- AI 自动启用模板
- AI 自动改写当前 active 模板
- AI 自动生成下一轮正式模板
- 用户主动取消 AI 任务
- WebSocket / SSE 推送

---

## 三、数据库设计
### 3.1 本模块直接涉及的表

| 表名 | 角色 |
|------|------|
| `ai_task_records` | AI 异步任务主表 |
| `ai_task_tool_calls` | AI 工具调用明细表 |
| `cycle_template_versions` | AI 生成模板的来源追溯落点 |
| `users` | 账号层 AI 权限、角色与用户归属 |
| `user_profiles` | 模板生成与总结分析所需基础档案 |
| `user_current_body_metrics` | 当前身体指标快照 |
| `cycle_runs` | 周期总结目标对象 |
| `training_sessions` | 周期总结训练会话 |
| `training_session_exercises` | 周期总结动作记录 |
| `training_session_exercise_items` | 周期总结动作执行项 |
| `training_session_exercise_item_metrics` | 周期总结动作参数实际值 |
| `cycle_templates` / `cycle_template_days` / `cycle_day_exercises` / `cycle_day_exercise_items` / `cycle_day_exercise_item_metrics` | 生成模板落库与模板结构读取 |
| `exercises` 及其关联表 | 模板生成时系统动作库筛选与校验 |

### 3.2 `ai_task_records`

表职责：

- 持久化 AI 异步任务主记录
- 管理任务状态机
- 存放请求摘要、输入摘要、最终结构化结果
- 关联模板版本或周期运行对象

关键字段：

| 字段 | 说明 |
|------|------|
| `task_type` | `template_generation` / `cycle_summary` |
| `client_request_id` | 前端去重请求 ID，用于幂等 |
| `related_entity_type` | `cycle_template_version` / `cycle_run` |
| `related_entity_id` | 关联对象主键 |
| `provider` | 模型提供方，如 `deepseek` |
| `model` | 模型名称 |
| `prompt_version` | Prompt 版本号 |
| `request_payload_json` | 接口入参摘要 |
| `input_summary_json` | 整理后的 AI 上下文摘要 |
| `result_json` | 通过校验后的最终结构化结果 |
| `output_preview` | 截断预览文本 |
| `status` | `pending/running/succeeded/failed` |
| `tool_call_count` | 工具调用轮次 |
| `repair_attempt_count` | JSON 修复次数 |
| `latency_ms` | 总耗时 |
| `error_code` | AI 任务错误码 |
| `error_message` | 错误描述 |
| `created_at/started_at/completed_at/updated_at` | 生命周期时间戳 |

索引与约束：

- `uk_ai_task_records_user_task_request (user_id, task_type, client_request_id)`
- `idx_ai_task_records_user_task_created (user_id, task_type, created_at)`
- `idx_ai_task_records_user_status_created (user_id, status, created_at)`
- `idx_ai_task_records_related_entity (related_entity_type, related_entity_id)`

实现约束：

- 不保存完整原始 Prompt
- 不保存完整原始模型对话全文
- `result_json` 只保存系统校验通过后的结果

### 3.3 `ai_task_tool_calls`

表职责：

- 记录每轮工具调用
- 记录工具名、请求摘要、响应摘要、状态与耗时
- 支撑排查 tool calling 链路问题

关键字段：

| 字段 | 说明 |
|------|------|
| `task_id` | 所属 AI 任务 |
| `round_no` | 第几轮调用 |
| `tool_name` | 工具名称 |
| `request_summary_json` | 工具请求摘要 |
| `response_summary_json` | 工具响应摘要 |
| `status` | `succeeded/failed` |
| `latency_ms` | 工具调用耗时 |
| `error_message` | 工具错误摘要 |

索引：

- `idx_ai_task_tool_calls_task_round (task_id, round_no)`
- `idx_ai_task_tool_calls_tool_created (tool_name, created_at)`

### 3.4 `cycle_template_versions.source_task_id`

作用：

- 把 AI 生成出的模板版本反查回对应 AI 任务
- 支撑“生成结果页查看设计说明”和后续效果分析

写入规则：

- AI 生成模板成功时：
  - `source_type = ai_generated`
  - `source_task_id = ai_task_records.id`
- 后续用户手动编辑版本时：
  - `source_task_id = NULL`
  - `source_type` 由 `plan` 模块自己的版本策略控制

### 3.5 关系说明

```text
users
  -> ai_task_records
       -> ai_task_tool_calls
       -> cycle_template_versions (source_task_id)

cycle_runs
  -> training_sessions
      -> training_session_exercises
          -> training_session_exercise_items
              -> training_session_exercise_item_metrics
```

---

## 四、核心业务规则与状态机
### 4.1 权限规则

- 所有 AI 接口都必须登录
- AI 能力默认只对具备权限的账号开放
- 权限判断依赖：
  - `users.account_tier`
  - `users.platform_role`
- 不具备 AI 权限时：
  - 能力查询接口返回 `available=false`
  - 提交接口直接拒绝并返回 `AI_FEATURE_NOT_AVAILABLE`

### 4.2 模板生成资料完整度规则

`template_generation` 最低要求字段：

- `gender`
- `birthDate`
- `heightCm`
- `goalType`
- `trainingLevel`
- `currentWeightKg`

若缺失：

- `GET /api/ai-coach/capabilities` 返回 `templateGeneration.ready=false`
- `POST /api/ai-coach/template-generations` 再次校验并拒绝

### 4.3 周期总结可执行规则

`cycle_summary` 最低硬门槛：

- `cycleRunId` 属于当前用户
- `cycle_runs.status = completed`

资料不完整时：

- 不阻塞任务执行
- 在结果中返回 `dataCompletenessNotice`

### 4.4 AI 任务状态机

状态：

- `pending`
- `running`
- `succeeded`
- `failed`

流转：

```text
pending -> running -> succeeded
pending -> running -> failed
pending -> failed
```

说明：

- `pending`：任务已创建，尚未被执行器消费
- `running`：已进入 AI 链路执行
- `succeeded`：结果校验通过且已完成持久化
- `failed`：权限、工具调用、模型超时、JSON 修复失败或结果校验失败

### 4.5 Tool calling 规则

- 单次 AI 任务最多 6 轮工具调用
- 只开放只读工具
- 模型不得直接写数据库
- 模型不得直接调用模板启用、资料修改、训练修改等写操作

### 4.6 JSON 修复规则

- 首次输出后由后端做结构和业务校验
- 若失败，则把原 JSON 与错误列表回传给模型修复
- 最多修复 2 次
- 超过 2 次仍失败则任务结束为 `failed`

### 4.7 结果落库规则

模板生成：

- 只生成 `draft`
- 生成成功后写入 `cycle_template*` 结构
- `result_json` 保留 `draftTemplate + generationRationale`

周期总结：

- 只写 AI 任务结果，不创建新模板
- `related_entity_type = cycle_run`

---

## 五、API 设计
### 5.1 Base Path 与鉴权

- 外部访问前缀：`/api/ai-coach`
- Controller 建议映射：`/ai-coach`
- 统一返回：`ApiResponse<T>`
- 统一要求 Bearer Token

### 5.2 接口总览

| 编号 | 方法 | 路径 | 作用 |
|------|------|------|------|
| AI1 | GET | `/api/ai-coach/capabilities` | 获取权限、资料完整度、可用场景摘要 |
| AI2 | POST | `/api/ai-coach/template-generations` | 提交模板生成任务 |
| AI3 | GET | `/api/ai-coach/template-generations/{taskId}` | 查询模板生成任务结果 |
| AI4 | POST | `/api/ai-coach/cycle-summaries` | 提交周期总结任务 |
| AI5 | GET | `/api/ai-coach/cycle-summaries/{taskId}` | 查询周期总结任务结果 |

### 5.3 AI1 能力查询

核心实现步骤：

1. 读取当前用户身份
2. 计算 AI 权限可用性
3. 读取基础档案与当前身体指标
4. 计算 `templateGeneration.ready`
5. 查询最近一个 `completed cycle_run`
6. 计算 `cycleSummary.ready`
7. 返回场景摘要

事务边界：

- 只读，无事务要求

### 5.4 AI2 提交模板生成任务

核心实现步骤：

1. 鉴权
2. 校验 `sceneType/goalType/cycleLength/includeCardio`
3. 校验当前账号 AI 权限
4. 校验最低资料完整度
5. 处理 `clientRequestId` 幂等
6. 创建 `ai_task_records(status=pending)`
7. 提交异步执行器
8. 返回任务受理结果

事务边界：

- 创建任务记录与提交执行信号应在同一事务内完成
- 任务真正执行在异步线程内单独开启事务

### 5.5 AI3 查询模板生成任务

核心实现步骤：

1. 鉴权
2. 按 `taskId + userId + taskType=template_generation` 查询任务
3. 返回基础状态
4. 若 `succeeded`，返回 `draftTemplate + generationRationale`

失败语义：

- 任务不存在、类型不匹配、越权访问统一返回 `AI_TASK_NOT_FOUND`

### 5.6 AI4 提交周期总结任务

核心实现步骤：

1. 鉴权
2. 校验当前账号 AI 权限
3. 校验 `cycleRunId` 属于当前用户
4. 校验 `cycle_runs.status=completed`
5. 处理 `clientRequestId` 幂等
6. 创建 `ai_task_records(status=pending, related_entity_type=cycle_run)`
7. 提交异步执行器

事务边界：

- 与 AI2 相同

### 5.7 AI5 查询周期总结任务

核心实现步骤：

1. 鉴权
2. 按 `taskId + userId + taskType=cycle_summary` 查询任务
3. 返回基础状态
4. 若 `succeeded`，返回结构化总结结果

---

## 六、安全设计
### 6.1 当前已存在的安全基础设施

当前仓库已存在：

- [SecurityConfig.java](/D:/Computer%20Science/DailyForge/backend/src/main/java/com/dailyforge/config/SecurityConfig.java)
- [JwtAuthenticationFilter.java](/D:/Computer%20Science/DailyForge/backend/src/main/java/com/dailyforge/infrastructure/security/JwtAuthenticationFilter.java)
- [AuthSecurityUtils.java](/D:/Computer%20Science/DailyForge/backend/src/main/java/com/dailyforge/infrastructure/security/AuthSecurityUtils.java)
- `RestAuthenticationEntryPoint`
- `RestAccessDeniedHandler`

当前放行路径只有：

- `/auth/**`
- `/docs/**`
- `/actuator/health`
- `/error`

结论：

- `ai_coach` 模块所有接口天然走现有 JWT 鉴权链路
- 无需额外修改匿名放行规则

### 6.2 用户身份获取

应用层统一通过：

- `AuthSecurityUtils.getCurrentUserId()`

禁止：

- Controller 直接解析 JWT Claims
- 用前端传入 userId 作为归属判断依据

### 6.3 数据归属控制

必须校验归属的对象：

- `ai_task_records.user_id`
- `cycle_runs.user_id`
- AI 生成产生的模板也必须归当前用户

### 6.4 工具权限边界

AI 暴露工具必须满足：

- 只读
- 只查当前用户可访问数据
- 禁止越权访问其他用户对象
- 禁止返回密码、JWT、邀请码敏感内部信息

### 6.5 日志脱敏策略

允许记录：

- `taskId`
- `userId`
- `taskType`
- `provider`
- `model`
- `promptVersion`
- `toolName`
- `roundNo`
- `repairAttempt`
- `latencyMs`
- `finalStatus`

禁止记录：

- 完整 access token
- 完整原始 prompt
- 完整原始模型全文输出
- 用户长文本备注原文的全量拼接日志

---

## 七、错误码设计
### 7.1 复用现有错误码

- `UNAUTHORIZED`
- `FORBIDDEN`
- `INVALID_ARGUMENT`
- `RESOURCE_NOT_FOUND`
- `INTERNAL_SERVER_ERROR`

### 7.2 建议新增错误码

| 错误码 | HTTP 状态 | 含义 |
|------|------|------|
| `AI_FEATURE_NOT_AVAILABLE` | 403 | 当前账号未开通 AI 能力 |
| `AI_REQUIRED_PROFILE_MISSING` | 400 | 缺少模板生成所需基础档案字段 |
| `AI_REQUIRED_BODY_METRIC_MISSING` | 400 | 缺少模板生成所需身体指标字段 |
| `AI_CYCLE_RUN_NOT_COMPLETED` | 409 | 周期总结目标循环未完成 |
| `AI_TASK_NOT_FOUND` | 404 | AI 任务不存在、类型不匹配或不属于当前用户 |
| `AI_OUTPUT_INVALID` | 500 | AI 输出无法通过结构/业务校验 |
| `AI_SERVICE_TIMEOUT` | 504 | AI 调用超时 |
| `AI_SERVICE_UNAVAILABLE` | 503 | AI 服务不可用 |

### 7.3 错误码使用原则

- 对前端参数问题使用 `INVALID_ARGUMENT`
- 对 AI 前置资料缺失使用专用错误码，便于前端引导补资料
- 对任务查询越权场景统一返回 `AI_TASK_NOT_FOUND`
- `AI_OUTPUT_INVALID` 表示服务端链路失败，不暴露为前端输入错误

---

## 八、Java 代码结构设计
### 8.1 目标包结构

```text
com.dailyforge.modules.aicoach
├─ application
│  ├─ assembler
│  │  └─ AiCoachAssembler.java
│  └─ service
│     ├─ AiCoachCapabilityApplicationService.java
│     ├─ AiTemplateGenerationApplicationService.java
│     ├─ AiCycleSummaryApplicationService.java
│     ├─ AiTaskQueryApplicationService.java
│     └─ AiTaskPersistenceApplicationService.java
├─ domain
│  ├─ model
│  │  ├─ AiTaskStatus.java
│  │  ├─ AiTaskType.java
│  │  ├─ AiRelatedEntityType.java
│  │  └─ AiPromptVersion.java
│  └─ service
│     ├─ AiPermissionDomainService.java
│     ├─ AiReadinessDomainService.java
│     ├─ AiTaskPolicyService.java
│     ├─ AiOutputValidationDomainService.java
│     └─ AiTaskIdempotencyDomainService.java
├─ infrastructure
│  ├─ ai
│  │  ├─ client
│  │  │  ├─ AiModelClient.java
│  │  │  └─ SpringAiOpenAiModelClient.java
│  │  ├─ context
│  │  │  ├─ TemplateGenerationContextBuilder.java
│  │  │  └─ CycleSummaryContextBuilder.java
│  │  ├─ prompt
│  │  │  ├─ TemplateGenerationPromptBuilder.java
│  │  │  ├─ CycleSummaryPromptBuilder.java
│  │  │  └─ AiRepairPromptBuilder.java
│  │  ├─ tool
│  │  │  ├─ AiToolRegistry.java
│  │  │  ├─ AiToolDispatcher.java
│  │  │  └─ handler
│  │  ├─ executor
│  │  │  ├─ AiTaskExecutor.java
│  │  │  └─ AiJsonRepairService.java
│  │  └─ model
│  └─ persistence
│     ├─ entity
│     │  ├─ AiTaskRecordEntity.java
│     │  └─ AiTaskToolCallEntity.java
│     └─ mapper
│        ├─ AiTaskRecordMapper.java
│        └─ AiTaskToolCallMapper.java
└─ interfaces
   ├─ dto
   │  ├─ TemplateGenerationRequest.java
   │  └─ CycleSummaryRequest.java
   ├─ rest
   │  └─ AiCoachController.java
   └─ vo
      ├─ AiCoachCapabilitiesResponse.java
      ├─ AiAsyncTaskAcceptedResponse.java
      ├─ AiTaskResultResponse.java
      ├─ TemplateGenerationTaskResultResponse.java
      └─ CycleSummaryTaskResultResponse.java
```

### 8.2 核心类职责

| 类名 | 职责 |
|------|------|
| `AiCoachController` | 暴露 AI1-AI5 接口，做参数接收、Swagger 注解、统一响应返回 |
| `AiCoachCapabilityApplicationService` | 查询 AI 权限、资料完整度、最近可分析循环摘要 |
| `AiTemplateGenerationApplicationService` | 创建模板生成任务并提交异步执行 |
| `AiCycleSummaryApplicationService` | 创建周期总结任务并提交异步执行 |
| `AiTaskQueryApplicationService` | 轮询查询任务状态与结果 |
| `AiPermissionDomainService` | 判断 AI 能力是否可用 |
| `AiReadinessDomainService` | 计算模板生成和周期总结的 ready 状态 |
| `AiTaskPolicyService` | 统一封装任务状态流转、类型匹配、归属判断 |
| `AiTaskIdempotencyDomainService` | 处理 `clientRequestId` 幂等 |
| `AiOutputValidationDomainService` | 校验 AI 返回 JSON 的结构与业务合法性 |
| `TemplateGenerationContextBuilder` | 组装模板生成场景上下文 |
| `CycleSummaryContextBuilder` | 组装周期总结场景上下文 |
| `AiToolRegistry` | 注册当前场景允许暴露的只读工具 |
| `AiToolDispatcher` | 执行工具调用、记录工具调用明细 |
| `AiTaskExecutor` | 异步消费 AI 任务，驱动模型、工具调用、修复、落库 |
| `AiJsonRepairService` | 封装 JSON 修复两轮流程 |

### 8.3 DTO / VO 清单

Request DTO：

- `TemplateGenerationRequest`
- `CycleSummaryRequest`

Response VO：

- `AiCoachCapabilitiesResponse`
- `AiAsyncTaskAcceptedResponse`
- `AiTaskBaseResponse`
- `TemplateGenerationTaskResultResponse`
- `CycleSummaryTaskResultResponse`
- `GenerationRationaleResponse`
- `AiGeneratedDraftTemplateResponse`

### 8.4 Assembler / Mapping 约定

- 不允许 Controller 直接暴露 Entity
- `AiTaskRecordEntity` 只在 persistence 与 application 内部流转
- 接口响应统一通过 `AiCoachAssembler` 组装
- `generationRationale` 与 `draftTemplate` 必须显式分开映射

### 8.5 Swagger / OpenAPI 注解约定

Controller：

- `@Tag(name = "AI Coach")`
- `@Operation`
- `@ApiResponses`
- `@SecurityRequirement(name = "bearerAuth")`

DTO / VO：

- 统一补 `@Schema`
- 对以下字段给出示例和枚举说明：
  - `taskType`
  - `taskStatus`
  - `sceneType`
  - `goalType`
  - `missingRequiredFields`
  - `basisType`

---

## 九、AI 链路与工具设计
### 9.1 模型接入方案

本模块按已确认方案实现：

- 技术框架：Spring AI
- 客户端协议：OpenAI 兼容接口
- 实际模型：DeepSeek

当前仓库事实：

- `pom.xml` 尚未引入 Spring AI 依赖
- 本模块实现阶段需补充 Spring AI 与 OpenAI 兼容客户端依赖

### 9.2 异步执行模型

MVP 推荐实现：

1. 提交接口在主线程内只负责：
   - 校验
   - 创建任务
   - 提交异步执行
2. 异步执行器负责：
   - 更新 `running`
   - 调用模型
   - 驱动工具调用
   - 校验 / 修复 / 落库
   - 更新最终状态

建议新增：

- `ThreadPoolTaskExecutor aiCoachTaskExecutor`

### 9.3 Tool calling 白名单

本期建议开放：

- `get_user_ai_capability_context`
- `get_user_profile_context`
- `get_user_current_body_metrics_context`
- `search_candidate_exercises`
- `get_exercise_detail`
- `get_template_generation_constraints`
- `get_cycle_run_summary`
- `get_cycle_run_sessions_detail`
- `get_cycle_run_aggregated_analysis`

必须禁止：

- `create_cycle_template_draft`
- `activate_cycle_template`
- `update_profile`
- `update_workout_session`
- 任意写操作工具

### 9.4 Prompt 分层

建议拆分为：

1. `System Prompt`
   - 角色、边界、只返回 JSON 的刚性约束
2. `Scenario Prompt`
   - 当前任务说明、结果结构、禁止事项
3. `Context Payload`
   - 首轮结构化上下文数据

### 9.5 JSON 修复流程

```text
模型返回 JSON
-> 结构校验
-> 业务校验
-> 若失败，生成修复输入
-> 模型修复
-> 再次校验
-> 最多 2 次
```

修复输入必须包含：

- 原始 JSON
- 错误清单
- 正确的 schema 摘要
- “只返回完整 JSON” 的明确约束

---

## 十、事务、一致性与幂等
### 10.1 事务边界

| 方法 | 是否事务 | 说明 |
|------|------|------|
| `getCapabilities` | 否 | 只读 |
| `submitTemplateGenerationTask` | 是 | 创建任务并提交异步执行 |
| `submitCycleSummaryTask` | 是 | 创建任务并提交异步执行 |
| `queryTaskResult` | 否 | 只读 |
| `AiTaskExecutor.execute(taskId)` | 是 | 单次执行链路中的状态更新与结果持久化 |
| `persistGeneratedDraftTemplate` | 是 | 创建模板草稿版本与回写任务关联 |

### 10.2 幂等策略

幂等锚点：

- `uk_ai_task_records_user_task_request`

处理方式：

1. 提交接口收到 `clientRequestId`
2. 先查同一 `user_id + task_type + client_request_id`
3. 若存在：
   - 直接返回已有任务受理信息
4. 若不存在：
   - 创建新任务

### 10.3 并发控制

重点并发点：

- 同一用户重复点击创建同一 AI 任务
- AI 任务与模板落库结果写回
- 同一个 `cycleRunId` 被短时间重复发起总结

建议控制：

- 创建任务阶段依赖唯一索引兜底
- 模板草稿写入阶段使用单事务保证 `cycle_templates` 与 `cycle_template_versions` 一致
- 查询任务结果只看最终 `ai_task_records`，不从前端缓存推导状态

### 10.4 结果一致性

模板生成成功的定义必须同时满足：

1. AI 任务结果结构通过校验
2. `cycle_template*` 草稿写入成功
3. `cycle_template_versions.source_task_id` 回写成功
4. `ai_task_records.status` 更新为 `succeeded`

任一失败：

- 整体回滚
- 任务置为 `failed`

---

## 十一、配置与扩展点
### 11.1 待新增配置项

建议统一挂在：

- `dailyforge.ai`

建议配置结构：

```yaml
dailyforge:
  ai:
    enabled: true
    provider: deepseek
    model: deepseek-chat
    base-url: https://...
    api-key: ${DAILYFORGE_AI_API_KEY:}
    timeout: PT120S
    max-tool-rounds: 6
    max-repair-attempts: 2
    template-generation-prompt-version: template_generation_v1
    cycle-summary-prompt-version: cycle_summary_v1
```

### 11.2 外部依赖

- Spring AI
- OpenAI 兼容模型客户端
- DeepSeek API
- MySQL
- Redis

Redis 在本期不是必需依赖，但可作为后续扩展点：

- AI 限流
- 短期任务去重缓存
- 热门能力结果缓存

### 11.3 后续扩展点

- AI 饮食计划
- AI 基于周期总结直接生成新草稿模板
- AI 任务取消
- AI 历史任务列表与后台运营视图
- 结果版本对比与 Prompt A/B 实验

---

## 十二、测试设计
### 12.1 单元测试重点

- `AiPermissionDomainServiceTest`
  - 账号有无 AI 权限
- `AiReadinessDomainServiceTest`
  - 缺少基础档案字段
  - 缺少体重
  - `cycle_summary` 在无 completed run 下返回 not ready
- `AiTaskPolicyServiceTest`
  - 状态流转合法性
  - 任务类型匹配
  - 任务归属判断
- `AiOutputValidationDomainServiceTest`
  - 模板结构非法
  - 动作不存在
  - `basisType` 非法
  - 总结结果字段缺失

### 12.2 集成测试重点

- AI1 能力查询成功
- AI2 在缺资料时返回专用错误码
- AI2 幂等提交复用已有任务
- AI3 查询运行中任务
- AI3 查询成功任务并返回草稿与设计说明
- AI4 对未完成循环拒绝
- AI4 对已完成循环成功创建任务
- AI5 查询成功总结任务
- 越权查询任务统一返回 `AI_TASK_NOT_FOUND`

### 12.3 安全测试重点

- 未登录访问返回 401
- 无 AI 权限提交任务返回 403
- 当前用户不能查询其他用户任务
- 工具调用不会读取其他用户数据

---

## 十三、实施顺序建议

建议按以下顺序推进：

1. 手动执行 `V7__ai_coach_schema_upgrade.sql`
2. 在 `pom.xml` 中补齐 Spring AI 与模型客户端依赖
3. 创建 `com.dailyforge.modules.aicoach` 模块骨架
4. 实现 `ai_task_records` / `ai_task_tool_calls` Entity 与 Mapper
5. 实现 AI1 能力查询链路
6. 实现 AI2 / AI4 的任务创建、幂等与异步调度
7. 实现 AI3 / AI5 结果查询
8. 实现 `template_generation` 场景执行器
9. 打通草稿模板落库与 `source_task_id` 回写
10. 实现 `cycle_summary` 场景执行器
11. 补错误码、Swagger、日志和测试

---

## 十四、当前发现的实现层冲突与缺口
### 14.1 模块包名冲突

当前仓库已有：

- `com.dailyforge.modules.ai` 占位包

但本轮文档统一目标是：

- `com.dailyforge.modules.aicoach`

建议：

- 直接新建 `aicoach` 模块
- 不在旧 `modules.ai` 里继续扩写，避免与已确认文档命名漂移

### 14.2 `source_type` 语义待统一

当前 `V7__ai_coach_schema_upgrade.sql` 中对 `cycle_template_versions.source_type` 的注释是：

- `manual/ai_generated`

而旧 `cycle_template` DDD 曾扩展过更丰富来源语义。  
建议：

- AI 模块一期只依赖 `manual/ai_generated`
- 若 `plan` 模块后续继续使用更多来源值，需要单独统一数据库注释与模块文档

### 14.3 运行时迁移机制现状

当前 Flyway 已关闭，意味着：

- 生产/本地库不会自动执行 V7
- AI 模块开发前必须手动确认数据库已完成 V7 升级

### 14.4 依赖缺口

当前 `pom.xml` 尚无 Spring AI 依赖。  
这不是文档冲突，但会阻塞正式编码。

---

## 十五、结论

`ai_coach` 模块的一期核心不是“接个模型接口”，而是把 AI 变成一个受系统规则约束的、可追溯、可轮询、可修复、可落库的结构化后端能力。

这份 DDD 的关键收口点是：

1. 以 `ai_task_records + ai_task_tool_calls` 作为 AI 任务真相。
2. 以只读 tool calling + 后端校验兜底作为安全边界。
3. 以 `draftTemplate` 和 `generationRationale` 分离作为模板生成场景的核心结果结构。
4. 以异步任务、幂等索引、两轮 JSON 修复和模板版本追溯作为实现骨架。
5. 明确当前仓库里哪些基础设施已存在，哪些依赖和模块代码仍待新增。

按本文档推进后，`ai_coach` 可以先落地 `template_generation`，再平滑扩展到 `cycle_summary` 与后续 AI 能力。

---

## 十六、2026-08-01 实现补充

### 16.1 默认 timeout 已调整为 `PT120S`

真实 DeepSeek 联调中，`template_generation` 场景会经历：

- 初始模型调用
- 多轮 tool calling
- tool 返回后的后续模型生成

对这类链路，`PT30S` 偏紧，容易把较慢但仍正常处理中的请求直接打成失败。

当前后端实现已将默认配置调整为：

- `dailyforge.ai.timeout = PT120S`

该值用于覆盖 MVP 阶段的模板生成主链路；后续如继续出现慢请求，可再按场景拆分更细的 timeout 策略。

### 16.2 `DeepSeekOpenAiModelClient` 错误分类职责

当前实现中，模型客户端负责把上游失败分为两类对外错误码：

- `AI_SERVICE_TIMEOUT`
  - 读取超时
  - 请求超时
  - 其他可识别的 timeout 场景
- `AI_SERVICE_UNAVAILABLE`
  - HTTP 4xx
  - HTTP 5xx
  - 网络访问异常
  - 无法拿到有效响应

说明：

- 对前端仍保持既有错误码契约稳定
- 更细粒度的诊断信息只保留在后端日志中

### 16.3 AI 调用可观测性补充

本轮实现后，模型调用日志最少应包含：

- `taskId`
- `taskType`
- `stage`
- `provider`
- `model`
- `timeoutMs`
- `httpStatus`
- `responsePreview`
- `rootCause`

其中：

- `stage` 当前至少区分：
  - `initial-generation`
  - `tool-followup`
  - `json-repair`
- `responsePreview` 只允许保留截断摘要
- `rootCause` 用于区分 timeout、DNS、连接失败、HTTP 4xx/5xx 等问题

### 16.4 日志脱敏补充

除第 6.5 节已有约束外，本轮再补充以下禁止项：

- API Key
- 完整上游响应体
- 完整工具调用参数原文

允许记录：

- 响应体截断摘要
- 根因异常类名
- 任务阶段信息

### 16.5 错误码语义补充

需要明确：

- `AI_SERVICE_UNAVAILABLE` 不代表“本次请求未真正调用到 AI”
- 它也可能表示：
  - tool calling 已经发生
  - 部分模型轮次已经成功
  - 失败发生在后续某一次模型调用

因此后续排障必须结合：

- `ai_task_records`
- `ai_task_tool_calls`
- 后端结构化日志

一起判断真实失败点。
