# DailyForge Agent Rules

本文件是 DailyForge 的多 agent 执行入口规则。

详细制度、角色边界、完整 Pipeline 请查看：

- [docs/agent协作规范.md](docs/agent协作规范.md)

如果本文件与详细版冲突，以 `docs/agent协作规范.md` 为准；日常执行优先按本文件快速决策。

---

## 1. 总体原则

1. 所有实现工作默认先由主控会话确认需求，再决定是否创建 subagent。
2. 所有 agent 在开始前，必须优先读取本轮最新的：
   - `docs/prd/**`
   - `docs/interfaces/**`
   - 本轮改造清单或验收标准
3. 不允许凭旧对话记忆或历史实现习惯直接开工。
4. 写入范围必须尽量互斥；高耦合文件不要交给多个 subagent 并行修改。
5. 当前仓库技术栈必须作为唯一默认约束：
   - 前端：React + TypeScript + Tailwind CSS
   - 后端：Spring Boot + MyBatis / MyBatis-Plus
   - 后端统一响应体：`ApiResponse<T>`
   - 数据库迁移：`backend/src/main/resources/db/migration/**` 顺序 SQL

---

## 2. 默认角色

### 常驻角色

- 主控会话（产品经理 / 项目协调中心）
- 前端架构师
- 后端应用服务构建师
- 代码质量 & 规范审查员

### 按需角色

- 数据层 & SQL 迁移构建师
- DTO & 类型映射师
- 测试用例生成师
- 版本控制 & 提交管家

说明：

- 常驻角色表示几乎每个模块都可能用到
- 按需角色只在特定任务阶段启用

---

## 3. 何时优先本地处理，何时创建 subagent

### 优先由主控会话本地处理

以下情况不要先创建 subagent：

1. 需求仍不清楚
2. 业务规则仍未确认
3. PRD / 接口文档还没定稿
4. 当前下一步被一个小的阻塞问题卡住，主控会话可直接解决
5. 只是要做模块拆解、契约校验、提交前汇总

### 优先创建 subagent

以下情况可创建 subagent：

1. 需求已明确，任务边界清晰
2. 有独立写入范围
3. 前后端或数据层任务可并行
4. 本地下一步不依赖该结果即可继续推进
5. 准备进入测试、审查、提交前门禁阶段

---

## 4. 任务到角色的默认映射

### 新模块需求整理阶段

主控会话本地优先使用：

- `prd-writer`
- `api-contract-writer`
- `iteration-planner`

用途：

- 产出 PRD
- 产出接口文档
- 生成前后端任务清单

### 前端实现阶段

优先创建：前端架构师

默认优先 skill：

- `frontend-module-scaffolder`
- `react-page-builder`
- `component-pattern-enforcer`
- `form-interaction-designer`
- `api-client-integrator`
- `frontend-type-sync`
- `tailwind-ui-refiner`

适用任务：

- 页面开发
- 组件开发
- 交互优化
- API 对接
- 前端类型同步

### 后端实现阶段

优先创建：后端应用服务构建师

默认优先 skill：

- `spring-module-scaffolder`
- `controller-service-builder`
- `api-response-enforcer`
- `swagger-contract-sync`
- `business-rule-guard`
- `auth-permission-checker`

适用任务：

- Controller / Service / Domain 逻辑实现
- 统一响应与异常规范接入
- Swagger 与接口文档同步
- 权限和业务规则接入

### 数据库 / 持久层改造阶段

仅在确实涉及 schema 或 mapper 时创建：数据层 & SQL 迁移构建师

默认优先 skill：

- `sql-migration-writer`
- `schema-reviewer`
- `index-designer`
- `mybatis-mapper-builder`
- `query-performance-checker`
- `seed-data-writer`

适用任务：

- migration 脚本
- 表结构调整
- 索引设计
- MyBatis Mapper / SQL
- 查询性能审查

### DTO / 映射复杂变更阶段

仅在 DTO / VO / Assembler 复杂时创建：DTO & 类型映射师

默认优先 skill：

- `dto-vo-designer`
- `assembler-builder`
- `sensitive-field-filter`
- `naming-format-normalizer`
- `frontend-backend-type-bridge`

适用任务：

- DTO / VO 设计
- Assembler 编写
- 敏感字段过滤
- 前后端字段和格式对齐

### 测试阶段

准备提交或回归时创建：测试用例生成师

默认优先 skill：

