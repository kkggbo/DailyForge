# DailyForge Frontend Exercise 模块详细设计

> 版本：v2.0  
> 日期：2026-07-17  
> 模块归属：`frontend/src/features/exercise`

---

## 1. 文档目标

本文档用于定义前端 `exercise` 模块在当前阶段的职责边界、目录结构、接口消费方式、类型模型，以及它与 `cycle-template` 模块之间的协作关系。

这次设计的核心目标不是单独做“动作详情页”，而是把“系统动作查询能力”从模板编辑器中抽离出来，形成一个可复用、可维护的前端能力层，支撑以下场景：

1. `cycle-template` 模块的动作选择弹窗。
2. 模板编辑页中动作卡的摘要展示。
3. 保存后重新进入编辑页时，动作卡摘要的回填。
4. 后续 `training session`、AI 计划生成、动作详情展示等能力的复用。

---

## 2. 模块定位

`exercise` 模块是前端的“系统动作查询能力层”，不负责训练模板结构编辑，也不直接负责模板保存请求体拼装。

当前职责：

1. 对接 `/api/exercises` 相关接口。
2. 定义动作查询、筛选元数据、动作详情的前端类型。
3. 提供动作类型、动作模式、结构类型等字段的展示格式化能力。
4. 提供模板编辑页可直接消费的动作摘要映射能力。

当前不负责：

1. 用户自定义动作 CRUD。
2. 动作后台管理。
3. 动作详情独立页面路由。
4. 模板结构初始化规则。
5. 模板编辑器的撤销、脏状态、保存逻辑。

---

## 3. 与 Cycle Template 的边界

`cycle-template` 是当前 `exercise` 模块的主要消费者，但两者的职责必须明确拆开。

### 3.1 exercise 模块负责

1. 返回系统动作筛选分类。
2. 返回系统动作搜索结果。
3. 返回系统动作详情。
4. 把列表项或详情对象映射为动作卡头部可展示的摘要元数据。

### 3.2 cycle-template 模块负责

1. 用户选择动作后，如何初始化模板中的动作结构。
2. `defaultStructureType` 对应的执行项默认创建规则。
3. 动作被替换时，如何整卡重建。
4. 动作备注、执行项、指标的表单状态和撤销历史。
5. 模板保存请求体组装。

### 3.3 当前协作原则

1. 模板编辑器只依赖 `exerciseId`、`exerciseName`、`defaultStructureType` 来写入业务表单。
2. 动作卡头部展示的肌群、器械、动作类型等摘要不进入业务表单。
3. 动作摘要元数据通过 `exercise` 模块提供的 mapper 和详情接口回填，不污染 `dirty`、`baseline`、`undo`。

---

## 4. 目录结构

```text
frontend/src/features/exercise
├─ api
│  └─ exercise.ts
├─ lib
│  ├─ exercise-enums.ts
│  ├─ exercise-formatters.ts
│  └─ exercise-mappers.ts
└─ types
   └─ exercise.ts
```

说明：

1. `api/` 只负责请求封装。
2. `types/` 只定义接口契约相关类型。
3. `lib/exercise-formatters.ts` 负责中文展示映射。
4. `lib/exercise-enums.ts` 负责错误码文案映射。
5. `lib/exercise-mappers.ts` 负责把查询结果转成动作卡 UI 摘要。

---

## 5. 接口设计

文件：`frontend/src/features/exercise/api/exercise.ts`

### 5.1 `getSystemExerciseFilterOptions`

对应接口：

- `GET /api/exercises/system/filter-options`

用途：

1. 为动作选择弹窗提供一级分类列表。
2. 为当前一级分类提供二级细分肌肉筛选项。

前端消费约定：

1. 弹窗打开时先请求该接口。
2. 默认选中返回结果中的第一个分类。
3. 当前分类变化时，二级肌肉筛选随之重置。

### 5.2 `searchSystemExercises`

对应接口：

- `GET /api/exercises/system`

查询参数类型：

```ts
type SystemExerciseSearchQuery = {
  keyword?: string;
  categoryCode?: string;
  exerciseType?: string;
  movementType?: string;
  structureType?: StructureType;
  sceneType?: string;
  muscleId?: number;
  page?: number;
  pageSize?: number;
};
```

当前主要消费方式：

1. `categoryCode`
2. `muscleId`
3. `keyword`
4. `page`
5. `pageSize`

联合查询语义：

