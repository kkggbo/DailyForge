# DailyForge

DailyForge 是一个面向健身用户的 Web 项目，目标是帮助用户完成训练计划管理、训练打卡、饮食建议、身体数据记录与历史统计，并为后续 AI 生成计划与周期总结预留清晰的产品和技术结构。

## 当前进度

当前仓库已经完成从“基础搭建”到“核心 MVP 模块开始落地”的过渡，现阶段已有可运行的前后端工程与本地开发环境。

已完成的核心模块：
- `auth`：注册、登录、刷新 token、获取当前用户、邀请码兑换
- `profile`：基础档案、身体指标记录、档案引导页、AI 资料完整度提示
- `cycle_template`：正式模板/草稿模板管理、详情页、编辑页、激活切换、复制、删除、运行中模板未来天编辑

当前仍在后续阶段的模块：
- `training session / workout`：训练打卡闭环
- 饮食建议
- 历史统计与趋势分析
- AI 真正落地的计划生成与总结能力

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

### 3. Cycle Template 模块

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

前端页面已接入：
- `/cycle-templates`
- `/cycle-templates/create`
- `/cycle-templates/:templateId`
- `/cycle-templates/:templateId/edit`

说明：
- 草稿编辑采用“前端本地修改 + 手动保存”模式
- AI 生成草稿入口已预留，但 AI 能力本身仍属于后续迭代内容

## 数据库与 SQL

当前数据库初始化脚本位于：

- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_base_data.sql`
- `backend/src/main/resources/db/migration/V4__cycle_template_schema_upgrade.sql`

说明：
- 项目当前未在运行时启用 Flyway 自动迁移
- 现阶段由开发者按顺序手动执行 SQL

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
3. `backend/src/main/resources/db/migration/V4__cycle_template_schema_upgrade.sql`

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
- Controller 仅声明资源路径，例如 `/auth`、`/profile`、`/cycle-templates`
- Swagger 与 OpenAPI 文档地址也统一挂载在 `/api/docs/...`

## 接口与文档索引

### 产品与设计

- [MVP需求清单 v1](docs/MVP需求清单%20v1.md)
- [技术选型方案](docs/技术选型方案.md)
- [项目目录结构设计](docs/项目目录结构设计.md)
- [数据库设计](docs/数据库设计.md)
- [MySQL建表草案](docs/MySQL建表草案.md)

### PRD

- [Profile PRD](docs/prd/profile_PRD.md)
- [Cycle Template PRD](docs/prd/cycle_template_PRD.md)

### 接口文档

- [Auth 接口文档](docs/interfaces/auth_接口文档.md)
- [Profile 接口文档](docs/interfaces/profile_接口文档.md)
- [Cycle Template 接口文档](docs/interfaces/cycle_template_接口文档.md)

### DDD / 实现文档

- [Auth 模块 DDD](docs/backend/auth_module/auth_DDD.md)
- [Cycle Template 后端 DDD](docs/backend/cycle_template_module/cycle_template_DDD.md)
- [Cycle Template 前端 DDD](docs/frontend/cycle_template_module/cycle_template_DDD.md)

## 测试情况

当前仓库已包含并逐步补充以下测试方向：
- `auth` 相关单元测试与集成测试
- `cycle_template` 领域策略测试
- `cycle_template` 集成测试

说明：
- 本 README 只描述当前仓库中的测试覆盖方向，不代表所有模块都已达到完整测试覆盖

## 下一步建议

按照当前进度，下一阶段最自然的继续方向是：

1. 开始 `training session` 模块，打通“模板 -> 当天训练 -> 打卡 -> 推进到下一天”的闭环
2. 明确训练打卡的数据结构与接口契约
3. 在完成训练记录后，再继续做历史统计与趋势分析
4. 最后再引入真正的 AI 计划生成与周期总结能力
