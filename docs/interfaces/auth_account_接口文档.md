# DailyForge 账号管理 接口文档

> 版本：v0.1
> 日期：2026-09-02
> 状态：待评审
> 关联 PRD：`docs/prd/auth_account_PRD.md`
> 模块名称：`auth`（账号管理）

---

## 1. 文档范围

定义账号管理四个接口：修改用户名、修改密码、发送找回验证码、重置密码。沿用 `auth_接口文档.md` 的通用约定（鉴权、统一响应体 `ApiResponse<T>`）。

---

## 2. 接口列表

| 编号 | 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- | --- |
| AU1 | PUT | `/api/auth/username` | 是 | 修改用户名 |
| AU2 | POST | `/api/auth/password/change` | 是 | 修改密码 |
| AU3 | POST | `/api/auth/password/forgot-code` | 否 | 发送找回验证码 |
| AU4 | POST | `/api/auth/password/reset` | 否 | 校验验证码并重置密码 |

---

## 3. 接口详情

### 3.1 AU1 修改用户名

**请求体** `UpdateUserNameRequest`：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `userName` | string | 是 | 新用户名：2~20 位，中文/字母/数字/下划线 |

**成功响应 200**：`ApiResponse<CurrentUserResponse>`（更新后的用户信息）。

**校验与错误**：
- 未登录 / token 无效 → `UNAUTHORIZED`（401）
- `userName` 为空、长度或字符非法 → `INVALID_ARGUMENT`（400）
- 与其它用户重名（不区分大小写）→ `USERNAME_ALREADY_EXISTS`（409）

**业务逻辑**：
1. 取当前登录用户 ID。
2. 校验 `userName` 规则。
3. 查重：排除自身，是否存在同名用户。
4. 更新 `users.user_name`。
5. 返回最新 `CurrentUserResponse`。

### 3.2 AU2 修改密码

**请求体** `ChangePasswordRequest`：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `oldPassword` | string | 是 | 当前密码 |
| `newPassword` | string | 是 | 新密码：6~18 位 |
| `confirmPassword` | string | 是 | 确认新密码，需与 `newPassword` 一致 |

**成功响应 200**：`ApiResponse<Void>`。

**校验与错误**：
- 未登录 / token 无效 → `UNAUTHORIZED`（401）
- 旧密码错误 → `PASSWORD_INCORRECT`（400）
- 两次密码不一致 → `PASSWORD_CONFIRM_MISMATCH`（400）
- 新密码长度非法 → `INVALID_ARGUMENT`（400）
- 新密码与旧密码相同 → `PASSWORD_SAME_AS_OLD`（400）

**业务逻辑**：
1. 取当前用户，用 `PasswordEncoder` 校验旧密码。
2. 校验新密码规则与两次一致、新 ≠ 旧。
3. 更新 `users.password_hash`。

> 本期不使其它会话失效。

### 3.3 AU3 发送找回验证码

**请求体** `ForgotPasswordCodeRequest`：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `email` | string | 是 | 注册邮箱 |

**成功响应 200**：`ApiResponse<Void>`（**无论邮箱是否存在都返回成功**，防枚举）。

**校验与错误**：
- `email` 格式非法 → `INVALID_ARGUMENT`（400）
- 同邮箱 60 秒内重复发送 → `FORGOT_CODE_TOO_FREQUENT`（400/429）
- 邮件发送失败 → `EMAIL_SEND_FAILED`（500/502）

**业务逻辑**：
1. 校验 email 格式。
2. 限流：同邮箱 60 秒内不重复发送。
3. 生成 6 位数字验证码，写入 Redis `forgot:{email}`（TTL 10 分钟，含尝试计数）。
4. 通过 QQ 邮箱 SMTP 发送邮件（验证码 + 10 分钟有效期）。
5. 邮箱不存在时**不发送也不留痕**，但同样返回成功。

### 3.4 AU4 重置密码

**请求体** `ResetPasswordRequest`：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `email` | string | 是 | 注册邮箱 |
| `code` | string | 是 | 6 位验证码 |
| `newPassword` | string | 是 | 新密码：6~18 位 |
| `confirmPassword` | string | 是 | 确认新密码 |

**成功响应 200**：`ApiResponse<Void>`。

**校验与错误**：
- 验证码错误 → `FORGOT_CODE_INVALID`（400）
- 验证码过期或不存在 → `FORGOT_CODE_EXPIRED`（400）
- 验证码尝试超 5 次 → `FORGOT_CODE_ATTEMPTS_EXCEEDED`（400）
- 两次密码不一致 → `PASSWORD_CONFIRM_MISMATCH`（400）
- 新密码长度非法 → `INVALID_ARGUMENT`（400）

**业务逻辑**：
1. 从 Redis 取验证码，校验（错误计数 +1，超 5 次删除并拒绝）。
2. 校验新密码规则与两次一致。
3. 更新 `users.password_hash`（按 email 查用户）。
4. 删除 Redis 验证码。

---

## 4. 错误码汇总（新增）

| 错误码 | 说明 |
| --- | --- |
| `USERNAME_ALREADY_EXISTS` | 用户名已被占用（409） |
| `PASSWORD_INCORRECT` | 旧密码错误（400） |
| `PASSWORD_SAME_AS_OLD` | 新密码与旧密码相同（400） |
| `FORGOT_CODE_INVALID` | 验证码错误（400） |
| `FORGOT_CODE_EXPIRED` | 验证码过期/不存在（400） |
| `FORGOT_CODE_ATTEMPTS_EXCEEDED` | 验证码尝试超限（400） |
| `FORGOT_CODE_TOO_FREQUENT` | 发送过于频繁（400/429） |
| `EMAIL_SEND_FAILED` | 邮件发送失败（500/502） |

---

## 5. 邮件配置（QQ 邮箱 SMTP）

- 后端新增 `spring-boot-starter-mail`。
- 配置（环境变量注入，不入库）：
  - `MAIL_HOST=smtp.qq.com`
  - `MAIL_PORT=465`（SSL）
  - `MAIL_USERNAME=<发件 QQ 邮箱>`
  - `MAIL_PASSWORD=<QQ 邮箱授权码>`
  - `MAIL_FROM=<发件邮箱>`

---

## 6. 前端调用顺序建议

1. 账号设置页：加载 `/auth/me` 显示当前用户名 → 修改用户名 `PUT /auth/username` → 更新登录态。
2. 账号设置页：修改密码 `POST /auth/password/change`。
3. 登录页「忘记密码」→ `/forgot-password`：
   - 步骤 1：`POST /auth/password/forgot-code`。
   - 步骤 2：`POST /auth/password/reset` → 成功跳登录。
