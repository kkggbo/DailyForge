# DailyForge AI Coach 真实 DeepSeek 接入后端改造清单

> 版本：v1.0  
> 日期：2026-08-01  
> 状态：待开发  
> 模块：`backend / ai_coach`

---

## 1. 文档目标

本文档用于把当前 `ai_coach` 模块从“本地规则占位实现”改造成“真实 DeepSeek 调用实现”。

当前代码虽然已经具备：

- AI 任务创建
- AI 任务状态流转
- AI 结果落库
- AI 结果查询接口

但实际执行链路中并没有真正调用外部模型，而是由本地 Java 代码直接生成模板和总结结果。

本次改造的目标是：

1. 接入真实 DeepSeek 模型调用
2. 接入 Spring AI + OpenAI 兼容调用链路
3. 实现只读 tool calling
4. 实现 JSON 结构校验与最多两轮修复
5. 真实记录 AI 调用轨迹，而不是固定写死 `toolCallCount = 0`
6. 保持现有接口契约不变，优先兼容当前前端

---

## 2. 当前问题确认

当前后端存在以下事实：

### 2.1 没有真实模型调用

以下类当前并未调用任何外部 AI：

- `AiTaskExecutor`
- `AiTemplateGenerationService`
- `AiCycleSummaryService`

当前执行路径是：

```text
AiCoachApplicationService
-> AiTaskExecutor
-> AiTemplateGenerationService.processTask / AiCycleSummaryService.processTask
-> 本地规则生成结果
```

### 2.2 provider / model 只是任务元数据

当前 `ai_task_records` 中的：

- `provider`
- `model`
- `promptVersion`

只是落库记录，不代表真正调用了对应模型。

### 2.3 tool_call_count 和 repair_attempt_count 为伪数据

当前实现中：

- `toolCallCount`
- `repairAttemptCount`

会被直接写为 `0`，并不反映真实调用过程。

### 2.4 当前模板生成和周期总结都是规则生成

当前代码中存在明显的本地规则生成逻辑，例如：

- 按固定动作池选动作
- 按本地公式计算组数、次数、重量
- 直接在 Java 里拼接 rationale 文案
- 直接在 Java 里拼接 cycle summary 文案

这类逻辑不能再继续作为“主执行路径”存在。

---

## 3. 改造目标状态

本次改造完成后，目标链路应为：

```text
提交 AI 任务
-> 创建 ai_task_records(pending)
-> 异步执行器取任务
-> 构建场景上下文
-> 调用 DeepSeek 模型
-> 模型按需调用只读 tools
-> 模型返回结构化 JSON
-> 后端做结构校验 + 业务校验
-> 如失败，进入最多 2 轮 JSON 修复
-> 校验通过后落库结果
-> 更新任务状态 succeeded / failed
```

要求：

1. 不允许在真实 AI 调用失败后静默切回本地规则生成
2. 不允许把“是否真的调用 AI”做成不可见状态
3. 不允许继续把伪造的 `tool_call_count = 0` 当作正常行为

---

## 4. 本次改造范围

### 4.1 包含

- `ai_coach` 模块真实模型调用接入
- tool calling 基础设施
- JSON 修复链路
- 模板生成与周期总结执行链路重构
- AI 任务记录真实化
- 测试补齐

### 4.2 不包含

- 前端页面改造
- 新增 AI 对话式聊天接口
- 新增 AI 饮食功能
- 新增新的数据库迁移版本

说明：

- 本轮优先在现有 V7 表结构基础上完成后端真实 AI 接入
- 若开发过程中确认 V7 字段不足，再单独补数据库改造清单和迁移脚本

---

## 5. 代码改造项

## 5.1 模型客户端接入

### 目标

新增真正的模型调用适配层，统一封装对 DeepSeek 的调用。

### 要求

新增：

- `infrastructure/ai/client/AiModelClient.java`
- `infrastructure/ai/client/SpringAiOpenAiModelClient.java`

职责：

- 使用 Spring AI 发起真实模型调用
- 读取 `AiCoachProperties` 中的：
  - `provider`
  - `model`
  - `baseUrl`
  - `apiKey`
  - `timeout`
- 支持普通 JSON 输出模式
- 支持 tool calling 模式

