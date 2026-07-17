# DailyForge Exercise 模块接口文档

> 版本：v2.0  
> 更新日期：2026-07-17  
> 模块归属：`backend` 单体应用，建议代码包归属 `com.dailyforge.modules.exercise`  
> 文档状态：动作选择器适配版接口契约

---

## 1. 文档说明

本文档描述 DailyForge 当前 MVP 阶段“系统动作查询”相关接口契约。

本模块的定位不是训练模板本身，而是为以下场景提供统一动作数据来源：

- `cycle_template` 模板编辑页动作选择
- 模板编辑器中的“动作弹窗选择器”
- 后续 `training_session` 训练打卡动作展示
- 动作详情查看
- 后续 AI 生成计划时的动作引用与过滤

当前范围包含：

1. 系统动作筛选元数据查询
2. 系统动作搜索 / 列表查询
3. 系统动作详情查询

当前不包含：

- 用户自定义动作 CRUD
- 系统动作后台管理
- 动作收藏
- 动作评论 / 社区内容

对外接口前缀统一为：

- `/api/exercises`

统一返回包装：

- `ApiResponse`

统一成功响应示例：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

统一失败响应示例：

```json
{
  "code": "EXERCISE_NOT_FOUND",
  "message": "exercise not found",
  "data": null
}
```

---

## 2. 通用约定

### 2.1 鉴权约定

当前文档中的接口默认都要求登录态：

```http
Authorization: Bearer <accessToken>
```

说明：

- 当前动作查询接口主要服务于登录后的模板编辑与训练相关页面
- 如果后续产品需要把部分动作库开放给游客访问，再单独调整放行策略

### 2.2 数据来源约定

当前接口只返回：

- 系统动作

不返回：

- 用户自定义动作
- 已禁用动作

对应数据库语义：

- `owner_user_id IS NULL`
- `is_active = 1`

### 2.3 与 cycle_template 的关系

`cycle_template` 前端编辑器依赖本模块返回：

- `exerciseId`
- `exerciseName`
- `defaultStructureType`

前端在用户选择动作后，应以：

- `defaultStructureType`

作为初始化模板动作结构的唯一依据，而不是自行推断。

### 2.4 结构类型约定

当前 MVP 仅支持两种动作默认结构类型：

- `set_based`
- `single_segment`

说明：

- `set_based`
  - 适用于力量训练、器械训练、自重训练
- `single_segment`
  - 适用于跑步、爬坡、椭圆机、骑行等持续型动作

### 2.5 肌肉分层与筛选语义约定

数据库层当前已经具备肌肉层级关系：

- `muscles.parent_id`
- `muscles.muscle_level`
- `muscles.sort_order`

但前端动作弹窗选择器需要的是更偏产品视角的“粗粒度分类”，而不是直接暴露数据库中的原始层级树。

因此本模块区分两层概念：

- 数据层肌肉
  - 对应数据库 `muscles` 表中的真实节点，例如“胸大肌中部”“三角肌前束”
- 选择器分类
  - 对应前端左侧一级分类，例如“胸”“背”“肩”“腿”“手臂”“核心”“有氧”

说明：

- 选择器分类不要求与数据库中的 `group` 节点一一对应
- 例如“手臂”可以聚合 `肱二头肌`、`肱三头肌`、`前臂`
- 例如“腿”可以聚合 `臀部`、`腿部`、`小腿`

### 2.6 查询参数语义约定

为避免“粗分类筛选”和“精确肌肉筛选”混淆，列表接口参数语义拆分如下：

- `categoryCode`
  - 面向前端动作弹窗左侧一级分类
  - 示例：`chest` / `back` / `shoulder` / `arms` / `legs` / `core` / `cardio`
- `muscleId`
  - 面向精确肌肉筛选
  - 适用于二级筛选或未来高级筛选
  - 语义为“精确匹配该肌肉节点”，不是“自动包含其全部子节点”

---

## 3. 接口列表

