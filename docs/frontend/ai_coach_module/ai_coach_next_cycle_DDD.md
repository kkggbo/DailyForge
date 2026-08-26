# DailyForge Frontend AI 下一周期模板生成 详细设计

> 版本：v0.2
> 日期：2026-08-25
> 模块归属：`frontend/src/features/ai-coach`
> 契约来源：`docs/interfaces/ai_coach_next_cycle_接口文档.md`
> 关联 PRD：`docs/prd/ai_coach_next_cycle_PRD.md`
> 前置文档：`docs/frontend/ai_coach_module/ai_coach_DDD.md`（`ai_coach` 模块既有 DDD，本文档是其增量扩展）

> 变更记录：v0.2（2026-08-25）——按主控「方案 A」更新：`cycle_templates` 增 `scene_type` / `include_cardio`（迁移 V8），`CycleTemplateDetailResponse` 增 `sceneType` / `includeCardio`，前端预填改为从 `getCycleTemplateDetail` 精确还原四项，删除原「预填来源缺口」中风险。

---

## 1. 文档目标

本文档定义 `next_cycle_generation`（根据上一周期实际表现 + 上轮 AI 周期总结，生成下一周期模板草稿）在前端的实现边界、类型改动、两个触发入口的组件改动方案、生成表单复用、提交与轮询、结果展示，以及目录/文件规划，作为本轮前端开发与联调的技术基线。

本文档是 `docs/frontend/ai_coach_module/ai_coach_DDD.md` 的**增量补充**：它只定义 `next_cycle_generation` 这一新增固定场景在前端的落地方式，不重写 `ai_coach` 模块既有结构。所有既有 `template_generation` / `cycle_summary` 行为与契约保持不变。

核心已确认决策（来自 PRD）：

1. **双入口**：周期总结结果页 + 周期总结历史卡片。
2. **必须有上轮总结**才能生成；无总结时提示先生成周期总结，不发起生成。
3. **预填可编辑**：表单预填上轮模板值，但所有字段用户都可改。
4. **上轮表现取该 cycleRun 自身聚合**（由后端负责，前端只透传 `sourceCycleRunId`）。
5. **隐藏字段**：`sourceCycleRunId` / `sourceSummaryTaskId` 随提交携带，不展示给用户。

---

## 2. 模块定位与职责边界

### 2.1 定位

`next_cycle_generation` 是 `ai_coach` 模块的第三个**结构化固定 AI 场景**，前端职责与其父模块完全一致——它仍然只是「AI 工具调度层」，只负责：

- 从既有周期总结结果/历史定位 `sourceCycleRunId` 与 `sourceSummaryTaskId`
- 以预填可编辑表单发起生成请求
- 轮询异步任务
- 复用既有模板生成结果组件展示草稿
- 跳转 `cycle_template` 编辑草稿

### 2.2 职责边界

`next_cycle_generation` 前端**负责**：

- 两个入口的入口按钮与可用性判断
- 生成表单的预填与提交
- 异步任务轮询与结果展示（复用）
- 错误提示（含 `AI_CYCLE_SUMMARY_REQUIRED`）

`next_cycle_generation` 前端**不负责**：

- 模板编辑、保存、启用（始终跳转 `cycle_template`）
- 自主读取「最近 N 次表现」做上下文组装（由后端基于 `sourceCycleRunId` 聚合）
- 独立的下一周期历史页（本版不做，见 PRD §9）
- 周期总结自动触发下一周期（必须人工点击发起）

---

## 3. 与现有 `ai_coach` feature 的结构关系

新增场景**不加新的 feature 目录**，全部落在既有 `src/features/ai-coach` 内，复用其 `api / components / lib / pages / types` 五层结构，与现有 `template_generation`、`cycle_summary` 平行共存。