1. `categoryCode` 表示一级分类筛选。
2. `muscleId` 表示二级细分肌肉筛选。
3. `keyword` 在保留当前分类和肌肉筛选条件下做模糊搜索。
4. 当三者同时存在时，按“与”关系过滤。

### 5.3 `getSystemExerciseDetail`

对应接口：

- `GET /api/exercises/system/{exerciseId}`

本轮主要用途：

1. 编辑页首次加载已有模板时，补齐动作卡头部摘要。
2. 解决“保存后重新进入编辑页仍需展示肌群/器械摘要”的问题。

注意：

1. 当前模板详情接口本身不返回这些展示摘要。
2. 因此前端需要在编辑页额外请求动作详情做 UI 回填。

---

## 6. 类型模型

文件：`frontend/src/features/exercise/types/exercise.ts`

### 6.1 核心枚举类型

```ts
type StructureType = "set_based" | "single_segment";
type SceneType = "home" | "gym" | "both" | string;
```

说明：

1. `StructureType` 由后端动作元数据决定。
2. 模板编辑器不允许前端手动猜测或修改动作的默认结构。

### 6.2 列表轻量肌肉对象

```ts
type ExerciseListItemMuscle = {
  muscleId: number;
  muscleName: string;
  muscleCode: string;
};
```

说明：

1. v1 中动作列表里肌群是 `string[]`。
2. v2 升级为轻量对象数组，便于后续做高亮、跳转、筛选联动。

### 6.3 筛选元数据

```ts
type ExerciseFilterMuscleOption = {
  muscleId: number;
  muscleName: string;
  muscleCode: string;
  parentMuscleId: number | null;
  parentMuscleName: string | null;
  sortOrder: number;
};

type ExerciseCategoryOption = {
  categoryCode: string;
  categoryName: string;
  sortOrder: number;
  children: ExerciseFilterMuscleOption[];
};

type ExerciseFilterOptionsResponse = {
  categories: ExerciseCategoryOption[];
};
```

说明：

1. 一级分类用于弹窗左侧导航。
2. `children` 用于当前分类下的二级细分肌肉筛选。
3. 前端不直接消费数据库肌肉树，而是消费后端整理后的产品化筛选结果。

### 6.4 动作列表项

```ts
type SystemExerciseOption = {
  exerciseId: number;
  exerciseName: string;
  exerciseType: ExerciseType;
  movementType: MovementType | null;
  defaultUnit: string | null;
  defaultStructureType: StructureType;
  videoUrl: string | null;
  primaryMuscles: ExerciseListItemMuscle[];
  secondaryMuscles: ExerciseListItemMuscle[];
  equipmentNames: string[];
};
```

说明：

1. 这是动作选择弹窗右侧结果列表的核心模型。
2. 模板编辑器只把其中的少数字段写入业务表单。
3. 其余字段主要用于选择时辅助判断。

### 6.5 动作详情对象

