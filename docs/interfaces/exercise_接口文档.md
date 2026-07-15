# DailyForge Exercise 模块接口文档

> 版本：v1.0  
> 更新日期：2026-07-15  
> 模块归属：`backend` 单体应用，建议代码包归属 `com.dailyforge.modules.exercise` 或现阶段动作查询所在模块  
> 文档状态：接口契约设计阶段

---

## 1. 文档说明

本文档描述 DailyForge 当前 MVP 阶段“系统动作查询”相关接口契约。

本模块的定位不是训练模板本身，而是为以下场景提供统一动作数据来源：

- `cycle_template` 模板编辑页动作选择
- 后续 `training_session` 训练打卡动作展示
- 动作详情查看
- 后续 AI 生成计划时的动作引用与过滤

当前范围仅包含：

1. 系统动作搜索 / 列表查询
2. 系统动作详情查询

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

- 当前动作查询接口主要服务于登录后的训练模板编辑与训练相关页面
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

### 2.5 肌肉与器械字段约定

为了便于前端展示与筛选，当前详情接口建议直接返回聚合后的肌肉和器械信息，而不是只返回关系表 id。

肌肉关系分为：

- `primary`
- `secondary`

器械关系当前只需要体现：

- 名称
- 使用场景

---

## 3. 接口列表

| 编号 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|------|
| E1 | GET | `/api/exercises/system` | 是 | 系统动作搜索 / 列表查询 |
| E2 | GET | `/api/exercises/system/{exerciseId}` | 是 | 系统动作详情查询 |

---

## 4. 公共数据结构

## 4.1 系统动作列表项

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
    "胸大肌"
  ],
  "secondaryMuscles": [
    "肱三头肌",
    "三角肌前束"
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
| `primaryMuscles` | `string[]` | 是 | 主要肌肉名称列表 |
| `secondaryMuscles` | `string[]` | 是 | 次要肌肉名称列表 |
| `equipmentNames` | `string[]` | 是 | 相关器械名称列表 |

说明：

- 列表接口中的肌肉和器械字段可以按“轻量展示”返回，不要求附带完整对象结构
- 若前端只做下拉搜索，也可以只使用其中最小字段集：
  - `exerciseId`
  - `exerciseName`
  - `defaultStructureType`

## 4.2 肌肉对象

```json
{
  "muscleId": 11,
  "muscleName": "胸大肌",
  "muscleCode": "pectoralis_major",
  "relationType": "primary"
}
```

## 4.3 器械对象

```json
{
  "equipmentId": 3,
  "equipmentName": "杠铃",
  "sceneType": "gym"
}
```

---

## 5. 接口详情

### 5.1 E1 系统动作搜索 / 列表查询

- 路径：`GET /api/exercises/system`
- 作用：返回系统动作列表，供模板编辑器搜索、选择、筛选使用

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `keyword` | `string` | 否 | 动作名称关键词，按名称模糊匹配 |
| `exerciseType` | `string` | 否 | 动作类型过滤 |
| `movementType` | `string` | 否 | 动作模式过滤 |
| `structureType` | `string` | 否 | 默认结构类型过滤：`set_based` / `single_segment` |
| `sceneType` | `string` | 否 | 器械使用场景过滤，例如 `home` / `gym` |
| `muscleId` | `number` | 否 | 按主要或次要肌肉过滤 |
| `page` | `number` | 否 | 页码，默认 `1` |
| `pageSize` | `number` | 否 | 每页数量，默认 `20`，最大 `100` |

请求示例：

```http
GET /api/exercises/system?keyword=bench&structureType=set_based&page=1&pageSize=20
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
          "胸大肌"
        ],
        "secondaryMuscles": [
          "肱三头肌",
          "三角肌前束"
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
- 默认按名称排序；如果后续前端需要更明显的搜索体验，可增加：
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

### 5.2 E2 系统动作详情查询

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
        "muscleId": 11,
        "muscleName": "胸大肌",
        "muscleCode": "pectoralis_major",
        "relationType": "primary"
      }
    ],
    "secondaryMuscles": [
      {
        "muscleId": 21,
        "muscleName": "肱三头肌",
        "muscleCode": "triceps_brachii",
        "relationType": "secondary"
      },
      {
        "muscleId": 31,
        "muscleName": "三角肌前束",
        "muscleCode": "anterior_deltoid",
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

## 7. 与其他模块的衔接

### 7.1 与 cycle_template 的衔接

`cycle_template` 前端编辑器依赖本模块的：

- `defaultStructureType`

约定如下：

1. 用户选择动作后，前端必须以 `defaultStructureType` 初始化动作结构
2. 前端不应自行猜测某动作是 `set_based` 还是 `single_segment`
3. 后端在保存模板时仍会再次校验：
   - 请求中的 `structureType`
   - 是否与动作的 `defaultStructureType` 一致

### 7.2 与 training_session 的衔接

后续训练打卡模块也建议复用：

- 动作名称
- 动作结构类型
- 肌肉信息
- 视频示范地址

因此本模块不建议只做成 `cycle_template` 的内部私有接口，而应作为独立动作查询能力维护。

---

## 8. 当前实现建议

如果你准备先快速补齐最小可用版本，我建议分两步：

### 第一步：最小可用

先实现：

- `GET /api/exercises/system`
- `GET /api/exercises/system/{exerciseId}`

最少返回字段：

- `exerciseId`
- `exerciseName`
- `defaultStructureType`

### 第二步：增强返回

再补：

- `exerciseType`
- `movementType`
- `defaultUnit`
- `videoUrl`
- `primaryMuscles`
- `secondaryMuscles`
- `equipmentNames` / `equipments`

这样可以先满足模板联调，再逐步满足动作展示体验。

---

## 9. 备注

- 当前文档只覆盖“系统动作查询”，不覆盖动作管理写接口。
- 如果后续确认要支持“用户自定义动作”，建议单独新增：
  - `exercise_PRD.md`
  - `exercise_接口文档_v2.md`
  或拆出“system exercise / user exercise”两组接口文档。
