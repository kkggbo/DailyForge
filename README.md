# DailyForge

DailyForge 是一个面向健身用户的 Web 项目，目标是帮助用户完成训练计划管理、训练打卡、饮食建议、身体数据记录与历史统计，并为后续 AI 生成计划与周期总结预留清晰的产品和技术结构。

## 当前进度

当前仓库已经完成从“基础搭建”到“核心 MVP 模块逐步落地”的过渡，现阶段已有可运行的前后端工程与本地开发环境。

已完成的核心模块：
- `auth`：注册、登录、刷新 token、获取当前用户、邀请码兑换
- `profile`：基础档案、身体指标记录、档案引导页、AI 资料完整度提示
- `exercise`：系统动作搜索、系统动作详情、动作选择器筛选元数据、分类筛选、为模板编辑器提供 `defaultStructureType`
- `cycle_template`：正式模板/草稿模板管理、详情页、编辑页、激活切换、复制、删除、运行中模板未来天编辑、动作选择器弹窗与结构化动作编辑体验优化
- `workout`：训练工作台、当前 Day 自动初始化、训练保存与打卡、休息日打卡、训练历史详情、循环结束后的下一步选择
- `ai_coach`：AI 能力概览、异步训练模板生成任务、异步周期总结任务、AI 任务状态查询、AI 生成草稿模板写入与结果结构化返回

当前仍在后续阶段的模块：
- 饮食建议
- 历史统计与趋势分析
- AI 训练建议精细化

## 技术栈

### 前端

- TypeScript 5.7
- React 19
- Vite 6.3
- Tailwind CSS 4.2
- pnpm 9

### 后端

- Java 21
- Spring Boot 3.2.5
- Maven
- MyBatis-Plus 3.5.5
- Spring Security
- JWT
- springdoc-openapi

### 数据与基础设施

- MySQL 8.0
- Redis 7
- Docker Compose

## 当前项目结构

```text
DailyForge/
├─ backend/
├─ frontend/
├─ deploy/
├─ docs/
├─ change-log/
└─ README.md
```

## 已落地能力

### 1. 认证模块

后端已完成：
- 注册
- 登录
- 获取当前用户信息
- 刷新 token
- 登出占位接口
- 邀请码兑换
- 统一接口路径与 Swagger 访问路径约定

前端已完成：
- 登录页
- 注册页
- 基础登录态路由保护

相关文档：
- `docs/interfaces/auth_接口文档.md`
- `docs/backend/auth_module/auth_DDD.md`

### 2. Profile 模块

已完成：
- 基础档案页
- 身体指标记录页
- 首次登录引导页
- AI 使用前资料完整度提示
- 最近一条身体指标删除确认

后端已完成：
- 获取基础档案
- 更新基础档案
- 获取当前身体指标快照
- 获取身体指标历史列表
- 新增身体指标记录
- 删除最近一条身体指标记录
- 获取 AI 资料完整度摘要

前端页面已接入：
- `/profile`
- `/profile/onboarding`
- `/profile/ai-completion`

相关文档：
- `docs/prd/profile_PRD.md`
- `docs/interfaces/profile_接口文档.md`
- `docs/frontend/profile_module/`

### 3. Exercise 模块

已完成：
- 系统动作搜索 / 列表查询
- 系统动作详情查询
- 动作选择器筛选元数据查询
- 支持按一级分类 `categoryCode` 查询动作
- 列表项返回轻量肌肉对象，支持前端分类与标签展示
- 为 `cycle_template` 编辑器提供 `defaultStructureType`
- 支持按系统动作真实结构初始化模板动作参数结构

当前动作查询接口用于：
- 模板编辑器动作弹窗选择
- 左侧分类筛选与关键词联合搜索
- 动作结构初始化
- 后续训练打卡和动作展示的统一动作来源

相关文档：
- `docs/interfaces/exercise_接口文档.md`
- `docs/backend/exercise_module/exercise_DDD.md`
- `docs/frontend/exercise_module/exercise_DDD.md`

