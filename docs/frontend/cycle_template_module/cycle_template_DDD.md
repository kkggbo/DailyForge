# DailyForge Frontend Cycle Template 模块详细设计

> 版本：v1.0  
> 日期：2026-07-14  
> 模块归属：`frontend/src/features/cycle-template`

---

## 1. 文档目标

本文档用于定义 `cycle_template` 模块的前端技术实现方案，面向：

- 页面与路由设计
- 前端状态模型
- API 对接方式
- 模板编辑器交互
- 激活、切换、删除等关键业务动作的前端行为

这是一份“可直接指导前端开发”的技术文档，默认以后端已完成实现为前提。

---

## 2. 设计输入

本方案基于以下文档整理：

- [cycle_template_PRD.md](/D:/Computer%20Science/DailyForge/docs/prd/cycle_template_PRD.md)
- [cycle_template_接口文档.md](/D:/Computer%20Science/DailyForge/docs/interfaces/cycle_template_接口文档.md)

同时参考当前前端已有模式：

- `profile` 模块的页面级请求编排
- `shared/api/http.ts` 的统一请求封装
- 现有 `auth` / `app` 的受保护路由结构

---

## 3. 模块定位

`cycle_template` 是 DailyForge 训练计划层的核心模块。

它在前端中的职责不是“展示一个静态计划列表”，而是承接三类能力：

1. 草稿模板管理
2. 正式模板管理
3. 当前激活模板与后续训练打卡模块之间的衔接

它和其他模块的关系：

- `profile`：训练目标等资料会影响 AI 生成草稿模板
- `training-session`：依赖当前激活模板与 `currentDayIndex`
- `ai`：AI 生成模板草稿后，最终仍回到本模块确认和启用

---

## 4. 推荐目录结构

建议新增目录：

```text
src/features/cycle-template
├─ api
│  └─ cycle-template.ts
├─ components
│  ├─ CycleTemplateTabNav.tsx
│  ├─ FormalTemplateList.tsx
│  ├─ DraftTemplateList.tsx
│  ├─ TemplateCard.tsx
│  ├─ ActiveTemplateBanner.tsx
│  ├─ TemplateDetailHeader.tsx
│  ├─ TemplateDayTabs.tsx
│  ├─ TemplateDayEditor.tsx
│  ├─ TemplateExerciseEditor.tsx
│  ├─ TemplateReadOnlyDayList.tsx
│  ├─ ActivateTemplateDialog.tsx
│  ├─ DeleteTemplateDialog.tsx
│  └─ LeaveEditorConfirmDialog.tsx
├─ hooks
│  ├─ useCycleTemplateEditor.ts
│  └─ useLeaveConfirm.ts
├─ lib
│  ├─ cycle-template-enums.ts
│  ├─ cycle-template-mappers.ts
│  ├─ cycle-template-formatters.ts
│  ├─ cycle-template-validators.ts
│  └─ cycle-template-history.ts
├─ pages
│  ├─ CycleTemplatePage.tsx
│  ├─ CycleTemplateDetailPage.tsx
│  ├─ CycleTemplateCreatePage.tsx
│  └─ CycleTemplateEditPage.tsx
└─ types
   └─ cycle-template.ts
```

说明：

- `CycleTemplatePage` 负责正式模板 / 草稿模板两大入口页。
- `DetailPage` 与 `EditPage` 分离，避免一个页面同时承担只读与重编辑逻辑。
- 编辑器复杂度较高，建议单独抽 `useCycleTemplateEditor` 管理本地草稿态、撤销、重置和离开确认。

---

## 5. 路由设计

建议新增受保护路由：

- `/cycle-templates`
- `/cycle-templates/create`
- `/cycle-templates/:templateId`
- `/cycle-templates/:templateId/edit`

路由语义：

- `/cycle-templates`
  训练模板首页，默认展示“正式模板”Tab。
- `/cycle-templates/create`
  创建新的草稿模板。
- `/cycle-templates/:templateId`
  模板详情页，只读。
- `/cycle-templates/:templateId/edit`
  模板编辑页。

导航建议：

- 在已登录应用导航中新增“训练模板”入口。
- 这是训练主流程核心模块，优先级应和“个人资料”同级。

---

## 6. 页面设计

