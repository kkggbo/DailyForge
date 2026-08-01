# DailyForge AI接入与提示词上下文设计

> 版本：v1.0  
> 日期：2026-07-31  
> 模块归属：`backend` 单体应用，建议代码包为 `com.dailyforge.modules.aicoach`  
> 文档状态：实现设计初稿

---

## 1. 文档目标

本文档用于定义 DailyForge `ai_coach` 模块在 MVP 阶段的 AI 接入方案、Prompt 分层、上下文组织方式、工具调用边界、结果修复策略与可观测性要求。

本文档主要服务于：

- 后端 AI 调用链路实现
- `template_generation` 场景实现
- `cycle_summary` 场景实现
- AI 输出结构校验与修复逻辑实现
- 后续提示词迭代、模型切换、失败排查与效果优化

本文档与以下文档配套使用：

- [ai_coach_PRD.md](../../prd/ai_coach_PRD.md)
- [ai_coach_接口文档.md](../../interfaces/ai_coach_接口文档.md)
- [cycle_template_接口文档_v2.md](../../interfaces/cycle_template_接口文档_v2.md)
- [workout_接口文档.md](../../interfaces/workout_接口文档.md)

---

## 2. 本期已确认方案

本期按以下已确认方案设计：

1. AI 接入技术路线：
   - Spring AI
   - OpenAI 兼容客户端
   - 接入 DeepSeek 模型
2. 调用模式：
   - 采用“类智能体”工作流
   - 后端把整理后的 prompt 与可用 tools 一次性发给模型
   - 模型按需调用只读工具补充数据
   - 数据足够后返回完整 JSON
3. 数据获取策略：
   - MVP 允许把完整训练明细作为可用上下文来源
   - 同时提供只读工具让模型按需继续查询
4. 修复策略：
   - 首次输出后端先做结构校验
   - 若有问题，把原 JSON 与问题清单发回模型修复
   - 最多修复 2 次
5. 审计策略：
   - 不保存完整原始 prompt
   - 只保存摘要、版本号与截断预览
6. 产品优先级：
   - 优先实现 `template_generation`

---

## 3. 设计原则

### 3.1 AI 负责建议生成，系统负责规则兜底

AI 可以负责：

- 生成训练模板草稿建议
- 生成模板设计说明
- 对已完成循环做复盘总结与建议

系统必须负责：

- 权限校验
- 资料完整度校验
- 任务状态机管理
- 工具调用边界控制
- 动作合法性校验
- 模板结构校验
- 最终结果落库
- 错误处理与审计记录

原则：

- 不直接信任 AI 输出可落库
- 所有 AI 输出都必须经过系统校验

### 3.2 Prompt、工具调用、业务规则三层分离

不要把所有规则都堆进自然语言 Prompt。

建议分成三层：

1. 业务前置校验层
   - 在调用模型前判断当前请求是否允许进入 AI 流程
2. 模型工作层
   - 由 system prompt、scenario prompt、tools、上下文组成
3. 结果校验层
   - 校验结构、枚举、动作、模板、数值、必填字段

### 3.3 优先结构化输出

MVP 要求模型返回严格 JSON。

原因：

- 易解析
- 易校验
- 易回显
- 易修复
- 易替换模型

### 3.4 AI 只能调用只读工具

本期不允许模型直接执行：

- 写数据库
- 创建草稿模板
- 启用模板
- 修改用户资料
- 修改训练记录

所有持久化操作只能由后端在校验通过后执行。

### 3.5 Tool calling 轮次受限

为了避免成本、时延与失控调用链膨胀，本期默认约束：

- 单次 AI 任务最多 6 轮工具调用

超限处理：

- 终止本次模型执行
- 标记任务失败
- 返回 `AI_SERVICE_UNAVAILABLE` 或专用内部错误

---

## 4. 接入总体架构

建议后端内部按以下职责拆分。

### 4.1 `AiCoachApplicationService`

职责：

- 接收接口层请求
- 校验权限与基础业务前置条件
- 创建 AI 任务
- 调度具体场景执行器

### 4.2 `AiTaskOrchestrator`

职责：

- 驱动整次 AI 任务流程
- 维护工具调用轮次
- 维护首次生成与修复流程
- 管理任务状态流转：
  - `pending`
  - `running`
  - `succeeded`
  - `failed`

### 4.3 `AiScenarioExecutor`