```ts
type SystemExerciseDetailResponse = {
  exerciseId: number;
  exerciseName: string;
  exerciseType: ExerciseType;
  movementType: MovementType | null;
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

1. 详情对象比列表项更完整。
2. 当前主要用于编辑页回填动作摘要。
3. 后续也可以直接服务于动作详情页或训练打卡页。

### 6.6 动作卡摘要元数据

```ts
type ExerciseCardMeta = {
  exerciseId: number;
  exerciseType: ExerciseType | null;
  movementType: MovementType | null;
  defaultUnit: string | null;
  videoUrl: string | null;
  primaryMuscles: string[];
  secondaryMuscles: string[];
  equipmentNames: string[];
};
```

说明：

1. 这是纯 UI 展示对象。
2. 只用于动作卡头部摘要。
3. 不参与模板保存请求体。

---

## 7. Mapper 设计

文件：`frontend/src/features/exercise/lib/exercise-mappers.ts`

当前提供两个 mapper：

1. `mapSystemExerciseOptionToCardMeta`
2. `mapSystemExerciseDetailToCardMeta`

设计意图：

1. 用户在弹窗中刚选中动作时，直接用列表项生成摘要，立即显示在卡头部。
2. 用户保存后重新进入编辑页时，再用详情接口回填同样结构的摘要数据。
3. 保证动作卡展示层不关心数据来源差异。

---

## 8. 格式化与错误处理

### 8.1 格式化

文件：`frontend/src/features/exercise/lib/exercise-formatters.ts`

当前负责：

1. `formatExerciseStructureType`
2. `formatExerciseType`
3. `formatMovementType`

原则：

1. 后端保留英文枚举值。
2. 前端统一负责中文标签映射。
3. 未识别值优先回显原值，避免页面直接空白。

### 8.2 错误处理

文件：`frontend/src/features/exercise/lib/exercise-enums.ts`

当前提供：

```ts
getExerciseErrorMessage(error, fallback)
```

用途：

1. 将接口错误码统一转成中文提示。
2. 供动作选择弹窗加载分类、加载动作列表、加载动作详情时复用。

---

## 9. 动作选择弹窗集成方案

当前消费方：`frontend/src/features/cycle-template/components/CycleTemplateEditor.tsx`

### 9.1 弹窗打开流程

1. 用户点击“添加动作”或“更换动作”。
2. 编辑器进入纯 UI 的 picker 状态，不写入业务表单。
3. 先请求 `getSystemExerciseFilterOptions`。
4. 默认选中第一个分类。
5. 自动用 `categoryCode + page=1 + pageSize=20` 请求动作列表。

### 9.2 弹窗内筛选规则

1. 切换一级分类时，清空当前 `muscleId`。
2. 切换二级细分肌肉时，保留当前分类并重新查询。
3. 搜索框保留当前分类和肌肉筛选，300ms debounce 自动查询。
4. 按 Enter 时立即查询。

### 9.3 选择动作后的行为

1. `append` 模式：选择后插入新的动作卡。
2. `replace` 模式：选择后重建目标动作卡。
3. 动作刚被选中时，直接用列表项写入 `exerciseMetaById` 缓存。

---

## 10. 动作摘要回填方案

### 10.1 为什么需要补请求

模板详情接口当前不返回动作卡头部所需的摘要数据，例如：

1. 主要肌群
2. 次要肌群
3. 器械名称

如果只依赖模板详情接口，保存后重新进入编辑页时，这些摘要会丢失。

### 10.2 当前方案

在 `CycleTemplateEditor` 中维护一个 `exerciseMetaById` 只读缓存：

1. 新增/替换动作时，用列表项即时写入缓存。
2. 编辑页初始加载时，提取表单中的去重 `exerciseId`。
3. 对缓存缺失的 `exerciseId` 调用 `getSystemExerciseDetail`。
4. 用 `mapSystemExerciseDetailToCardMeta` 回填缓存。

### 10.3 设计收益

1. 摘要展示不依赖模板接口改造。
2. UI 缓存不进入 `dirty` 和 `undo`。
3. 保存前后视觉表现一致。

---

## 11. 状态边界

### 11.1 属于 exercise 模块的数据

1. 接口响应对象。
2. 筛选元数据。
3. 动作卡摘要对象。

### 11.2 不属于 exercise 模块的数据

1. 当前弹窗是否打开。
2. 当前是 append 还是 replace。
3. 当前目标 dayIndex / exerciseLocalId。
4. 模板编辑器的撤销栈、baseline、dirty。

这些状态留在 `cycle-template` 页面层或编辑器组件层管理。

---

## 12. 已落地文件与职责

### `frontend/src/features/exercise/api/exercise.ts`

负责：

1. `getSystemExerciseFilterOptions`
2. `searchSystemExercises`
3. `getSystemExerciseDetail`

### `frontend/src/features/exercise/types/exercise.ts`

负责：

1. 类型定义。
2. 动作列表项、详情、筛选元数据契约。

### `frontend/src/features/exercise/lib/exercise-mappers.ts`

负责：

1. 列表项 -> 动作卡摘要
2. 详情对象 -> 动作卡摘要

### `frontend/src/features/exercise/lib/exercise-formatters.ts`

负责：

1. 枚举值中文格式化。

### `frontend/src/features/exercise/lib/exercise-enums.ts`

负责：

1. 错误码中文提示映射。

---

## 13. 风险与注意事项

1. 模板编辑器强依赖后端返回正确的 `defaultStructureType`。
2. `categoryCode` 的产品语义必须由后端稳定维护，避免前后端各自定义一套。
3. 详情回填采用“按需补请求”策略，若未来模板中动作很多，需要关注请求并发量。
4. 当前列表接口只取第一页结果，后续如果动作库增大，可能要补分页交互或懒加载。

---

## 14. 后续可扩展方向

1. 抽离通用 `SystemExercisePicker` 组件，供 `training session`、AI 页面复用。
2. 新增动作详情页或抽屉。
3. 补充场景、结构类型、动作类型等更细粒度筛选 UI。
4. 引入缓存策略，减少重复详情请求。
5. 如果未来支持用户自定义动作，需要拆分 `system exercise` 与 `user exercise` 两套前端模型。
