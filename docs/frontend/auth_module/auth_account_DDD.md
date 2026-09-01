# DailyForge Frontend 账号管理 详细设计

> 版本：v0.1
> 日期：2026-09-02
> 模块归属：`frontend/src/features/auth`
> 契约来源：`docs/interfaces/auth_account_接口文档.md`
> 关联 PRD：`docs/prd/auth_account_PRD.md`

---

## 1. 文档目标

本文档定义「账号管理」三项功能在前端的实现边界、类型、页面、交互与接线：**修改用户名**、**修改密码**、**密码找回（忘记密码）**。沿用 `docs/frontend/ai_coach_module/ai_coach_DDD.md` 的结构与风格，作为本轮前端开发与联调的技术基线。

它是 `auth` 功能模块的**增量扩展**：不改动既有注册/登录/退出/兑换邀请码行为，只新增三个账号能力及其入口。

---

## 2. 模块目标

- **已登录用户**可在「账号设置」页修改用户名（唯一、非空、2~20 位、中文/字母/数字/下划线）与修改密码（需旧密码，新密码 6~18 位、两次一致、不与旧密码相同）。
- **未登录用户**可通过登录页「忘记密码」进入独立流程，用邮箱验证码重置密码。
- 修改用户名成功后**同步更新登录态用户名**（`AuthProvider.currentUser` 与本地会话存储）。

---

## 3. 模块定位与职责边界

### 3.1 职责

`auth` 账号管理前端**负责**：

- 账号设置页（`/account`）：用户名表单 + 密码表单，前后端校验与错误提示。
- 忘记密码流程页（`/forgot-password`）：邮箱→验证码→重置 两步流程，发送倒计时。
- 登录页「忘记密码」入口。
- 调用四个新接口并统一错误映射。
- 修改用户名后刷新登录态用户名。

`auth` 账号管理前端**不负责**：

- 邮件发送 / 验证码生成 / Redis 存储（全在后端）。
- 修改邮箱、二次验证、token 失效（PRD §10 本版不含）。
- 数据迁移（V10 唯一索引由数据层角色处理）。

### 3.2 依赖

- `shared/api/http.ts`：统一 `request` 封装。
- `AuthProvider`：提供 `accessToken` / `currentUser` / 用户名更新能力。
- 登录/注册既有页面与 `auth` 的 `api/auth.ts`。

---

## 4. 与现有前端的关系

- `api/auth.ts`：既有注册/登录/me/logout/invite，新增 4 个方法与请求类型（同文件扩展）。
- `AuthProvider`：`AuthContextValue` 新增 `updateUserName` 方法，成功后更新 `currentUser.userName` 与 `session.user.userName`。
- `LoginPage`：表单底部「还没有账号？」旁新增「忘记密码」链接 → `/forgot-password`。
- `ProfilePage`：顶部新增「账号设置」入口链接 → `/account`。
- `router.tsx`：新增 `/account`（受保护）、`/forgot-password`（游客可访问）路由。

---

## 5. 推荐目录结构

账号管理复用 `auth` 既有结构，不新增 feature 目录。类型与请求方法并入 `api/auth.ts`；页面新增 `pages/AccountPage.tsx` 与 `pages/ForgotPasswordPage.tsx`。

```text
src/features/auth
├─ api
│  └─ auth.ts        # + 新增请求/响应类型与方法
├─ lib
│  └─ auth-validation.ts   # + 用户名/密码校验（可选，或并入页面）
├─ pages
│  ├─ AccountPage.tsx       # + 新增
│  ├─ ForgotPasswordPage.tsx# + 新增
│  ├─ LoginPage.tsx         # + 忘记密码链接
│  └─ RegisterPage.tsx
└─ ...
```

说明：校验规则（用户名 2~20 位、字符集；密码 6~18 位、两次一致、新旧不同）抽到 `auth-validation.ts` 供两个页面复用。

---

## 6. 路由设计

`router.tsx` 新增：

```tsx
// 受保护（登录态）
{
  path: "/account",
  element: <AccountPage />
}
// 游客可访问（无需登录）
{
  path: "/forgot-password",
  element: <ForgotPasswordPage />
}
```

- `/forgot-password` 放 `GuestOnlyOutlet` children（与 `/`、`/register` 并列），避免登录态跳转。
- `/account` 放 `ProtectedOutlet` children。

---

## 7. 数据模型设计（`api/auth.ts` 内类型）

### 7.1 修改用户名

```ts
export type UpdateUserNameRequest = {
  userName: string;
};

// 成功响应复用 CurrentUserResponse（更新后的用户）
```

### 7.2 修改密码

