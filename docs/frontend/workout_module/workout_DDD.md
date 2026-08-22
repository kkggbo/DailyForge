# DailyForge Frontend Workout 模块详细设计

> 版本：v1.1
> 日期：2026-08-23
> 模块归属：`frontend/src/features/workout`
> 契约来源：`docs/interfaces/workout_接口文档.md`

## 模块职责

`workout` 承接当前激活循环的实际训练记录。`cycle-template` 定义计划，`workout` 使用后端创建的快照填写实际参数、完成打卡并查看历史；不提供自由训练或已完成记录编辑。

## 目录结构

```text
src/features/workout
├── api/workout.ts
├── components/WorkoutPanel.tsx
├── lib/workout.ts
├── pages/WorkoutPage.tsx
├── pages/WorkoutHistoryDetailPage.tsx
└── types/workout.ts
```

## 数据流

1. `/workout` 先调用 W1 获取 `workspaceState`、真实 `currentDayIndex` 和 `defaultDayIndex`。
2. 仅当 W1 返回 `active` 时，对该默认 Day 自动调用 W2；W2 返回的 `day` 直接用于渲染。
3. Day 导航统一调用 W3，导航处理不调用 W2，因此浏览历史或未来 Day 不会创建 session。
4. 编辑器维护本地字符串输入态，提交时生成全量 `SavePayload`：空文本和空数字显式映射为 `null`，动作、执行项与参数均覆盖 session 快照。
5. W4 与 W5 复用同一 payload。W5 成功后采用 `completedDay` 替换当前详情，保留 `selectedDayIndex`，不跳到 `nextCurrentDayIndex`。
6. W8 成功后重新执行 W1，再自动执行 W2 初始化新的 Day 1；周期总结入口现指向 `/ai-coach/cycle-summary`（以及历史入口 `/ai-coach/history?tab=cycle-summaries`），不再有本地「AI 占位」行为。

## 服务端权威字段

前端不自行推断以下字段：`workspaceState`、`currentDayIndex`、`defaultDayIndex`、`dayState`、`viewMode`、`canInitializeSession`、`sessionStatus`、`sessionType`、`metricUnit`、`cycleRunStatus`。

其中 `viewMode` 是编辑、只读与未来预览的唯一交互依据；前端不会用 Day 索引关系替代它。

## 组件边界

- `WorkoutPage`：W1-W5、W7-W9 编排，页面 loading/error/action 状态。
- `WorkoutPanel`：Day 导航、进行中表单、只读快照和最近记录展示。
- `WorkoutHistoryDetailPage`：W6 只读详情。

## 非目标

- 不新增动作、执行项或参数。
- 不编辑 completed/cancelled session。
- 不实现真实 AI 总结。
- 不在前端推断循环推进或模板快照。