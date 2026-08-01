# DailyForge Frontend AI Coach 模块详细设计

> 版本：v1.0
> 日期：2026-08-01
> 模块归属：`frontend/src/features/ai-coach`
> 契约来源：`docs/interfaces/ai_coach_接口文档.md`

---

## 1. 文档目标

本文档用于定义 `ai_coach` 模块在前端的实现边界、目录结构、页面职责、状态流、API 调用顺序与模块间衔接方式，作为后续前端开发和联调的技术基线。

本文档优先服务以下工作：

- 在现有前端工程中新增正式的 `ai-coach` 业务模块
- 用正式异步任务流替换 `cycle-template` 中旧的 AI 占位实现
- 明确 `ai_coach` 与 `profile`、`cycle_template`、`workout` 的前端衔接
- 为后续页面微调、UI 重构和测试补充提供稳定结构

---

## 2. 模块目标

前端 `ai_coach` 模块不是开放式聊天页面，而是一个结构化 AI 工具模块，当前 MVP 只承接两个固定场景：

1. AI 生成训练模板草稿
2. AI 分析已完成循环并输出周期总结

当前版本的前端设计目标：

1. 让用户在单独的 AI Coach 入口中查看自己是否具备 AI 使用权限
2. 让用户在发起 AI 请求前就能看到资料完整度和缺失项提示
3. 让用户通过异步任务页查看生成中、失败和成功结果，而不是等待同步阻塞
4. 让模板生成结果与 `cycle_template` 模块平滑衔接，让周期总结结果与 `workout` 模块平滑衔接

---

## 3. 模块定位

`ai_coach` 在前端中的定位是“AI 工具调度层”，不是独立数据源。

它依赖其他模块提供的业务上下文：

- `auth`
  - 提供登录态与 `accessToken`
  - 提供 `currentUser.accountTier` 等展示信息
- `profile`
  - 提供资料补录跳转入口
  - 承接 AI 使用前的资料缺失修复
- `cycle_template`
  - 承接 AI 生成出的 `draftTemplate`
  - 用户最终编辑、保存、启用仍在 `cycle_template` 模块完成
- `workout`
  - 提供已完成循环的来源
  - 承接周期总结场景中的历史背景和后续跳转

因此，`ai_coach` 前端本身只负责：

- 能力状态展示
- AI 请求发起
- 异步任务轮询
- 结构化结果展示
- 业务模块之间的跳转编排

它不负责：

- 模板编辑
- 训练记录编辑
- 长对话聊天
- AI 自由问答输入框

---

## 4. 与现有前端的关系

当前仓库还没有正式的 `frontend/src/features/ai-coach` 目录，但已经存在旧的 AI 占位入口：

- `frontend/src/features/cycle-template/components/AiGeneratePanel.tsx`

该组件当前只是旧占位实现，特点是：

- 仍走旧的 `cycle_template` AI 占位接口
- 不符合新的 `ai_coach` 异步任务契约
- 不具备能力检查、资料缺失提示、任务轮询和结构化结果页

因此本模块落地后，前端应完成以下迁移：

1. 新建正式的 `ai-coach` feature
2. 在导航中新增 `AI Coach` 入口
3. 将 `cycle-template` 页面中的 AI 入口改为跳转到 `ai_coach`
4. 不再继续扩展旧的 `AiGeneratePanel.tsx`

---

## 5. 推荐目录结构