```ts
export type ChangePasswordRequest = {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
};
```

### 7.3 发送找回验证码

```ts
export type ForgotPasswordCodeRequest = {
  email: string;
};
```

### 7.4 重置密码

```ts
export type ResetPasswordRequest = {
  email: string;
  code: string;
  newPassword: string;
  confirmPassword: string;
};
```

---

## 8. API 层设计（`api/auth.ts` 新增）

统一使用 `request`，均解析 `ApiResponse<T>`。

| 方法 | 接口 | 鉴权 | 成功响应 |
| --- | --- | --- | --- |
| `updateUserName(accessToken, payload)` | `PUT /api/auth/username` | 是 | `CurrentUserResponse` |
| `changePassword(accessToken, payload)` | `POST /api/auth/password/change` | 是 | `void` |
| `sendForgotPasswordCode(payload)` | `POST /api/auth/password/forgot-code` | 否 | `void` |
| `resetPassword(payload)` | `POST /api/auth/password/reset` | 否 | `void` |

```ts
export function updateUserName(accessToken: string, payload: UpdateUserNameRequest) {
  return request<CurrentUserResponse>("/auth/username", {
    method: "PUT", accessToken, body: payload
  });
}
export function changePassword(accessToken: string, payload: ChangePasswordRequest) {
  return request<void>("/auth/password/change", {
    method: "POST", accessToken, body: payload
  });
}
export function sendForgotPasswordCode(payload: ForgotPasswordCodeRequest) {
  return request<void>("/auth/password/forgot-code", {
    method: "POST", body: payload
  });
}
export function resetPassword(payload: ResetPasswordRequest) {
  return request<void>("/auth/password/reset", {
    method: "POST", body: payload
  });
}
```

---

## 9. 页面数据流

### 9.1 `AccountPage`（/account，登录态）

- 顶部「账号设置」标题 + 返回入口。
- **修改用户名区**：读取 `currentUser.userName` 预填；输入新用户名 → `updateUserName(accessToken, { userName })` → 成功调 `auth.updateUserName`（更新登录态）+ 提示「用户名已更新」。
- **修改密码区**：旧密码 + 新密码 + 确认 → 前端校验（6~18 位 / 两次一致 / 新旧不同）→ `changePassword` → 成功提示「密码已修改」，清空输入。

校验规则：

| 字段 | 规则 |
| --- | --- |
| 用户名 | 非空；2~20 位；仅中文/字母/数字/下划线 |
| 新密码 | 6~18 位 |
| 确认密码 | 与 newPassword 一致 |
| 新旧密码 | newPassword ≠ oldPassword |

### 9.2 `ForgotPasswordPage`（/forgot-password，游客）

两步流程，用 `step: 1 | 2` 控制。

- **步骤 1**：邮箱输入 → `sendForgotPasswordCode({ email })` → 成功进入步骤 2 并启动 60s 倒计时（禁重发，倒计时结束可重发）。
- **步骤 2**：验证码 + 新密码 + 确认 → 前端校验 → `resetPassword({ email, code, newPassword, confirmPassword })` → 成功跳转登录页（`/login`），可带提示。

发送成功提示文案：面向用户，「验证码已发送到邮箱，10 分钟内有效；若未收到可稍后重发」。

> 防枚举语义：接口对不存在邮箱也返回成功，前端不额外判断邮箱是否存在。

### 9.3 `LoginPage` 入口

- 表单下方「还没有账号？」下方/附近新增「忘记密码」`Link to="/forgot-password"`。

---

## 10. 本地状态设计

不引入全局状态库，页面级 `useState`。

- `AccountPage`：
  - 用户名区：`userName`、`isUpdatingName`、`nameError`、`nameSuccess`
  - 密码区：`oldPassword/newPassword/confirmPassword`、`isChangingPassword`、`passwordError`、`passwordSuccess`
- `ForgotPasswordPage`：
  - `step`、`email`、`code`、`newPassword`、`confirmPassword`
  - `isSending`、`sendError`、`countdown`（60s 倒计时秒数，`useEffect` 定时递减）
  - `isResetting`、`resetError`

---

## 11. 前端约束与规则