## 6.1 `CycleTemplatePage`

作用：

- 模板管理主入口
- 承接正式模板 / 草稿模板两类列表
- 展示当前激活模板摘要

页面结构：

1. 页面标题区
2. 当前激活模板横幅 `ActiveTemplateBanner`
3. Tab 切换 `正式模板` / `草稿模板`
4. 对应列表区域

首屏请求建议并行加载：

1. `GET /api/cycle-templates/formal`
2. `GET /api/cycle-templates/drafts`
3. `GET /api/cycle-templates/active/current`

说明：

- 即使 `formal` 返回里已经有 `activeTemplateId`，仍建议保留 `active/current` 请求。
- 因为后续训练打卡模块也会依赖它，前端模型保持统一更好。

## 6.2 `CycleTemplateDetailPage`

作用：

- 查看模板完整结构
- 提供进入编辑、激活、复制、删除等动作入口

页面结构：

1. 模板头部信息
2. 状态标签与动作按钮区
3. Day 列表只读展示

请求：

- `GET /api/cycle-templates/{templateId}`

## 6.3 `CycleTemplateCreatePage`

作用：

- 新建手动草稿模板

页面结构：

1. 基础信息区
2. Day Tab 区
3. 当前 Day 编辑区
4. 页面底部操作区

## 6.4 `CycleTemplateEditPage`

作用：

- 编辑草稿模板
- 编辑正式模板
- 编辑运行中模板的可编辑未来部分

请求：

- `GET /api/cycle-templates/{templateId}`

同一套编辑器页面要根据 `status` 切换行为：

- `draft`
- `inactive`
- `active`

---

## 7. 数据模型设计

以下类型建议直接按接口文档定义，减少转换成本。

## 7.1 模板状态

```ts
export type CycleTemplateStatus = "draft" | "active" | "inactive" | "deleted";
```

## 7.2 正式模板列表项

```ts
export type FormalTemplateListItem = {
  templateId: number;
  templateName: string;
  cycleLength: number | null;
  goalType: string | null;
  status: "active" | "inactive";
  isActive: boolean;
  currentDayIndex: number | null;
  updatedAt: string;
};
```

## 7.3 草稿模板列表项

```ts
export type DraftTemplateListItem = {
  templateId: number;
  templateName: string;
  cycleLength: number | null;
  configuredDayCount: number;
  createdAt: string;
  updatedAt: string;
};
```

## 7.4 当前激活模板摘要

```ts
export type ActiveTemplateSummaryResponse = {
  templateId: number;
  templateName: string;
  cycleLength: number;
  currentDayIndex: number;
  currentDayName: string | null;
  editableFromDayIndex: number;
  startedAt: string;
};
```

## 7.5 模板详情

```ts
export type CycleTemplateDetailResponse = {
  templateId: number;
  templateName: string;
  goalType: string | null;
  status: CycleTemplateStatus;
  cycleLength: number | null;
  isActive: boolean;
  currentDayIndex: number | null;
  editableFromDayIndex: number;
  canActivate: boolean;
  canDelete: boolean;
  createdAt: string;
  updatedAt: string;
  days: CycleTemplateDayDetail[];
};
```

```ts
export type CycleTemplateDayDetail = {
  dayIndex: number;
  dayName: string;
  isRestDay: boolean;
  isLocked: boolean;
  exercises: CycleTemplateExerciseDetail[];
};
```

```ts
export type CycleTemplateExerciseDetail = {
  sortOrder: number;
  exerciseId: number;
  exerciseName: string;
  targetSets: number | null;
  targetRepsMin: number | null;
  targetRepsMax: number | null;
  targetWeightKg: number | null;
  targetDurationSeconds: number | null;
  restSeconds: number | null;
  targetRpe: number | null;
  note: string | null;
  targetExtraJson: Record<string, unknown> | null;
};
```

---

## 8. API 层设计

建议文件：

- `src/features/cycle-template/api/cycle-template.ts`

建议封装以下方法：