```text
src/features/ai-coach
├─ api
│  └─ ai-coach.ts
├─ components
│  ├─ AiCoachCapabilityCard.tsx
│  ├─ AiCoachUnavailableState.tsx
│  ├─ AiCoachMissingFieldsNotice.tsx
│  ├─ TemplateGenerationForm.tsx
│  ├─ TemplateGenerationResult.tsx
│  ├─ GenerationRationalePanel.tsx
│  ├─ CycleSummaryLaunchCard.tsx
│  ├─ CycleSummaryResult.tsx
│  └─ AiTaskStatusPanel.tsx
├─ lib
│  ├─ ai-coach-enums.ts
│  ├─ ai-coach-formatters.ts
│  ├─ ai-coach-mappers.ts
│  ├─ ai-coach-polling.ts
│  └─ ai-coach-storage.ts
├─ pages
│  ├─ AiCoachPage.tsx
│  ├─ TemplateGenerationPage.tsx
│  ├─ TemplateGenerationTaskPage.tsx
│  ├─ CycleSummaryPage.tsx
│  └─ CycleSummaryTaskPage.tsx
└─ types
   └─ ai-coach.ts
```

说明：

- 目录风格保持与现有 `profile`、`workout`、`cycle-template` 一致
- 首版不强制新增 `hooks` 目录，轮询逻辑先放在 `lib/ai-coach-polling.ts`
- 如后续页面逻辑膨胀，再拆出 `useAiTaskPolling` 等 hooks

---

## 6. 路由设计

建议新增以下受保护路由，统一挂在 `ProtectedOutlet` 下：

- `/ai-coach`
- `/ai-coach/template-generation`
- `/ai-coach/template-generation/tasks/:taskId`
- `/ai-coach/cycle-summary`
- `/ai-coach/cycle-summary/tasks/:taskId`

各路由职责如下：

### 6.1 `AiCoachPage`

路由：`/ai-coach`

作用：

- AI Coach 首页
- 加载 `GET /api/ai-coach/capabilities`
- 展示两个能力入口：
  - AI 生成模板
  - AI 分析周期
- 展示可用性、资料完整度和最近可分析循环摘要

### 6.2 `TemplateGenerationPage`

路由：`/ai-coach/template-generation`

作用：

- 承载 AI 模板生成条件表单
- 在页面顶部再次展示当前能力和资料完整度
- 条件满足时允许发起 AI 生成任务

### 6.3 `TemplateGenerationTaskPage`

路由：`/ai-coach/template-generation/tasks/:taskId`

作用：

- 轮询模板生成任务
- 展示 `pending / running / failed / succeeded` 四种任务状态
- 成功后展示：
  - `draftTemplate`
  - `generationRationale`

### 6.4 `CycleSummaryPage`

路由：`/ai-coach/cycle-summary`

作用：

- 承载 AI 周期总结发起入口
- 默认基于 `capabilities.cycleSummary.latestCompletedCycleRunId` 发起分析
- 当前版本不在前端额外实现 completed cycle 列表选择器

### 6.5 `CycleSummaryTaskPage`

路由：`/ai-coach/cycle-summary/tasks/:taskId`

作用：

- 轮询周期总结任务
- 展示结构化总结结果
- 提供跳转回模板页或训练页的入口

---

## 7. 页面入口设计

当前前端应至少提供三个入口：

1. 顶部导航入口
   - 在 `AppShell.tsx` 中新增 `AI Coach`
2. 模板模块入口
   - 在 `cycle-template` 首页的 AI 入口中跳转到 `/ai-coach/template-generation`
3. 训练模块入口
   - 在 `workout` 的循环完成页中跳转到 `/ai-coach/cycle-summary`

入口策略：

- 顶部导航是稳定主入口
- 业务页入口是场景快捷入口
- 无 AI 权限时入口仍可见，但进入后显示明确提示，而不是静默消失

---

## 8. 数据模型设计

## 8.1 能力状态模型

前端应直接消费后端返回的能力结构，不自行推断。

```ts
export type MissingFieldCode =
  | "gender"
  | "birthDate"
  | "heightCm"
  | "goalType"
  | "trainingLevel"
  | "currentWeightKg";
```

```ts
export type TemplateGenerationCapability = {
  available: boolean;
  ready: boolean;
  missingRequiredFields: MissingFieldCode[];
  allowedSceneTypes: ("gym" | "home")[];
  allowedGoalTypes: ("fat_loss" | "muscle_gain" | "health_maintenance")[];
  minCycleLength: number;
  maxCycleLength: number;
};
```

