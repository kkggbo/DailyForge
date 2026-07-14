# 2026-07-11 Changelog

时间范围：2026-07-11 至 2026-07-12 凌晨

## 今日概览

今天的开发重点不是堆功能页面，而是把 DailyForge 的第一层地基搭起来。仓库已经从一个概念项目，推进到“有明确产品边界、有数据库方案、有后端工程骨架、有本地运行条件、有首个业务模块落地”的状态。

## 今日完成内容

### 1. 完成 MVP 需求与技术方案沉淀

新增并整理了以下文档：

- `docs/MVP需求清单 v1.md`
- `docs/技术选型方案.md`
- `docs/项目目录结构设计.md`

这部分完成了以下关键决策：

- 明确项目核心目标用户与 MVP 范围
- 确定前后端分离架构
- 确定后端采用模块化单体而不是一开始就做微服务
- 提前为 AI 接口接入预留模块位置

### 2. 完成数据库设计初稿与 ER 图

新增并整理了以下数据库相关内容：

- `docs/数据库设计.md`
- `docs/MySQL建表草案.md`
- `docs/ER图_MVP.png`

这部分完成了以下关键工作：

- 梳理用户、身体指标、动作、器械、肌肉、循环模板、训练记录等核心实体
- 明确了“循环模板 + 训练执行记录”的核心建模思路
- 对邀请码、角色、账户权益、训练计划版本等业务点做了结构设计
- 输出了可继续落地为 SQL 的数据库说明文档

### 3. 生成数据库初始化脚本

新增：

- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `backend/src/main/resources/db/migration/V2__seed_base_data.sql`

本轮处理过的关键点包括：

- 建表顺序与部分外键依赖处理
- `training_sessions.completed_at` 语义修正
- 部分冗余索引清理
- 基础种子数据补充

后续策略调整为：

- 暂不依赖 Flyway 在运行时自动迁移
- 当前阶段由开发者手动执行 SQL 脚本完成初始化

### 4. 初始化后端工程

新增后端工程目录与 Maven 工程骨架：

- `backend/pom.xml`
- `backend/src/main/java/com/dailyforge/...`
- `backend/src/main/resources/...`

后端基础设施已落地：

- `ApiResponse`
- `ErrorCode`
- `BusinessException`
- `GlobalExceptionHandler`
- `SecurityConfig`
- `OpenApiConfig`

这意味着后续所有业务模块都可以在统一响应结构、统一异常模型和统一安全边界上继续开发，而不是每个模块各写一套。

### 5. 落地首个业务模块：Auth

今天最实质性的业务代码成果，是把认证模块做到了可运行、可测试、可写文档的程度。

已完成能力：

- 注册
- 登录
- 获取当前用户信息
- 刷新 token
- 登出占位接口
- 邀请码兑换

相关代码与文档包括：

- `backend/src/main/java/com/dailyforge/modules/auth/...`
- `docs/backend/auth_module/auth_接口文档.md`
- `docs/backend/auth_module/auth_DDD.md`

这一版 Auth 的特点：

- 不是只写接口壳子，而是连权限层级、邀请码权益、异常语义、接口文档一起做了
- 邀请码与账户权益升级逻辑已经进入领域服务和应用服务层
- JWT 鉴权链路已经具备雏形

### 6. 补齐认证相关测试

今天仓库中已经加入了多类认证测试：

- `AuthIntegrationTest`
- `JwtTokenServiceTest`
- `AccountTierPolicyServiceTest`
- `PasswordPolicyServiceTest`

我本次核对到的测试报告里：

- `AuthIntegrationTest`：11 项，`0 failures / 0 errors`
- `JwtTokenServiceTest`：4 项，`0 failures / 0 errors`

说明今天的认证模块不是“只写代码未验证”，而是至少对关键链路做了可执行验证，尤其包括：

- 注册事务
- 登录签发 token
- token 刷新
- 受保护接口鉴权
- 邀请码重复兑换拦截
- 并发兑换超发保护

### 7. 完成本地基础设施方案

新增：

- `deploy/docker-compose.local.yml`
- `deploy/local-services.example.md`
- `docs/本地基础设施启动说明.md`

当前本地依赖服务包括：

- MySQL 8.0
- Redis 7
- phpMyAdmin

同时本地 Spring 配置也已落地，当前策略为：

- 本地开发参数直接写在配置文件中
- 不强制依赖环境变量

### 8. 处理 Flyway 与 MySQL 兼容性问题

今天在基础设施联通阶段，明确遇到了 Flyway 与 MySQL 版本兼容问题。最终做出的决策是：