| 层 | 复用 | 新增 / 扩展 |
| --- | --- | --- |
| `types/ai-coach.ts` | `TemplateGenerationTaskResult`、`GeneratedDraftTemplate`、`AiTaskBase`、`AiTaskAcceptedResponse` | 新增 payload / capability / snapshot / 任务响应类型；`AiTaskType` 增 `next_cycle_generation` |
| `api/ai-coach.ts` | `request`、`getAiCoachCapabilities` | 新增 `createNextCycleGenerationTask`、`getNextCycleGenerationTask` |
| `components/` | `TemplateGenerationForm`、`TemplateGenerationResult`、`AiTaskStatusPanel`、`AiTaskHistoryList` | 新增 `NextCycleGenerationModal`；扩展 `TemplateGenerationForm`（`initialValues`）、`AiTaskHistoryList`（动作插槽） |
| `lib/` | `ai-coach-polling.ts`（泛型化） | 新增 `next-cycle-generation.ts`（预填构建）；扩展 `ai-coach-enums.ts`（错误码文案） |
| `pages/` | 轮询任务页模式（参考 `TemplateGenerationTaskPage`） | 新增 `NextCycleGenerationTaskPage` |
| `app/router.tsx` | 受保护路由 | 新增 `/ai-coach/next-cycle-generation/tasks/:taskId` |

### 3.1 与 `cycle_template` / `workout` 的衔接

- **`cycle_template`**：预填值来源（`getCycleTemplateDetail`，方案 A 后返回 `sceneType` / `includeCardio`）与草稿编辑跳转目标（`/cycle-templates/:templateId/edit`）。
- **`workout`**：周期总结结果的 `cycleRunId` 即入口 A 的 `sourceCycleRunId`，复用既有的结果链路，无新增接入。

> **后端契约依赖（方案 A，主控已确认）**：`cycle_templates` 新增 `scene_type` / `include_cardio` 两列（迁移 V8），AI 模板生成落库时写入；`CycleTemplateDetailResponse` 新增 `sceneType` / `includeCardio` 字段。前端仅消费该契约，不改 `backend/**` / `db/**`（见 §12.3）。

---

## 4. 新增 / 变更类型（`types/ai-coach.ts`）

> 契约字段以 `docs/interfaces/ai_coach_next_cycle_接口文档.md` 为准。全部字段名 / 类型 / 语义严格对齐接口文档，前端不自行推断。

### 4.1 任务类型枚举扩展

```ts
// AiTaskType 由两项扩为三项
export type AiTaskType =
  | "template_generation"
  | "cycle_summary"
  | "next_cycle_generation";
```

说明：

- 该类型被 `AiTaskBase` / `AiTaskAcceptedResponse` 的泛型约束引用，扩展后既有场景不受影响（既有字面量仍在联合类型中）。

### 4.2 能力字段 `NextCycleGenerationCapability`

```ts
export type NextCycleGenerationCapability = {
  available: boolean;
  ready: boolean;
  latestCompletedCycleRunId: number | null;
  latestCompletedAt: string | null;
  missingReason: string | null;
};
```

`AiCoachCapabilities` 新增字段（向后兼容，旧字段不变）：

```ts
export type AiCoachCapabilities = {
  aiEnabled: boolean;
  accountTier: string;
  platformRole: string;
  templateGeneration: TemplateGenerationCapability;
  cycleSummary: CycleSummaryCapability;
  nextCycleGeneration: NextCycleGenerationCapability;
};
```

> 注：`nextCycleGeneration` 能力字段用于整体可用性展示；表单渲染仍复用 `templateGeneration`（含 `allowedSceneTypes` / `allowedGoalTypes` / 周期范围），因为生成条件语义与模板生成一致。

### 4.3 请求 payload `CreateNextCycleGenerationPayload`

```ts
export type CreateNextCycleGenerationPayload = {
  clientRequestId: string;
  sourceCycleRunId: number;
  sourceSummaryTaskId: number | null;
  sceneType: SceneType;
  goalType: GoalType;
  cycleLength: number;
  includeCardio: boolean;
  additionalRequirements: string | null;
};
```

### 4.4 requestSnapshot 类型

`AiTaskBase` 的 `requestSnapshot` 目前是 `TemplateGenerationRequestSnapshot`。为保留该字段的判别能力，新增下一周期专用 snapshot 并扩展 `AiTaskBase`：