```ts
export type CycleSummaryCapability = {
  available: boolean;
  ready: boolean;
  latestCompletedCycleRunId: number | null;
  latestCompletedAt: string | null;
  recommendedMissingFields: MissingFieldCode[];
};
```

```ts
export type AiCoachCapabilities = {
  aiEnabled: boolean;
  accountTier: string;
  platformRole: string;
  templateGeneration: TemplateGenerationCapability;
  cycleSummary: CycleSummaryCapability;
};
```

## 8.2 异步任务模型

```ts
export type AiTaskType = "template_generation" | "cycle_summary";

export type AiTaskStatus = "pending" | "running" | "succeeded" | "failed";
```

```ts
export type AiTaskAcceptedResponse = {
  taskId: number;
  taskType: AiTaskType;
  taskStatus: AiTaskStatus;
  createdAt: string;
  pollAfterSeconds: number;
};
```

```ts
export type AiTaskBase = {
  taskId: number;
  taskType: AiTaskType;
  taskStatus: AiTaskStatus;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  errorCode: string | null;
  errorMessage: string | null;
};
```

## 8.3 模板生成表单模型

页面表单继续使用字符串态，避免输入过程中被强制转换打断。

```ts
export type TemplateGenerationForm = {
  sceneType: "gym" | "home";
  goalType: "fat_loss" | "muscle_gain" | "health_maintenance";
  cycleLengthText: string;
  includeCardio: boolean;
};
```

提交映射为：

```ts
export type CreateTemplateGenerationPayload = {
  clientRequestId: string;
  sceneType: "gym" | "home";
  goalType: "fat_loss" | "muscle_gain" | "health_maintenance";
  cycleLength: number;
  includeCardio: boolean;
};
```

## 8.4 模板生成结果模型

前端不重新发明模板结构，直接沿用 `cycle_template` 已有的只读展示结构语义。

```ts
export type GeneratedDraftTemplate = {
  templateId: number;
  templateName: string;
  templateStatus: "draft";
  cycleLength: number;
  days: unknown[];
};
```

```ts
export type GenerationRationale = {
  overallDesignSummary: string;
  dayRationales: {
    dayIndex: number;
    dayName: string;
    focusSummary: string;
    rationale: string;
  }[];
  keyExerciseRationales: {
    dayIndex: number;
    exerciseId: number;
    exerciseName: string;
    rationale: string;
  }[];
  intensityRationale: {
    basisType: "historical_performance" | "starting_recommendation";
    summary: string;
  };
  warnings: string[];
};
```

```ts
export type TemplateGenerationTaskResult = {
  draftTemplate: GeneratedDraftTemplate;
  generationRationale: GenerationRationale;
};
```

## 8.5 周期总结请求与结果模型

```ts
export type CreateCycleSummaryPayload = {
  clientRequestId: string;
  cycleRunId: number;
};
```

```ts
export type CycleSummaryTaskResult = {
  cycleRunId: number;
  templateId: number;
  templateName: string;
  runNo: number;
  cycleLength: number;
  executionOverview: string;
  strengths: string[];
  issues: string[];
  causeAnalysis: string[];
  nextCycleSuggestions: string[];
  risks: string[];
  dataCompletenessNotice: string | null;
};
```

---

## 9. API 层设计

`src/features/ai-coach/api/ai-coach.ts` 对外暴露：

| 方法 | 接口 | 作用 |
|------|------|------|
| `getAiCoachCapabilities` | `GET /api/ai-coach/capabilities` | 获取当前用户 AI 能力与资料完整度 |
| `createTemplateGenerationTask` | `POST /api/ai-coach/template-generations` | 发起模板生成任务 |
| `getTemplateGenerationTask` | `GET /api/ai-coach/template-generations/{taskId}` | 查询模板生成任务状态与结果 |
| `createCycleSummaryTask` | `POST /api/ai-coach/cycle-summaries` | 发起周期总结任务 |
| `getCycleSummaryTask` | `GET /api/ai-coach/cycle-summaries/{taskId}` | 查询周期总结任务状态与结果 |

