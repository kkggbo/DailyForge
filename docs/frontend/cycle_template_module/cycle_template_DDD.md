# DailyForge Frontend Cycle Template 模块详细设计 v2

> 版本：v2.0  
> 日期：2026-07-15  
> 模块归属：`frontend/src/features/cycle-template`

---

## 1. 文档目标

本文档用于定义 `cycle_template` 模块在“动作参数模型重构”后的前端改造方案，面向以下工作：

- 基于新版接口契约重构前端类型、API 和编辑器模型
- 明确现有前端实现与后端 v2 能力之间的差距
- 指导 `cycle-template` 模块下一轮前端开发与重构
- 为后续 `training session` 模块复用三层动作结构提供前置约束

本文档优先级说明：

1. 以 [cycle_template_接口文档_v2.md](/D:/Computer%20Science/DailyForge/docs/interfaces/cycle_template_%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3_v2.md) 为主，作为前端对接契约
2. 以 [动作参数模型改造草案.md](/D:/Computer%20Science/DailyForge/docs/%E5%8A%A8%E4%BD%9C%E5%8F%82%E6%95%B0%E6%A8%A1%E5%9E%8B%E6%94%B9%E9%80%A0%E8%8D%89%E6%A1%88.md) 理解三层模型、交互抽象与扩展边界
3. 以 [cycle_template_PRD.md](/D:/Computer%20Science/DailyForge/docs/prd/cycle_template_PRD.md) 保证页面流程与业务行为不跑偏

---

## 2. 现状与改造原因

当前前端 `cycle-template` 模块已经完成 v1 版本，具备以下能力：

- 模板首页、详情页、创建页、编辑页、启用/复制/删除流程
- 草稿与正式模板的基础管理
- 当前启用模板摘要展示
- 运行中模板的“未来训练日可编辑”限制

但当前前端实现仍然基于 v1 固定动作字段模型，核心问题如下：

1. 一个动作仍使用固定字段描述目标，例如 `targetSets`、`targetRepsMin`、`targetWeightKg`
2. 无法表达“同一动作不同组参数不同”的真实训练结构
3. 无法自然表达有氧动作、单段动作和未来多段动作
4. 编辑器仍是“单层动作表单”，不是“动作 -> 执行项 -> 指标”的结构编辑器
5. 系统动作搜索结果没有消费 `defaultStructureType`

因此，v2 前端改造的本质不是“字段换名字”，而是把编辑器和数据模型整体升级为三层结构。

---

## 3. 模块定位

`cycle_template` 在前端中承担三层职责：

1. 训练模板管理入口
2. 三层动作结构编辑器承载层
3. 与未来 `training session` 计划结构对齐的前置模块

这意味着：

- 本模块不只是普通 CRUD 页面
- 本模块的编辑器结构设计会直接影响后续训练打卡模块
- 本次重构应优先保证结构语义清晰，而不是追求表单实现最省事

---

## 4. 本次改造范围

### 4.1 包含

- 重构 `cycle-template` 类型定义
- 重构 API 请求/响应结构
- 重构编辑器本地表单模型
- 重构动作编辑 UI 为三层结构
- 支持 `set_based` 与 `single_segment`
- 接入新版错误码
- 接入系统动作返回的 `defaultStructureType`
- 重写前端校验逻辑
- 更新列表页、详情页、创建页、编辑页文档设计

### 4.2 不包含

- `training session` 模块开发
- AI 真正生成模板
- `interval_segment`、`dropset`、`superset` 等高阶结构
- 用户自定义参数字典
- 用户手工修改 `structureType`

---

## 5. 路由与页面结构

本次改造不新增路由，继续沿用现有四条受保护路由：

- `/cycle-templates`
- `/cycle-templates/create`
- `/cycle-templates/:templateId`
- `/cycle-templates/:templateId/edit`

页面职责保持不变，但编辑区语义升级：

### 5.1 `CycleTemplatePage`

作用：

- 正式模板 / 草稿模板列表入口
- 展示当前激活模板摘要
- 提供“新建草稿”与“AI 生成草稿”入口

本页受 v2 影响较小，主要变化在于：

- 复制、详情、编辑后进入的新页面将展示三层动作结构
- 列表页不需要感知动作层级细节

### 5.2 `CycleTemplateDetailPage`

作用：

- 只读展示模板完整结构

v2 变化：

- `exercises` 不再展示固定目标字段
- 改为展示：
  - 动作
  - 执行项列表
  - 每个执行项的指标列表

### 5.3 `CycleTemplateCreatePage`

作用：

- 创建手动草稿模板

v2 变化：

