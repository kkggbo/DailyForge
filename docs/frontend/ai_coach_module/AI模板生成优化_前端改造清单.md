# AI模板生成优化前端改造清单

> 版本：v1.1  
> 日期：2026-08-23  
> 已于 2026-08-23 复核，现状见 [ai_coach_DDD.md](./ai_coach_DDD.md) 与 [cycle_template_DDD.md](../cycle_template_module/cycle_template_DDD.md)。

## 1. 背景与目标

本次改造目标是让 AI 模板生成、历史展示、模板详情的前端体验更清晰，减少重复状态提示，并与后端新增字段完成契约对齐。

## 2. 任务优先级

### Must

1. 模板列表页展示 AI 生成标识。
2. AI 模板任务详情页成功态简化状态面板。
3. AI 模板任务详情页展示补充要求。
4. 模板详情页改为按 Day 切换浏览。
5. AI 历史页去掉重复状态 badge。
6. 最近工具调用展示中文名。

### Should

1. 同步更新前端类型定义。
2. 补充页面测试与回归测试。

## 3. 前端改造项

### 3.1 模板列表页增加 AI 标识

- 在 [frontend/src/features/cycle-template/components/CycleTemplateCards.tsx](../../../frontend/src/features/cycle-template/components/CycleTemplateCards.tsx) 中：
  - 正式模板卡片右上角显示“AI生成”标识
  - 草稿模板卡片若来源为 AI 生成，同样显示标识
- 判断依据优先使用后端返回的 `sourceType`。
- 标识样式需与当前 Tailwind 视觉风格一致。

### 3.2 AI 模板任务详情页简化成功态

- 在 [frontend/src/features/ai-coach/components/AiTaskStatusPanel.tsx](../../../frontend/src/features/ai-coach/components/AiTaskStatusPanel.tsx) 中：
  - `succeeded` 状态下隐藏“当前阶段”
  - `succeeded` 状态下隐藏“最近工具调用”
  - 去掉重复的“已完成”状态展示
- 在 [frontend/src/features/ai-coach/pages/TemplateGenerationTaskPage.tsx](../../../frontend/src/features/ai-coach/pages/TemplateGenerationTaskPage.tsx) 中：
  - 去掉不必要的 `Draft Preview / 草稿模板预览` 外层包裹卡
  - 成功态保留模板正文与 AI 设计说明

### 3.3 AI 模板任务详情页展示补充要求

- 将后端返回的 `additionalRequirements` 显示在任务详情页。
- 若为空则不展示。
- 建议放在任务基本信息或设计说明附近，便于用户回看本次输入条件。

### 3.4 模板详情页改为 Day 切换模式

- 在 [frontend/src/features/cycle-template/components/CycleTemplateReadOnly.tsx](../../../frontend/src/features/cycle-template/components/CycleTemplateReadOnly.tsx) 中：
  - 将当前“纵向展开所有 Day”的方式改为“点击 Day 卡片切换查看”
  - 默认展示第一个 Day 或当前高亮 Day
  - 保持与 [frontend/src/features/workout/components/WorkoutPanel.tsx](../../../frontend/src/features/workout/components/WorkoutPanel.tsx) 类似的交互节奏
- AI 模板任务详情页的草稿预览先不改该交互。

### 3.5 AI 历史页去重状态 badge

- 在 [frontend/src/features/ai-coach/components/AiTaskHistoryList.tsx](../../../frontend/src/features/ai-coach/components/AiTaskHistoryList.tsx) 中：
  - 终态记录只显示一个状态提示
  - 避免“已完成 + 已完成”或“已失败 + 已失败”的重复展示

### 3.6 最近工具调用展示中文名

- 在 [frontend/src/features/ai-coach/components/AiTaskStatusPanel.tsx](../../../frontend/src/features/ai-coach/components/AiTaskStatusPanel.tsx) 中：
  - 优先展示 `toolDisplayName`
  - 若缺失，再回退到 `toolName`
- 工具名不再直接暴露给普通用户作为主展示文本。

## 4. 类型与测试改造项

- 更新 [frontend/src/features/ai-coach/types/ai-coach.ts](../../../frontend/src/features/ai-coach/types/ai-coach.ts)：
  - 新增 `toolDisplayName`
  - 新增任务详情请求快照字段或 `additionalRequirements`
- 更新 [frontend/src/features/cycle-template/types/cycle-template.ts](../../../frontend/src/features/cycle-template/types/cycle-template.ts)：
  - 新增模板来源字段 `sourceType`
- 补充/更新测试：
  - `TemplateGenerationTaskPage.test.tsx`
  - `AiCoachHistoryPage` 相关测试
  - `CycleTemplateDetailPage` / `CycleTemplateReadOnly` 相关测试

## 5. 依赖关系

1. 后端先返回 `toolDisplayName`、`additionalRequirements`、`sourceType`。
2. 前端再完成类型与页面渲染。
3. 最后补测试，避免字段存在但展示未更新。

## 6. 验证要求

1. AI 模板卡片能显示“AI生成”。
2. AI 模板任务完成后页面更干净，不显示冗余阶段信息。
3. AI 任务历史页状态展示不重复。
4. 模板详情页可按 Day 切换查看。
5. 最近工具调用显示中文。

## 7. 交付边界

- 不修改后端业务逻辑。
- 不改 AI 模板生成结果结构。
- 仅做展示、类型同步和必要测试补齐。