统一约束：

- 所有请求继续复用 `shared/api/http.ts`
- 所有接口都要求 `accessToken`
- 前端不再使用旧的 `generateDraftTemplateByAi` 占位接口

---

## 10. 页面数据流

## 10.1 `AiCoachPage`

初始化请求：

1. `GET /api/ai-coach/capabilities`

页面基于同一份能力数据渲染：

- 模板生成能力卡
- 周期总结能力卡
- 缺失字段提示
- 最近 completed cycle 摘要

说明：

- 首页不提前创建任务
- 首页不加载生成结果或总结结果

## 10.2 `TemplateGenerationPage`

初始化请求：

1. `GET /api/ai-coach/capabilities`

提交流程：

1. 校验 `templateGeneration.available`
2. 校验 `templateGeneration.ready`
3. 用户填写表单
4. `POST /api/ai-coach/template-generations`
5. 成功后跳转 `/ai-coach/template-generation/tasks/:taskId`

说明：

- 如果 `ready = false`，页面应优先展示缺失字段和去补录入口
- 当前版本不允许前端绕过 `ready = false` 强行提交

## 10.3 `TemplateGenerationTaskPage`

初始化请求：

1. `GET /api/ai-coach/template-generations/{taskId}`

轮询规则：

1. 进入页面立即请求一次
2. 若状态为 `pending` 或 `running`，按照 `pollAfterSeconds` 继续轮询
3. 若状态为 `failed` 或 `succeeded`，停止轮询

成功后页面展示：

- 草稿模板预览
- AI 设计说明
- “去编辑草稿”按钮
- “返回 AI Coach”按钮

## 10.4 `CycleSummaryPage`

初始化请求：

1. `GET /api/ai-coach/capabilities`

提交流程：

1. 校验 `cycleSummary.available`
2. 校验 `cycleSummary.ready`
3. 读取 `latestCompletedCycleRunId`
4. `POST /api/ai-coach/cycle-summaries`
5. 成功后跳转 `/ai-coach/cycle-summary/tasks/:taskId`

说明：

- 当前版本不额外拉取 completed cycle 列表
- 当前版本只围绕后端给出的最近一个 completed cycle 工作

## 10.5 `CycleSummaryTaskPage`

初始化请求：

1. `GET /api/ai-coach/cycle-summaries/{taskId}`

轮询规则同模板生成任务页。

成功后页面展示：

- 本轮执行概览
- 做得好的地方
- 主要问题
- 原因分析
- 下一轮建议
- 风险提醒
- 资料完整度提醒

底部动作建议：

- 去模板页
- 返回训练工作台
- 返回 AI Coach 首页

---

## 11. 本地状态设计

当前版本不引入全局状态库，沿用现有项目模式：

- 页面级 `useState`
- 少量 `useEffect`
- 公共逻辑放 `lib`

## 11.1 能力状态

页面级维护：

- `capabilities`
- `isLoadingCapabilities`
- `capabilityError`

## 11.2 表单状态

`TemplateGenerationPage` 页面级维护：

- `form`
- `formErrors`
- `isSubmitting`

## 11.3 任务状态

任务页统一维护：

- `task`
- `isPolling`
- `pollError`
- `lastResolvedAt`

建议把轮询封装成 `startAiTaskPolling` 或 `pollAiTaskUntilTerminal`，放在 `lib/ai-coach-polling.ts`，由页面控制启动和停止。

## 11.4 本地持久化

当前版本只建议做一个轻量能力：

- 记录用户最近访问过的任务页 `taskId`

用途：