- 不再创建“固定字段动作卡”
- 改为创建“三层结构动作卡”

### 5.4 `CycleTemplateEditPage`

作用：

- 编辑草稿模板
- 编辑未启用正式模板
- 编辑运行中模板的当前天及未来天

v2 变化最大，核心在于：

- 支持动作结构初始化
- 支持执行项增删
- 支持指标增删
- 支持根据结构类型限制编辑行为

---

## 6. 当前前端实现与 v2 的差距

基于当前代码，主要差距如下：

### 6.1 类型层

当前 [`cycle-template.ts`](/D:/Computer%20Science/DailyForge/frontend/src/features/cycle-template/types/cycle-template.ts) 仍定义为：

- `CycleTemplateExerciseResponse` 直接挂固定目标字段
- `SaveCycleTemplateExercisePayload` 直接发固定目标字段
- `EditorExerciseForm` 仍是固定字段输入模型

这些类型需要整体替换。

### 6.2 编辑器层

当前 [`CycleTemplateEditor.tsx`](/D:/Computer%20Science/DailyForge/frontend/src/features/cycle-template/components/CycleTemplateEditor.tsx) 仍是：

- 一个动作卡
- 一组固定数值输入
- 一个备注输入

这与 v2 三层结构完全不匹配，需要重构为组合式编辑器。

### 6.3 映射与校验层

当前：

- `detailToEditorForm` 基于固定字段映射
- `editorFormToPayload` 基于固定字段组包
- `cycle-template-validators.ts` 基于固定字段校验

这些逻辑都需要替换。

### 6.4 系统动作搜索层

当前动作搜索结果只消费：

- `exerciseId`
- `exerciseName`

而 v2 需要最少消费：

- `exerciseId`
- `exerciseName`
- `defaultStructureType`

否则前端无法根据动作元数据正确初始化编辑结构。

---

## 7. 推荐目录结构

继续复用现有目录，不做模块拆散，但内部职责重组为：

```text
src/features/cycle-template
├─ api
│  └─ cycle-template.ts
├─ components
│  ├─ CycleTemplateCards.tsx
│  ├─ CycleTemplateDialogs.tsx
│  ├─ CycleTemplateReadOnly.tsx
│  ├─ CycleTemplateEditor.tsx
│  ├─ ExerciseStructureEditor.tsx
│  ├─ ExerciseItemEditor.tsx
│  ├─ MetricEditor.tsx
│  └─ AiGenerateButton.tsx
├─ hooks
│  └─ useCycleTemplateEditor.ts
├─ lib
│  ├─ cycle-template-enums.ts
│  ├─ cycle-template-formatters.ts
│  ├─ cycle-template-mappers.ts
│  ├─ cycle-template-metric-config.ts
│  └─ cycle-template-validators.ts
├─ pages
│  ├─ CycleTemplatePage.tsx
│  ├─ CycleTemplateDetailPage.tsx
│  ├─ CycleTemplateCreatePage.tsx
│  └─ CycleTemplateEditPage.tsx
└─ types
   └─ cycle-template.ts
```

说明：

- 可以保留现有文件名，避免路由与导入大面积波动
- 但建议新增结构化子组件，否则 `CycleTemplateEditor.tsx` 会过大
- `cycle-template-metric-config.ts` 用于维护前端指标字典和结构类型规则

---

## 8. v2 数据模型设计

## 8.1 后端响应模型

前端应以三层结构定义响应类型。

```ts
export type StructureType = "set_based" | "single_segment";

export type ItemType = "set" | "segment";

export type MetricKey =
  | "weight_kg"
  | "reps"
  | "duration_seconds"
  | "distance_km"
  | "speed_kmh"
  | "pace_seconds_per_km"
  | "incline_percent"
  | "rest_seconds"
  | "rpe"
  | "intensity_level";
```

```ts
export type CycleTemplateMetricResponse = {
  sortOrder: number;
  metricKey: MetricKey;
  metricValueNumber: number;
  metricUnit: string | null;
};
```

```ts
export type CycleTemplateItemResponse = {
  itemIndex: number;
  itemType: ItemType;
  itemName: string | null;
  note: string | null;
  metrics: CycleTemplateMetricResponse[];
};
```

```ts
export type CycleTemplateExerciseResponse = {
  sortOrder: number;
  exerciseId: number;
  exerciseName: string;
  structureType: StructureType;
  note: string | null;
  items: CycleTemplateItemResponse[];
};
```

### 8.2 后端请求模型

请求体中不再出现固定目标字段。

