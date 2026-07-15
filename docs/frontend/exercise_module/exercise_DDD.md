# DailyForge Frontend Exercise 模块详细设计

> 版本：v1.0  
> 日期：2026-07-15  
> 模块归属：`frontend/src/features/exercise`

---

## 1. 文档目标

本文档用于定义前端 `exercise` 模块的职责边界、目录结构、类型模型、API 对接方式，以及它与 `cycle-template` 模块之间的集成关系。

本轮设计目标不是单独开发“动作详情页”，而是先把“系统动作查询”从训练模板编辑器中抽离成独立前端能力，解决以下问题：

1. 模板编辑页需要稳定、可复用的系统动作搜索入口。
2. `cycle-template` 不应该继续内聚动作查询接口与动作元数据类型。
3. 后续 `training session`、动作详情展示、AI 计划引用动作时，需要复用同一套动作查询 API 与类型。

---

## 2. 设计输入

本文档基于以下文档与现有代码整理：

- [exercise_接口文档.md](/D:/Computer%20Science/DailyForge/docs/interfaces/exercise_%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3.md)
- [cycle_template_DDD.md](/D:/Computer%20Science/DailyForge/docs/frontend/cycle_template_module/cycle_template_DDD.md)
- 当前前端代码中的 `cycle-template` 编辑器实现

接口契约以 `exercise_接口文档.md` 为准。

---

## 3. 模块定位

`exercise` 模块是前端的“系统动作查询能力层”，不是模板模块的内部私有工具。

它的职责是：

1. 对接 `/api/exercises` 相关接口。
2. 沉淀动作查询与动作详情的数据类型。
3. 提供动作类型、结构类型、动作模式等字段的展示格式化能力。
4. 为上层业务模块提供可复用的查询入口。

它当前不负责：

1. 用户自定义动作 CRUD。
2. 动作后台管理。
3. 动作详情独立页面路由。
4. 收藏、评论、社区等扩展行为。

---

## 4. 与 cycle-template 的关系

当前 `exercise` 模块的首个消费方是 `cycle-template` 编辑器。

集成原则如下：

1. 模板编辑器只通过 `exercise` 模块获取系统动作候选数据。
2. 用户选中动作后，模板编辑器只消费以下最小关键字段：
   - `exerciseId`
   - `exerciseName`
   - `defaultStructureType`
3. `defaultStructureType` 是模板编辑器初始化动作结构的唯一依据，前端不能自行猜测动作属于 `set_based` 还是 `single_segment`。
4. 动作搜索的展示信息可以比模板实际提交所需字段更多，例如：
   - `exerciseType`
   - `movementType`
   - `primaryMuscles`
   - `secondaryMuscles`
   - `equipmentNames`

---

## 5. 目录结构

```text
src/features/exercise
├─ api
│  └─ exercise.ts
├─ lib
│  ├─ exercise-enums.ts
│  └─ exercise-formatters.ts
└─ types
   └─ exercise.ts
```

说明：

1. 当前轮次先沉淀 API、类型、格式化与错误映射。
2. 组件层仍由 `cycle-template` 页面承载，因为当前只存在模板编辑场景。
3. 当后续出现“动作详情抽屉”、“全局动作选择器”、“训练打卡动作查看”时，再考虑新增 `components/` 或 `hooks/`。

---

## 6. API 层设计

文件：`frontend/src/features/exercise/api/exercise.ts`

### 6.1 `searchSystemExercises`

对应接口：

- `GET /api/exercises/system`

前端查询参数类型：

```ts
type SystemExerciseSearchQuery = {
  keyword?: string;
  exerciseType?: string;
  movementType?: string;
  structureType?: "set_based" | "single_segment";
  sceneType?: string;
  muscleId?: number;
  page?: number;
  pageSize?: number;
};
```

当前模板编辑页实际使用：

- `keyword`
- `page`
- `pageSize`

其余筛选参数先在 API 层保留，不提前做前端界面。

### 6.2 `getSystemExerciseDetail`

对应接口：

- `GET /api/exercises/system/{exerciseId}`

当前轮次先完成 API 封装与类型沉淀，模板编辑页暂不调用。

保留原因：

1. 后续动作详情浮层可直接复用。
2. `training session` 可能需要查看动作说明、器械、视频、肌群等信息。

---

## 7. 类型模型

文件：`frontend/src/features/exercise/types/exercise.ts`

### 7.1 结构类型

```ts
type StructureType = "set_based" | "single_segment";
```

说明：

1. 该类型由后端动作元数据决定。
2. 模板编辑器不能手工修改。

### 7.2 系统动作搜索结果

```ts
type SystemExerciseOption = {
  exerciseId: number;
  exerciseName: string;
  exerciseType: string;
  movementType: string | null;
  defaultUnit: string | null;
  defaultStructureType: StructureType;
  videoUrl: string | null;
  primaryMuscles: string[];
  secondaryMuscles: string[];
  equipmentNames: string[];
};
```

说明：

1. 这是模板编辑器搜索结果列表的核心展示模型。
2. 模板实际提交只依赖 `exerciseId` 和 `defaultStructureType`。
3. 其余字段用于帮助用户判断动作是否适合当前训练日。

### 7.3 系统动作详情

