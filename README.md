# DailyForge

DailyForge 是一个面向健身用户的 Web 项目，覆盖训练计划管理、训练打卡、身体指标记录，以及后续�?AI 训练模板生成和周期总结�?
## 当前进度

当前仓库已经完成从基础 MVP 到核心模块落地的过渡，前后端均具备可运行的本地开发环境�?
已完成的核心模块�?- `auth`：注册、登录、刷�?token、当前用户、邀请码兑换
- `profile`：基础档案、身体指标记录、首次引导、AI 完整度提�?- `exercise`：系统动作搜索、详情、分类筛选、选择器数�?- `cycle_template`：正式模�?/ 草稿模板、启用切换、编辑、复制、软删除、动作参数三层结�?- `workout`：训练工作台、当�?Day 初始化、保存与打卡、历史详情、周期结束后的下一步选择
- `ai_coach`：AI 模板生成、周期总结、任务历史、最近工具调用、结构化结果回写

当前仍在后续阶段的方向：
- 饮食建议
- 历史统计与趋势分�?- AI 训练建议精细�?
## 关键说明

- `cycle_template` 已升级为 `动作 -> 执行�?-> 参数` 三层结构�?- `workout` 复用同一套结构化动作数据，但训练记录独立保存，不直接依赖模板编辑态�?- `ai_coach` 已接�?Spring AI + DeepSeek，支持真实模型调用、tool calling、结果修复与任务历史�?- AI 相关请求快照、工具中文名、模板来源字段已同步到接口与前端展示�?
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

## 数据库迁�?
当前数据库初始化与升级脚本位于：

- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_base_data.sql`
- `backend/src/main/resources/db/migration/V3__profile_schema_upgrade.sql`
- `backend/src/main/resources/db/migration/V4__cycle_template_schema_upgrade.sql`
- `backend/src/main/resources/db/migration/V5__cycle_template_structure_v2.sql`
- `backend/src/main/resources/db/migration/V6__workout_schema_upgrade.sql`
- `backend/src/main/resources/db/migration/V7__ai_coach_schema_upgrade.sql`

说明�?- 运行时未启用 Flyway 自动迁移
- 当前按顺序手动执�?SQL 脚本

## 本地启动

### 1. 启动基础设施

```powershell
docker compose -f deploy/docker-compose.local.yml up -d
```

默认包含�?- MySQL 8.0
- Redis 7
- phpMyAdmin

### 2. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

### 3. 启动前端

```powershell
cd frontend
pnpm dev
```

## 文档索引

- `docs/prd/`
- `docs/interfaces/`
- `docs/backend/`
- `docs/frontend/`
- `change-log/`
