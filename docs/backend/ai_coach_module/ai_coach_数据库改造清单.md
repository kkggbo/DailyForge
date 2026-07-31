# DailyForge ai_coach 数据库改造清单

> 版本：v1.0  
> 日期：2026-07-31  
> 模块归属：`backend` 单体应用，建议代码包为 `com.dailyforge.modules.aicoach`  
> 文档状态：数据库改造清单初稿

---

## 1. 文档目标

本文档用于整理 DailyForge `ai_coach` 模块在 MVP 阶段所需的数据库改造项，重点回答以下问题：

- 现有数据库是否已经能支撑 AI 模块
- 哪些表需要新增
- 哪些旧表需要重命名或扩展
- 哪些字段、索引、关联关系是本期必须
- 哪些改造属于建议项，可视工程节奏决定是否本期落地

本文档只聚焦数据库结构改造，不展开接口、Prompt 或业务流程细节。

配套文档：

- [ai_coach_PRD.md](../../prd/ai_coach_PRD.md)
- [ai_coach_接口文档.md](../../interfaces/ai_coach_接口文档.md)
- [AI接入与提示词上下文设计.md](./AI接入与提示词上下文设计.md)

---

## 2. 当前现状判断

## 2.1 现有业务源数据已经基本够用

`ai_coach` 模块本期依赖的业务数据，当前数据库已经基本具备：

- 用户权限与权益
  - `users`
- 基础档案
  - `user_profiles`
- 当前身体指标
  - `user_current_body_metrics`
- 身体指标历史
  - `body_metric_logs`
- 系统动作库
  - `exercises`
  - `exercise_muscles`
  - `exercise_equipments`
  - `muscles`
  - `equipments`
- 模板结构
  - `cycle_templates`
  - `cycle_template_versions`
  - `cycle_template_days`
  - `cycle_day_exercises`
  - `cycle_template_day_exercise_items`
  - `cycle_template_day_exercise_item_metrics`
- 已完成训练与循环
  - `cycle_runs`
  - `training_sessions`
  - `training_session_exercises`
  - `training_session_exercise_items`
  - `training_session_exercise_item_metrics`

结论：

- `profile`
- `exercise`
- `cycle_template`
- `workout`

这四个模块当前表结构已经足够作为 `ai_coach` 的数据来源，本期不建议为了 AI 再去改这些业务真相表的核心结构。

## 2.2 当前真正不足的是 AI 任务持久化模型

现有库里已经有一张早期预留表：

- `ai_generation_records`

但它目前更像“单次 AI 生成留痕表”，而不是“完整 AI 异步任务表”。

当前问题：

1. 表名语义过窄
   - 现在不仅有模板生成，还有周期总结
   - `generation` 无法准确覆盖 `cycle_summary`
2. 状态语义不完整
   - 当前只抽象为简单调用状态，不足以支撑：
     - `pending`
     - `running`
     - `succeeded`
     - `failed`
3. 缺少异步任务关键字段
   - `client_request_id`
   - `started_at`
   - `completed_at`
   - `error_code`
   - `repair_attempt_count`
   - `tool_call_count`
4. 缺少与模板版本的稳定追溯关系
5. 缺少工具调用明细留痕能力

结论：

- 本期数据库改造的核心，不在用户表或训练表
- 而在 AI 任务表及其追溯模型

---

## 3. 改造原则

### 3.1 AI 任务表不是业务真相表

AI 表的作用是：

- 管理异步任务状态
- 保存结构化输出结果
- 保存摘要级输入输出留痕
- 支撑结果查询、排错、运营分析

AI 表不应该替代：

- 模板真相表
- 循环真相表
- 训练真相表

### 3.2 业务真相仍落在原业务表

对于 `template_generation`：

- AI 任务成功后，真正模板数据仍然写入 `cycle_template*` 相关表

对于 `cycle_summary`：

- 总结结果可以保存在 AI 任务表的 `result_json`
- 但不能反向覆盖训练记录真相

### 3.3 不保存完整原始 Prompt

按当前已确认方案：

- 不保存完整原始 prompt
- 不保存完整原始模型交互全文

数据库只保存：

- 输入摘要
- 输出结构化结果
- 输出截断预览
- promptVersion

### 3.4 优先保留追溯能力

本期最重要的追溯关系：

- 哪个 AI 任务生成了哪个模板版本
- 哪个 AI 任务分析了哪个 `cycle_run`

---

## 4. 核心结论

本期数据库改造建议分为两层：

### 4.1 本期必须改

1. 将 `ai_generation_records` 升级为通用 AI 任务表
2. 支撑异步任务状态机与轮询
3. 为模板生成结果建立“AI 任务 -> 模板版本”的追溯关系

### 4.2 本期建议改

1. 增加 AI 工具调用明细表
2. 增加更细的索引与去重约束

---

## 5. 本期必须改造项

## 5.1 现有 `ai_generation_records` 需要语义升级

### 建议方案

推荐把：

- `ai_generation_records`

重命名为：

- `ai_task_records`

原因：

