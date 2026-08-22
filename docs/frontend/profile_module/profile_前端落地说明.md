# DailyForge Frontend Profile 模块落地说明

> 日期：2026-08-23  
> 对应前端目录：`frontend/src/features/profile`

## 1. 本次已实现范围

- `ProfilePage`：个人资料只读总览页（基础档案 + 最新身体指标），顶部提供「更新个人信息」「查看身体指标历史记录」两个入口。
- `ProfileEditPage`：编辑页（基础档案表单 + 身体指标录入）。
- `BodyMetricHistoryPage`：身体指标历史页（分页列表 + 删除最新记录）。
- `ProfileOnboardingPage`：首次登录后的两步引导页。
- `ProfileAiCompletionPage`：AI 场景补录页，支持 `scene` 与 `redirect`。
- `profile.ts`：完整封装 `profile` 模块后端接口。
- `http.ts` 与 `uuid.ts`：query 参数 / 结构化错误 / UUID 生成能力。
- `AppShell` / `router`：接入个人资料导航与受保护路由。
- onboarding 本地状态：按用户维度记录是否已完成首次引导。

> 已移除：`ProfileTabNav`（tab 切换）与 `CompletionSummaryBanner`（资料完成度 banner）。

## 2. 当前路由行为

- `/profile`
  - 个人资料只读总览。
- `/profile/edit`
  - 编辑基础档案 + 新增身体指标记录。
- `/profile/metrics/history`
  - 身体指标历史记录。
- `/profile/onboarding`
  - 首次登录后，从 `/app` 自动跳转进入；完成或跳过后写入本地标记，再回到 `/app`。
- `/profile/ai-completion`
  - 独立补录页，支持 `scene` 与 `redirect`。

## 3. 目录职责

- `api/profile.ts`
  - 负责请求 `/api/profile/*` 接口。
- `types/profile.ts`
  - 维护接口响应、请求体、表单值、场景类型定义。
- `lib/profile-enums.ts`
  - 维护枚举值与中文标签映射、AI 场景文案。
- `lib/profile-formatters.ts`
  - 负责时间、数字、空值显示格式化。
- `lib/profile-mappers.ts`
  - 负责表单初始值转换、缺失字段映射、AI 场景辅助逻辑。
- `lib/onboarding-storage.ts`
  - 负责首次引导完成状态的本地持久化。
- `components/*`
  - 负责表单、摘要卡片、历史列表、删除确认弹层等复用 UI。
- `pages/*`
  - 负责页面级状态编排、接口联动刷新、跳转逻辑。

## 4. 关键交互约定

- 登录成功后仍然先进入 `/app`，未完成 onboarding 时自动跳到 `/profile/onboarding`。
- 总览页只读展示；编辑与历史拆到独立页面。
- `BasicProfileForm`（编辑页）保存成功后刷新 `basicProfile`。
- `BodyMetricForm`（编辑页）新增成功后刷新 `snapshot`。
- 历史页删除最新记录成功后重新加载第一页。

## 5. 当前实现选择

- 本次未引入 React Query，继续使用页面级状态。
- `bodyType` 继续按自由文本处理。
- AI 补录页已先落成独立路由，后续 AI 页面可直接跳转复用。
- onboarding 完成状态仅用于前端体验控制，不参与后端业务判定。

## 6. 已验证结果

- `pnpm install --reporter append-only`
- `pnpm build`

构建结果已通过，当前前端可以正常产出生产包。
