# DailyForge

DailyForge 是一个面向健身用户的 Web 项目，目标是帮助用户完成训练计划制定、训练与饮食建议、训练打卡和历史数据统计，并为后续接入 AI 个性化分析与计划生成能力预留清晰结构。

## 当前状态

当前仓库已完成第一天的基础建设，重点落在产品方案、数据库设计、后端工程骨架、认证模块雏形和本地基础设施方案上。

- 产品侧已经完成 MVP 需求梳理
- 架构侧已经确定为前后端分离、模块化单体后端
- 数据侧已经完成数据库设计文档、ER 图和初始化 SQL 草案
- 后端已经完成 Spring Boot 工程初始化和首个 `auth` 模块
- 本地开发环境已经提供 Docker Compose 方案
- 前端工程尚未初始化

## 项目目标

MVP 聚焦以下能力：

- 账号体系
- 健身计划
- 训练打卡
- 饮食建议
- 历史统计

目标用户包括：

- 新手健身用户：需要基础知识、入门引导和 AI 建议
- 进阶训练者：需要更灵活的计划编排、执行记录和数据统计

## 当前技术方案

### 前端

- TypeScript 5.7
- React 19
- Vite 6.3
- Tailwind CSS 4.2
- pnpm 9.15

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

## 已完成内容

### 1. 产品与架构文档

- MVP 需求清单
- 技术选型方案
- 项目目录结构设计
- 数据库设计
- MySQL 建表草案
- 后端基础设施设计
- 本地基础设施启动说明

### 2. 数据库设计与初始化脚本

- 已完成数据库实体与关系设计
- 已产出一版 ER 图
- 已生成初始化 SQL：
  - `backend/src/main/resources/db/migration/V1__init_schema.sql`
  - `backend/src/main/resources/db/migration/V2__seed_base_data.sql`

说明：

- 当前阶段不在运行时启用 Flyway
- 初始化 SQL 由开发者手动执行

### 3. 后端工程初始化

已初始化 `backend` 工程，并建立以下基础结构：

- `common`
- `config`
- `infrastructure`
- `modules`

已完成的基础设施包括：

- 统一返回体 `ApiResponse`
- 错误码 `ErrorCode`
- 业务异常 `BusinessException`
- 全局异常处理 `GlobalExceptionHandler`
- 安全配置 `SecurityConfig`
- OpenAPI 配置 `OpenApiConfig`

### 4. Auth 模块首版

当前已经实现并补充文档的认证能力包括：

- 注册
- 登录
- 获取当前用户信息
- 刷新 token
- 登出占位接口
- 邀请码兑换

当前鉴权方案基于：

- Spring Security
- JWT Access Token / Refresh Token
- 邀请码驱动的账户权益升级

### 5. 本地基础设施

已提供本地开发用编排文件：

- `deploy/docker-compose.local.yml`

当前包含服务：

- MySQL 8.0
- Redis 7
- phpMyAdmin

## 目录结构

```text
DailyForge/
├─ backend/
├─ deploy/
├─ docs/
├─ change-log/
└─ README.md
```

## 本地启动说明

当前可以先启动后端和本地依赖，前端后续补建。

### 1. 启动本地基础设施

在项目根目录执行：

```powershell
docker compose -f deploy/docker-compose.local.yml up -d
```

### 2. 手动初始化数据库

按顺序执行以下 SQL：

1. `backend/src/main/resources/db/migration/V1__init_schema.sql`
2. `backend/src/main/resources/db/migration/V2__seed_base_data.sql`

### 3. 启动后端

可在 IDEA 中直接运行 `DailyForgeApplication`，或在 `backend` 目录执行：

```powershell
mvn spring-boot:run
```

### 4. 常用访问地址

- 业务接口当前前缀：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/api/docs/swagger`
- OpenAPI 文档：`http://localhost:8080/api/docs/api`
- phpMyAdmin：`http://localhost:8081`

### 5. 当前接口路径规则

当前项目的接口前缀策略已经统一，实际情况如下：

- 已配置全局 `server.servlet.context-path=/api`
- 当前业务接口由 Controller 只声明资源路径，例如 `@RequestMapping("/auth")`
- 当前 Swagger 相关地址通过 `springdoc` 配置为 `/docs/swagger` 和 `/docs/api`，对外访问时会自动带上 `/api`

因此当前对外访问结果是：

- 业务接口走 `/api/...`
- Swagger 走 `/api/docs/...`

因此当前正确的 Swagger 地址就是 `http://localhost:8080/api/docs/swagger`。

### 6. 当前统一方案

当前后端已经统一为“全局前缀负责 API 命名空间，Controller 只写资源路径”的模式：

1. 启用全局配置：`server.servlet.context-path=/api`
2. Controller 只保留资源路径，例如 `@RequestMapping("/auth")`
3. 业务接口统一收敛到 `/api/...`
4. Swagger 与 OpenAPI 统一挂到 `/api/docs/swagger`、`/api/docs/api`
5. Spring Security 放行规则与内部资源路径保持一致

采用这套方案后，路径会更稳定：

- 业务接口：`/api/auth/...`
- Swagger UI：`/api/docs/swagger`
- OpenAPI：`/api/docs/api`
- 后续新增模块也按同一规则组织

## 测试情况

当前仓库中已经存在认证相关单元测试与集成测试，覆盖了：

- JWT 生成与解析
- token 类型校验
- token 过期校验
- 注册、登录、刷新 token
- 受保护接口鉴权
- 邀请码兑换
- 邀请码并发兑换超发保护

## 文档索引

- [MVP需求清单 v1](docs/MVP需求清单%20v1.md)
- [技术选型方案](docs/技术选型方案.md)
- [项目目录结构设计](docs/项目目录结构设计.md)
- [数据库设计](docs/数据库设计.md)
- [MySQL建表草案](docs/MySQL建表草案.md)
- [后端基础设施设计](docs/后端基础设施设计.md)
- [本地基础设施启动说明](docs/本地基础设施启动说明.md)
- [Auth 模块接口文档](docs/backend/auth_module/auth_接口文档.md)
- [Auth 模块 DDD 设计](docs/backend/auth_module/auth_DDD.md)

## 下一步建议

按当前项目进度，下一阶段建议优先处理以下事项：

1. 初始化前端工程
2. 打通注册/登录后的基础页面与接口联调
3. 开始 `plan`、`workout`、`profile` 领域建模
4. 清理和补强仓库规范，例如补充更多忽略规则、提交规范和后续 CI

## 说明

这个仓库当前更接近“基础建设完成、核心业务即将展开”的状态。第一天最重要的价值不是做出很多页面，而是把业务边界、技术路线、数据库结构和后端基建先立住，这一步已经完成得比较扎实。