```ts
export type NextCycleGenerationRequestSnapshot = {
  sceneType: SceneType;
  goalType: GoalType;
  cycleLength: number;
  includeCardio: boolean;
  additionalRequirements: string | null;
  sourceCycleRunId: number;
  sourceSummaryTaskId: number | null;
};

// AiTaskBase.requestSnapshot 扩展为联合类型
requestSnapshot?: TemplateGenerationRequestSnapshot | NextCycleGenerationRequestSnapshot | null;
```

> 说明：`requestSnapshot` 仅为结果页的补充要求回显服务（与 `TemplateGenerationTaskPage` 现有逻辑一致）。若实现对 snapshot 字段辨识成本高，本版可只读取 `additionalRequirements`（两类型共有），不强做字段级判别，见 §10 风险说明。

### 4.5 任务响应类型

```ts
export type NextCycleGenerationTaskResponse = AiTaskResponse<
  "next_cycle_generation",
  TemplateGenerationTaskResult
>;
```

结果类型直接复用 `TemplateGenerationTaskResult`（`draftTemplate` + `generationRationale`），不新增结果模型。

### 4.6 表单预填源类型（内部 UI 用）

预填值来自 `getCycleTemplateDetail` 返回的 `CycleTemplateDetailResponse`（方案 A 后已含 `sceneType` / `includeCardio`，见 §3.1）。新增一个纯 UI 的预填模型（不参与网络契约）：

```ts
// lib/next-cycle-generation.ts
export type NextCyclePrefill = {
  templateName: string | null;
  goalType: GoalType | null;
  cycleLength: number | null;
  sceneType: SceneType | null;
  includeCardio: boolean | null;
};
```

> 依赖：该预填模型依赖后端方案 A——`cycle_templates` 新增 `scene_type` / `include_cardio` 两列（迁移 V8），AI 模板生成落库时写入，`CycleTemplateDetailResponse` 新增 `sceneType` / `includeCardio` 字段（见 §3.1）。据此前端可从 `getCycleTemplateDetail` 精确还原 `sceneType` / `includeCardio` / `goalType` / `cycleLength` 四项，预填完整，无默认值回退缺口。

---

## 5. API 层新增（`api/ai-coach.ts`）

| 方法 | 接口 | 作用 |
| --- | --- | --- |
| `createNextCycleGenerationTask(accessToken, payload)` | `POST /api/ai-coach/next-cycle-generations` | 发起下一周期模板生成，返回 `AiTaskAcceptedResponse<"next_cycle_generation">` |
| `getNextCycleGenerationTask(accessToken, taskId)` | `GET /api/ai-coach/next-cycle-generations/{taskId}` | 查询任务状态与结果，返回 `NextCycleGenerationTaskResponse` |

```ts
export function createNextCycleGenerationTask(
  accessToken: string,
  payload: CreateNextCycleGenerationPayload
) {
  return request<AiTaskAcceptedResponse<"next_cycle_generation">>(
    "/ai-coach/next-cycle-generations",
    { method: "POST", accessToken, body: payload }
  );
}

export function getNextCycleGenerationTask(
  accessToken: string,
  taskId: number
) {
  return request<NextCycleGenerationTaskResponse>(
    `/ai-coach/next-cycle-generations/${taskId}`,
    { accessToken }
  );
}
```

统一约束：复用 `shared/api/http.ts` 的 `request`，均需 `accessToken`。`GET /ai-coach/next-cycle-generations/history` 本版不做（PRD §9）。

---

## 6. 生成表单（复用 / 扩展 `TemplateGenerationForm`）

### 6.1 需求对照

- **复用**：既有字段（`sceneType` / `goalType` / `cycleLengthText` / `includeCardio` / `additionalRequirements`）与校验逻辑原样保留。
- **预填可编辑**：新增可选 `initialValues` prop，优先用其初始化表单，替代默认值；用户仍可改任意字段。
- **隐藏字段**：`sourceCycleRunId` / `sourceSummaryTaskId` **不进表单 state**，由外层 `NextCycleGenerationModal` 持有，提交时合并进 payload。

### 6.2 组件改动

`components/TemplateGenerationForm.tsx`：

```ts
type TemplateGenerationFormProps = {
  capability: TemplateGenerationCapability;
  isSubmitting: boolean;
  submitError: string | null;
  initialValues?: Partial<TemplateGenerationFormValues>; // 新增，可选
  onSubmit: (form: TemplateGenerationFormValues) => void;
};
```