按场景拆分：

- `TemplateGenerationExecutor`
- `CycleSummaryExecutor`

职责：

- 请求上下文构建器组装上下文
- 请求 Prompt 构建器生成模型输入
- 驱动模型与工具多轮交互
- 返回结构化 JSON 候选结果

### 4.4 `AiContextBuilder`

按场景拆分：

- `TemplateGenerationContextBuilder`
- `CycleSummaryContextBuilder`

职责：

- 读取业务数据
- 清洗与裁剪上下文
- 生成首轮直接输入模型的基础上下文

### 4.5 `AiToolRegistry`

职责：

- 注册当前场景允许暴露的只读工具
- 控制工具白名单
- 为模型调用提供统一入口

### 4.6 `AiPromptBuilder`

职责：

- 组装 system prompt
- 组装场景任务说明
- 组装输出 schema 说明
- 注入禁止事项与边界条件

### 4.7 `AiOutputValidator`

职责：

- 校验 JSON 是否可解析
- 校验字段结构是否完整
- 校验枚举、数值、动作与模板结构是否合法

### 4.8 `AiResultPersistenceService`

职责：

- `template_generation` 成功后创建 `cycle template draft`
- `cycle_summary` 成功后保存总结结果
- 更新 AI 任务记录

---

## 5. 模型接入设计

### 5.1 技术选型

本期建议：

- 基于 Spring AI 封装统一 AI 客户端
- 使用 OpenAI 兼容接口
- 实际接入 DeepSeek 模型

建议抽象一个统一接口：

```java
public interface AiModelClient {
    AiModelResponse generateWithTools(AiModelRequest request);
}
```

避免业务层直接依赖某个具体 SDK 类型。

### 5.2 请求参数建议

统一请求对象建议包含：

- `model`
- `temperature`
- `maxOutputTokens`
- `timeout`
- `systemPrompt`
- `userPrompt`
- `tools`
- `responseMode`
  - `json`
- `toolChoicePolicy`
  - `auto`

### 5.3 温度建议

#### 模板生成

- `temperature`：中低

原因：

- 允许一定灵活度
- 但不希望模板结构和动作选择过度发散

#### 周期总结

- `temperature`：低

原因：

- 更偏分析与归纳
- 不需要强创造性

### 5.4 超时与重试

建议：

- 单次模型调用超时：15~30 秒
- 网络层或上游超时最多重试 1 次
- 不对“模型返回结构非法”做无限自动重试

---

## 6. AI任务完整执行流程

### 6.1 首次执行流程

```text
接口请求
-> 前置业务校验
-> 创建 AI 任务（pending）
-> 进入 running
-> 构建首轮上下文
-> 发送 prompt + tools 给模型
-> 模型按需调用只读 tools
-> 模型返回完整 JSON
-> 后端做结构与业务校验
-> 通过则落库并 succeeded
-> 失败则进入修复流程
```

### 6.2 修复流程

若首次输出未通过校验：

1. 后端整理修复输入：
   - 原始 JSON
   - 校验错误列表
   - 再次强调输出 schema
2. 把修复请求发给模型
3. 再做一次校验
4. 若仍失败，再做第 2 次修复
5. 修复 2 次后仍失败，则任务 `failed`

### 6.3 修复轮次上限

本期固定：

- 最多修复 2 次

不建议在 MVP 中做更复杂的自反思链。

---

## 7. Tool calling 方案

### 7.1 为什么采用 tool calling

采用 tool calling 的原因：

- 首轮 prompt 不需要塞入全部可能数据
- 模型可以自行判断还缺什么信息
- 有利于后续扩展更多 AI 场景
- 有利于把数据查询逻辑和 prompt 逻辑分离

### 7.2 本期工具边界

本期只开放只读工具。

工具原则：

- 单一职责
- 明确输入输出
- 返回结构稳定
- 不暴露无关敏感字段

### 7.3 本期建议开放的工具

#### 通用工具

1. `get_user_ai_capability_context`
   - 返回当前用户 AI 权限、角色、权益层级、资料完整度摘要

2. `get_user_profile_context`
   - 返回 AI 相关基础档案字段

3. `get_user_current_body_metrics_context`
   - 返回当前身体指标快照

#### 模板生成场景工具

4. `search_candidate_exercises`
   - 按场景、肌群、动作类型、结构类型查询候选动作