### 异常映射要求

需要把外部调用异常统一映射为项目错误码：

- 超时 -> `AI_SERVICE_TIMEOUT`
- 上游不可用 / 网络异常 -> `AI_SERVICE_UNAVAILABLE`
- 空响应 / 非法响应 -> `AI_OUTPUT_INVALID`

---

## 5.2 任务执行链路重构

### 目标

把当前 `AiTaskExecutor` 从“分发给本地规则 service”改造成“真实 AI 编排执行入口”。

### 要求

保留：

- `AiTaskExecutor`

但职责调整为：

1. 加锁读取任务
2. 将任务状态从 `pending` 改为 `running`
3. 根据 `taskType` 分发到不同场景执行器
4. 处理模型调用异常
5. 处理工具调用异常
6. 处理 JSON 修复失败
7. 更新任务最终状态

### 禁止

不允许继续保留以下主路径：

```text
AiTaskExecutor -> AiTemplateGenerationService.processTask -> 本地生成
AiTaskExecutor -> AiCycleSummaryService.processTask -> 本地生成
```

---

## 5.3 场景执行器拆分

### 目标

把不同 AI 场景的“上下文准备 + prompt 组装 + 工具调用 + 模型输出处理”拆开。

### 要求

新增：

- `TemplateGenerationExecutor`
- `CycleSummaryExecutor`

职责：

- 构建当前场景请求
- 注入 system prompt / scenario prompt
- 注册当前场景允许的 tools
- 调用模型
- 返回结构化 JSON 结果

### 约束

场景执行器不负责：

- 直接写业务表
- 直接改模板状态
- 直接手工拼业务 VO

---

## 5.4 上下文构建器

### 目标

把“提供给模型的输入上下文”独立成可维护组件。

### 要求

新增：

- `TemplateGenerationContextBuilder`
- `CycleSummaryContextBuilder`

#### 模板生成上下文至少包含

- 用户基础档案
- 用户当前身体指标
- 本次生成请求
- 模板结构约束摘要

#### 周期总结上下文至少包含

- cycle run 基本信息
- 版本快照信息
- training sessions 明细
- 动作状态、失败原因、实际值偏差、备注、感受
- 聚合统计摘要

### 原则

- 上下文结构化
- 尽量避免把完整数据库对象原样塞给模型
- 对超长字段进行摘要或裁剪

---

## 5.5 Prompt 构建器

### 目标

把当前不同场景的 prompt 规则做成明确组件，而不是散落在 service 代码中。

### 要求

新增：

- `TemplateGenerationPromptBuilder`
- `CycleSummaryPromptBuilder`
- `AiRepairPromptBuilder`

### 需要明确写入 prompt 的约束

#### 模板生成

- 只允许生成 `draft`
- 周期长度必须等于请求值
- 动作必须来自系统动作库
- 输出结构必须符合当前模板模型
- 没有历史表现时，重量只能是起始建议

#### 周期总结

- 只允许分析 `completed cycle_run`
- 只输出总结和建议
- 不自动生成正式模板
- 不得伪装成医学诊断

#### JSON 修复

- 只返回完整 JSON
- 不允许返回解释文字
- 不允许返回 patch

---

## 5.6 Tool calling 基础设施

### 目标

让模型通过只读工具按需补充上下文数据。

### 要求

新增：

- `AiToolRegistry`
- `AiToolDispatcher`

建议首批工具：

- `get_user_profile_context`
- `get_user_current_body_metrics_context`
- `search_candidate_exercises`
- `get_exercise_detail`
- `get_template_generation_constraints`
- `get_cycle_run_summary`
- `get_cycle_run_sessions_detail`
- `get_cycle_run_aggregated_analysis`

### 安全要求

1. 只允许只读工具
2. 只能读取当前用户有权限访问的数据
3. 禁止模型直接调用写操作
4. 禁止任意 SQL / 任意 HTTP 访问

### 轮次要求

- 单次任务最多 `maxToolRounds`
- 当前默认值为 6
- 超限直接失败

---

## 5.7 AI 工具调用记录真实化

### 目标

把 `ai_task_tool_calls` 从“设计存在”变成“真实记录工具调用明细”。

### 要求