初始化逻辑改为：

```ts
const [form, setForm] = useState<TemplateGenerationFormValues | null>(() =>
  createTemplateGenerationForm(capability, initialValues)
);
```

新增一个 mapper（放在 `lib/ai-coach-mappers.ts`），与既有 `createDefaultTemplateGenerationForm` 并列：

```ts
export function createTemplateGenerationForm(
  capability: TemplateGenerationCapability,
  initialValues?: Partial<TemplateGenerationFormValues>
): TemplateGenerationForm | null {
  const base = createDefaultTemplateGenerationForm(capability);
  if (!base) return null;
  if (!initialValues) return base;
  return {
    ...base,
    sceneType: initialValues.sceneType ?? base.sceneType,
    goalType: initialValues.goalType ?? base.goalType,
    cycleLengthText: initialValues.cycleLengthText ?? base.cycleLengthText,
    includeCardio: initialValues.includeCardio ?? base.includeCardio,
    additionalRequirements: initialValues.additionalRequirements ?? ""
  };
}
```

约束与影响：

- 该 prop 为可选，未传时行为与现状完全一致（既有 `TemplateGenerationPage` 调用点不受影响）。
- 表单标题 / 文案若需区分「下一周期」语义，通过传入 `initialValues.templateName` 之外的展示字段或表单文案扩展点处理；默认沿用「设置本次生成条件」。
- `sourceCycleRunId` / `sourceSummaryTaskId` **不**作为 `initialValues` 或表单字段出现，避免污染表单 state。

---

## 7. 两个入口的组件改动方案

### 7.1 入口 A：周期总结结果页 `CycleSummaryTaskPage`

位置：`/ai-coach/cycle-summary/tasks/:taskId`，`succeeded` 且 `result` 存在时的底部动作区（当前只有一个「查看对应模板」链接）。

改动：

- 底部动作行（`<div className="flex flex-wrap gap-3">`）内，在「查看对应模板」链接**旁**新增「生成下一周期模板」按钮。
- 该场景 `succeeded` 必然已有总结，因此按钮**始终展示**（满足决策 2）。
- 点击打开 `NextCycleGenerationModal`，入参：
  - `sourceCycleRunId = task.result.cycleRunId`
  - `sourceSummaryTaskId = 当前页 taskId`（`task.taskId`）
  - `prefillTemplateId = task.result.templateId`（预填来源）
- 页面新增 `useState` 控制 `nextCycleModalOpen`。

```tsx
{task?.taskStatus === "succeeded" && task.result ? (
  <>
    <CycleSummaryResult result={task.result} />
    <div className="flex flex-wrap gap-3">
      <Link to={`/cycle-templates/${task.result.templateId}`} className={primaryClass}>
        查看对应模板
      </Link>
      <button type="button" onClick={() => setNextCycleModalOpen(true)} className={secondaryClass}>
        生成下一周期模板
      </button>
    </div>
  </>
) : null}

<NextCycleGenerationModal
  open={nextCycleModalOpen}
  onClose={() => setNextCycleModalOpen(false)}
  sourceCycleRunId={task?.result?.cycleRunId ?? 0}
  sourceSummaryTaskId={task?.taskId ?? null}
  prefillTemplateId={task?.result?.templateId ?? null}
/>
```

### 7.2 入口 B：周期总结历史卡片 `AiCoachHistoryPage` + `AiTaskHistoryList`

位置：`/ai-coach/history?tab=cycle-summaries`，每个周期总结卡片动作区（当前有一个「查看总结详情」按钮）。

改动分两层：

**(1) `AiTaskHistoryList`（组件，泛型）新增动作插槽 prop：**

```ts
type AiTaskHistoryListProps<TRecord extends HistoryRecordBase> = {
  // ...既有 props
  renderCardActions?: (record: TRecord) => ReactNode; // 新增，可选
};
```

动作区改为横向 flex，把既有任务链接与 `renderCardActions` 并排渲染：

