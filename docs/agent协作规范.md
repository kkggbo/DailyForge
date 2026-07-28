# DailyForge 多 Agent 协作规范

> 版本：v1.0  
> 日期：2026-07-28  
> 适用范围：DailyForge 项目主控会话与各类 Subagent 协作流程  
> 文档状态：正式协作规范

---

## 一、文档目标

本文档用于统一 DailyForge 项目在 Codex 中的多 Agent 协作方式，明确：

- 主控会话与各类 Subagent 的职责边界
- 各 Agent 的允许写入范围
- 需求确认、开发、测试、审查、提交的标准执行顺序
- 当前项目真实技术栈下的约束，避免 Agent 输出与仓库现状不匹配的实现

本规范优先服务于以下目标：

1. 降低你在多个会话之间来回切换的管理成本
2. 减少前后端、文档、测试之间的上下文不同步
3. 保证每次开发都以最新 PRD、接口文档和改造清单为依据
4. 让主控会话可以作为项目级协调中心，而不是所有工作的唯一执行者

---

## 二、协作原则

### 2.1 文档优先

所有 Agent 在开始执行前，必须先读取主控会话最新生成的：

- PRD
- 接口文档
- 改造清单
- 验收标准

不得凭记忆、自行脑补或使用过期结论继续开发。

### 2.2 以当前仓库技术栈为准

当前项目以后端 `Spring Boot + MyBatis-Plus / MyBatis Mapper`、前端 `React + TypeScript + Tailwind CSS` 为准。

禁止引入与当前项目风格明显冲突的默认约束，例如：

- 强行改成 JPA Repository 风格
- 强行改成 `Result<T>` 响应体
- 强行引入与现有架构不一致的基础设施模式

### 2.3 后端统一响应体

后端统一响应体以项目当前使用的：

- `ApiResponse<T>`

为准，不使用 `Result<T>`。

### 2.4 数据库变更原则

数据库变更以：

- `backend/src/main/resources/db/migration/**`

下的顺序 SQL 脚本为准。

当前阶段：

- 不强制启用运行时 Flyway 自动迁移
- SQL 迁移脚本仍需保持清晰顺序和版本兼容性

### 2.5 写入范围必须互斥

多 Agent 并行开发时，写入范围必须尽量互斥。

若两个角色需要改同一批高耦合文件，应优先：

- 收敛为一个 Agent 负责
- 或先由主控会话拆清楚顺序，再分阶段执行

避免交叉覆盖、重复修改和冲突合并。

### 2.6 提交前置门禁

所有提交前必须完成：

1. 契约联调校验
2. 测试执行
3. 增量代码审查

未满足门禁条件，不允许进入正式提交流程。

---

## 三、Agent 角色总览

本项目采用：

- 4 个常驻角色
- 4 个按需角色

说明：

- 常驻角色是绝大多数模块开发都会用到的基础角色
- 按需角色只在特定类型任务中启用，避免角色拆分过细带来管理负担

---

## 四、常驻角色

## 4.1 主控会话（产品经理 / 项目协调中心）

### 核心职责

1. 和你确认需求、边界、优先级
2. 产出 PRD、接口文档、改造清单、验收标准
3. 调用不同的 Subagent 执行相关任务
4. 汇总各个方向的结果，判断是否能进入联调
5. 在联调前负责执行契约联调校验
6. 在提交流程前检查：
   - 文档是否同步
   - 前后端是否按同一契约实现
   - 测试与审查结果是否满足提交条件

### 契约联调校验范围

主控会话必须比对以下内容是否一致：

- `docs/interfaces/**`
- 前端 `frontend/src/features/**/api/*.ts`
- 前端 `frontend/src/features/**/types/*.ts`
- 后端 `Controller` 参数、DTO、VO
- 后端 Swagger 注解与接口路径

### 禁止事项

- 不直接承担大规模前后端实现
- 不跳过文档直接让 Agent 开发
- 不在测试或审查失败时推进提交

---

## 4.2 前端架构师 & 组件构建师 (Frontend Architect)

### 核心职责