5. `get_exercise_detail`
   - 查询单个动作详情、默认结构类型、主要/次要肌肉、器械信息

6. `get_template_generation_constraints`
   - 返回系统模板结构约束、metricKey 封闭字典、结构类型限制

#### 周期总结场景工具

7. `get_cycle_run_summary`
   - 返回循环摘要信息

8. `get_cycle_run_sessions_detail`
   - 返回该循环下完整训练明细

9. `get_cycle_run_aggregated_analysis`
   - 返回系统预处理后的统计摘要

### 7.4 不开放的工具

本期不开放：

- `create_cycle_template_draft`
- `activate_cycle_template`
- `update_profile`
- `update_workout_session`
- 任意写操作工具

理由：

- 防止 AI 越权执行持久化操作
- 防止错误输出直接污染业务数据

### 7.5 Tool calling 调度约束

建议约束：

- 单轮最多调用 1 个工具
- 单次任务最多 6 轮工具调用
- 同一工具重复调用超过 2 次可视为异常

超限时：

- 中断任务
- 记录工具调用轨迹
- 标记失败

---

## 8. Prompt 分层设计

建议每个场景都拆为三段。

### 8.1 System Prompt

职责：

- 定义模型角色
- 定义通用边界
- 定义必须返回 JSON

模板方向示意：

```text
你是 DailyForge 的健身训练辅助模型。
你只能基于系统提供的数据与工具进行分析和生成。
你不是医生，不能提供医疗诊断。
你必须遵守系统训练模板结构约束。
你必须返回严格 JSON，不允许输出 JSON 之外的解释文本。
如果数据不足，可以调用工具继续获取信息。
如果信息仍不足，需要在结果中明确表达不确定性，而不是伪造精确结论。
```

### 8.2 Scenario Prompt

职责：

- 定义当前任务
- 定义当前输出结构
- 定义当前禁止事项

### 8.3 Context Payload

职责：

- 作为首轮上下文直接输入模型
- 提供当前场景最必要的信息

原则：

- 尽量结构化
- 不把全部历史和全部动作库全文一次性平铺成自然语言

---

## 9. 场景一：Template Generation

### 9.1 首轮上下文建议

本场景优先实现，首轮建议直接提供以下结构化上下文：

```json
{
  "userProfile": {
    "gender": "male",
    "age": 28,
    "heightCm": 178,
    "goalType": "muscle_gain",
    "trainingLevel": "beginner",
    "injuryNotes": "左膝旧伤"
  },
  "currentBodyMetrics": {
    "weightKg": 76.5,
    "bodyFatRate": null,
    "bmi": null
  },
  "generationRequest": {
    "sceneType": "gym",
    "goalType": "muscle_gain",
    "cycleLength": 4,
    "includeCardio": true
  },
  "templateConstraints": {
    "allowedStructureTypes": [
      "set_based",
      "single_segment"
    ],
    "allowedMetricKeys": [
      "weight_kg",
      "reps",
      "duration_seconds",
      "distance_km",
      "speed_kmh",
      "pace_seconds_per_km",
      "incline_percent",
      "rest_seconds",
      "rpe",
      "intensity_level"
    ]
  }
}
```

### 9.2 首轮不直接塞完整动作库

即使采用 tool calling，本场景也不建议把完整动作库原样塞进首轮 prompt。

建议方式：

- 首轮只提供约束与用户信息
- 由模型再调用 `search_candidate_exercises`
- 若需要，再调 `get_exercise_detail`

原因：

- 成本更低
- 更可控
- 后续扩展更方便

### 9.3 Template Generation 的 Prompt 重点

需要明确告诉模型：

- 只能生成 `draft`
- 周期长度必须等于用户请求值
- 动作必须来自系统返回的候选动作
- `structureType` 必须与动作默认结构一致
- `single_segment` 只能 1 个 `segment`
- `set_based` 只能使用 `set`
- 允许空白天，系统会视为休息日
- 没有历史训练数据时，重量和强度只能作为起始建议

### 9.4 输出结构

模型必须返回：

- `draftTemplate`
- `generationRationale`

且不能返回：

- 多余包裹层
- Markdown
- 代码块
- 自然语言前后缀

### 9.5 模板生成结果校验

后端至少校验：