```tsx
<div className="mt-4 flex flex-wrap justify-end gap-2">
  <Link to={getTaskLink(record)} className={actionClass}>
    {taskLinkLabel}
  </Link>
  {renderCardActions?.(record)}
</div>
```

- `renderCardActions` 可选：未提供时行为与现状一致（模板生成历史不受影响）。
- 因 `AiTaskHistoryList` 是泛型组件，它无法访问具体记录类型，因此按钮的具体数据（`cycleRunId` / `templateId`）由**页面层**通过闭包注入。

**(2) `AiCoachHistoryPage`（周期总结 tab）传入 `renderCardActions`：**

```tsx
<AiTaskHistoryList
  // ...既有 props
  renderCardActions={(record) => {
    if (record.taskStatus !== "succeeded") return null; // 仅 succeeded 显示（决策/PRD）
    return (
      <button
        type="button"
        className={secondaryActionClass}
        onClick={() => openNextCycleModal(record)}
      >
        生成下一周期模板
      </button>
    );
  }}
/>
```

页面层新增：

- `const [nextCycleTarget, setNextCycleTarget] = useState<CycleSummaryHistoryItem | null>(null);`
- `openNextCycleModal(record)` 设置目标并打开模态框。
- 渲染 `NextCycleGenerationModal`，入参取自 `nextCycleTarget`：
  - `sourceCycleRunId = record.cycleRunId`
  - `sourceSummaryTaskId = record.taskId`
  - `prefillTemplateId = record.templateId`

> 可见性规则（PRD 5.1 入口 B）：仅对 `succeeded` 的总结卡片显示该按钮；非 succeeded 卡片 `renderCardActions` 返回 `null`，视觉上保持原样。

---

## 8. `NextCycleGenerationModal`（新增组件）

位置：`src/features/ai-coach/components/NextCycleGenerationModal.tsx`。沿用 `ProfileCompletionModal` 的 `open / onClose` 受控模态框模式（同款弹层样式）。

### 8.1 Props

```ts
type NextCycleGenerationModalProps = {
  open: boolean;
  onClose: () => void;
  sourceCycleRunId: number;
  sourceSummaryTaskId: number | null;
  prefillTemplateId: number | null;
};
```

### 8.2 职责与数据流

1. 打开时（`open && accessToken`）并行拉取：
   - `getAiCoachCapabilities(accessToken)` → 取 `capabilities.templateGeneration` 作为表单 capability；`capabilities.nextCycleGeneration` 作为可用性兜底展示。
   - `prefillTemplateId` 存在时调用 `getCycleTemplateDetail(accessToken, prefillTemplateId)` 构建 `NextCyclePrefill`。
2. 构建 `initialValues` 传给 `TemplateGenerationForm`（预填完整，全部来自上轮模板，见 §4.6）：
   - `sceneType` ← 预填值
   - `goalType` ← 预填值
   - `cycleLength` ← 预填值（字符串化 `cycleLength` 为 `cycleLengthText`）
   - `includeCardio` ← 预填值
   - `additionalRequirements` ← 空字符串
   - 若预填拉取失败（非 404 的业务错误），回退到纯默认值，仍允许编辑提交。
3. 表单 `onSubmit(form)` 时：
   - 生成 `clientRequestId`（`crypto.randomUUID()`，与既有生成流程一致）
   - `normalizeOptionalText(form.additionalRequirements)` 归一化
   - 组装 `CreateNextCycleGenerationPayload`（合并 `sourceCycleRunId` / `sourceSummaryTaskId`）
   - 调 `createNextCycleGenerationTask` → 成功后 `onClose()` 并 `navigate(/ai-coach/next-cycle-generation/tasks/${taskId})`
4. 错误处理：
   - 提交失败用 `getAiCoachErrorMessage` 映射（含 `AI_CYCLE_SUMMARY_REQUIRED`，见 §11），经 `submitError` 传给表单展示。
   - 表单内部仍负责字段级校验（周期范围、补充要求长度）。

### 8.3 状态

- `isLoadingPrefill`（预填拉取中）
- `isSubmitting`
- `submitError: string | null`
- 表单能力 `capability`

---

## 9. 提交与轮询

### 9.1 提交

