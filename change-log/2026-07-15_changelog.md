# 2026-07-15 Changelog

时间范围：2026-07-15

## 今日概览

今天这轮开发的核心成果，是把 `cycle_template` 从固定字段动作参数模型正式升级到了结构化三层模型，同时补齐了 `exercise` 系统动作查询模块，让模板编辑器终于可以通过真实动作数据完成动作搜索与结构初始化。

到这一步，项目里“动作”不再只是一个被模板引用的静态名称，而是已经成为一个独立的系统能力来源：
- 后端有专门的动作查询服务、策略和接口
- 前端模板编辑器已经接入系统动作搜索
- 模板动作参数结构已和动作默认结构类型联动

## 今日完成内容

### 1. 落地 Exercise 系统动作查询模块

新增后端动作查询相关代码，核心包括：
- `ExerciseController`
- `ExerciseQueryApplicationService`
- `SystemExerciseLookupService`
- `ExerciseQueryPolicyService`
- `ExerciseAssembler`
- 动作查询相关 Entity / Mapper / VO / DTO

当前已完成能力：
- 系统动作搜索 / 列表查询
- 系统动作详情查询
- 过滤用户自定义动作，仅返回系统动作
- 返回 `defaultStructureType` 供模板编辑器初始化动作结构

这意味着动作库第一次从“数据库里有数据”升级成了“前后端可直接依赖的查询模块”。

### 2. 完成 cycle_template v2 三层动作结构改造

今天对 `cycle_template` 做了中等规模重构，核心变化是把旧的固定字段：
- `targetSets`
- `targetRepsMin`
- `targetWeightKg`
- `targetDurationSeconds`

升级为新的三层模型：
- 动作 `exercise`
- 执行项 `item`
- 参数 `metric`

后端新增 / 重构内容包括：
- `StructureType`
- `ItemType`
- `MetricKey`
- `ExerciseStructurePolicyService`
- `CycleDayExerciseItemEntity`
- `CycleDayExerciseItemMetricEntity`
- `CycleDayExerciseItemMapper`
- `CycleDayExerciseItemMetricMapper`
- `CycleTemplateVersionDomainService`
- `CycleTemplateCommandApplicationService`
- `CycleTemplateQueryApplicationService`
- `CycleTemplateActivationApplicationService`

这次改造的价值很大，因为它让模板终于可以自然表达：
- 按组动作
- 单段持续有氧动作
- 每组 / 每段不同参数

### 3. 完成数据库结构升级脚本

新增：
- `backend/src/main/resources/db/migration/V5__cycle_template_structure_v2.sql`

本轮数据库升级的核心方向是：
- 给 `exercises` 增加 `default_structure_type`
- 把 `cycle_day_exercises` 从固定参数表改造成动作头表
- 新增：
  - `cycle_day_exercise_items`
  - `cycle_day_exercise_item_metrics`

这一步让数据库结构正式和新的动作参数模型对齐。

### 4. 完成前端模板编辑器改造并接入动作搜索

前端 `cycle-template` 模块完成了较大幅度升级，主要包括：
- 接入系统动作搜索接口
- 根据 `defaultStructureType` 初始化动作结构
- 新增：
  - `ExerciseStructureEditor`
  - `ExerciseItemEditor`
  - `MetricEditor`
- 重写：
  - `CycleTemplateEditor`
  - `useCycleTemplateEditor`
  - 映射、校验、格式化与类型定义

今天最关键的联调结果是：
- 模板创建草稿
- 编辑
- 保存
- 启用

这些主流程已经能在新结构下跑通。

### 5. 补齐 Exercise 与 Cycle Template 的文档体系

新增或重写文档：
- `docs/interfaces/exercise_接口文档.md`
- `docs/interfaces/cycle_template_接口文档_v2.md`
- `docs/backend/exercise_module/exercise_DDD.md`
- `docs/frontend/exercise_module/exercise_DDD.md`
- `docs/backend/cycle_template_module/cycle_template_DDD_v2.md`
- `docs/backend/cycle_template_module/动作参数模型数据库改造清单.md`
- `docs/backend/cycle_template_module/动作参数模型改造后端影响分析.md`
- `docs/backend/cycle_template_module/动作参数模型改造清单.md`

这轮文档最大的价值，是把“动作默认结构类型”和“模板动作三层模型”的边界写清楚了，后续不会轻易再回到固定字段模型。

### 6. 补充测试

新增和增强测试包括：
- `ExerciseIntegrationTest`
- `ExerciseQueryPolicyServiceTest`
- `ExerciseStructurePolicyServiceTest`
- `CycleTemplateIntegrationTest`
- `CycleTemplatePolicyServiceTest`

说明今天的改造不是“只把代码改到能编译”，而是已经开始为新结构补回相应测试覆盖。

## 今日产出总结

今天真正完成的，不只是“多了一个动作搜索接口”，而是 DailyForge 第一次把“动作库”和“训练模板结构”正式打通了。

这件事的意义在于：
- 模板编辑器以后不再靠硬编码字段理解动作
- 系统动作可以决定模板动作的默认结构
- 未来训练打卡模块可以直接复用今天确定下来的三层模型

从项目演进角度看，这一天相当于把 `cycle_template` 从 MVP 第一版推进到了一个更接近长期可演进版本的形态。

## 做得好的地方

- 你没有只补一个“临时可用”的动作搜索，而是顺手把动作模块独立成了可复用查询能力。
- 这次 `cycle_template v2` 改造虽然大，但范围控制得还算清楚，主线没有漂移。
- 前后端和文档一起推进，减少了“接口先改完、前端再猜”的风险。
- `defaultStructureType` 作为动作结构初始化依据这件事已经定死，这是很重要的设计收口。

## 可以继续优化的地方

- 今天主流程已经通了，但新结构下的极端校验场景还值得继续补测试，例如重复 metric、非法结构类型、空 item 等。
- 当前虽然接上了动作搜索，但动作筛选能力还偏基础，后续可以再补肌肉、器械、场景过滤的前端体验。
- `training_session` 还没开始，建议尽快承接今天的新结构，不然这套模型的价值还没完全发挥出来。

## 我对今天开发质量的评价

这轮开发是很扎实的，而且技术判断是对的。你不是为了赶进度继续往旧固定字段模型上打补丁，而是把动作参数模型一次性抬到了一个能支撑后续训练打卡和统计分析的层级。

这类改造最难的不是写出代码，而是同时把数据库、后端、前端、测试和文档都拉齐。今天这件事你已经做到了一个比较完整的程度。

## 建议的下一步

1. 开始 `training session` 模块设计，直接复用今天确定的三层动作结构。
2. 先定义训练打卡时“计划值”和“实际值”的映射关系。
3. 再做训练完成后的当前日推进与历史记录沉淀。
