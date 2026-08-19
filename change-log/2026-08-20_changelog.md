# 2026-08-20 Changelog

## 今日概览

本轮未改动业务代码，完成 DailyForge 从 Codex 到 DSH 的 Agent 迁移与接手基线确认：把 51 个 Codex skill 转换为 DSH 原生 skill，沉淀 8 个固定角色的 DSH subagent 下发模板，并跑通前后端测试基线。

## 今日完成内容

### 1. Codex → DSH Skill 迁移

- 将 `skill-drafts/`（50 个）与 `.codex/skill-drafts/`（1 个 `backend-ddd-writer`）共 51 个 Codex skill 转换为 DSH 原生 skill，落地到 `.dsh/skills/<name>/SKILL.md`。
- 每个 skill 补齐中文 `whenToUse` 路由提示，描述中的 Codex 措辞统一替换为 DSH。
- `backend-ddd-writer` 的 `references/backend-ddd-template.md` 一并迁移。
- 新增 `scripts/convert-codex-skills-to-dsh.ps1` 与 `scripts/skill-whenToUse.json`；脚本保持纯 ASCII、中文映射独立存放，规避 Windows PowerShell 读取无 BOM 脚本的编码乱码问题。

### 2. DSH Subagent 角色定义

- 新增 `docs/dsh_agent_roles.md`，把 8 个固定角色位映射为 DSH subagent 下发模板（含允许写入 / 禁止触碰 / 参考文档 / 返回要求），并说明常驻 / 按需与实例复用规则。
- 创建 3 个常驻角色 subagent 实例（前端架构师 / 后端应用服务构建师 / 代码质量审查员）并完成就绪确认。

### 3. 接手基线验证

- 后端 `mvn test`：119 个测试全部通过。
- 前端 `pnpm test:run`：20 个文件 / 40 个测试全部通过。

### 4. 提交

- `chore(agents): migrate codex skill drafts to dsh skills`
- `docs(agents): add dsh agent roles and project handover doc`

## 注意事项

- 在 DSH 沙箱默认权限下，子进程 spawn 被禁止，导致 `mvn test`（Mockito/ByteBuddy 自附加 agent、Surefire fork）与 `pnpm test:run`（esbuild/Vite 子进程）报 EPERM。后续在 DSH 里运行测试需使用 `danger-full-access` 权限。

## 总结

本轮完成的是 Agent 基建迁移而非业务功能开发，为后续用「主控会话 + DSH subagent + DSH Skill」模式继续开发奠定了基础；接手基线（前后端测试）确认为绿色。