- 本期不仅有生成任务，还有总结任务
- 未来还可能有饮食建议、AI 改版建议等
- `generation` 这个名字会把表语义锁死在“生成”

### 如果暂时不想改表名

也可以保留旧表名：

- `ai_generation_records`

但这只是“低迁移成本方案”，不是推荐方案。

问题在于：

- 表名会长期与业务语义不一致
- 后续越扩越别扭

### 我对这点的建议

本期既然还在 AI 模块起步阶段，优先建议直接改成：

- `ai_task_records`

这样后面不会背语义包袱。

---

## 5.2 `ai_task_records` 表设计

### 表职责

用于保存：

- AI 异步任务主记录
- 任务状态
- 与业务对象的关联
- 结构化结果
- 摘要级审计信息

### 建议字段

- `id`
- `user_id`
- `task_type`
- `client_request_id`
- `related_entity_type`
- `related_entity_id`
- `provider`
- `model`
- `prompt_version`
- `request_payload_json`
- `input_summary_json`
- `result_json`
- `output_preview`
- `status`
- `tool_call_count`
- `repair_attempt_count`
- `latency_ms`
- `error_code`
- `error_message`
- `created_at`
- `started_at`
- `completed_at`
- `updated_at`

### 字段语义说明

#### `task_type`

建议值：

- `template_generation`
- `cycle_summary`

说明：

- 直接与接口文档中的 `taskType` 对齐
- 不再沿用旧的 `scenario` 命名

#### `client_request_id`

说明：

- 用于同一用户重复点击时的幂等去重
- 可为空
- 非空时参与唯一性约束

#### `related_entity_type` + `related_entity_id`

建议值：

- 对模板生成结果：
  - `related_entity_type = cycle_template_version`
  - `related_entity_id = 新生成的模板版本 ID`
- 对周期总结结果：
  - `related_entity_type = cycle_run`
  - `related_entity_id = 被分析的 cycle_run.id`

说明：

- 用模板版本而不是模板主表做关联，更精确
- 因为 AI 生成对应的是一个具体版本输出，而不是模板这个抽象容器本身

#### `request_payload_json`

说明：

- 保存接口层业务请求体
- 例如模板生成时的：
  - `sceneType`
  - `goalType`
  - `cycleLength`
  - `includeCardio`
- 不保存完整 prompt

#### `input_summary_json`

说明：

- 保存后端整理后的摘要级上下文
- 不保存完整原始 prompt
- 也不建议保存完整训练备注全文

#### `result_json`

说明：

- 保存最终通过系统校验后的结构化结果
- 对 `template_generation`：
  - 包含 `draftTemplate`
  - 包含 `generationRationale`
- 对 `cycle_summary`：
  - 保存总结结果结构

#### `output_preview`

说明：

- 保存截断预览文本
- 用于后台快速查看
- 不代替 `result_json`

#### `status`

建议值：

- `pending`
- `running`
- `succeeded`
- `failed`

说明：

- 与接口文档中的 `taskStatus` 对齐

#### `tool_call_count`

说明：

- 记录本次任务总共调用了多少轮工具

#### `repair_attempt_count`

说明：

- 记录结构修复尝试次数
- 本期理论上最大为 `2`

#### `error_code`

建议用于保存：

- `AI_OUTPUT_INVALID`
- `AI_SERVICE_TIMEOUT`
- `AI_SERVICE_UNAVAILABLE`
- 其他系统内部 AI 失败码

说明：

- 比单独只有 `error_message` 更利于统计与排查

### 建议索引

- 主键：`id`
- 普通索引：`(user_id, task_type, created_at)`
- 普通索引：`(user_id, status, created_at)`
- 普通索引：`(related_entity_type, related_entity_id)`
- 唯一索引：`(user_id, task_type, client_request_id)`

说明：

- MySQL 唯一索引允许多个 `NULL`，因此 `client_request_id` 可选仍然成立

### 建议时间字段策略

- `created_at`
  - 任务创建时间
- `started_at`
  - 实际开始执行时间
- `completed_at`
  - 成功或失败结束时间
- `updated_at`
  - 最近更新时间

### 与当前旧表字段的对应建议

如果当前直接升级旧表，可按以下方式理解：

- `scenario` -> 改为 `task_type`
- `input_json` -> 改为 `input_summary_json`
- `output_json` -> 改为 `result_json`
- 保留：
  - `provider`
  - `model`
  - `prompt_version`
  - `latency_ms`
  - `error_message`

新增：

- `client_request_id`
- `request_payload_json`
- `output_preview`
- `error_code`
- `tool_call_count`
- `repair_attempt_count`
- `started_at`
- `completed_at`
- `updated_at`

---

## 5.3 `cycle_template_versions` 需要增加 AI 来源追溯字段

### 建议新增字段

- `source_task_id`

类型建议：

- `BIGINT UNSIGNED NULL`

外键指向：

- `ai_task_records.id`

### 为什么要加这个字段

模板生成场景中，最终草稿版本是业务真相的一部分。

如果不把模板版本和 AI 任务建立直接关联，后续会出现几个问题：