```ts
export type SaveCycleTemplateMetricPayload = {
  sortOrder: number;
  metricKey: MetricKey;
  metricValueNumber: number;
};
```

```ts
export type SaveCycleTemplateItemPayload = {
  itemIndex: number;
  itemType: ItemType;
  itemName: string | null;
  note: string | null;
  metrics: SaveCycleTemplateMetricPayload[];
};
```

```ts
export type SaveCycleTemplateExercisePayload = {
  sortOrder: number;
  exerciseId: number;
  structureType: StructureType;
  note: string | null;
  items: SaveCycleTemplateItemPayload[];
};
```

### 8.3 系统动作搜索模型

v2 必须升级：

```ts
export type SystemExerciseOption = {
  exerciseId: number;
  exerciseName: string;
  defaultStructureType: StructureType;
};
```

前端不应再依赖：

- `exerciseType`
- `movementType`
- `defaultUnit`

来推测结构类型。

---

## 9. 前端编辑器表单模型

前端编辑器应维护独立于接口 DTO 的本地表单结构，继续全部使用字符串承接输入态。

### 9.1 顶层表单

```ts
export type CycleTemplateEditorForm = {
  templateName: string;
  goalType: string;
  cycleLength: string;
  days: EditorDayForm[];
};
```

### 9.2 Day 表单

```ts
export type EditorDayForm = {
  dayIndex: number;
  dayName: string;
  exercises: EditorExerciseForm[];
};
```

### 9.3 动作表单

```ts
export type EditorExerciseForm = {
  localId: string;
  sortOrder: number;
  exerciseId: number | null;
  exerciseName: string;
  structureType: StructureType | null;
  note: string;
  items: EditorItemForm[];
};
```

### 9.4 执行项表单

```ts
export type EditorItemForm = {
  localId: string;
  itemIndex: number;
  itemType: ItemType;
  itemName: string;
  note: string;
  metrics: EditorMetricForm[];
};
```

### 9.5 指标表单

```ts
export type EditorMetricForm = {
  localId: string;
  sortOrder: number;
  metricKey: MetricKey | "";
  metricValueNumberText: string;
};
```

说明：

- `metricUnit` 不进入表单提交模型
- 单位由 `metricKey` 推导显示
- 数值输入保留字符串态，避免输入中间态被强转中断

---

## 10. 结构规则与前端约束

## 10.1 `structureType` 规则

前端不允许用户手动修改 `structureType`。

来源规则：

- 用户选择系统动作
- 系统动作返回 `defaultStructureType`
- 前端用该值初始化动作结构
- 提交时原样带上 `structureType`

### 10.2 `set_based` 规则

前端约束：

- `itemType` 只能为 `set`
- 至少 1 个执行项
- 允许多个执行项
- 每个执行项至少 1 个指标

默认初始化建议：

- 新增动作后自动创建 1 个 `set`
- 默认 `itemName = "第1组"`

### 10.3 `single_segment` 规则

前端约束：

- `itemType` 只能为 `segment`
- 只能有 1 个执行项
- 不允许新增第 2 个执行项
- 默认 `itemIndex = 1`

默认初始化建议：

- 新增动作后自动创建 1 个 `segment`
- 默认 `itemName = "主训练段"`

### 10.4 指标规则

前端约束：

- 同一执行项下 `metricKey` 不允许重复
- 每个执行项至少保留 1 个指标
- 当前 MVP 所有指标都必须提交 `metricValueNumber`

---

## 11. 指标字典配置设计

建议新增 `lib/cycle-template-metric-config.ts`，统一维护以下信息：

```ts
type MetricMeta = {
  key: MetricKey;
  label: string;
  unitLabel: string | null;
  step: string;
  min?: number;
  max?: number;
  allowedStructureTypes: StructureType[];
};
```

推荐首批配置：

- `weight_kg`
- `reps`
- `duration_seconds`
- `distance_km`
- `speed_kmh`
- `pace_seconds_per_km`
- `incline_percent`
- `rest_seconds`
- `rpe`
- `intensity_level`

前端用途：

1. 指标下拉选项
2. 单位显示
3. 数值步进控制
4. 范围校验
5. 根据结构类型过滤可选项

建议第一版过滤策略：

- `set_based` 默认优先展示：`weight_kg`、`reps`、`duration_seconds`、`rest_seconds`、`rpe`
- `single_segment` 默认优先展示：`duration_seconds`、`distance_km`、`speed_kmh`、`pace_seconds_per_km`、`incline_percent`、`intensity_level`

---

## 12. 映射层设计

`cycle-template-mappers.ts` 需要整体重写。

## 12.1 详情转表单

新增：