- JSON 可解析
- 顶层字段完整
- `cycleLength` 合法
- 每个动作 `exerciseId` 存在
- 每个动作 `structureType` 正确
- `itemType` 合法
- `metricKey` 合法
- `metricValueNumber` 非负
- `generationRationale.intensityRationale.basisType` 合法

### 9.6 成功后落库策略

校验通过后：

- 由后端把 `draftTemplate` 转为 `cycle_template` 草稿写入
- `generationRationale` 作为 AI 结果的一部分单独保存

不建议：

- 直接把 `generationRationale` 写入模板主表结构

---

## 10. 场景二：Cycle Summary

### 10.1 上下文策略

按你的要求，MVP 允许把完整训练明细作为可用上下文来源。

本场景建议策略：

- 首轮直接提供循环摘要 + 完整训练明细
- 允许模型再调用工具补充资料或统计视角

这样可以兼顾：

- MVP 先快速出效果
- 后续再做更细的上下文裁剪优化

### 10.2 首轮上下文建议

建议首轮直接包含：

- `cycleRun` 基本信息
- 模板信息
- 全部 `trainingSessions`
- 每个动作状态
- 计划值与实际值差异
- 失败原因
- 动作感受 / 备注
- 训练整体备注
- 用户基础档案
- 最新身体指标

### 10.3 建议保留的系统预处理摘要

虽然 MVP 允许直接给完整训练明细，但仍建议后端补一份轻量统计摘要，供模型更稳地读取：

- 本轮完成 Day 数
- 部分完成动作数
- skipped / failed 动作数
- 失败原因分布
- 常见偏差动作
- 计划与实际的主要偏差方向

### 10.4 Cycle Summary 的 Prompt 重点

需要明确告诉模型：

- 当前任务是复盘总结，不是生成模板
- 只允许分析 `completed cycle_run`
- 不允许输出医疗诊断
- 不允许自动改写历史记录
- 可以给出下轮建议
- 第一版不能直接返回新模板

### 10.5 输出结构

必须返回：

- `executionOverview`
- `strengths`
- `issues`
- `causeAnalysis`
- `nextCycleSuggestions`
- `risks`
- `dataCompletenessNotice`

### 10.6 结果校验

后端至少校验：

- 所有必填字段存在
- 数组字段类型正确
- 文本长度不过长
- 结果不能为空壳

---

## 11. 输出修复策略

### 11.1 修复触发条件

以下情况触发修复：

- JSON 无法解析
- 顶层字段缺失
- 枚举值非法
- 模板结构不符合 `cycle_template v2`
- 动作不存在
- `generationRationale` 缺关键字段

### 11.2 修复 Prompt 输入

修复时建议输入：

- 原始模型输出
- 校验错误列表
- 正确 schema 摘要
- 明确要求“只返回修复后的完整 JSON”

### 11.3 修复次数

固定：

- 最多 2 次

若 2 次后仍失败：

- 任务 `failed`
- 记录失败原因

### 11.4 修复时禁止事项

修复 Prompt 必须强调：

- 不允许省略原本要求字段
- 不允许输出解释文本
- 不允许只返回局部 patch
- 必须返回完整 JSON

---

## 12. 审计、日志与可观测性

### 12.1 建议持久化字段

建议 AI 任务记录表至少包含：

- `taskId`
- `userId`
- `taskType`
- `status`
- `modelName`
- `providerName`
- `promptVersion`
- `toolCallCount`
- `repairAttemptCount`
- `inputSummaryJson`
- `outputPreview`
- `errorCode`
- `errorMessage`
- `createdAt`
- `startedAt`
- `completedAt`
- `latencyMs`

### 12.2 不保存完整原文

按已确认方案：

- 不保存完整原始 prompt
- 不保存完整训练备注全文

建议保存：

- 摘要
- 截断预览
- promptVersion
- hash

### 12.3 结构化日志建议

建议关键日志字段：

- `traceId`
- `userId`
- `taskId`
- `taskType`
- `modelName`
- `toolName`
- `toolCallRound`
- `repairAttempt`
- `latencyMs`
- `finalStatus`

---

## 13. Prompt 版本管理

建议每个场景显式维护：

- `promptVersion`

示例：

- `template_generation_v1`
- `cycle_summary_v1`
- `template_generation_repair_v1`
- `cycle_summary_repair_v1`

作用：

- 跟踪不同版本效果
- 方便回滚
- 方便问题排查

---

## 14. 安全与边界控制