1. **复用后端错误码映射**：`getAuthErrorMessage`（新增于 `api/auth.ts` 或 `lib/auth-validation.ts`）把 `ApiRequestError` 的 `code` 映射为中文文案：
   - `USERNAME_ALREADY_EXISTS` →「该用户名已被占用」；`USERNAME_INVALID` →「用户名需为 2~20 位中英文、数字或下划线」。
   - `PASSWORD_INCORRECT` →「旧密码不正确」；`PASSWORD_CONFIRM_MISMATCH` →「两次输入的密码不一致」；`PASSWORD_SAME_AS_OLD` →「新密码不能与旧密码相同」。
   - `FORGOT_CODE_INVALID/EXPIRED/ATTEMPTS_EXCEEDED/TOO_FREQUENT` → 对应「验证码错误/过期/尝试次数超限/发送过于频繁」。
   - `EMAIL_SEND_FAILED` →「验证码发送失败，请稍后重试」。
2. **前端也做基础校验**（用户名/密码规则、两次一致、新旧不同），后端仍是最终守门。
3. **修改用户名后同步登录态**：成功后必须通过 `AuthProvider.updateUserName` 更新 `currentUser` 与本地存储，避免下次刷新回退旧名。
4. **防枚举**：忘记密码发送验证码对不存在的邮箱同样显示成功，不泄露账号是否存在。

---

## 12. 错误处理设计

沿用 `ApiRequestError` + `getAuthErrorMessage`。`AccountPage` / `ForgotPasswordPage` 各自的表单区独立持有错误态（用户名错误不污染密码区，反之亦然）。

---

## 13. 入口接入

- `ProfilePage` 顶部新增「账号设置」链接（`/account`）。
- `LoginPage` 新增「忘记密码」链接（`/forgot-password`）。
- `router.tsx` 注册两个路由。

---

## 14. 验收标准

1. `/account` 页可访问；显示当前用户名；修改用户名成功提示并同步登录态；重名（不区分大小写）/非法长度/非法字符被拒并提示。
2. 修改密码：旧密码错误、两次不一致、新旧相同、长度非法均被拒并提示；成功后提示「密码已修改」。
3. `/forgot-password` 流程：步骤 1 输入邮箱发送验证码成功进入步骤 2，60s 倒计时禁重发；步骤 2 输入验证码 + 新密码重置成功跳登录页；验证码错误/过期/超限给出提示。
4. 登录页出现「忘记密码」链接；个人资料页出现「账号设置」入口。
5. 前端 `pnpm test` 通过；契约联调校验通过（与接口文档字段/错误码一致）。

---

## 15. 本轮改动文件清单

### 新增

| 文件 | 说明 |
| --- | --- |
| `frontend/src/features/auth/pages/AccountPage.tsx` | 账号设置页 |
| `frontend/src/features/auth/pages/ForgotPasswordPage.tsx` | 忘记密码流程页 |
| `frontend/src/features/auth/lib/auth-validation.ts` | 用户名/密码校验 + `getAuthErrorMessage` |

### 修改

| 文件 | 改动 |
| --- | --- |
| `frontend/src/features/auth/api/auth.ts` | 新增 4 个请求/响应类型 + 4 个方法 |
| `frontend/src/features/auth/pages/LoginPage.tsx` | 新增「忘记密码」链接 |
| `frontend/src/features/profile/pages/ProfilePage.tsx` | 顶部新增「账号设置」入口 |
| `frontend/src/app/providers/AuthProvider.tsx` | `AuthContextValue` 新增 `updateUserName` 并同步 currentUser/存储 |
| `frontend/src/app/router.tsx` | 注册 `/account`、`/forgot-password` |

### 不改动

- `backend/**`、`db/**`。
- 既有注册/登录/退出/邀请码行为与类型。

---

## 16. 风险与未完成项

| 风险 / 缺口 | 等级 | 说明与对策 |
| --- | --- | --- |
| 密码找回依赖后端邮件/Redis 上线 | 低 | 后端由后端角色实现；前端仅对接接口，邮件发送失败走 `EMAIL_SEND_FAILED` 提示 |
| 修改用户名同步登录态存储 | 低 | 需同时更新 `currentUser` 与本地会话 `session.user.userName`，避免刷新回退 |
| 防枚举与前端感知冲突 | 低 | 前端对所有合法邮箱统一提示「验证码已发送」，不区分账号是否存在 |

---

## 17. 设计结论

`auth` 账号管理是既有认证模块的增量扩展：

1. 四个新接口在 `api/auth.ts` 统一封装，类型严格对齐接口文档。
2. 「账号设置」页承载修改用户名与修改密码，均做前端校验 + 后端错误码映射。
3. 「忘记密码」为两步流程页，含 60s 发送倒计时；登录页提供入口。
4. 修改用户名后通过 `AuthProvider.updateUserName` 同步登录态用户名与本地存储。
5. 个人资料页与登录页分别提供「账号设置」与「忘记密码」入口。

如后续进入实现阶段，本文档可直接作为 `auth` 账号管理前端开发与联调的执行基线。
