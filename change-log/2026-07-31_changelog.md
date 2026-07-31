# 2026-07-31 Changelog

## 今日概览

本轮完成了 `ai_coach` 模块的后端首版落地，重点不是直接接入真实模型，而是先把 AI 任务链路、数据库结构、接口契约、结果结构和与 `cycle_template` / `workout` 的数据关系搭稳。当前项目已经具备“提交 AI 模板生成任务”和“提交 AI 周期总结任务”的完整后端闭环，并保留了后续接入 Spring AI + DeepSeek 的清晰扩展点。

## 今日完成内容

### 1. AI Coach 模块后端首版落地

- 新增 `ai_coach` 模块的 Controller、Application Service、Assembler、DTO/VO、配置类、持久层实体与 Mapper。
- 新增 AI 能力概览接口，用于返回 AI 是否可用、模板生成准备度、周期总结准备度及缺失资料提示。
- 新增 AI 训练模板生成任务提交与结果查询接口。
- 新增 AI 周期总结任务提交与结果查询接口。
- AI 模板生成任务当前会真实创建 `cycle_template draft`，并回写 `cycle_template_versions.source_task_id`。
- AI 周期总结任务当前会基于已完成 `cycle_run`、训练快照和模板版本快照生成结构化总结结果。

### 2. 数据库与配置接入

- 新增 `V7__ai_coach_schema_upgrade.sql`，引入 AI 任务记录表、工具调用记录表，以及模板版本来源任务字段。
- `CycleTemplateVersionEntity` 同步增加 `sourceTaskId` 字段，用于追踪 AI 生成草稿来源。
- `application.yml` 增加 AI coach 相关配置项，为后续模型接入预留配置入口。
- `ErrorCode` 增加 AI 相关业务错误码，用于统一响应和前端判定。

### 3. 关键风险修复

- 修复了 AI 任务在事务内创建后立即异步调度导致的竞态问题。
  - 当前改为在事务 `afterCommit` 后再派发异步任务，避免任务卡在 `pending`。
- 修复了 AI 权限判断未覆盖 `admin` 角色的问题。
  - 当前 `admin` 角色即使不是 AI tier，也可以使用 AI 能力。
- 修复了周期总结读取“当前模板主表”导致历史漂移的问题。
  - 当前优先使用 `training_sessions.template_name_snapshot` 和 `cycle_run.templateVersionId` 对应的版本快照。
- 修复了 stub 阶段伪造 tool call 记录的问题。
  - 当前不再写入假 `ai_task_tool_calls`，`toolCallCount` 固定为 `0`。

### 4. 文档补齐

- 新增 `docs/prd/ai_coach_PRD.md`。
- 新增 `docs/interfaces/ai_coach_接口文档.md`。
- 新增 `docs/backend/ai_coach_module/AI接入与提示词上下文设计.md`。
- 新增 `docs/backend/ai_coach_module/ai_coach_数据库改造清单.md`。
- 新增 `docs/backend/ai_coach_module/ai_coach_DDD.md`。
- 更新 `README.md`，同步 `ai_coach` 模块能力、V7 SQL、文档索引与测试状态。

## 验证结果

- 定向编译：`mvn -q -DskipTests compile` 通过。
- 定向测试：`mvn "-Dtest=AiCoachApplicationServiceTest,AiCycleSummaryServiceTest" test` 通过，6/6。
- 后端全量测试：`mvn -q test` 通过。
- 增量代码审查：无高风险、无中风险。

## 今日总结

今天做得正确的地方是，你没有为了“先跑起来”就把 AI 直接硬接到模型，而是先把任务状态流、数据结构、权限规则、历史快照语义和错误码体系做扎实。这对 DailyForge 这种后续会高度依赖 AI 输出可解释性和可追踪性的项目很重要，尤其是 `source_task_id`、异步任务表和历史快照优先这几个点，都是以后排障和迭代的基础。

可以继续优化的地方是，当前 `ai_coach` 仍然是后端 stub 版本，虽然闭环已完整，但还没有真实模型行为，也还缺少“事务提交后才派发”的专门自动化回归测试。下一轮如果进入真实模型接入，最好继续保持现在这种“先把边界和回退机制讲清楚，再接模型”的节奏，不要让模型接入反过来拖乱已有业务结构。

## 后续建议

- 优先做一轮手动验证，重点检查 AI 任务状态链路是否稳定表现为 `pending -> running -> succeeded/failed`。
- 用 `admin + free tier` 账号实际调用一次 AI 能力，确认权限逻辑与预期一致。
- 下一阶段可进入 Spring AI + DeepSeek 的真实接入，并补齐 tool calling、结果格式修复重试和审计摘要策略。