- 刷新恢复
- 返回 AI Coach 首页时展示“最近任务”快捷入口

如首版实现压力较大，也可以先不落地本地持久化，只保留 URL 恢复能力。

---

## 12. 前端约束与规则

## 12.1 必须信任后端的字段

前端不得自行推断以下内容：

- `templateGeneration.available`
- `templateGeneration.ready`
- `templateGeneration.missingRequiredFields`
- `cycleSummary.available`
- `cycleSummary.ready`
- `cycleSummary.recommendedMissingFields`
- `taskStatus`
- `generationRationale.intensityRationale.basisType`

## 12.2 模板结果只读边界

`TemplateGenerationTaskPage` 中展示的 `draftTemplate` 只用于：

- 结果页预览
- 跳转编辑前的确认

真正编辑、保存、启用模板时：

- 必须跳转到 `cycle_template` 模块
- 后续操作以 `cycle_template` 正式接口为准

前端不得：

- 直接在 `ai_coach` 结果页写模板编辑逻辑
- 把 `generationRationale` 混入模板保存请求

## 12.3 周期总结只读边界

周期总结结果当前只是结构化建议展示，不直接触发：

- 新模板创建
- 模板自动修改
- 循环自动重启

前端只能提供跳转入口，不自动执行后续动作。

---

## 13. 组件边界设计

## 13.1 `AiCoachCapabilityCard`

职责：

- 展示单个 AI 场景能力摘要
- 承载进入按钮
- 展示 `available / ready` 状态

不负责：

- 发起任务
- 展示复杂结果

## 13.2 `AiCoachMissingFieldsNotice`

职责：

- 将缺失字段编码映射为中文提示
- 提供“去补充资料”按钮

说明：

- 字段文案映射集中放在 `ai-coach-enums.ts`
- 页面不要散落硬编码

## 13.3 `TemplateGenerationForm`

职责：

- 维护本次生成条件表单 UI
- 不直接发请求，只通过 `onSubmit` 回调向页面抛出标准化表单值

## 13.4 `AiTaskStatusPanel`

职责：

- 统一展示任务中间态和失败态
- 供模板生成任务页和周期总结任务页复用

建议展示：

- 当前状态
- 创建时间
- 开始时间
- 完成时间
- 失败错误信息

## 13.5 `TemplateGenerationResult`

职责：

- 展示 AI 生成成功结果
- 内部再拆：
  - 模板预览区
  - 设计说明区

## 13.6 `CycleSummaryResult`

职责：

- 展示结构化总结文本
- 不内嵌额外编辑行为

---

## 14. 错误处理设计

前端继续通过 `ApiRequestError` 识别结构化错误。

当前重点错误码：

- `UNAUTHORIZED`
- `FORBIDDEN`
- `AI_FEATURE_NOT_AVAILABLE`
- `AI_REQUIRED_PROFILE_MISSING`
- `AI_REQUIRED_BODY_METRIC_MISSING`
- `AI_CYCLE_RUN_NOT_COMPLETED`
- `AI_TASK_NOT_FOUND`
- `AI_OUTPUT_INVALID`
- `AI_SERVICE_TIMEOUT`
- `AI_SERVICE_UNAVAILABLE`
- `INVALID_ARGUMENT`

建议错误语义：

- `AI_FEATURE_NOT_AVAILABLE`
  - 当前账号未开通 AI 功能
- `AI_REQUIRED_PROFILE_MISSING`
  - 基础档案不完整，无法生成模板
- `AI_REQUIRED_BODY_METRIC_MISSING`
  - 身体指标不完整，无法生成模板
- `AI_CYCLE_RUN_NOT_COMPLETED`
  - 当前循环尚未完成，暂不能分析
- `AI_TASK_NOT_FOUND`
  - 任务不存在、类型不匹配或无权访问
- `AI_OUTPUT_INVALID`
  - 本次 AI 结果未通过系统校验，请稍后重试