新增或完善：

- `AiTaskToolCallEntity`
- `AiTaskToolCallMapper`

每次工具调用记录：

- `taskId`
- `roundNo`
- `toolName`
- `requestSummaryJson`
- `responseSummaryJson`
- `status`
- `latencyMs`
- `errorMessage`

同时同步更新：

- `ai_task_records.tool_call_count`

---

## 5.8 JSON 校验与修复链路

### 目标

模型输出必须先校验，再决定是否可落库。

### 要求

新增：

- `AiOutputValidationDomainService`
- `AiJsonRepairService`

### 校验分层

#### 第一层：结构校验

- JSON 可解析
- 顶层字段完整
- 数组 / 对象结构正确
- 枚举合法

#### 第二层：业务校验

模板生成场景：

- `cycleLength` 合法
- `exerciseId` 存在
- `structureType` 与动作默认结构一致
- `metricKey` 合法
- 数值非负

周期总结场景：

- 必要字段齐全
- 数组字段类型正确
- 文本长度不过长

### 修复策略

- 首次失败后触发修复
- 最多修复 2 次
- 每次修复都要重新做完整校验
- 超过 2 次仍失败 -> `failed`

同时真实更新：

- `repair_attempt_count`

---

## 5.9 模板生成落库逻辑重构

### 目标

保留现有模板落库能力，但不再由本地规则直接构造模板内容。

### 要求

保留：

- `AiTemplateGenerationService`

但职责改成：

1. 接收已经通过校验的 AI 结果
2. 创建 `cycle_template`
3. 创建 `cycle_template_version`
4. 保存完整版本内容
5. 回写 `source_task_id`
6. 回写 `ai_task_records.result_json`

### 必须删除或废弃的本地规则生成职责

不再允许它承担：

- 固定动作池选取
- 固定公式计算重量组数
- 本地硬编码 rationale 文案生成

### 说明

如果短期内需要保留旧逻辑做开发调试，可将其降级为：

- `RuleBasedTemplateGenerator`

但不能继续作为默认生产路径。

---

## 5.10 周期总结落库逻辑重构

### 目标

保留现有周期总结数据读取能力，但不再由本地规则直接生成总结文案。

### 要求

保留：

- `AiCycleSummaryService`

但职责改成：

1. 读取周期总结所需数据
2. 向执行器提供上下文
3. 接收通过校验的 AI 结果
4. 落库到 `ai_task_records.result_json`

### 必须删除或废弃的本地规则生成职责

不再允许它承担：

- 本地拼接 `strengths`
- 本地拼接 `issues`
- 本地拼接 `causes`
- 本地拼接 `suggestions`

如需保留旧实现，仅允许降级为：

- `RuleBasedCycleSummaryGenerator`

且不能作为生产默认执行路径。

---

## 5.11 执行来源标识

### 目标

让后续排查能明确看出结果到底来自真实 AI 还是本地规则。

### 建议

如果当前数据库字段允许不足，建议后续增加：

- `execution_mode`

可选值：

- `ai_provider`
- `rule_based`

### 本轮要求

如果本轮不改表，则至少要在：

- 日志
- `result_json` 摘要
- `input_summary_json`

中明确留下“真实模型调用”痕迹，不要再次出现“看起来像 AI，实际是本地规则”的情况。

---

## 5.12 配置完善

### 目标

把 AI 调用必要配置真正用起来。

### 需要确认生效的配置

- `dailyforge.ai.enabled`
- `dailyforge.ai.provider`
- `dailyforge.ai.model`
- `dailyforge.ai.base-url`
- `dailyforge.ai.api-key`
- `dailyforge.ai.timeout`
- `dailyforge.ai.max-tool-rounds`
- `dailyforge.ai.max-repair-attempts`
- `dailyforge.ai.template-generation-prompt-version`
- `dailyforge.ai.cycle-summary-prompt-version`

### 运行保护建议

如果：

- `dailyforge.ai.enabled = true`
- 但 `apiKey` 为空

建议：

- 启动时记录明确错误日志
- 或在第一次调度 AI 任务时立即失败并返回明确错误

避免系统继续伪装成“AI 可用”状态。

---

## 5.13 错误码与日志

### 需要确认接入的错误码

