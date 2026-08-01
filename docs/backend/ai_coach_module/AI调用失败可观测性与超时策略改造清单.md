# DailyForge AI 调用失败可观测性与超时策略改造清单

> 版本：v1.0  
> 日期：2026-08-01  
> 模块归属：`backend` / `ai_coach`  
> 文档状态：用于指导本轮排查修复与文档同步

---

## 1. 背景

在 2026-08-01 的一次真实 DeepSeek 联调中，AI 模板生成前端报错：

- `AI_SERVICE_UNAVAILABLE`
- message: `ai service is unavailable`

但结合任务记录与工具调用记录可以确认：

- 请求确实调用了 DeepSeek
- `task_id=3`
- `tool_call_count=13`
- `repair_attempt_count=0`
- 前 13 次 tool call 全部成功
- 失败发生在第 3 轮 tool call 之后的后续模型调用阶段

这说明当前问题不是“未调用 AI”，而是：

1. 真实模型调用链已经启动
2. 后续某一次模型请求失败
3. 失败原因在当前日志与错误映射中被过度收敛，导致只能看到笼统的 `AI_SERVICE_UNAVAILABLE`

---

## 2. 当前问题

### 2.1 上游失败缺少足够可观测性

当前 `DeepSeekOpenAiModelClient` 对以下异常处理过于粗粒度：

- `ResourceAccessException`
- `RestClientException`

现状问题：

- 未区分连接失败、读取超时、上游 4xx、上游 5xx
- 未记录 HTTP 状态码
- 未记录上游响应体摘要
- 未记录根因异常类型
- 排查时无法判断是超时、限流、网关错误还是服务端错误

### 2.2 任务日志无法直接定位到具体模型请求失败点

当前 `AiTaskExecutor` 只记录：

- `taskId`
- `taskType`
- `errorCode`

但对于真实模型调用过程缺少更细粒度信息：

- 第几轮模型调用失败
- 是否处于 tool calling 之后
- 是否仍在首轮生成阶段
- 是否进入 repair 阶段

### 2.3 timeout 默认值偏保守

当前配置：

- `dailyforge.ai.timeout = PT30S`

对“多轮 tool calling + 最终返回大 JSON”的模板生成场景偏紧。

已观察到：

- 任务总耗时 `135333 ms`
- 第 3 轮 tool call 完成后到失败间隔较长

说明当前超时策略可能过于保守，容易把可恢复的慢请求直接打成服务不可用。

### 2.4 错误码语义对外可用，但对内排障信息不足

对前端来说，保留：

- `AI_SERVICE_UNAVAILABLE`
- `AI_SERVICE_TIMEOUT`

这样的稳定错误码是合理的。

但对后端排查来说，还需要日志层面保留：

- HTTP 状态
- 响应摘要
- 异常根因
- 调用阶段

---

## 3. 本轮改造目标

### 3.1 目标

1. 保持现有对外错误码契约基本不变
2. 增强 DeepSeek 调用失败时的可观测性
3. 提高模板生成场景的默认 timeout 容忍度
4. 为后续继续排查限流、网关错误、响应超时等问题提供日志依据

### 3.2 非目标

本轮不做：

- 改造 AI 接口契约
- 引入重试机制
- 改造 tool calling 轮次策略
- 改造 prompt 结构
- 改造数据库表结构
- 变更前端错误码处理逻辑

---

## 4. 代码改造项

### 4.1 `DeepSeekOpenAiModelClient`

需要改造：

1. 增加结构化日志
2. 区分以下失败类型：
   - 连接/网络访问异常
   - Socket 超时
   - HTTP 4xx
   - HTTP 5xx
   - 其他 `RestClientException`
3. 记录以下诊断字段：
   - `taskId`
   - `taskType`
   - `provider`
   - `model`
   - `timeoutMs`
   - HTTP `status`
   - 响应体截断摘要
   - 根因异常类名
4. 日志中禁止输出：
   - `apiKey`
   - 完整 prompt
   - 完整响应体

建议结果：

- timeout -> 抛 `AI_SERVICE_TIMEOUT`
- 其余上游不可用/HTTP 错误 -> 抛 `AI_SERVICE_UNAVAILABLE`

### 4.2 `AiConversationService`

需要改造：

1. 为每次模型调用补充调用上下文
2. 至少能在客户端日志中区分：
   - 第几次模型调用
   - 当前是否处于 tool calling 循环
   - 当前是否要求至少一次 tool call
3. 如有必要，向模型客户端传递轻量上下文对象，而不是大面积重构调用链

### 4.3 `AiModelRequest` / AI 调用模型对象

如当前对象无法承载诊断上下文，需要最小改造：

- 增加 `taskId`
- 增加 `taskType`
- 增加 `attemptNo` 或 `roundNo`

要求：

- 仅用于日志与问题定位
- 不影响对外接口契约

### 4.4 `application.yml`

需要调整：

- `dailyforge.ai.timeout`

建议默认值：

- 从 `PT30S` 提高到 `PT90S` 或 `PT120S`

本轮优先建议：

- `PT90S`

原因：

- 对模板生成这种长响应场景更稳妥
- 仍能避免无限等待
- 不会过度放大单次失败占用

---

## 5. 文档同步项

### 5.1 `AI接入与提示词上下文设计.md`

补充：

- 模型调用失败分类
- 失败日志字段
- timeout 默认策略
- 当前不记录完整 prompt / 完整响应体，只记录摘要

### 5.2 `ai_coach_DDD.md`

补充：

- `DeepSeekOpenAiModelClient` 的错误分类职责
- `AiConversationService` 的调用轮次诊断职责
- timeout 默认值更新
- AI 调用可观测性要求

### 5.3 `ai_coach_接口文档.md`

补充或澄清：

- 对外错误码不变
- `AI_SERVICE_TIMEOUT` 与 `AI_SERVICE_UNAVAILABLE` 的语义边界
- 该错误码由上游 AI 服务超时 / 不可用触发，不代表未进入 AI 调用

---

## 6. 验收标准

完成后应满足：

1. 再次出现 DeepSeek 调用失败时，后端日志能区分：
   - timeout
   - HTTP 4xx
   - HTTP 5xx
   - 其他网络访问异常
2. 日志能定位到：
   - `taskId`
   - `taskType`
   - 第几次模型调用失败
3. 日志不泄露：
   - API Key
   - 完整 prompt
   - 完整响应体
4. 现有前端契约不需要同步改代码即可继续消费错误码
5. 默认 timeout 已提高到更适合模板生成的级别

---

## 7. 实施顺序

1. 先改 `DeepSeekOpenAiModelClient`
2. 再补 `AiConversationService` 调用上下文
3. 再调整 `application.yml` timeout
4. 最后同步 DDD、AI 接入设计文档、接口文档

---

## 8. 风险提示

1. 本轮增强日志后，仍可能发现真实问题来自：
   - DeepSeek 限流
   - 上游网关超时
   - 单次响应过大
   - 模型在 tool calling 后处理过慢
2. 如果提高 timeout 后仍频繁失败，下一轮应继续评估：
   - 是否增加重试
   - 是否缩短 prompt / 上下文
   - 是否减少 tool call 轮次
   - 是否按场景拆分不同 timeout