| 方法 | 接口 | 用途 |
|------|------|------|
| `getFormalTemplates` | `GET /api/cycle-templates/formal` | 获取正式模板列表 |
| `getDraftTemplates` | `GET /api/cycle-templates/drafts` | 获取草稿模板列表 |
| `getCycleTemplateDetail` | `GET /api/cycle-templates/{templateId}` | 获取模板详情 |
| `createDraftTemplate` | `POST /api/cycle-templates/drafts` | 新建手动草稿 |
| `generateDraftTemplateByAi` | `POST /api/cycle-templates/drafts/ai-generate` | AI 生成草稿占位 |
| `updateDraftTemplate` | `PUT /api/cycle-templates/drafts/{templateId}` | 更新草稿模板 |
| `updateFormalTemplate` | `PUT /api/cycle-templates/{templateId}` | 更新正式模板 |
| `copyCycleTemplate` | `POST /api/cycle-templates/{templateId}/copy` | 复制模板为草稿 |
| `activateCycleTemplate` | `POST /api/cycle-templates/{templateId}/activate` | 启用模板 |
| `getCurrentActiveTemplate` | `GET /api/cycle-templates/active/current` | 获取当前激活摘要 |
| `deleteCycleTemplate` | `DELETE /api/cycle-templates/{templateId}` | 删除模板 |

---

## 9. 编辑器表单模型

建议编辑器内部统一维护一个前端草稿态，而不是直接拿接口详情对象就地改。

```ts
export type CycleTemplateEditorForm = {
  templateName: string;
  goalType: string;
  cycleLength: string;
  days: EditorDayForm[];
};
```

```ts
export type EditorDayForm = {
  dayIndex: number;
  dayName: string;
  exercises: EditorExerciseForm[];
};
```

```ts
export type EditorExerciseForm = {
  localId: string;
  sortOrder: number;
  exerciseId: number | null;
  exerciseName: string;
  targetSets: string;
  targetRepsMin: string;
  targetRepsMax: string;
  targetWeightKg: string;
  targetDurationSeconds: string;
  restSeconds: string;
  targetRpe: string;
  note: string;
  targetExtraJsonText: string;
};
```

说明：

- 输入组件统一维护字符串，避免用户在中间输入态被数字转换打断。
- `targetExtraJson` 建议先以前端 `textarea + JSON 文本` 形式落地。
- `localId` 用于拖拽排序和本地未保存项渲染。

---

## 10. 本地状态与编辑历史

## 10.1 页面级状态

`CycleTemplatePage` 建议维护：

- `activeTab`
- `formalList`
- `draftList`
- `activeSummary`
- `isLoadingPage`
- `pageError`
- `activateDialogState`
- `deleteDialogState`

`CycleTemplateEditPage` 建议维护：

- `detail`
- `form`
- `selectedDayIndex`
- `fieldErrors`
- `pageError`
- `isLoadingDetail`
- `isSaving`
- `isDirty`
- `historyStack`
- `hasUnsavedChanges`

## 10.2 撤销与重置

根据 PRD，草稿编辑页需要：

- 撤销上一步本地操作
- 撤销本次全部修改，恢复为最近一次已保存状态

建议实现：

- `historyStack: CycleTemplateEditorForm[]`
- 每次关键编辑动作 push 一份浅/深拷贝后的表单快照
- `resetToLastSaved` 直接回到 `detail -> form` 的初始映射结果

注意：

- 这是前端本地交互能力，不涉及后端撤销接口。

## 10.3 离开未保存提示

编辑页在以下场景需要提示：

- 路由跳转
- 关闭标签页
- 刷新页面

提示语义：

- “当前修改尚未保存，离开后将丢失。”

---

## 11. 页面交互规则

## 11.1 模板首页 Tab

Tab 固定两个：

- `正式模板`
- `草稿模板`

默认进入：

- `正式模板`

## 11.2 正式模板卡片行为

对于 `inactive` 模板：

- 查看详情
- 编辑
- 复制
- 启用
- 删除

对于 `active` 模板：

- 查看详情
- 编辑
- 复制
- 不提供直接删除

## 11.3 草稿模板卡片行为

- 继续编辑
- 查看详情
- 复制
- 删除
- 确认启用

## 11.4 Day Tab 编辑器

采用：

- 顶部 Day Tab
- 中部单日内容编辑

规则：

- `cycleLength` 非空时，只展示 `1 ~ cycleLength` 的 Day Tab
- 空白天允许存在
- 空白天默认视为休息日，不强制额外勾选

