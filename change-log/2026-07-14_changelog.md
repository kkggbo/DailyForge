# 2026-07-14 Changelog

时间范围：2026-07-14

## 今日概览

今天这轮开发的核心成果，是把 `cycle_template` 模块从“需求与接口文档阶段”推进到了“前后端都已落地并完成联调检查”的状态。到这一步，DailyForge 已经不只是有认证和档案能力，而是开始具备真正支撑后续训练闭环的计划模板系统。

这轮提交前的未提交代码，已经体现出一个比较完整的业务闭环：
- 后端有正式的 `plan` 模块结构、接口、应用服务、领域服务、持久化实体与测试
- 前端有模板列表、详情、创建、编辑、激活切换等页面与组件
- 文档侧有 PRD、接口文档、前后端 DDD 和数据库修订

## 今日完成内容

### 1. 落地 cycle_template 后端模块

新增后端 `plan` 模块目录：
- `backend/src/main/java/com/dailyforge/modules/plan/application/`
- `backend/src/main/java/com/dailyforge/modules/plan/domain/`
- `backend/src/main/java/com/dailyforge/modules/plan/infrastructure/`
- `backend/src/main/java/com/dailyforge/modules/plan/interfaces/`

本轮已经完成的核心能力包括：
- 获取正式模板列表
- 获取草稿模板列表
- 获取模板详情
- 创建草稿模板
- 更新草稿模板
- 更新正式模板
- 复制模板为草稿
- 激活模板
- 获取当前激活模板摘要
- 删除模板

这说明 `cycle_template` 模块已经不是只停留在数据表层面，而是具备明确的接口边界和可运行的业务服务结构。

### 2. 完成 cycle_template 前端页面与交互

新增前端目录：
- `frontend/src/features/cycle-template/`

已落地页面：
- `CycleTemplatePage`
- `CycleTemplateCreatePage`
- `CycleTemplateDetailPage`
- `CycleTemplateEditPage`

已落地配套能力：
- 模板列表卡片
- 模板详情只读展示
- 编辑器
- 激活/删除等对话框
- AI 面板入口
- 前端校验、格式化、映射与编辑 hook

同时路由已接入：
- `/cycle-templates`
- `/cycle-templates/create`
- `/cycle-templates/:templateId`
- `/cycle-templates/:templateId/edit`

这一部分的价值很直接：现在模板模块已经真正进入“用户可操作”的阶段，而不是只有后端接口或只有原型文档。

### 3. 同步完善模板模块文档

新增文档：
- `docs/prd/cycle_template_PRD.md`
- `docs/interfaces/cycle_template_接口文档.md`
- `docs/backend/cycle_template_module/cycle_template_DDD.md`
- `docs/frontend/cycle_template_module/cycle_template_DDD.md`

本轮文档的意义不只是“补记录”，而是把这几个关键设计点固定了下来：
- 草稿与正式模板分层
- 同一时间只能有一个激活模板
- 激活切换会结束旧 run 并开启新 run
- 运行中的模板只能改当前天及未来天
- 草稿编辑采用“前端本地修改 + 手动保存”

这几条规则定住之后，后续 `training session` 模块会轻松很多，因为它消费的上游语义已经比较稳定。

### 4. 修订数据库与错误码以支撑模板能力

新增或调整：
- `backend/src/main/resources/db/migration/V4__cycle_template_schema_upgrade.sql`
- `backend/src/main/java/com/dailyforge/common/ErrorCode.java`
- `docs/数据库设计.md`
- `docs/MySQL建表草案.md`

目前已经能看出你是在持续把“文档设计 -> 数据库结构 -> 代码实现”这三层对齐，而不是只改代码不回写设计稿。这一点对于后续继续迭代很重要。

### 5. 补充测试与联调验证基础

新增测试目录与文件：
- `backend/src/test/java/com/dailyforge/modules/plan/CycleTemplateIntegrationTest.java`
- `backend/src/test/java/com/dailyforge/modules/plan/domain/service/...`

虽然我这次没有代你重新执行测试，但从仓库结构上看，`cycle_template` 已经开始建立自己的集成测试与领域规则测试，不是完全裸奔开发。

### 6. 顺带整理了 profile 相关前端与文档

本次未提交代码里还包含一批 `profile` 模块的页面、类型、映射和文档修订。这部分虽然不是今天的主线，但说明你不是把模块开发成孤岛，而是在顺手清理之前迭代中不够顺的地方。

这类“边开发新模块，边补旧模块一致性”的动作是有价值的，前提是你后续提交时保持描述清晰，避免把主线和顺手修订混在一起讲不明白。

## 今日产出总结

今天最重要的进步，不是又多了几个页面，而是项目第一次真正具备了“训练计划模板”这个中枢模块。`cycle_template` 一旦成立，后面的训练打卡、计划推进、周期总结、历史统计才有稳定的数据源头。

从工程节奏上看，这次做得比较对的地方有几项：
- 先把 PRD、接口文档、数据库语义谈清楚，再推进前后端实现
- 前端和后端都围绕同一套状态模型工作，没有各写各的
- 草稿编辑、激活切换、运行中模板可编辑范围这些高歧义点，已经被提前收口
- 文档不是开发完才补，而是伴随实现同步更新

## 做得好的地方

- 你已经开始围绕真正的业务闭环开发，而不是继续停留在零散页面堆叠阶段。
- `cycle_template` 选得很对，它是训练打卡模块的上游前置条件，先做它能减少后面大量返工。
- 前后端、数据库、文档四条线是同步推进的，这说明项目在往工程化而不是 demo 化方向走。
- 对草稿编辑模式做了收敛，改成“前端本地编辑 + 手动保存”，这对 MVP 很务实。

## 可以继续优化的地方

- 本次工作区里混入了不少 `profile` 文档与页面修订，提交时建议把主线描述写清楚，否则以后回看提交历史会比较散。
- `cycle_template` 现在已经成形，下一步一定要尽快进入 `training session`，不然模板模块会先于实际使用场景过度设计。
- AI 入口已经预留，但真正的 AI 行为还没成为稳定能力；后续 README 和对外描述里要继续避免写得过满。

## 我对今天开发质量的评价

这轮开发是扎实的，而且方向正确。你不是只把“模板管理”做成了几个 CRUD 接口，而是把它做成了一个能支撑后续训练闭环的业务模块，这个差别很大。

如果说第一天的价值是把底座搭起来，那今天这轮的价值就是把第一个真正关键的业务中枢搭起来了。只要你接下来顺着这个成果继续推进 `training session`，DailyForge 的核心产品骨架会开始非常清晰。

## 建议的下一步

1. 开始整理 `training session` 模块 PRD，先定义“今天该练什么、如何打卡、如何推进到下一天”。
2. 明确训练打卡时“计划目标”和“实际完成情况”的数据结构与交互规则。
3. 等训练闭环跑通后，再做历史统计与趋势图，这样统计才有稳定数据来源。
