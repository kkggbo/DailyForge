# 2026-08-01 Changelog

## 今日概览

本轮把 `ai_coach` 从“后端任务闭环已具备但模型仍属占位”的状态推进到“前后端可联调、真实 DeepSeek 已接入、结果可追踪、失败更易排查”的可用阶段。后端完成真实模型调用、tool calling、工具调用记录、结果修复与超时策略调整；前端补齐 AI Coach 页面、发起入口、任务轮询与结果展示；配套接口文档、DDD 与改造清单同步更新。

## 今日完成内容

### 1. AI Coach 后端真实接入

- 接入真实 DeepSeek OpenAI 兼容客户端，不再停留在 stub/占位实现。
- 新增 `AiConversationService`、模型客户端、执行器、上下文与 prompt 组装，支持模板生成与周期总结两类任务。
- 新增 `AiCoachToolSupportService`、`AiCoachToolConfig` 与工具注册/分发能力，支持多轮只读 tool calling。
- `AiTaskRecordMapper` 与相关持久化链路改造为真实记录 `tool_call_count` 与 `ai_task_tool_calls`，不再写死保守值。
- 模板生成与周期总结服务改为走真实 AI 调用，并把结构化结果回写到既有任务记录与草稿模板链路。

### 2. 可观测性与 timeout 修复

- 增强 DeepSeek 调用失败日志，区分 timeout、网络失败、HTTP 失败与客户端异常，便于排障。
- 默认 `dailyforge.ai.timeout` 调整到更适合“多轮 tool calling + 大 JSON 输出”的场景。
- 补齐 AI 输出修复与校验链路，降低模型返回 JSON 偏差导致的失败率。
- 更新接口与错误响应说明，使前端能更稳定识别 AI 服务超时与失败状态。

### 3. AI Coach 前端页面与联调

- 新增 `frontend/src/features/ai-coach/**` 功能模块，包含 API、类型、枚举、轮询与展示组件。
- 新增 AI Coach 首页、模板生成页、周期总结页、两个任务结果页及对应测试。
- `AppShell` 与路由接入 `/ai-coach` 主入口，`cycle_template` 与 `workout` 页面补齐跳转入口。
- 页面已支持能力概览、资料缺失提示、任务提交、状态轮询与结构化结果展示。

### 4. 文档同步

- 更新 `README.md`，修正 AI Coach 已真实接入的状态描述。
- 更新 `docs/interfaces/ai_coach_接口文档.md`、`docs/backend/ai_coach_module/ai_coach_DDD.md` 与 AI 接入设计文档。
- 新增 AI 调用失败可观测性与超时策略改造清单、真实 DeepSeek 接入后端改造清单、前端 AI Coach DDD。

## 验证结果

- 后端 `mvn test` 通过，110 tests。
- 前端 `pnpm.cmd test:run` 通过，17 files / 35 tests。
- 前端 `pnpm.cmd build` 通过。
- 契约联调已收口，后端路由、前端 API/types 与接口文档一致。
- 最新增量审查结论为低风险，无高/中风险阻塞项。

## 提交注意事项

- 本次提交应排除 `dbdata/**` 本地导出内容。
- `frontend/tsconfig.app.tsbuildinfo` 仅作为构建副产物，不纳入本次提交。