1. 很难从模板版本反查当时的 AI 设计说明
2. 很难区分这个版本是人工创建还是 AI 创建
3. 很难做模板效果分析与 Prompt 回溯

### 相关约定

- 当模板版本来自 AI 生成时：
  - `source_type = ai_generated`
  - `source_task_id = 对应 ai_task_records.id`
- 当模板版本来自人工创建或编辑时：
  - `source_task_id = NULL`

### 建议索引

- 普通索引：`source_task_id`

---

## 6. 本期建议改造项

## 6.1 建议新增 `ai_task_tool_calls`

### 是否是本期必须

不是产品功能上的必须，但我建议本期就加。

原因：

- 你本期采用的是类智能体 + tool calling
- 如果没有工具调用明细表，很多问题只能靠日志排查
- MVP 初期最容易出问题的不是业务表，而是模型调用链本身

### 表职责

用于记录：

- 本次 AI 任务每一轮调用了哪个工具
- 工具调用是否成功
- 工具调用耗时
- 请求参数摘要
- 返回结果摘要

### 建议字段

- `id`
- `task_id`
- `round_no`
- `tool_name`
- `request_summary_json`
- `response_summary_json`
- `status`
- `latency_ms`
- `error_message`
- `created_at`

### 字段语义

#### `round_no`

说明：

- 表示第几轮工具调用
- 本期默认最多 `6`

#### `tool_name`

建议值示例：

- `get_user_profile_context`
- `get_user_current_body_metrics_context`
- `search_candidate_exercises`
- `get_exercise_detail`
- `get_cycle_run_summary`
- `get_cycle_run_sessions_detail`

#### `request_summary_json`

说明：

- 保存工具参数摘要
- 不建议保存过长原始参数全文

#### `response_summary_json`

说明：

- 保存工具结果摘要
- 对返回很大的训练明细，不建议整段原样持久化

### 建议索引

- 主键：`id`
- 普通索引：`(task_id, round_no)`
- 普通索引：`(tool_name, created_at)`

---

## 6.2 是否需要新增独立“AI 结果表”

本期我不建议新增：

- `ai_template_generation_results`
- `ai_cycle_summary_results`

原因：

1. 当前任务结果已经可以放进 `ai_task_records.result_json`
2. 模板真相已经落在 `cycle_template*`
3. 周期总结当前也没有“复杂列表页 / 历史筛选页”需求

结论：

- 本期先不拆独立结果表
- 后续如果需要做 AI 历史记录中心，再考虑按场景拆结果表

---

## 7. 明确不需要改的表

本期不建议为 AI 单独改以下表：

### 7.1 `users`

原因：

- 当前已有：
  - `platform_role`
  - `account_tier`
- 已足够支撑 AI 权限判断

### 7.2 `user_profiles`

原因：

- 当前字段已覆盖：
  - `gender`
  - `birth_date`
  - `height_cm`
  - `training_level`
  - `goal_type`
  - `injury_notes`

### 7.3 `body_metric_logs` / `user_current_body_metrics`

原因：

- 当前已经足够支撑 AI 读取身体指标
- 本期重点不是补更多身体字段，而是先接入 AI 调用链

### 7.4 `cycle_templates` 主表

原因：

- 模板本体不需要增加 AI 说明字段
- AI 设计说明不应污染模板主结构

### 7.5 `cycle_runs` / `training_sessions` / `training_session_*`

原因：

- 当前已经能提供完整训练明细与循环结果
- AI 只读消费，不需要反向改这批表的结构

---

## 8. 推荐迁移顺序

如果后续写 SQL migration，我建议顺序如下：

1. 处理 `ai_generation_records`
   - 推荐重命名为 `ai_task_records`
   - 并完成字段升级
2. 新增 `ai_task_tool_calls`
3. 修改 `cycle_template_versions`
   - 新增 `source_task_id`
4. 补充相关外键与索引
5. 补充种子数据或注释说明（如果需要）

---

## 9. 推荐落地方案总结

## 9.1 推荐方案

### 必须做

1. 把现有 AI 记录表升级为通用 AI 任务表
2. 支撑异步任务状态、幂等、修复次数、结构化结果持久化
3. 给 `cycle_template_versions` 增加 `source_task_id`

### 建议做

1. 增加 `ai_task_tool_calls`

## 9.2 不推荐方案

### 不推荐继续保留现状不动

原因：

- 当前 `ai_generation_records` 无法准确承载：
  - 异步任务
  - tool calling
  - JSON 修复
  - 模板版本追溯

### 不推荐把 AI 说明直接塞进模板业务表

原因：

- 模板主结构会变脏
- 后续模板人工编辑与 AI 说明会强耦合

### 不推荐一开始就拆很多 AI 子结果表

原因：

- MVP 过重
- 当前没有足够消费场景支撑这些额外表

---

## 10. 我对后续实现的建议

如果下一步就要进入后端开发，我建议优先按这条顺序推进：

1. 先把这份清单确认
2. 基于它写：
   - `V7__ai_coach_schema_upgrade.sql`
3. 再写：
   - `ai_coach_DDD.md`
4. 最后实现：
   - 任务表
   - tool calling
   - template_generation

