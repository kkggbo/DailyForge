# DailyForge Frontend Profile 模块落地说明

> 日期：2026-07-12  
> 对应前端目录：`frontend/src/features/profile`

## 1. 本次已实现范围

- `ProfilePage`：个人资料主页面，包含“基础档案”和“身体指标”两个 Tab。
- `ProfileOnboardingPage`：首次登录后的两步引导页。
- `ProfileAiCompletionPage`：AI 场景补录页，支持 `scene` 与 `redirect`。
- `profile.ts`：完整封装 `profile` 模块后端接口。
- `http.ts`：补充 query 参数支持和结构化错误抛出能力。
- `AppShell` / `router`：接入个人资料导航与受保护路由。
- onboarding 本地状态：按用户维度记录是否已完成首次引导。

## 2. 当前路由行为

- `/profile`
  - 个人资料主入口。
- `/profile/onboarding`
  - 首次登录后，从 `/app` 自动跳转进入。
  - 完成或跳过后写入本地标记，再回到 `/app`。
- `/profile/ai-completion`
  - 作为独立补录页使用。
  - 支持：
    - `scene=ai-plan`
    - `scene=ai-nutrition`
    - `scene=ai-summary`
    - `redirect=/some/path`

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

- 登录成功后仍然先进入 `/app`。
- `/app` 会检查当前用户是否已完成 onboarding。
- 未完成时自动跳到 `/profile/onboarding`。
- `BasicProfileForm` 保存成功后刷新：
  - `basicProfile`
  - `completionSummary`
- `BodyMetricForm` 新增成功后刷新：
  - `basicProfile`
  - `completionSummary`
  - `snapshot`
  - `history`
- 删除最新记录成功后也刷新上述四块数据。

## 5. 当前实现选择

- 本次未引入 React Query，继续使用页面级状态。
- `bodyType` 继续按自由文本处理。
- AI 补录页已先落成独立路由，后续 AI 页面可直接跳转复用。
- onboarding 完成状态仅用于前端体验控制，不参与后端业务判定。

## 6. 已验证结果

- `pnpm install --reporter append-only`
- `pnpm build`

构建结果已通过，当前前端可以正常产出生产包。
