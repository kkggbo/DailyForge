# AI模板生成优化后端改造清单

## 1. 背景与目标

本次改造围绕 AI 模板生成与历史展示体验优化，目标是让后端补齐必要契约、返回更适合前端展示的数据，并提升 AI 生成过程的可观测性与稳定性。

## 2. 任务优先级

### Must

1. 将 AI 工具调用最大轮次提升到 `50`。
2. 为 AI 最近工具调用补充中文展示名字段。
3. 为 AI 模板生成任务详情补充本次生成条件回显。
4. 为 `cycle_template` 模板列表/详情补充模板来源标识。

### Should

1. 同步更新 `ai_coach` 与 `cycle_template` 相关接口文档。
2. 同步更新 `ai_coach_DDD.md` 与相关后端设计文档。
3. 补充后端测试，覆盖新增字段与映射逻辑。

## 3. 后端改造项

### 3.1 提高 AI 工具调用轮次上限

- 修改 `backend/src/main/resources/application.yml` 中 `dailyforge.ai.max-tool-rounds` 为 `50`。
- 修改 `AiCoachProperties` 默认值为 `50`。
- 检查 `AiConversationService` 的轮次限制判断是否完全读取配置值，避免残留硬编码限制。
- 更新相关文档中关于最大轮次的说明。

### 3.2 为最近工具调用补充中文名

- 在 `AiTaskLatestToolCallResponse` 中新增 `toolDisplayName` 字段。
- 在 `AiCoachAssembler` 中为常见工具名建立映射：
  - `get_user_profile_context`
  - `get_user_current_body_metrics_context`
  - `get_template_generation_constraints`
  - `search_candidate_exercises`
  - `get_exercise_detail`
  - `get_cycle_run_aggregated_analysis`
- 保留原始 `toolName`，前端优先展示 `toolDisplayName`，必要时回退 `toolName`。
- 若后续新增工具，要求同步补齐映射。

### 3.3 回显 AI 模板生成条件

- 在模板生成任务详情响应中补充本次请求快照。
- 推荐返回结构：
  - `sceneType`
  - `goalType`
  - `cycleLength`
  - `includeCardio`
  - `additionalRequirements`
- 若希望最小改动，也可只补 `additionalRequirements`，但建议使用完整请求快照，便于后续扩展。
- 同步更新历史记录接口的字段说明，避免前端误判任务上下文。

### 3.4 暴露模板来源标识

- 在 `cycle_template` 模板列表与模板详情接口中补充来源字段。
- 优先返回后端语义字段 `sourceType`，前端再把 `ai_generated` 映射成“AI生成”。
- 若列表与详情当前只返回业务展示字段，需要检查是否统一补齐：
  - 正式模板列表
  - 草稿模板列表
  - 模板详情
- 该字段应能区分：
  - 手工创建
  - AI 生成

## 4. 文档改造项

- 更新 [docs/interfaces/ai_coach_接口文档.md](../../interfaces/ai_coach_接口文档.md)：
  - `maxToolRounds = 50`
  - `toolDisplayName`
  - `additionalRequirements` / `requestSnapshot`
- 更新 [docs/interfaces/cycle_template_接口文档_v2.md](../../interfaces/cycle_template_接口文档_v2.md)：
  - 模板来源字段说明
- 更新 [docs/backend/ai_coach_module/ai_coach_DDD.md](./ai_coach_DDD.md)：
  - AI 返回结构
  - 工具调用展示语义
  - 任务详情字段说明

## 5. 验证要求

1. `AiConversationService` 不再因为 12 轮限制提前失败。
2. 任务详情接口能返回中文工具名。
3. 模板任务详情能看到本次生成条件。
4. 模板列表/详情能识别 AI 生成来源。
5. 后端测试覆盖新增字段与映射。

## 6. 交付边界

- 不修改前端页面结构。
- 不新增数据库迁移。
- 不重构 AI 任务核心流程，只补展示与契约。