提交即「组装 payload → `createNextCycleGenerationTask` → 跳转任务页」，见 §8.2。幂等依赖 `clientRequestId`（后端去重，与 `template_generation` / `cycle_summary` 一致）。

### 9.2 轮询（polling hook 泛型化）

既有 `lib/ai-coach-polling.ts` 只提供 `isAiTaskTerminal` / `getAiTaskPollDelayMs` 两个纯函数，任务页把轮询逻辑内联在 `useEffect`（`TemplateGenerationTaskPage` / `CycleSummaryTaskPage` 重复）。本轮**泛型化**为一个可复用 hook，供 `NextCycleGenerationTaskPage` 使用：

```ts
// lib/ai-coach-polling.ts
export type UseAiTaskPollingOptions<TTaskType extends AiTaskType, TResult> = {
  accessToken: string | null;
  taskId: number;
  loadTask: (
    token: string,
    taskId: number
  ) => Promise<AiTaskResponse<TTaskType, TResult>>;
};

export function useAiTaskPolling<TTaskType extends AiTaskType, TResult>(
  options: UseAiTaskPollingOptions<TTaskType, TResult>
) {
  // 返回 { task, isLoading, pageError }
  // 内部：进入即请求一次；非终态按 getAiTaskPollDelayMs 递归定时；终态或组件卸载即停止；失败经 getAiCoachErrorMessage 映射
}
```

要点：

- 内部封装既有 `cancelled` 清理、`timerId` 清理、终态判断、初始 loading 逻辑。
- `loadTask` 由调用方注入，泛型区分任务类型与结果类型，天然适配三种场景。
- 本版**只为新任务页使用**该 hook；既有 `TemplateGenerationTaskPage` / `CycleSummaryTaskPage` 是否同步重构为可选低风险项（见 §10 未完成项），避免扩大本轮改动面。

### 9.3 `NextCycleGenerationTaskPage`

位置：`src/features/ai-coach/pages/NextCycleGenerationTaskPage.tsx`，路由 `/ai-coach/next-cycle-generation/tasks/:taskId`。

数据流（与 `TemplateGenerationTaskPage` 同构）：

1. `useAiTaskPolling<"next_cycle_generation", TemplateGenerationTaskResult>`，`loadTask = getNextCycleGenerationTask`。
2. 顶部返回链接：`返回周期总结`（可回 `/ai-coach/cycle-summary/tasks/:sourceSummaryTaskId` 或历史 tab）+ `查看生成历史`（本版历史 tab 暂无下一周期，见 §10，可暂用 `/ai-coach/history`）。
3. `AiTaskStatusPanel` 展示任务中间态 / 失败态。
4. `succeeded` 时复用 `TemplateGenerationResult` 展示 `draftTemplate` + `generationRationale`。
5. 底部动作：`去编辑草稿`（→ `/cycle-templates/:templateId/edit`）+ `返回模板列表`（→ `/cycle-templates`），与模板生成结果页一致。

---

## 10. 风险与契约缺口

| 风险 / 缺口 | 等级 | 说明与对策 |
| --- | --- | --- |
| ~~预填缺 `sceneType` / `includeCardio` 来源~~ | ~~中~~ | **已解决（方案 A）**：`cycle_templates` 增 `scene_type` / `include_cardio`（迁移 V8），`CycleTemplateDetailResponse` 增 `sceneType` / `includeCardio`，预填可精确还原四项，不再回退默认值。见 §3.1 / §4.6。 |
| 独立下一周期历史 tab 暂缺 | 低 | 接口文档明确本版不做 `/history`；任务页的「查看生成历史」暂回落 `/ai-coach/history`（默认模板生成 tab），或隐藏该链接。需在联调时明确。 |
| `requestSnapshot` 联合类型判别成本 | 低 | 本版结果页仅回显 `additionalRequirements`（两类型共有字段），不做字段级判别；若需展示 source 信息再补判别逻辑。 |
| 既有任务页轮询重构范围 | 低 | 泛型 hook 只对新页生效；既有页重构列为可选后续项，避免扩大本轮改动。 |

---

## 11. 错误处理设计

沿用 `ApiRequestError` + `getAiCoachErrorMessage`。在 `lib/ai-coach-enums.ts` 新增一个错误码文案：