- 从当前运行链路中移除 Flyway 依赖
- 保留版本化 SQL 脚本目录
- 初始化时改为手动执行 SQL

这是一个正确的阶段性取舍。因为当前首要目标是先让项目能稳定启动，而不是为了迁移工具继续在版本兼容上消耗时间。

### 8.1 修复接口路径不统一问题

在这一天的收尾阶段，还补做了一次非常必要的整理：统一后端接口路径与 Swagger 文档路径。

对应提交：
- `54c00b817ed59426e85f149d1bc825b7ad04571a`

这次修复涉及：
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/dailyforge/config/SecurityConfig.java`
- `backend/src/main/java/com/dailyforge/modules/auth/interfaces/rest/AuthController.java`
- `backend/src/test/java/com/dailyforge/modules/auth/AuthIntegrationTest.java`
- `README.md`

完成后的统一规则是：
- 全局启用 `server.servlet.context-path=/api`
- Controller 只声明资源路径，例如 `/auth`
- 对外业务接口统一为 `/api/...`
- Swagger UI 统一为 `/api/docs/swagger`
- OpenAPI 文档统一为 `/api/docs/api`

这次改动的价值不只是“路径修了一下”，而是把后端对外暴露接口的命名空间策略正式定住了。这个动作很重要，因为如果路径策略在项目早期一直摇摆，后面前端联调、接口文档、测试用例和安全放行规则都会一起反复改。

### 9. 增加仓库忽略规则

新增：

- `.gitignore`

当前已处理的忽略项包括：

- `backend/target/`
- `backend/.mvn/repository/`
- `.idea/`
- 常见系统文件

## 今日产出总结

今天的工作有两个最明显的优点。

第一，你没有一开始就急着做页面，而是先把需求边界、数据库模型、后端基建和认证模块打稳。这对于一个想长期演进、还准备接 AI 的项目来说是正确路径。很多项目早期失败，不是因为功能做得少，而是因为底层结构太乱，后面每加一个功能都要返工。你今天避免了这个问题。

第二，你在“设计文档 - 数据库结构 - 初始化 SQL - 后端代码 - 测试 - 本地基础设施”这条链路上是连续推进的，不是碎片式开发。这说明你已经在按工程项目而不是练手 demo 的方式推进 DailyForge，这一点很重要。

## 做得正确的地方

- 先定 MVP，再定技术栈，再做工程，顺序是对的
- 及时把讨论内容沉淀成文档，降低后续回忆成本
- 数据库设计没有只停留在“几张表”，而是开始围绕真实业务流程建模
- 认证模块不只是登录注册，还把邀请码权益和角色体系一并考虑进来了
- 已经开始写测试，尤其把并发兑换这种高风险点纳入验证，这个意识很好
- 遇到 Flyway 兼容问题时没有硬扛，而是做阶段性降级，决策是务实的

## 可以继续优化的地方

- 当前仓库里仍然有 `.codex/` 处于未忽略状态，建议补进 `.gitignore`
- 后续需要注意不要把更多本地运行产物、IDE 缓存或临时文件带进仓库
- 根级 README 到今天才补上，这一步应该尽量更早做，方便后续任何时候回看项目状态
- 文档很多是当天快速沉淀的，后面建议逐步做一版“目录索引 + 文档分层”，避免文档数量上涨后查找成本变高
- 认证模块已经起步，但前端工程还没初始化，下一阶段要尽快把前后端联调链路打通，否则后端设计反馈会偏慢
- 目前数据库初始化改成手动执行是合理的，但等项目稳定后，还是建议再引回迁移工具，否则长期演进时脚本管理会逐渐变重

## 我对今天开发质量的评价

如果把今天定义为“项目第一天”，这一天是合格且偏优秀的。你做的不是表面热闹的功能堆叠，而是把未来三到四周最容易踩坑的基础问题先解决了：需求收敛、结构边界、数据库抽象、认证模型、本地环境和测试意识。

真正值得保持的是你今天的工程节奏：

- 先讨论清楚再落文档
- 文档确定后再写结构
- 结构确定后再写代码
- 写完代码后再补测试和启动链路

这条节奏如果后面还能保持，DailyForge 会明显比很多个人项目更稳。

## 建议的下一步

1. 初始化前端工程并建立基础路由、登录页和首页骨架
2. 把当前 Auth 模块和前端登录态管理联通
3. 开始 `profile`、`plan`、`workout` 的最小闭环设计与接口定义
4. 继续补仓库规范，例如 `.gitignore`、提交信息规范、后续 CI