- `AI_FEATURE_NOT_AVAILABLE`
- `AI_REQUIRED_PROFILE_MISSING`
- `AI_REQUIRED_BODY_METRIC_MISSING`
- `AI_CYCLE_RUN_NOT_COMPLETED`
- `AI_OUTPUT_INVALID`
- `AI_SERVICE_TIMEOUT`
- `AI_SERVICE_UNAVAILABLE`

### 日志要求

至少记录：

- `taskId`
- `taskType`
- `provider`
- `model`
- `toolName`
- `roundNo`
- `repairAttempt`
- `latencyMs`
- `finalStatus`

### 日志禁止项

禁止输出：

- 完整 API Key
- 完整原始 prompt
- 完整原始模型全文输出
- 敏感用户信息全文

---

## 6. 测试改造项

## 6.1 单元测试

需要补齐：

- `AiModelClient` 成功调用测试
- `AiModelClient` 超时测试
- `AiModelClient` 空响应测试
- `AiOutputValidationDomainService` 模板结果非法测试
- `AiOutputValidationDomainService` 周期总结结果非法测试
- `AiJsonRepairService` 修复成功测试
- `AiJsonRepairService` 修复两次后失败测试

## 6.2 集成测试

需要补齐：

- AI 模板生成任务创建 -> 执行 -> draft 落库成功
- AI 模板生成失败时任务状态变更正确
- AI 周期总结任务创建 -> 执行 -> 结果落库成功
- tool calling 真实记录到 `ai_task_tool_calls`
- `tool_call_count` 与 `repair_attempt_count` 真实更新
- 调用失败时不产生模板草稿

## 6.3 回归关注点

必须验证：

1. 现有前端轮询接口无需改动即可继续工作
2. 现有 `AI1-AI5` 接口路径和响应结构不漂移
3. 当前 AI 权限与 readiness 校验逻辑不被破坏

---

## 7. 推荐实施顺序

建议后端按以下顺序开发：

1. 接入 Spring AI / DeepSeek 基础客户端
2. 完成 `AiModelClient` 与配置生效
3. 完成 `AiTaskExecutor` 编排重构
4. 完成 `template_generation` 的：
   - context builder
   - prompt builder
   - tool calling
   - JSON 校验
   - JSON 修复
   - 落库
5. 完成 `cycle_summary` 的同等链路
6. 完成日志与错误码收口
7. 完成测试补齐

---

## 8. 验收标准

本轮改造完成后，至少满足以下标准：

1. AI 模板生成时真实消耗 DeepSeek API
2. API Key 使用记录可在模型平台侧看到
3. `ai_task_records.tool_call_count` 不再固定为 0
4. `ai_task_records.repair_attempt_count` 能反映真实修复轮次
5. 模板生成不再由本地规则直接产出
6. 周期总结不再由本地规则直接产出
7. AI 调用失败时任务明确失败，不静默回退
8. 前端现有接口调用链无需同步大改

---

## 9. 风险与注意事项

### 9.1 上下文过大

`cycle_summary` 场景可能因训练明细较大导致：

- token 消耗过高
- 响应慢
- 模型截断

需要在实现中预留摘要化与裁剪能力。

### 9.2 DeepSeek tool calling 兼容性

需确认当前使用的：

- DeepSeek 模型版本
- Spring AI OpenAI 兼容层

对 tool calling 与 JSON 输出模式的兼容程度。

如果兼容性不足，需要在基础客户端层做适配。

### 9.3 不要保留隐式兜底

如果继续保留“模型失败自动本地生成”作为默认逻辑，会导致：

- 无法判断真实 AI 质量
- 无法判断 token 消耗
- 无法判断错误来源

因此禁止在生产默认链路中保留隐式兜底。

---

## 10. 结论

本次改造不是简单“把 API Key 配上”，而是要把当前 `ai_coach` 模块从：

```text
伪 AI 占位实现
```

升级为：

```text
真实模型调用 + 只读工具链 + 校验修复 + 可审计任务系统
```

在这次改造完成前，当前 `ai_coach` 模块仍应被视为：

- 已有完整接口壳
- 但尚未真正完成 AI 接入

本清单确认后，后端可直接据此进入正式开发。