1. 只处理 `frontend/` 和前端文档
2. 根据 PRD、接口文档、前端改造清单实现页面、组件、交互、API 封装、类型定义
3. 维护前端模块结构清晰、组件职责清晰
4. 在不偏离现有风格的前提下优化交互体验

### 技术约束

1. 使用 TypeScript 严格模式
2. 明确定义 `type` / `interface` / `Props`
3. 遵循函数式组件 + Hooks 模式
4. 样式遵循项目当前约定：
   - Tailwind CSS
   - 必要时少量本地样式封装
5. 优先复用现有：
   - `features/**`
   - `types/**`
   - `lib/**`
   - `api/**`

### 允许写入

- `frontend/**`
- `docs/frontend/**`

### 禁止触碰

- `backend/**`
- `db/**`

---

## 4.3 后端应用服务构建师 (Backend Application Builder)

### 核心职责

1. 只处理后端接口实现、应用服务、领域逻辑、后端文档
2. 在写实现代码前，必须先确认：
   - 接口文档已更新
   - Swagger 注解/接口路径已明确
3. 按项目当前后端风格实现：
   - `Controller`
   - `Application Service / Service`
   - `Domain Service`
   - `Mapper`
4. 接入统一异常处理和统一响应体 `ApiResponse<T>`

### 技术约束

1. 使用 Spring Boot 标准分层
2. 使用当前项目风格：
   - `Controller -> ApplicationService/Service -> Persistence Mapper`
3. DTO / VO / Entity 必须分层，不得直接暴露 Entity 给前端
4. 后端接口必须同步更新 Swagger 注解
5. 优先保持与现有模块一致的包结构、命名和返回风格

### 允许写入

- `backend/src/main/java/**`
- `backend/src/test/java/**` 中与后端逻辑直接相关的测试
- `docs/backend/**`
- `docs/interfaces/**`

### 禁止触碰

- `frontend/**`
- `db/migration/**`

说明：

- 若本轮明确启用“数据层 Agent”，则后端应用服务构建师对 persistence / schema 相关文件只读。

---

## 4.4 代码质量 & 规范审查员 (Quality Guardian)

### 核心职责

1. 只读审查，不允许修改任何代码
2. 审查范围限定为增量变更（`git diff`）
3. 检查明显 Bug、风险、规范问题
4. 输出审查报告，并给出风险等级：
   - 高
   - 中
   - 低

### 审查重点

1. 明显 Bug
   - 空指针
   - 未捕获异常
   - 错误状态分支遗漏
   - 前端状态竞争 / 脏状态
2. 安全问题
   - SQL 注入
   - XSS
   - 硬编码密钥
   - 权限绕过
3. 规范问题
   - Java：优先遵循项目现有代码风格；无明确规则时参考阿里 Java 开发规范
   - React / TypeScript：遵循项目 ESLint / TypeScript 严格模式
4. 可读性
   - 命名是否清晰
   - 分层是否合理
   - 逻辑是否过度耦合

### 注释规范

1. 只对以下内容强制要求注释：
   - `public` 方法
   - 复杂业务逻辑（圈复杂度较高）
2. 注释必须解释 Why
3. 注释必须为中文
4. 必须检查注释是否与实际代码一致

### 审查结论规则

- 低风险：允许进入提交流程
- 中风险 / 高风险：阻塞提交，退回修改

### 允许写入

- 无

### 禁止触碰

- 全部写入操作

---

## 五、按需角色

## 5.1 数据层 & SQL 迁移构建师 (Data Layer / SQL Migration Agent)

### 启用时机

仅在以下场景启用：

- 建表 / 改表
- SQL 迁移脚本新增或调整
- 索引设计
- 持久化实体与 Mapper 需要同步改动

### 核心职责

1. 只处理：
   - `db/migration/**`
   - persistence `entity/**`
   - persistence `mapper/**`
2. SQL 脚本必须按当前 migration 顺序体系维护
3. 索引设计必须一起给出，不能只建表
4. 必须显式考虑查询性能与 N+1 风险

### 技术约束