- `detailToEditorForm(detail)`
- `exerciseResponseToEditorForm(exercise)`
- `itemResponseToEditorForm(item)`
- `metricResponseToEditorForm(metric)`

### 12.2 表单转请求

新增：

- `editorFormToPayload(form, options)`
- `mapExerciseToPayload(exercise, index)`
- `mapItemToPayload(item, index)`
- `mapMetricToPayload(metric, index)`

规则：

- `sortOrder`、`itemIndex` 前端在提交前统一重排
- `metricValueNumberText` 转为 `number`
- `itemName`、`note`、`dayName` 空串转 `null`

### 12.3 选择动作后的初始化

新增：

- `createExerciseFromSystemOption(option, sortOrder)`
- `createDefaultItemByStructureType(structureType)`

行为：

- 选中 `set_based` 动作时自动生成 1 个 `set`
- 选中 `single_segment` 动作时自动生成 1 个 `segment`

---

## 13. 校验层设计

`cycle-template-validators.ts` 需要从“固定字段校验”切换到“结构校验”。

### 13.1 顶层校验

- `templateName` 必填，最长 128
- `goalType` 最长 32
- `cycleLength` 草稿可空，非空时必须在 1 到 7

### 13.2 Day 校验

- `dayName` 最长 64
- `dayIndex` 不得超出 `cycleLength`

### 13.3 动作校验

- `exerciseId` 必填
- `structureType` 必填
- `note` 最长 500
- `items` 至少 1 个

### 13.4 执行项校验

- `itemType` 必须与 `structureType` 匹配
- `itemName` 最长 64
- `note` 最长 500
- `metrics` 至少 1 个
- `single_segment` 下执行项数量必须等于 1

### 13.5 指标校验

- `metricKey` 必填
- 同 item 下不得重复
- `metricValueNumberText` 必须可转为数值
- 按配置检查最小值、最大值、步进语义

### 13.6 错误摘要

继续保留当前“顶部错误摘要”能力，但映射路径要升级，例如：

- `templateName`
- `day.3.dayName`
- `day.2.exercise.xxx.structureType`
- `day.2.exercise.xxx.item.yyy.metrics.zzz.metricKey`

建议把错误摘要描述成人能读懂的路径：

- `Day 2 · 动作 1 · 第1组 · 重量`
- `Day 4 · 动作 2 · 主训练段 · 时长`

---

## 14. 编辑器交互设计

## 14.1 顶层布局

编辑页继续保持：

1. 基础信息区
2. Day Tab 区
3. 当前 Day 内容编辑区
4. 底部操作区

### 14.2 动作卡布局

每个动作卡建议包含：

- 动作标题区
- 搜索/重新选择动作区
- 结构类型说明标签
- 动作备注
- 执行项列表
- “新增执行项”按钮

说明：

- `single_segment` 动作隐藏“新增执行项”按钮
- `set_based` 动作显示“新增一组”按钮

### 14.3 执行项编辑区

每个执行项卡建议包含：

- 执行项名称
- 执行项备注
- 指标列表
- “新增指标”按钮
- 上移 / 下移 / 删除

### 14.4 指标编辑区

每个指标行建议包含：

- 指标类型下拉
- 数值输入框
- 单位展示
- 删除按钮

不建议：

- 让用户手填 `metricKey`
- 让用户手填 `metricUnit`

---

## 15. 页面级行为设计

## 15.1 首页

首页请求保持：

- `GET /api/cycle-templates/formal`
- `GET /api/cycle-templates/drafts`
- `GET /api/cycle-templates/active/current`

本页仅做轻量调整：

- “AI 生成草稿”继续只是按钮入口
- 不在首页承载三层结构编辑

## 15.2 详情页

详情页要从“固定字段卡片”改成“结构树”展示：

- Day
- 动作
- 执行项
- 指标

### 15.3 创建页

创建页流程：

1. 先填模板基础信息
2. 切换 Day
3. 在 Day 下新增动作
4. 选择动作后自动生成默认结构
5. 完善执行项和指标
6. 保存草稿

### 15.4 编辑页

编辑页流程与创建页一致，但要额外处理：

- `active` 模板的锁定日只读
- `inactive` 和 `draft` 可完整编辑

---

## 16. `useCycleTemplateEditor` 重构设计

当前 hook 只支持：

- 动作增删
- 固定字段更新
- 简单排序

v2 需要扩展为：

### 16.1 顶层能力

- 更新基础字段
- 更新 day 名称
- 新增动作
- 删除动作
- 选择系统动作并初始化结构

### 16.2 执行项能力