| 编号 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|------|
| E0 | GET | `/api/exercises/system/filter-options` | 是 | 动作选择器筛选元数据查询 |
| E1 | GET | `/api/exercises/system` | 是 | 系统动作搜索 / 列表查询 |
| E2 | GET | `/api/exercises/system/{exerciseId}` | 是 | 系统动作详情查询 |

---

## 4. 公共数据结构

### 4.1 选择器一级分类对象

```json
{
  "categoryCode": "chest",
  "categoryName": "胸",
  "sortOrder": 10
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `categoryCode` | `string` | 是 | 一级分类编码，面向前端筛选 |
| `categoryName` | `string` | 是 | 一级分类名称 |
| `sortOrder` | `number` | 是 | 排序值 |

### 4.2 选择器细分肌肉对象

```json
{
  "muscleId": 2,
  "muscleName": "胸大肌上沿",
  "muscleCode": "pectoralis_major_upper",
  "parentMuscleId": 1,
  "parentMuscleName": "胸大肌",
  "sortOrder": 11
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `muscleId` | `number` | 是 | 肌肉 ID |
| `muscleName` | `string` | 是 | 肌肉名称 |
| `muscleCode` | `string` | 是 | 肌肉编码 |
| `parentMuscleId` | `number \| null` | 否 | 父级肌肉 ID |
| `parentMuscleName` | `string \| null` | 否 | 父级肌肉名称 |
| `sortOrder` | `number` | 是 | 排序值 |

### 4.3 选择器分类分组对象

```json
{
  "categoryCode": "chest",
  "categoryName": "胸",
  "sortOrder": 10,
  "children": [
    {
      "muscleId": 2,
      "muscleName": "胸大肌上沿",
      "muscleCode": "pectoralis_major_upper",
      "parentMuscleId": 1,
      "parentMuscleName": "胸大肌",
      "sortOrder": 11
    },
    {
      "muscleId": 3,
      "muscleName": "胸大肌中部",
      "muscleCode": "pectoralis_major_middle",
      "parentMuscleId": 1,
      "parentMuscleName": "胸大肌",
      "sortOrder": 12
    }
  ]
}
```

### 4.4 系统动作列表项

```json
{
  "exerciseId": 1001,
  "exerciseName": "Barbell Bench Press",
  "exerciseType": "strength",
  "movementType": "push",
  "defaultUnit": "kg",
  "defaultStructureType": "set_based",
  "videoUrl": "https://example.com/videos/barbell-bench-press",
  "primaryMuscles": [
    {
      "muscleId": 3,
      "muscleName": "胸大肌中部",
      "muscleCode": "pectoralis_major_middle"
    }
  ],
  "secondaryMuscles": [
    {
      "muscleId": 14,
      "muscleName": "肱三头肌",
      "muscleCode": "triceps_brachii"
    },
    {
      "muscleId": 10,
      "muscleName": "三角肌前束",
      "muscleCode": "deltoid_front"
    }
  ],
  "equipmentNames": [
    "杠铃",
    "卧推凳"
  ]
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `exerciseId` | `number` | 是 | 系统动作 ID |
| `exerciseName` | `string` | 是 | 动作名称 |
| `exerciseType` | `string` | 是 | 动作类型 |
| `movementType` | `string \| null` | 否 | 动作模式，例如 `push/pull/squat/hinge/cardio` |
| `defaultUnit` | `string` | 是 | 默认单位 |
| `defaultStructureType` | `string` | 是 | `set_based` / `single_segment` |
| `videoUrl` | `string \| null` | 否 | 动作示范视频地址 |
| `primaryMuscles` | `ListItemMuscleResponse[]` | 是 | 主要肌肉轻量对象列表 |
| `secondaryMuscles` | `ListItemMuscleResponse[]` | 是 | 次要肌肉轻量对象列表 |
| `equipmentNames` | `string[]` | 是 | 相关器械名称列表 |

说明：

- 相比 v1，列表项中的肌肉信息从 `string[]` 升级为轻量对象数组
- 这样前端可直接复用这些字段做标签展示、高亮和后续扩展
- 当前列表接口仍保持轻量，不返回完整器械对象数组

### 4.5 列表项轻量肌肉对象

```json
{
  "muscleId": 3,
  "muscleName": "胸大肌中部",
  "muscleCode": "pectoralis_major_middle"
}
```

### 4.6 肌肉对象

```json
{
  "muscleId": 11,
  "muscleName": "胸大肌",
  "muscleCode": "pectoralis_major",
  "relationType": "primary"
}
```

### 4.7 器械对象

```json
{
  "equipmentId": 3,
  "equipmentName": "杠铃",
  "sceneType": "gym"
}
```

---

## 5. 接口详情

### 5.1 E0 动作选择器筛选元数据查询

- 路径：`GET /api/exercises/system/filter-options`
- 作用：返回动作弹窗选择器所需的左侧一级分类和可选细分肌肉列表

查询参数：

当前 MVP 无必传查询参数。

请求示例：

```http
GET /api/exercises/system/filter-options
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "categories": [
      {
        "categoryCode": "chest",
        "categoryName": "胸",
        "sortOrder": 10,
        "children": [
          {
            "muscleId": 2,
            "muscleName": "胸大肌上沿",
            "muscleCode": "pectoralis_major_upper",
            "parentMuscleId": 1,
            "parentMuscleName": "胸大肌",
            "sortOrder": 11
          },
          {
            "muscleId": 3,
            "muscleName": "胸大肌中部",
            "muscleCode": "pectoralis_major_middle",
            "parentMuscleId": 1,
            "parentMuscleName": "胸大肌",
            "sortOrder": 12
          }
        ]
      },
      {
        "categoryCode": "back",
        "categoryName": "背",
        "sortOrder": 20,
        "children": [
          {
            "muscleId": 6,
            "muscleName": "背阔肌",
            "muscleCode": "latissimus_dorsi",
            "parentMuscleId": 5,
            "parentMuscleName": "背部",
            "sortOrder": 21
          }
        ]
      }
    ]
  }
}
```

实现约定：

- 一级分类为产品层概念，不要求与数据库 `muscles` 表中的 `group` 节点完全一一对应
- 推荐当前返回以下一级分类：
  - `chest`
  - `back`
  - `shoulder`
  - `arms`
  - `legs`
  - `core`
  - `cardio`
- `arms` 可聚合：
  - `肱二头肌`
  - `肱三头肌`
  - `前臂`
- `legs` 可聚合：
  - `臀部`
  - `腿部`
  - `小腿`
- 响应中的 `children` 用于未来支持二级筛选
- 当前前端若暂时不做二级筛选，也可以只消费 `categories`

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401

### 5.2 E1 系统动作搜索 / 列表查询

- 路径：`GET /api/exercises/system`
- 作用：返回系统动作列表，供模板编辑器搜索、选择、筛选使用

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `keyword` | `string` | 否 | 动作名称关键词，按名称模糊匹配 |
| `categoryCode` | `string` | 否 | 一级分类过滤，例如 `chest` / `back` / `shoulder` / `arms` / `legs` / `core` / `cardio` |
| `muscleId` | `number` | 否 | 精确肌肉过滤，用于二级筛选或高级筛选 |
| `exerciseType` | `string` | 否 | 动作类型过滤 |
| `movementType` | `string` | 否 | 动作模式过滤 |
| `structureType` | `string` | 否 | 默认结构类型过滤：`set_based` / `single_segment` |
| `sceneType` | `string` | 否 | 器械使用场景过滤，例如 `home` / `gym` / `both` |
| `page` | `number` | 否 | 页码，默认 `1` |
| `pageSize` | `number` | 否 | 每页数量，默认 `20`，最大 `100` |

请求示例 1：按一级分类加载默认列表

```http
GET /api/exercises/system?categoryCode=chest&page=1&pageSize=20
Authorization: Bearer <accessToken>
```

请求示例 2：按一级分类 + 关键词联合过滤

```http
GET /api/exercises/system?categoryCode=legs&keyword=squat&page=1&pageSize=20
Authorization: Bearer <accessToken>
```

请求示例 3：按细分肌肉精确过滤

```http
GET /api/exercises/system?muscleId=24&page=1&pageSize=20
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "page": 1,
    "pageSize": 20,
    "total": 2,
    "records": [
      {
        "exerciseId": 1001,
        "exerciseName": "Barbell Bench Press",
        "exerciseType": "strength",
        "movementType": "push",
        "defaultUnit": "kg",
        "defaultStructureType": "set_based",
        "videoUrl": "https://example.com/videos/barbell-bench-press",
        "primaryMuscles": [
          {
            "muscleId": 3,
            "muscleName": "胸大肌中部",
            "muscleCode": "pectoralis_major_middle"
          }
        ],
        "secondaryMuscles": [
          {
            "muscleId": 14,
            "muscleName": "肱三头肌",
            "muscleCode": "triceps_brachii"
          },
          {
            "muscleId": 10,
            "muscleName": "三角肌前束",
            "muscleCode": "deltoid_front"
          }
        ],
        "equipmentNames": [
          "杠铃",
          "卧推凳"
        ]
      }
    ]
  }
}
```

实现约定：

- 只查询系统动作：
  - `owner_user_id IS NULL`
  - `is_active = 1`
- `categoryCode` 的语义是“按产品定义的一级分类映射出的肌肉集合过滤”
- `muscleId` 的语义是“精确匹配某一个肌肉节点”
- 当 `categoryCode` 和 `muscleId` 同时传入时，按“与”关系处理
- 默认按名称排序；如后续需要更明显的搜索体验，可增加：
  - 名称前缀匹配优先
  - 热门动作优先
- `defaultStructureType` 为前端初始化模板动作结构的关键字段，必须返回

最小可用字段集：

如果后端希望先快速落地最小版本，至少要返回：

- `exerciseId`
- `exerciseName`
- `defaultStructureType`

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `INVALID_ARGUMENT`：分页或过滤参数格式非法，HTTP 400

### 5.3 E2 系统动作详情查询

- 路径：`GET /api/exercises/system/{exerciseId}`
- 作用：返回单个系统动作的完整详情，供动作详情展示、模板回显、后续训练打卡展示使用

请求示例：

```http
GET /api/exercises/system/1001
Authorization: Bearer <accessToken>
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "exerciseId": 1001,
    "exerciseName": "Barbell Bench Press",
    "exerciseType": "strength",
    "movementType": "push",
    "defaultUnit": "kg",
    "defaultStructureType": "set_based",
    "videoUrl": "https://example.com/videos/barbell-bench-press",
    "calorieBurnReference": 6.5,
    "calorieReferenceUnit": "kcal/min",
    "primaryMuscles": [
      {
        "muscleId": 3,
        "muscleName": "胸大肌中部",
        "muscleCode": "pectoralis_major_middle",
        "relationType": "primary"
      }
    ],
    "secondaryMuscles": [
      {
        "muscleId": 14,
        "muscleName": "肱三头肌",
        "muscleCode": "triceps_brachii",
        "relationType": "secondary"
      },
      {
        "muscleId": 10,
        "muscleName": "三角肌前束",
        "muscleCode": "deltoid_front",
        "relationType": "secondary"
      }
    ],
    "equipments": [
      {
        "equipmentId": 3,
        "equipmentName": "杠铃",
        "sceneType": "gym"
      },
      {
        "equipmentId": 4,
        "equipmentName": "卧推凳",
        "sceneType": "gym"
      }
    ]
  }
}
```

字段说明补充：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `calorieBurnReference` | `number \| null` | 否 | 热量消耗参考值 |
| `calorieReferenceUnit` | `string \| null` | 否 | 热量参考单位 |
| `primaryMuscles` | `MuscleResponse[]` | 是 | 主要肌肉对象列表 |
| `secondaryMuscles` | `MuscleResponse[]` | 是 | 次要肌肉对象列表 |
| `equipments` | `EquipmentResponse[]` | 是 | 器械对象列表 |

实现约定：

- 若动作不存在、不是系统动作、或已禁用，统一返回 `EXERCISE_NOT_FOUND`
- `defaultStructureType` 必须返回
- 后续如增加“动作适合场景”“注意事项”等字段，可在详情接口中扩展，不建议优先扩到列表接口

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `EXERCISE_NOT_FOUND`：动作不存在或不可访问，HTTP 404

---

## 6. 推荐错误码

| 错误码 | HTTP 状态码 | 含义 |
|------|------|------|
| `UNAUTHORIZED` | 401 | 未登录或 token 无效 |
| `INVALID_ARGUMENT` | 400 | 查询参数格式非法 |
| `EXERCISE_NOT_FOUND` | 404 | 动作不存在、不是系统动作或已禁用 |

---

## 7. 与数据库结构的对应关系

### 7.1 当前数据库是否足够

当前数据库结构已经足以支撑本次动作选择器改造，不要求新增表：

- `muscles`
  - 提供层级关系：`parent_id`、`muscle_level`、`sort_order`
- `exercise_muscles`
  - 提供动作与主要/次要肌肉关系：`relation_type`
- `equipments`
  - 提供器械与使用场景：`scene_type`
- `exercise_equipments`
  - 提供动作与器械关系

### 7.2 为什么仍然需要新增 E0 接口

虽然数据库有肌肉树，但前端动作弹窗需要的是：

- 一级粗粒度分类
- 可直接渲染的排序结果
- 可选的细分肌肉列表

如果没有 E0，前端只能：

- 硬编码“胸、背、肩、腿、手臂、核心、有氧”
- 自己维护分类和数据库肌肉节点的映射关系

这会导致前后端存在两套分类定义，不利于后续维护。

因此推荐由后端统一输出动作选择器筛选元数据。

---

## 8. 与其他模块的衔接

### 8.1 与 cycle_template 的衔接

`cycle_template` 前端编辑器依赖本模块的：

- `defaultStructureType`
- `categoryCode` 对应的分类过滤能力
- 列表项中的轻量肌肉对象

约定如下：

1. 用户选择动作后，前端必须以 `defaultStructureType` 初始化动作结构
2. 前端不应自行猜测某动作是 `set_based` 还是 `single_segment`
3. 前端动作选择弹窗默认流程建议为：
   - 先请求 `E0`
   - 默认选中第一个一级分类
   - 再调用 `E1` 加载该分类下动作
4. 后端在保存模板时仍会再次校验：
   - 请求中的 `structureType`
   - 是否与动作的 `defaultStructureType` 一致

### 8.2 与 training_session 的衔接

后续训练打卡模块也建议复用：

- 动作名称
- 动作结构类型
- 肌肉信息
- 视频示范地址

因此本模块不建议只做成 `cycle_template` 的内部私有接口，而应作为独立动作查询能力维护。

---

## 9. 当前实现建议

如果你准备分阶段落地，我建议按下面顺序推进：

### 第一步：补齐动作选择器元数据

先实现：

- `GET /api/exercises/system/filter-options`

目标：

- 前端不再硬编码左侧分类
- 为后续二级肌肉筛选留出口

### 第二步：扩展列表接口筛选语义

改造：

- `GET /api/exercises/system`

新增重点：

- `categoryCode`
- 列表项中的轻量肌肉对象数组

目标：

- 支撑“左侧分类 + 顶部搜索框 + 右侧动作列表”的弹窗交互

### 第三步：保持详情接口稳定

继续复用：

- `GET /api/exercises/system/{exerciseId}`

目标：

- 用于动作详情查看
- 用于模板回显或后续训练页补充信息展示

---

## 10. 备注

- 当前文档只覆盖“系统动作查询”，不覆盖动作管理写接口。
- 当前文档中的 `filter-options` 和 `categoryCode` 为动作弹窗选择器新增契约，可能尚未在现有后端代码中完全实现。
- 如果后续确认要支持“用户自定义动作”，建议单独新增：
  - `exercise_PRD.md`
  - `exercise_接口文档_v3.md`
  或拆出“system exercise / user exercise”两组接口文档。