1. 不使用 JPA Repository 作为强制模式
2. 遵循当前项目 MyBatis / MyBatis-Plus 风格
3. 不允许把复杂持久化拼装逻辑塞到 Controller
4. 如果涉及关联读取，必须显式控制查询策略，不做隐式全量拉取

### 允许写入

- `backend/src/main/resources/db/migration/**`
- `backend/src/main/java/**/infrastructure/persistence/entity/**`
- `backend/src/main/java/**/infrastructure/persistence/mapper/**`

### 禁止触碰

- `controller/**`
- `frontend/**`

---

## 5.2 DTO & 类型映射师 (DTO Mapper)

### 启用时机

仅在以下场景启用：

- DTO / VO 字段较多
- 前后端字段映射复杂
- 返回结构重构
- 需要清理 Entity 暴露问题

### 核心职责

1. 只处理 Entity 与 DTO / VO 之间的转换
2. 不允许直接将 Entity 暴露给前端
3. 过滤敏感字段
4. 确保字段命名与时间格式转换正确
5. 保持前后端类型语义一致

### 当前项目适配说明

- 优先适配当前项目的手动 Assembler / Mapper 风格
- 不强制引入 MapStruct
- 前端类型同步时，可联动检查：
  - `frontend/src/features/**/types/**`
  - `frontend/src/features/**/lib/*mapper*.ts`

### 允许写入

- `backend/src/main/java/**/interfaces/dto/**`
- `backend/src/main/java/**/interfaces/vo/**`
- `backend/src/main/java/**/application/assembler/**`
- 必要时相关前端 `types/**`、`lib/**mapper**`

### 禁止触碰

- `controller/**`
- `service/**`
- `db/migration/**`

---

## 5.3 测试用例生成师 (Test Generator)

### 核心职责

1. 为新增或修改的代码生成测试用例
2. 执行测试并输出报告
3. 报告必须包含：
   - 通过 / 失败
   - 失败原因
   - 覆盖范围说明

### 前端测试要求

1. 生成 `*.test.tsx`
2. 使用：
   - React Testing Library
   - Vitest / Jest
3. 必须覆盖：
   - 用户点击
   - 输入
   - 状态变化
   - API Mock（如 MSW）

### 后端测试要求

1. 生成：
   - `*Test.java`
2. 使用：
   - JUnit 5
   - Mockito
   - 必要时 `@SpringBootTest`
   - Controller 层优先 `@WebMvcTest`
3. 必须遵循 Given-When-Then 结构

### 执行职责

- 负责执行单元测试
- 必要时执行模块级集成测试
- 输出测试结论供主控会话和 Git Steward 使用

### 允许写入

- `frontend/**/*.test.tsx`
- `backend/src/test/**`

### 禁止触碰

- 业务主代码 `src/main/**`
- 非测试用途文档

---

## 5.4 版本控制 & 提交管家 (Git Steward)

### 启用时机

仅在准备提交时启用。

### 核心职责

1. 只处理：
   - `.git`
   - 分支管理
   - Commit Message
   - `README.md`
   - `change-log/**`
2. 提交前必须确认：
   - 契约联调校验已通过
   - 测试通过
   - 代码审查风险等级为低
3. 生成规范化提交信息
4. 更新 README / changelog

### 提交规范

1. 允许在满足提交门禁的前提下直接提交到：
   - `main`
   - `master`
   - `develop`
   - 仍然禁止 `git push --force`
   - 仍然要求先完成契约联调校验、测试与代码审查
2. 必须遵守 Conventional Commits：
   - `feat:`
   - `fix:`
   - `refactor:`
   - `perf:`
   - `docs:`
   - `test:`
3. 如存在 Issue / Ticket，Commit Body 必须引用  
   例如：
   - `Closes #USER-123`
4. 若仓库配置了 pre-commit，则提交前执行
5. 绝对禁止：
   - `git push --force`

### 允许写入

- `.git/**`
- `README.md`
- `change-log/**`

### 禁止触碰

- 业务代码
- SQL 业务实现
- 在未满足提交门禁时直接 push 到 `main`、`master` 或 `develop`

---

## 六、推荐执行顺序（Pipeline）