### 4. Cycle Template 模块

已完成：
- 正式模板列表
- 草稿模板列表
- 模板详情页
- 草稿创建与编辑
- 模板复制为草稿
- 模板启用 / 激活切换
- 模板软删除
- 当前激活模板摘要查询
- 运行中模板仅允许编辑当前天及未来天
- 保存运行中模板时需要二次确认，并会覆盖当前 Day 未完成训练页填写记录
- 切换激活模板时会取消旧运行循环，保留已完成训练记录
- 动作参数模型升级为 `动作 -> 执行项 -> 参数` 三层结构
- 支持 `set_based` 与 `single_segment` 两类动作结构
- 模板编辑器“添加动作”改为弹窗选择器
- 动作卡片支持“更换动作”
- 动作备注改为按需展开
- 新增一组时默认复制上一组指标内容
- 去掉动作拖拽，保留上移 / 下移排序

前端页面已接入：
- `/cycle-templates`
- `/cycle-templates/create`
- `/cycle-templates/:templateId`
- `/cycle-templates/:templateId/edit`

说明：
- 草稿编辑采用“前端本地修改 + 手动保存”模式
- AI 生成草稿入口已预留，但 AI 能力本身仍属于后续迭代内容
- 当前模板编辑器已切换到新的结构化动作参数模型
- 当前动作选择器已支持粗粒度分类筛选：胸、背、肩、腿、手臂、核心、有氧

相关文档：
- `docs/prd/cycle_template_PRD.md`
- `docs/interfaces/cycle_template_接口文档.md`
- `docs/interfaces/cycle_template_接口文档_v2.md`
- `docs/backend/cycle_template_module/cycle_template_DDD.md`
- `docs/backend/cycle_template_module/cycle_template_DDD_v2.md`
- `docs/backend/cycle_template_module/动作参数模型数据库改造清单.md`
- `docs/动作参数模型改造草案.md`
- `docs/frontend/cycle_template_module/cycle_template_DDD.md`

### 5. Workout 模块

已完成：
- 训练工作台上下文查询
- 当前 Day 训练会话自动初始化或恢复
- 当前循环内历史 Day、当前 Day、未来 Day 浏览
- 训练日与休息日打卡
- 训练动作完成状态、失败 / 跳过原因、实际参数、感受备注记录
- 手动保存进行中的训练会话
- 完成打卡后推进当前循环，但前端保留在已完成 Day
- 最近训练记录列表与训练详情页
- 当前循环完成后的下一步选择：重启当前模板、跳转模板页、AI 分析占位
- 运行中切换模板时取消旧循环和未完成 session，保留历史训练记录

前端页面已接入：
- `/workout`
- `/workout/history/:sessionId`

说明：
- `workout` 复用 `cycle_template v2` 的三层动作结构快照
- 已创建或已完成 session 不直接依赖后续模板结构变化
- AI 分析接口当前返回 `WORKOUT_AI_NOT_IMPLEMENTED`，仅作为后续能力占位

相关文档：
- `docs/prd/workout_PRD.md`
- `docs/interfaces/workout_接口文档.md`
- `docs/backend/workout_module/workout_business_flow.md`
- `docs/backend/workout_module/数据库改造清单.md`
- `docs/frontend/workout_module/workout_DDD.md`
- `docs/frontend/workout_module/workout_页面说明.md`
- `docs/testing/workout_功能测试顺序建议.md`

### 6. AI Coach 模块

已完成：
- AI 能力概览查询
- AI 训练模板生成任务提交与结果查询
- AI 周期总结任务提交与结果查询
- AI 任务记录、状态流转与结构化结果返回
- AI 生成 `cycle_template draft` 与 `source_task_id` 回写
- AI 周期总结基于历史训练快照与模板版本快照生成摘要