## 11.5 运行中模板编辑限制

当模板为 `active` 时：

- `cycleLength` 不允许编辑
- `dayIndex < editableFromDayIndex` 的 Day Tab 只读
- `dayIndex >= editableFromDayIndex` 的 Day Tab 可编辑

UI 上必须明显区分：

- 已锁定天
- 可编辑天

建议：

- 锁定天标签显示锁图标或“已完成”标记
- 锁定天编辑区直接只读展示，而不是允许进入再报错

---

## 12. 激活与切换流程

## 12.1 激活入口

触发点：

- 正式模板卡片
- 草稿模板卡片
- 详情页操作区

## 12.2 激活确认弹窗

当前端检测到存在已激活模板，或用户点击启用动作时，需要弹出确认弹窗。

弹窗需展示：

- 即将启用的模板名称
- 周期长度
- 是否会结束当前循环
- 是否会从 Day 1 开始新 run

如果后端返回：

- `CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED`

前端策略：

- 保持弹窗打开
- 在弹窗内显示错误说明
- 用户确认后再发带 `confirmSwitch=true` 的请求

## 12.3 激活成功后前端行为

刷新：

1. 正式模板列表
2. 草稿模板列表
3. 当前激活模板摘要

并建议：

- 弹出成功提示
- 可选择跳回模板首页

---

## 13. 删除流程

## 13.1 删除弹窗

适用对象：

- `draft`
- `inactive`

不允许：

- `active`

## 13.2 删除成功后前端行为

刷新：

1. 对应列表
2. 如当前在详情页，则返回 `/cycle-templates`

## 13.3 删除失败处理

重点识别：

- `CYCLE_TEMPLATE_DELETE_FORBIDDEN`
- `CYCLE_TEMPLATE_NOT_FOUND`
- `CYCLE_TEMPLATE_STATUS_INVALID`

建议和 `profile` 模块保持一致：

- 页面级错误区展示一次
- 弹窗内部也展示当前动作错误，避免提示被遮挡或脱离上下文

---

## 14. AI 生成草稿占位流程

虽然后端当前返回 `501 CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED`，前端仍应先把入口和错误处理设计好。

建议：

- 在创建页或模板首页提供“AI 生成草稿”入口
- 当前点下后：
  - 调用接口
  - 如果返回 `CYCLE_TEMPLATE_AI_NOT_IMPLEMENTED`
  - 明确提示“AI 生成模板功能暂未开放”

不要做成静默失败。

---

## 15. 错误处理设计

前端需要重点识别以下错误码：

- `INVALID_ARGUMENT`
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

建议按场景映射为更易理解的前端文案。

例如：

- `CYCLE_TEMPLATE_SWITCH_CONFIRM_REQUIRED`
  - “当前已有激活模板，确认后将结束当前循环并切换到新模板。”
- `CYCLE_TEMPLATE_EDIT_FORBIDDEN`
  - “已完成的训练日不能修改，请只编辑当前天及之后的内容。”
- `CYCLE_TEMPLATE_SYSTEM_EXERCISE_REQUIRED`
  - “当前版本只能选择系统动作库中的动作。”

---

## 16. 推荐实现顺序

1. 完成 `types` 与 `api` 层。
2. 搭建 `/cycle-templates` 首页与两个 Tab 列表。
3. 接入 `active/current` 横幅摘要。
4. 完成详情页只读展示。
5. 完成创建草稿页。
6. 完成草稿编辑页。
7. 完成正式模板编辑限制逻辑。
8. 完成启用、复制、删除弹窗链路。
9. 最后补 AI 生成占位入口与错误处理。

---

## 17. 当前方案结论

前端 `cycle_template` 模块的难点不在简单 CRUD，而在：

1. 草稿态与正式态的行为差异
2. 运行中模板的部分可编辑限制
3. 激活切换的确认与刷新链路
4. 编辑器本地状态、撤销、未保存离开提示

因此，技术实现上不建议把它做成一个“单页大表单 + 一堆 if 判断”。

更合理的方式是：

- 首页、详情页、编辑页分离
- 业务弹窗独立组件化
- 编辑器单独抽 hook 管理本地草稿态与历史栈

这样后续再衔接 `training-session` 模块时，代价会小很多。
