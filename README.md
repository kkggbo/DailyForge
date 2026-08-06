# DailyForge

DailyForge 是一个面向健身训练场景的 Web 应用，当前聚焦于训练模板管理、训练打卡、身体指标记录，以及 AI 辅助训练模板生成和周期总结。

## 当前进度

当前仓库已经完成 MVP 核心链路的前后端落地，并具备本地可运行环境。

已完成模块：

- `auth`：注册、登录、刷新 Token、当前用户、邀请码兑换
- `profile`：基础档案、身体指标记录、首次欢迎引导、AI 使用前信息完整度提示
- `exercise`：系统动作库查询、关键词搜索、分类筛选、动作详情、模板动作选择器数据支持
- `cycle_template`：草稿模板、正式模板、启用切换、编辑保存、软删除、AI 生成来源标识
- `workout`：训练工作台、Day 导航、训练打卡、历史详情、周期结束后的后续选择
- `ai_coach`：AI 训练模板草稿生成、AI 周期总结、任务历史、工具调用记录、结构化结果回写

当前仍在后续规划中的方向：

- 饮食建议与饮食计划
- 历史统计与趋势分析
- AI 输出质量持续优化

## 关键设计说明

- `cycle_template` 已升级为 `动作 -> 执行项 -> 参数` 的三层结构，支持更灵活的训练动作建模。
- `workout` 会在创建训练会话时复制模板快照，训练记录独立保存，不直接依赖后续模板编辑状态。
- `ai_coach` 通过 Spring AI + OpenAI 兼容客户端接入 DeepSeek，支持 tool calling、多轮补数、结果格式校验与任务历史追踪。
- AI 任务历史、最近工具调用中文名、模板来源标识等信息已经同步到后端接口和前端展示层。

## 技术栈

### 前端

- React 19
- TypeScript 5.7
- Vite 6.3
- Tailwind CSS 4.2
- pnpm 9

### 后端

- Java 21
- Spring Boot 3.2.5
- MyBatis-Plus 3.5.5
- Spring Security
- JWT
- springdoc-openapi
- Maven

### 基础设施

- MySQL 8.0
- Redis 7
- Docker Compose

## 数据库迁移脚本

当前数据库初始化与升级脚本位于：

- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_base_data.sql`
- `backend/src/main/resources/db/migration/V3__profile_schema_upgrade.sql`
- `backend/src/main/resources/db/migration/V4__cycle_template_schema_upgrade.sql`
- `backend/src/main/resources/db/migration/V5__cycle_template_structure_v2.sql`
- `backend/src/main/resources/db/migration/V6__workout_schema_upgrade.sql`
- `backend/src/main/resources/db/migration/V7__ai_coach_schema_upgrade.sql`

说明：

- 当前运行时未启用 Flyway 自动迁移
- 本地开发默认按顺序手动执行 SQL 脚本

## 本地启动

### 1. 启动基础设施

```powershell
docker compose -f deploy/docker-compose.local.yml up -d
```

默认包含：

- MySQL 8.0
- Redis 7
- phpMyAdmin

### 2. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

说明：

- AI 功能需要单独配置真实的 API Key
- `application.yml` 中应保持占位值，敏感配置通过环境变量或本地覆盖文件注入

### 3. 启动前端

```powershell
cd frontend
pnpm install
pnpm dev
```

## 主要页面入口

- 训练模板：`/cycle-templates`
- 训练工作台：`/workout`
- AI 教练：首页入口与 `/ai-coach/**`
- Swagger 文档：请以当前后端安全配置和网关映射为准，不再在 README 中写死旧路径

## 文档索引

- `docs/prd/`：产品需求文档
- `docs/interfaces/`：接口文档
- `docs/backend/`：后端设计与改造文档
- `docs/frontend/`：前端设计文档
- `change-log/`：每日开发日志