当前实现说明：
- 已正式接入 Spring AI + DeepSeek，支持模板生成与周期总结的真实模型调用
- 已落地多轮 tool calling、工具调用明细持久化、结果 JSON 修复与结构化结果回写
- 已补齐 AI 调用失败可观测性与超时策略，默认 timeout 调整为更适合模板生成场景的 `PT120S`
- 前端已接入 `/ai-coach`、模板生成、周期总结与任务结果页，支持能力概览、任务轮询与结果展示
- AI 任务以异步方式执行，状态链路为 `pending -> running -> succeeded/failed`

相关文档：
- `docs/prd/ai_coach_PRD.md`
- `docs/interfaces/ai_coach_接口文档.md`
- `docs/backend/ai_coach_module/AI接入与提示词上下文设计.md`
- `docs/backend/ai_coach_module/ai_coach_数据库改造清单.md`
- `docs/backend/ai_coach_module/ai_coach_DDD.md`

## 数据库与 SQL

当前数据库初始化 / 升级脚本位于：

- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_base_data.sql`
- `backend/src/main/resources/db/migration/V3__profile_schema_upgrade.sql`
- `backend/src/main/resources/db/migration/V4__cycle_template_schema_upgrade.sql`
- `backend/src/main/resources/db/migration/V5__cycle_template_structure_v2.sql`
- `backend/src/main/resources/db/migration/V6__workout_schema_upgrade.sql`
- `backend/src/main/resources/db/migration/V7__ai_coach_schema_upgrade.sql`

说明：
- 项目当前未在运行时启用 Flyway 自动迁移
- 现阶段由开发者按顺序手动执行 SQL
- `V5` 用于把 `cycle_template` 动作参数模型升级到三层结构版本
- `V6` 用于升级训练会话、训练动作执行项和实际参数记录结构
- `V7` 用于新增 AI 任务记录、工具调用记录与模板来源追踪字段

## 本地启动

### 1. 启动基础设施

在项目根目录执行：

```powershell
docker compose -f deploy/docker-compose.local.yml up -d
```

默认包含：
- MySQL 8.0
- Redis 7
- phpMyAdmin

### 2. 初始化数据库

按顺序手动执行以下脚本：

1. `backend/src/main/resources/db/migration/V1__init_schema.sql`
2. `backend/src/main/resources/db/migration/V2__seed_base_data.sql`
3. `backend/src/main/resources/db/migration/V3__profile_schema_upgrade.sql`
4. `backend/src/main/resources/db/migration/V4__cycle_template_schema_upgrade.sql`
5. `backend/src/main/resources/db/migration/V5__cycle_template_structure_v2.sql`
6. `backend/src/main/resources/db/migration/V6__workout_schema_upgrade.sql`
7. `backend/src/main/resources/db/migration/V7__ai_coach_schema_upgrade.sql`

### 3. 启动后端

在 `backend` 目录执行：

```powershell
mvn spring-boot:run
```

或直接在 IDEA 中运行 `DailyForgeApplication`。

### 4. 启动前端

在 `frontend` 目录执行：

```powershell
pnpm install
pnpm dev
```

### 5. 常用地址

- 后端接口前缀：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/api/docs/swagger`
- OpenAPI 文档：`http://localhost:8080/api/docs/api`
- 前端开发环境：`http://localhost:5173`
- phpMyAdmin：`http://localhost:8081`

说明：
- 当前后端采用统一路径策略：`server.servlet.context-path=/api`
- Controller 仅声明资源路径，例如 `/auth`、`/profile`、`/cycle-templates`、`/exercises`、`/workouts`
- Swagger 与 OpenAPI 文档地址统一挂载在 `/api/docs/...`

## 接口与文档索引

### 产品与设计

- [MVP需求清单 v1](docs/MVP需求清单%20v1.md)
- [技术选型方案](docs/技术选型方案.md)
- [项目目录结构设计](docs/项目目录结构设计.md)
- [数据库设计](docs/数据库设计.md)
- [MySQL建表草案](docs/MySQL建表草案.md)
- [动作参数模型改造草案](docs/动作参数模型改造草案.md)