- `given-when-then-test-writer`
- `frontend-interaction-test-writer`
- `api-mock-builder`
- `backend-slice-test-writer`
- `service-unit-test-writer`
- `integration-test-runner`
- `coverage-reporter`

适用任务：

- 前端交互测试
- 后端切片 / 单元 / 集成测试
- API Mock
- 测试结果和覆盖范围汇总

### 契约联调阶段

主控会话本地优先使用：

- `contract-checker`

用途：

- 比对 `docs/interfaces/**`
- 比对前端 `api/*.ts` / `types/*.ts`
- 比对后端 Controller / DTO / VO / Swagger

说明：

- 契约联调校验默认由主控会话执行，不优先下放给其他 agent

### 增量审查阶段

创建：代码质量 & 规范审查员

默认优先 skill：

- `git-diff-reviewer`
- `bug-risk-scanner`
- `security-risk-scanner`
- `java-style-reviewer`
- `react-ts-style-reviewer`
- `comment-quality-reviewer`
- `review-report-writer`

说明：

- 审查范围默认限定在 `git diff`
- 风险等级分为：高 / 中 / 低

### 提交前阶段

创建：版本控制 & 提交管家

默认优先 skill：

- `branch-guard`
- `pre-commit-checker`
- `conventional-commit-writer`
- `readme-changelog-updater`
- `release-note-writer`
- `safe-git-operator`

说明：

- Git Steward 只在提交前启用
- 不作为常驻执行角色

---

## 5. Subagent 创建规则

创建 subagent 时，主控会话必须明确：

1. 任务目标
2. 修改范围
3. 参考文档
4. 禁止修改区域
5. 完成后必须返回：
   - 改动文件列表
   - 是否已自测
   - 风险或未完成项

推荐下发格式：

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

## 6. 默认 Pipeline

```text
1. 主控会话确认需求
2. 主控会话产出 PRD / 接口文档 / 改造清单
3. 前端与后端 subagent 并行实现
4. 数据层 / DTO 映射角色按需介入
5. 主控会话执行契约联调校验
6. 测试用例生成师补测试并执行
7. 代码质量审查员审查 git diff
8. Git Steward 做提交前检查、README/changelog、commit 准备
9. 主控会话汇总并决定是否提交 / 推进
```

---

## 7. 提交门禁

在进入 Git Steward 阶段前，默认必须满足：

1. 契约联调校验通过
2. 必要测试通过
3. 增量代码审查风险等级为低

若以下任一条件不满足，则不允许进入正式提交：

- 契约不一致
- 测试失败
- 审查报告风险为中或高

---

## 8. Git 安全规则

1. 允许在满足提交门禁的前提下直接提交到：
   - `main`
   - `master`
   - `develop`
   - 仍然禁止 `git push --force`
   - 仍然要求先完成契约校验、测试和审查
2. 禁止默认执行：
   - `git push --force`
   - 破坏性 reset
3. 提交信息默认遵循 Conventional Commits
4. README / changelog 更新应在提交前完成，而不是提交后补

---

## 9. 文档优先级

执行时文档优先级如下：

1. 当前用户在本轮对话中最新确认的决定
2. `AGENTS.md`
3. `docs/agent协作规范.md`
4. 其他模块文档：
   - `docs/prd/**`
   - `docs/interfaces/**`
   - `docs/backend/**`
   - `docs/frontend/**`

如果规则之间冲突，主控会话应显式指出，并优先遵循最新确认的人类决策。
---

## 10. 主控会话长期执行规则

1. 后续 DailyForge 开发默认只由主控会话与用户直接沟通，主控会话负责根据本文件和 `docs/agent协作规范.md` 主动分派 Subagent，不再要求用户手动把需求和文档转发给其他 Agent。
2. 每轮进入实现、测试、审查或提交阶段前，主控会话必须先判断是否需要调用 Subagent；当任务符合第 3 节“优先创建 subagent”的条件时，应主动使用 `tool_search` 查找可用多 Agent 工具，并按角色创建对应 Subagent。
3. 调用 Subagent 时，主控会话必须同时指定适用 skill、任务目标、写入范围、参考文档、禁止修改区域和返回要求。
4. 如果当前会话没有暴露可用的 Subagent 调度工具，主控会话必须明确说明，并在本地按同一 Pipeline 模拟执行，不得静默跳过测试、审查或 Git Steward 门禁。
5. 主控会话始终负责最终收敛：汇总 Subagent 输出、判断门禁是否通过、决定是否进入下一阶段，并把结论反馈给用户。