```text
1. 【主控会话】确认需求 -> 产出 PRD + 接口文档 + 改造清单 + 验收标准
         ↓
2. 【前端架构师】 + 【后端应用服务构建师】并行开发
         ↓
3. 【数据层 Agent】 / 【DTO Mapper】按需介入
         ↓
4. 【主控会话】执行契约联调校验
   - 比对 docs/interfaces
   - 比对前端 api/types
   - 比对后端 Controller/DTO/VO
   不通过则退回步骤 2
         ↓
5. 【测试用例生成师】补测试 + 执行测试
   测试不通过则退回步骤 2
         ↓
6. 【代码质量审查员】审查 git diff，输出风险等级
   中/高风险则退回步骤 2
         ↓
7. 【版本控制管家】生成 Commit Message + 更新 README/changelog + 提交到功能分支
         ↓
8. 【主控会话】汇总报告，判断是否进入联调环境 / 提交 / 推送
```

---

## 七、职责边界表

| Agent | 允许读取 | 允许写入 | 禁止触碰 |
|---|---|---|---|
| 主控会话 | 全项目 | `docs/prd/**`、`docs/interfaces/**`、改造清单、验收文档 | 大规模业务实现 |
| 前端架构师 | 全项目 | `frontend/**`、`docs/frontend/**` | `backend/**`、`db/**` |
| 后端应用服务构建师 | 全项目 | `backend/src/main/java/**`、`backend/src/test/**`、`docs/backend/**`、`docs/interfaces/**` | `frontend/**`、`db/migration/**` |
| 数据层 Agent | 全项目 | `backend/src/main/resources/db/migration/**`、`infrastructure/persistence/entity/**`、`infrastructure/persistence/mapper/**` | `controller/**`、`frontend/**` |
| DTO Mapper | 全项目 | `dto/**`、`vo/**`、`assembler/**`、必要时前端 `types/**` | `controller/**`、`db/migration/**` |
| 测试用例生成师 | 全项目只读 | `frontend/**/*.test.tsx`、`backend/src/test/**` | 主业务代码 |
| 代码质量审查员 | 全项目只读 | 无 | 任何写入 |
| 版本控制管家 | 全项目只读 | `.git/**`、`README.md`、`change-log/**` | 业务代码、`git push --force` |

---

## 八、主控会话的额外规则

主控会话在调用 Subagent 前，必须先做三件事：

1. 明确本轮目标
2. 明确各 Agent 的写入边界
3. 明确本轮验收标准

推荐下发给 Subagent 的任务格式：

```text
任务目标：
修改范围：
参考文档：
禁止修改：
完成后必须返回：
- 改动文件列表
- 是否已自测
- 风险或未完成项
```

---

## 九、适合当前项目的使用方式

### 9.1 日常最常用的组合

1. 主控会话
2. 前端架构师
3. 后端应用服务构建师

### 9.2 只有这些情况再额外开 Agent

- 改数据库：开数据层 Agent
- 字段映射复杂：开 DTO Mapper
- 准备提交前：开测试用例生成师、代码质量审查员、Git Steward

---

## 十、维护建议

当项目出现以下变化时，应同步更新本规范：

1. 后端基础设施发生变化  
   例如：正式启用 Flyway 自动迁移、引入新的统一响应模型
2. 前端工程结构发生明显调整  
   例如：目录拆分、状态管理方案切换、测试框架切换
3. 多 Agent 协作方式发生变化  
   例如：新增长期角色、调整门禁流程、引入新的固定技能

建议在以下场景复查本规范：

- 每完成一个较大模块
- 每出现一次明显的多 Agent 协作失配
- 每次准备调整项目开发流程时

---

## 十一、结论

这份规范适合当前 DailyForge 的项目阶段，因为它：

- 保留了工程化协作流程
- 对齐了当前仓库真实技术栈
- 避免角色拆得过细导致你自己管理成本过高
- 支持主控会话 + Subagent 的持续协作模式

后续新模块开发时，应默认以本规范作为协作章程，再由主控会话根据具体模块特点补充本轮专属的改造清单和验收标准。