- 新增执行项
- 删除执行项
- 更新执行项
- 执行项排序

### 16.3 指标能力

- 新增指标
- 删除指标
- 更新指标
- 指标排序

### 16.4 历史能力

- `undo`
- `resetToBaseline`
- `setBaselineToCurrent`
- `isDirty`
- `beforeunload` 提示

说明：

- 仍然不引入 React Query
- 仍然使用页面级请求编排
- 不新增全局状态库

---

## 17. API 层改造设计

`api/cycle-template.ts` 需要按 v2 契约重写请求和响应类型。

### 17.1 保持不变的方法

- `getFormalTemplates`
- `getDraftTemplates`
- `getCycleTemplateDetail`
- `createDraftTemplate`
- `generateDraftTemplateByAi`
- `updateDraftTemplate`
- `updateFormalTemplate`
- `copyCycleTemplate`
- `activateCycleTemplate`
- `getCurrentActiveTemplate`
- `deleteCycleTemplate`

### 17.2 必须变化的点

#### `searchSystemExercises`

返回值改造为：

```ts
type SystemExerciseOption = {
  exerciseId: number;
  exerciseName: string;
  defaultStructureType: StructureType;
};
```

#### 保存相关接口

提交体中的动作对象必须改为：

- `structureType`
- `note`
- `items`

不再包含：

- `targetSets`
- `targetRepsMin`
- `targetRepsMax`
- `targetWeightKg`
- `targetDurationSeconds`
- `restSeconds`
- `targetRpe`
- `targetExtraJson`

---

## 18. 错误码适配

在保留 v1 错误码基础上，前端需新增处理：

- `CYCLE_TEMPLATE_STRUCTURE_TYPE_INVALID`
- `CYCLE_TEMPLATE_ITEM_INVALID`
- `CYCLE_TEMPLATE_ITEM_COUNT_INVALID`
- `CYCLE_TEMPLATE_METRIC_KEY_INVALID`
- `CYCLE_TEMPLATE_METRIC_DUPLICATE`
- `CYCLE_TEMPLATE_METRIC_VALUE_INVALID`

建议映射文案：

- `CYCLE_TEMPLATE_STRUCTURE_TYPE_INVALID`
  - 当前动作结构类型不合法，或与系统动作默认结构不一致。
- `CYCLE_TEMPLATE_ITEM_INVALID`
  - 执行项结构不合法，请检查组/段设置。
- `CYCLE_TEMPLATE_ITEM_COUNT_INVALID`
  - 当前动作的执行项数量不符合结构要求。
- `CYCLE_TEMPLATE_METRIC_KEY_INVALID`
  - 存在不支持的训练参数。
- `CYCLE_TEMPLATE_METRIC_DUPLICATE`
  - 同一执行项下不能重复添加同一种参数。
- `CYCLE_TEMPLATE_METRIC_VALUE_INVALID`
  - 训练参数的数值格式不合法。

---

## 19. 列表页、详情页、编辑页的迁移策略

建议迁移顺序如下：

1. 先替换 `types/cycle-template.ts`
2. 再替换 `api/cycle-template.ts`
3. 重写 `cycle-template-mappers.ts`
4. 重写 `cycle-template-validators.ts`
5. 升级 `useCycleTemplateEditor.ts`
6. 重写 `CycleTemplateEditor.tsx`
7. 调整详情页只读展示
8. 最后收尾列表页和错误文案

原因：

- 页面 UI 最依赖底层类型和映射
- 如果先改页面，后改类型，开发过程中会反复返工

---

## 20. 对未来 `training session` 的前端价值

这次重构最大的长期收益是：

- `cycle_template` 和未来 `training session` 将共享相同的计划结构语义
- 后续打卡页可以直接复用：
  - 动作卡
  - 执行项卡
  - 指标渲染器
  - 指标格式化与单位显示

因此本次前端 DDD 明确建议：

- 不把三层结构写死在某一个页面组件里
- 尽量抽出可复用的结构型组件和配置型元数据

---

## 21. 本次设计结论

`cycle_template` 前端 v2 改造不是局部补丁，而是一次围绕“动作结构模型升级”的模块级重构。

结论如下：

1. 现有固定字段动作模型必须整体下线
2. 前端要升级为“动作 -> 执行项 -> 指标”的三层编辑器
3. `structureType` 必须来自系统动作元数据，不能由前端猜测或由用户自由输入
4. 指标字典必须前端配置化，避免页面里散落硬编码
5. 本次重构应优先服务后端 v2 契约和未来训练打卡模块复用

如果后续直接进入实现阶段，本文档可以作为前端改造的执行基线。