```ts
type SystemExerciseDetailResponse = {
  exerciseId: number;
  exerciseName: string;
  exerciseType: string;
  movementType: string | null;
  defaultUnit: string | null;
  defaultStructureType: StructureType;
  videoUrl: string | null;
  calorieBurnReference: number | null;
  calorieReferenceUnit: string | null;
  primaryMuscles: ExerciseMuscleRelation[];
  secondaryMuscles: ExerciseMuscleRelation[];
  equipments: ExerciseEquipment[];
};
```

说明：

1. 详情模型比搜索项更完整。
2. 当前未在模板页消费，但后续有扩展价值。

---

## 8. 展示格式化设计

文件：`frontend/src/features/exercise/lib/exercise-formatters.ts`

当前提供：

1. `formatExerciseStructureType`
2. `formatExerciseType`
3. `formatMovementType`

设计原则：

1. 后端继续返回英文枚举值，前端负责中文标签映射。
2. 映射不到的值直接回显原值，避免前后端枚举不同步时页面空白。

---

## 9. 错误处理设计

文件：`frontend/src/features/exercise/lib/exercise-enums.ts`

当前映射的错误码包括：

1. `EXERCISE_NOT_FOUND`
2. `INVALID_ARGUMENT`
3. `UNAUTHORIZED`
4. `TOKEN_INVALID`
5. `TOKEN_EXPIRED`
6. `FORBIDDEN`

对外暴露：

```ts
getExerciseErrorMessage(error, fallback)
```

作用：

1. 将后端错误码统一转换为中文提示。
2. 模板编辑页不直接写死错误文案。

---

## 10. 模板编辑页接入方案

当前动作查询入口位于：

- `frontend/src/features/cycle-template/components/ExerciseStructureEditor.tsx`

接入方式：

1. 通过 `exercise/api/exercise.ts` 调用 `searchSystemExercises`。
2. 搜索结果在动作卡内部展示为候选列表。
3. 用户点击候选项后，将整个 `SystemExerciseOption` 回传给 `cycle-template` 编辑器。
4. `cycle-template` 通过 `createExerciseFromSystemOption` 初始化：
   - `exerciseId`
   - `exerciseName`
   - `structureType = defaultStructureType`
   - 默认执行项

### 10.1 当前交互设计

模板编辑页中的动作搜索交互定义如下：

1. 用户输入关键词后点击“查询动作”或按 Enter。
2. 前端请求 `GET /api/exercises/system`。
3. 结果列表展示动作基础信息：
   - 动作名称
   - 结构类型
   - 动作类型
   - 动作模式
   - 默认单位
   - 主要肌群
   - 次要肌群
   - 器械
4. 点击候选项后立即完成选中，并重置候选列表。

### 10.2 为什么不直接做成下拉框

当前不使用简陋下拉项的原因：

1. 新接口已经提供了更多动作元数据，纯文本列表信息密度不足。
2. 模板编辑时，用户往往需要快速判断动作结构和肌群覆盖。
3. 卡片式结果更便于后续扩展视频、场景、筛选条件。

---

## 11. 与 cycle-template 编辑器的数据边界

`exercise` 模块不负责：

1. 把动作转换成模板执行项结构。
2. 决定新增几个执行项。
3. 决定执行项默认名称。
4. 决定模板保存请求体。

这些职责继续留在 `cycle-template` 模块：

1. `createExerciseFromSystemOption`
2. `createDefaultItemByStructureType`
3. `editorFormToPayload`
4. `useCycleTemplateEditor`

边界这样划分的好处是：

1. `exercise` 保持为纯查询能力，不掺入模板业务。
2. `cycle-template` 仍然掌握模板结构初始化规则。

---

## 12. 当前实现状态

本轮完成后，前端状态应为：

1. 已新增独立 `exercise` 前端模块。
2. 模板编辑页不再从 `cycle-template/api` 里查询动作。
3. 模板编辑页动作搜索已消费真实的动作查询响应结构。
4. 选中动作后，模板动作结构会依据 `defaultStructureType` 正确初始化。
5. 动作查询详情接口已在前端完成封装，但暂未进入页面流。

---

## 13. 后续可扩展点

后续如果产品继续推进动作模块，建议沿着以下方向扩展：

1. 新增 `exercise/components/SystemExercisePicker.tsx`，把模板编辑器里的搜索 UI 真正抽成通用组件。
2. 新增 `exercise/pages/ExerciseDetailPage.tsx` 或详情抽屉。
3. 在搜索 UI 中补充筛选条件：
   - `exerciseType`
   - `movementType`
   - `sceneType`
   - `structureType`
   - `muscleId`
4. 支持视频预览与动作详情跳转。
5. 为 `training session` 直接复用动作详情查询能力。

---

## 14. 风险与注意事项

1. 当前前端默认信任后端返回的 `defaultStructureType`，如果后端数据源错误，会直接影响模板结构初始化。
2. 搜索列表目前只做第一页查询，不支持前端分页交互，但 API 类型已保留分页字段。
3. 详情接口虽已封装，但尚未在 UI 中验证，需要后续联调。
4. 如果后端未来引入“用户自定义动作”，应避免直接复用当前 `system` 查询类型，需要拆分系统动作与用户动作模型。