```ts
case "AI_CYCLE_SUMMARY_REQUIRED":
  return "请先生成该周期的 AI 总结，再生成下一周期模板。";
```

前端重点处理的错误码（新增高亮 `AI_CYCLE_SUMMARY_REQUIRED`）：

- `AI_FEATURE_NOT_AVAILABLE`（403）
- `RESOURCE_NOT_FOUND` / `AI_TASK_NOT_FOUND`（404）
- `AI_CYCLE_RUN_NOT_COMPLETED`、**`AI_CYCLE_SUMMARY_REQUIRED`**（409）
- `AI_REQUIRED_PROFILE_MISSING`、`AI_REQUIRED_BODY_METRIC_MISSING`、`INVALID_ARGUMENT`（400）
- `AI_OUTPUT_INVALID`、`AI_SERVICE_TIMEOUT`、`AI_SERVICE_UNAVAILABLE`

任务页失败态优先展示后端 `errorCode + errorMessage`，页面级再补中文映射。

---

## 12. 目录 / 文件规划（本轮改动清单）

### 12.1 新增文件

| 文件 | 说明 |
| --- | --- |
| `frontend/src/features/ai-coach/components/NextCycleGenerationModal.tsx` | 生成表单弹层（预填 + 隐藏 source 字段 + 提交跳转） |
| `frontend/src/features/ai-coach/pages/NextCycleGenerationTaskPage.tsx` | 下一周期任务轮询 / 结果展示页 |
| `frontend/src/features/ai-coach/lib/next-cycle-generation.ts` | 预填构建、`NextCyclePrefill` 类型与归一化辅助 |

### 12.2 修改文件

| 文件 | 改动 |
| --- | --- |
| `frontend/src/features/ai-coach/types/ai-coach.ts` | `AiTaskType` 增 `next_cycle_generation`；新增 `NextCycleGenerationCapability`、`CreateNextCycleGenerationPayload`、`NextCycleGenerationRequestSnapshot`、`NextCycleGenerationTaskResponse`；`AiCoachCapabilities` 增 `nextCycleGeneration`；`AiTaskBase.requestSnapshot` 扩展联合 |
| `frontend/src/features/ai-coach/api/ai-coach.ts` | 新增 `createNextCycleGenerationTask`、`getNextCycleGenerationTask` |
| `frontend/src/features/ai-coach/lib/ai-coach-mappers.ts` | 新增 `createTemplateGenerationForm(capability, initialValues?)` |
| `frontend/src/features/ai-coach/lib/ai-coach-enums.ts` | 新增 `AI_CYCLE_SUMMARY_REQUIRED` 文案 |
| `frontend/src/features/ai-coach/lib/ai-coach-polling.ts` | 泛型化 `useAiTaskPolling` |
| `frontend/src/features/ai-coach/components/TemplateGenerationForm.tsx` | 新增可选 `initialValues` prop，初始化走 `createTemplateGenerationForm` |
| `frontend/src/features/ai-coach/components/AiTaskHistoryList.tsx` | 新增可选 `renderCardActions` 动作插槽 |
| `frontend/src/features/ai-coach/pages/AiCoachHistoryPage.tsx` | 周期总结 tab 传入 `renderCardActions`，新增模态框状态与入口 |
| `frontend/src/features/ai-coach/pages/CycleSummaryTaskPage.tsx` | 底部动作区新增「生成下一周期模板」入口与模态框 |
| `frontend/src/app/router.tsx` | 新增 `/ai-coach/next-cycle-generation/tasks/:taskId` 受保护路由 |
| `frontend/src/features/cycle-template/types/cycle-template.ts` | `CycleTemplateDetailResponse` 新增 `sceneType`、`includeCardio` 字段（对接方案 A 后端契约，供预填读取） |

### 12.3 不改动

- `template_generation` / `cycle_summary` 的既有契约、页面行为与类型字段。
- `backend/**`、`db/**`（前端角色禁止触碰）。
- 既有 `TemplateGenerationPage` / `TemplateGenerationTaskPage` / `CycleSummaryPage` 的主体逻辑（轮询 hook 重构为可选）。