- `AI_SERVICE_TIMEOUT`
  - AI 响应超时，请稍后重试
- `AI_SERVICE_UNAVAILABLE`
  - AI 服务暂不可用

说明：

- 任务页的失败态优先展示后端 `errorCode + errorMessage`
- 页面级再补充中文业务文案映射

---

## 15. 与现有模块的跳转关系

## 15.1 去补录资料

当模板生成资料不完整时，跳转：

- `/profile/ai-completion?scene=ai-plan&redirect=/ai-coach/template-generation`

当周期总结允许继续但建议补资料时，不强制跳转，只展示提示。

## 15.2 去编辑草稿

模板生成成功后，点击“去编辑草稿”跳转：

- `/cycle-templates/:templateId/edit`

## 15.3 从训练页进入总结

当用户在 `workout` 完成循环后，快捷入口可跳转：

- `/ai-coach/cycle-summary`

首版不通过 query 参数传入 `cycleRunId`，统一以 `capabilities.latestCompletedCycleRunId` 为准。

---

## 16. 页面状态设计

每个页面都应显式覆盖以下状态：

## 16.1 `AiCoachPage`

- `loading`
- `error`
- `available`
- `unavailable`

## 16.2 `TemplateGenerationPage`

- `loading capabilities`
- `unavailable`
- `missing required fields`
- `ready`
- `submitting`

## 16.3 `TemplateGenerationTaskPage`

- `loading task`
- `pending`
- `running`
- `failed`
- `succeeded`

## 16.4 `CycleSummaryPage`

- `loading capabilities`
- `unavailable`
- `no completed cycle`
- `ready`
- `submitting`

## 16.5 `CycleSummaryTaskPage`

- `loading task`
- `pending`
- `running`
- `failed`
- `succeeded`

说明：

- 页面状态必须来自真实接口返回，不用前端猜测
- “无权限”和“资料不完整”是业务态，不应与“接口报错”混为一类

---

## 17. 实现顺序建议

建议前端开发顺序如下：

1. 新增 `types/ai-coach.ts`
2. 新增 `api/ai-coach.ts`
3. 新增 `lib/ai-coach-enums.ts`、`formatters.ts`、`polling.ts`
4. 在 `router.tsx` 和 `AppShell.tsx` 中接入 `AI Coach` 路由与导航
5. 实现 `AiCoachPage`
6. 实现 `TemplateGenerationPage`
7. 实现 `TemplateGenerationTaskPage`
8. 实现 `CycleSummaryPage`
9. 实现 `CycleSummaryTaskPage`
10. 回收 `cycle-template` 中旧的 AI 占位入口

原因：

- 先稳住类型和 API 契约
- 再做首页与发起页
- 最后做轮询页和旧入口迁移

---

## 18. 当前版本非目标

当前前端 DDD 明确不包含以下内容：

- 聊天式 AI 页面
- 流式输出 UI
- WebSocket / SSE 推送
- 历史 AI 任务列表页
- 用户主动取消任务
- 多个 completed cycle 手动选择器
- 直接在 AI 结果页内编辑模板
- AI 自动生成下一轮正式模板

---

## 19. 本次设计结论

`ai_coach` 前端模块应作为一个独立的正式业务模块落地，而不是继续寄生在 `cycle_template` 中做占位扩展。

本次设计的核心结论如下：

1. 前端必须围绕 `capabilities + async task` 两层模型实现，而不是同步请求思维。
2. `ai_coach` 只负责发起、轮询、展示和跳转，不负责编辑模板或训练记录。
3. 模板生成结果与设计说明必须分开展示，并保持只读边界。
4. 周期总结当前只做结构化建议展示，不向下游自动执行任何变更。
5. 现有 `cycle-template` 中的旧 AI 占位面板应视为待迁移旧实现，不再继续扩展。

如后续进入实现阶段，本文档可直接作为 `ai_coach` 前端开发与联调的执行基线。
