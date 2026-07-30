# 2026-07-30 Changelog

## 今日概览

本轮完成了 `workout` 训练打卡闭环的全栈落地，并补齐与 `cycle_template` 的运行中模板联动规则。当前项目已经可以从激活模板进入训练工作台，记录当前 Day 的实际训练情况，完成打卡并推进循环。

## 今日完成内容

### 1. Workout 模块全栈实现

- 新增 `workout` 后端模块，覆盖训练上下文、当前 Day 初始化、训练保存、完成打卡、历史记录、训练详情、重启循环和 AI 分析占位接口。
- 新增训练会话、训练动作、执行项、实际参数等持久层结构与 V6 SQL 迁移脚本。
- 前端新增 `/workout` 训练工作台和 `/workout/history/:sessionId` 训练详情页。
- 支持历史 Day 只读、当前 Day 可编辑、未来 Day 预览。
- 支持休息日打卡、循环完成页、重启当前模板和跳转模板页。

### 2. 优化训练打卡交互

- 限制重量、次数、时长等实际参数的非法输入，避免负数和过长小数。
- 实际值改为按需填写，未填写时默认按计划完成并按计划值回显。
- 动作完成状态调整到动作卡片底部。
- 失败 / 跳过原因只在动作状态不是“已完成”时展示。
- 合并动作感受与调整备注，改为按需展开；整体感受和训练备注合并为一个默认展示输入框。

### 3. 对齐 Cycle Template 与 Workout 联动规则

- 切换激活模板时，旧 `cycle_run` 改为 `cancelled`，旧 run 下进行中的 `training_session` 同步取消，已完成训练记录保留。
- 保存运行中的 active 模板时，前端必须弹出确认提示。
- 后端新增 `confirmOverwriteCurrentSession` 校验，未确认时返回 `CYCLE_TEMPLATE_OVERWRITE_CONFIRM_REQUIRED`。
- 用户确认保存 active 模板后，当前 Day 的 `in_progress session` 会刷新为新模板快照，并覆盖当前训练页未完成填写记录。
- `completed` / `cancelled` 训练记录保持不可变。

### 4. 文档与协作规则更新

- 新增 `docs/prd/workout_PRD.md`。
- 新增 `docs/interfaces/workout_接口文档.md`。
- 新增 workout 后端业务流程、数据库改造清单、前端 DDD、页面说明与功能测试顺序建议。
- 更新 `cycle_template` 接口文档，明确 active 模板保存覆盖当前训练日的确认语义。
- 更新 `AGENTS.md` 和 `docs/agent协作规范.md`，明确主控会话后续主动调度 Subagent 的长期规则。
- `.gitignore` 新增 `skill-drafts/`，避免本地 skill 草稿进入仓库。

## 验证结果

- 用户手动功能测试：符合预期。
- 后端聚焦测试：`CycleTemplateIntegrationTest`、`WorkoutIntegrationTest`、`CycleTemplatePolicyServiceTest` 共 30 个测试通过。
- 前端聚焦测试：`CycleTemplateEditPage.test.tsx` 通过。
- 前端生产构建：`pnpm build` 通过。
- `git diff --check`：通过。
- 代码质量审查 Subagent：无高 / 中风险，仅发现的 EOF 空行低风险已修复。

## 今日总结

今天做得正确的地方是：你没有把 workout 简单做成“点开始训练再填日志”的传统流程，而是坚持了 DailyForge 的核心产品逻辑：模板定义计划，workout 记录实际执行，再为后续调整和 AI 总结沉淀数据。尤其是“打卡后页面停留当前 Day，刷新后再进入最新未打卡 Day”这个细节，能明显减少用户完成后的跳转突兀感。

可以继续优化的地方是：workout 模块已经进入真实业务复杂区，后续每次改动都要特别关注“模板快照”和“用户实际记录”的边界。凡是可能覆盖用户填写内容的行为，都应该像这次一样有前端明确提示和后端强制确认字段，不能只依赖 UI 约定。

## 后续建议

- 继续做一轮真实使用路径回归，重点检查移动端输入体验和长训练日的视觉密度。
- 下一阶段可以进入历史统计模块，用 workout 的训练数据和 profile 的身体指标数据做趋势展示。
- AI 分析能力可以先从“已完成 cycle_run 的总结输入数据结构”开始设计，不急着直接接模型。