### 14.1 医疗与风险边界

Prompt 中必须明确：

- 不是医疗模型
- 不提供诊断
- 不提供康复方案
- 不能把疼痛直接解释为医学结论

### 14.2 工具权限边界

模型只能调用当前场景下白名单工具。

不允许：

- 任意工具名调用
- 任意 SQL
- 任意 HTTP 外部访问

### 14.3 结果边界

即使模型返回了“建议立即启用模板”等内容，系统也不得自动执行。

---

## 15. 推荐实现顺序

按当前已确认优先级，建议：

### 第一阶段

- 先做 `template_generation`
- 打通：
  - 任务创建
  - tool calling
  - JSON 校验
  - 草稿落库
  - 设计说明返回

### 第二阶段

- 再做 `cycle_summary`
- 打通：
  - 完整训练明细输入
  - 总结结果返回
  - 数据完整度提醒

### 第三阶段

- 再做效果优化
  - 动作候选筛选优化
  - 周期总结上下文裁剪
  - Prompt 版本迭代

---

## 16. 我建议你后续再明确的 4 个细节

这 4 个点我这版先按默认建议写了，但后面真正实现前最好再拍一下板：

1. `template_generation` 是否允许模型跨多次工具调用自己补动作候选
   - 当前建议：允许
2. 同一任务 6 轮工具调用是否够用
   - 当前建议：MVP 先定 6，后面根据日志观察再调
3. 模板生成时工具返回的动作候选上限
   - 当前建议：单次查询最多返回 20~50 条候选
4. 周期总结时完整训练明细是否做长度截断
   - 当前建议：MVP 不主动截断，但保留后续按 token 超限回退成摘要模式的能力

---

## 17. 结论

本方案的核心是：

1. 用 Spring AI + OpenAI 兼容客户端接 DeepSeek。
2. 用类智能体模式驱动模型，通过只读 tools 按需补齐数据。
3. 用系统规则兜底，而不是把规则完全交给 Prompt。
4. 用“首次生成 + 最多两次 JSON 修复”保证结构稳定。
5. 优先落地 `template_generation`，先把能感知到的 AI 价值做出来。
## 18. 2026-08-01 实现补充

### 18.1 默认 timeout 已落地为 `PT120S`

当前后端真实实现已将：

- `dailyforge.ai.timeout`

默认值调整为：

- `PT120S`

原因：

- `template_generation` 属于“多轮 tool calling + 最终大 JSON 输出”场景
- 在真实 DeepSeek 联调中，`PT30S` 容易把较慢但仍在处理中的后续模型调用直接打成失败

### 18.2 上游失败分类

当前实现将模型调用失败分为两类对外错误码：

- `AI_SERVICE_TIMEOUT`
  - 可识别的超时场景
  - 包括读取超时、请求超时等
- `AI_SERVICE_UNAVAILABLE`
  - 非超时类上游不可用场景
  - 包括 HTTP `4xx`、HTTP `5xx`、网络访问异常、无法拿到有效响应

说明：

- 对前端继续保持稳定错误码契约
- 更细粒度失败原因只通过后端结构化日志暴露

### 18.3 模型客户端日志补充要求

当前实现要求模型客户端日志最少带上：

- `taskId`
- `taskType`
- `stage`
- `provider`
- `model`
- `timeoutMs`
- `httpStatus`
- `responsePreview`
- `rootCause`

其中：

- `stage` 当前至少区分：
  - `initial-generation`
  - `tool-followup`
  - `json-repair`
- `responsePreview` 只能保留上游响应体的截断摘要
- `rootCause` 用于区分 timeout、DNS、连接失败、HTTP 4xx/5xx 等排障方向

### 18.4 脱敏边界补充

日志中仍然禁止记录：

- API Key
- 完整 prompt
- 完整上游响应体
- 完整工具调用参数原文

允许记录：

- 响应体截断摘要
- 根因异常类名
- 调用阶段信息

### 18.5 排障口径补充

需要明确：

- `AI_SERVICE_UNAVAILABLE` 不代表“本次请求没有真正调用到 AI”
- 在真实联调中，完全可能已经发生：
  - 多轮 tool calling
  - 部分模型调用成功
  - 最终在后续某一次模型调用阶段失败

因此排障必须结合：

- `ai_task_records`
- `ai_task_tool_calls`
- 后端结构化日志

一起判断真实失败点。