---

## 13. 路由设计（新增）

在 `app/router.tsx` 受保护路由区新增：

```tsx
{
  path: "/ai-coach/next-cycle-generation/tasks/:taskId",
  element: <NextCycleGenerationTaskPage />
}
```

说明：

- 本版**不新增**「下一周期生成」的独立发起页路由（发起统一走两个入口的 `NextCycleGenerationModal`，成功后跳转到该任务页）。
- `/ai-coach` 重定向保持指向 `/ai-coach/template-generation`，不变。

---

## 14. 验收标准

1. **入口 A**：周期总结结果页 `succeeded` 时，底部「查看对应模板」旁出现「生成下一周期模板」按钮；点击打开表单。
2. **入口 B**：周期总结历史卡片仅在 `succeeded` 时，在「查看总结详情」旁出现「生成下一周期模板」按钮；非 succeeded 卡片不显示。
3. **表单**：`sceneType` / `goalType` / `cycleLength` / `includeCardio` 按上轮模板值（`getCycleTemplateDetail` 返回）精确预填，**全部可编辑**；`additionalRequirements` 初始为空。
4. **隐藏字段**：`sourceCycleRunId` / `sourceSummaryTaskId` 不在表单 UI 展示，随 payload 正确提交。
5. **无总结防护**：后端返回 `AI_CYCLE_SUMMARY_REQUIRED` 时，前端展示「请先生成该周期的 AI 总结」，不产生重复任务。
6. **提交与轮询**：提交后跳转 `/ai-coach/next-cycle-generation/tasks/:taskId`，按 `pollAfterSeconds` 轮询至终态。
7. **结果展示**：`succeeded` 后复用 `TemplateGenerationResult` 展示 `draftTemplate` + `generationRationale`；产物为 draft，可跳转编辑，不自动启用。
8. **类型同步**：`AiTaskType` / `AiCoachCapabilities` / 新增类型与 `docs/interfaces/ai_coach_next_cycle_接口文档.md` 一致。
9. 前端 `pnpm test` 通过；契约联调校验通过。

---

## 15. 实现顺序建议

1. `types/ai-coach.ts`（类型先行，契约对齐）
2. `api/ai-coach.ts`（API 封装）
3. `lib/`（`ai-coach-enums.ts` 错误码、`ai-coach-mappers.ts` 预填构造、`ai-coach-polling.ts` 泛型 hook、`next-cycle-generation.ts`）
4. `TemplateGenerationForm.tsx`（`initialValues`）
5. `NextCycleGenerationModal.tsx`
6. `AiTaskHistoryList.tsx`（`renderCardActions`）+ `AiCoachHistoryPage.tsx`（入口 B）
7. `CycleSummaryTaskPage.tsx`（入口 A）
8. `NextCycleGenerationTaskPage.tsx` + `router.tsx`
9. 前端测试补充 + 契约联调校验

原因：

- 先稳住类型与 API 契约，再扩展表单，最后落地两个入口与任务页，与既有 `ai_coach_DDD.md` 的顺序策略一致。

---

## 16. 设计结论

`next_cycle_generation` 前端不引入新结构，全部落在既有 `ai-coach` 五层架构内：

1. **双入口**分别扩展 `CycleSummaryTaskPage` 底部动作区与 `AiTaskHistoryList` 动作插槽，统一收敛到 `NextCycleGenerationModal`。
2. **生成表单**通过给 `TemplateGenerationForm` 增加可选 `initialValues` 实现预填可编辑，`sourceCycleRunId` / `sourceSummaryTaskId` 作为隐藏字段由弹层携带。
3. **提交与轮询**复用异步任务模型，新增泛型 `useAiTaskPolling` hook 供任务页使用。
4. **结果展示**完全复用 `TemplateGenerationResult`。
5. **预填来源**已按主控「方案 A」收敛：`cycle_templates` 增 `scene_type` / `include_cardio`（迁移 V8），`CycleTemplateDetailResponse` 增 `sceneType` / `includeCardio`，前端从 `getCycleTemplateDetail` 精确还原四项预填值，无默认值回退缺口。前端不触碰 `backend/**` / `db/**`。