### PRD

- [Profile PRD](docs/prd/profile_PRD.md)
- [Cycle Template PRD](docs/prd/cycle_template_PRD.md)
- [Workout PRD](docs/prd/workout_PRD.md)
- [AI Coach PRD](docs/prd/ai_coach_PRD.md)

### 接口文档

- [Auth 接口文档](docs/interfaces/auth_接口文档.md)
- [Profile 接口文档](docs/interfaces/profile_接口文档.md)
- [Exercise 接口文档](docs/interfaces/exercise_接口文档.md)
- [Cycle Template 接口文档](docs/interfaces/cycle_template_接口文档.md)
- [Cycle Template 接口文档 v2](docs/interfaces/cycle_template_接口文档_v2.md)
- [Workout 接口文档](docs/interfaces/workout_接口文档.md)
- [AI Coach 接口文档](docs/interfaces/ai_coach_接口文档.md)

### DDD / 实现文档

- [Auth 模块 DDD](docs/backend/auth_module/auth_DDD.md)
- [Exercise 后端 DDD](docs/backend/exercise_module/exercise_DDD.md)
- [Exercise 前端 DDD](docs/frontend/exercise_module/exercise_DDD.md)
- [动作选择器后端改造清单](docs/backend/exercise_module/动作选择器后端改造清单.md)
- [Cycle Template 后端 DDD](docs/backend/cycle_template_module/cycle_template_DDD.md)
- [Cycle Template 后端 DDD v2](docs/backend/cycle_template_module/cycle_template_DDD_v2.md)
- [Cycle Template 前端 DDD](docs/frontend/cycle_template_module/cycle_template_DDD.md)
- [动作选择器前端改造清单](docs/frontend/cycle_template_module/动作选择器前端改造清单.md)
- [Workout 业务流程](docs/backend/workout_module/workout_business_flow.md)
- [Workout 前端 DDD](docs/frontend/workout_module/workout_DDD.md)
- [Workout 页面说明](docs/frontend/workout_module/workout_页面说明.md)
- [AI Coach 提示词上下文设计](docs/backend/ai_coach_module/AI接入与提示词上下文设计.md)
- [AI Coach 数据库改造清单](docs/backend/ai_coach_module/ai_coach_数据库改造清单.md)
- [AI Coach 后端 DDD](docs/backend/ai_coach_module/ai_coach_DDD.md)

## 测试情况

当前仓库已包含并逐步补充以下测试方向：
- `auth` 相关单元测试与集成测试
- `exercise` 查询策略测试与集成测试
- `cycle_template` 领域策略测试
- `cycle_template` v2 结构化动作参数模型集成测试
- 前端 Vitest 交互测试：覆盖 auth、profile、exercise、cycle_template、workout 等关键交互
- 后端核心模块集成测试：覆盖 auth、profile、exercise、cycle_template、workout 等关键路径
- 本轮 workout 聚焦验证：后端 30 个测试通过，前端 cycle_template active 保存确认测试通过，前端生产构建通过
- 本轮 ai_coach 聚焦验证：后端 `mvn test` 110 个测试通过，前端 `pnpm.cmd test:run` 17 files / 35 tests 通过，`pnpm.cmd build` 通过，增量审查结论无中高风险

说明：
- 本 README 只描述当前仓库中的测试覆盖方向，不代表所有模块都已达到完整测试覆盖
- 前端生产构建已通过，测试文件不会被 `tsc -b` 纳入正式构建

## 下一步建议

按照当前进度，下一阶段最自然的继续方向是：

1. 继续手动回归 `workout` 与 `cycle_template` 的联动边界，确认真实使用流畅度
2. 开始历史统计与趋势分析，复用已沉淀的训练 session 和身体指标数据
3. 推进饮食建议模块，补齐 MVP 中的饮食建议能力
4. 基于已接入的 AI Coach 继续优化历史统计、饮食建议与更细粒度的 AI 训练建议
