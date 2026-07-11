# DailyForge Auth 模块接口文档

## 1. 文档说明

本文档描述 DailyForge 后端 `auth` 模块当前已经落地的接口行为，面向前后端联调、测试编写和后续模块开发。

- 模块归属：`backend` 单体应用
- Controller 内部基础路径：`/auth`
- 对外接口前缀：`/api/auth`
- 统一返回包装：`ApiResponse`
- 当前实现状态：已完成并通过测试

统一成功响应结构：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

统一失败响应结构：

```json
{
  "code": "TOKEN_INVALID",
  "message": "token is invalid",
  "data": null
}
```

## 2. 通用约定

### 2.1 路由约定

所有 Auth 接口统一挂载在：

`/api/auth`

当前已启用全局 `server.servlet.context-path=/api` 配置，Controller 内部基础路径为 `/auth`。
因此外部真实路径统一为文档中写明的 `/api/auth/...`，不会出现 `/api/api/auth/...`。

### 2.2 认证约定

- 匿名可访问：
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh-token`
- 需要 Bearer Token：
  - `GET /api/auth/me`
  - `POST /api/auth/logout`
  - `POST /api/auth/redeem-invite-code`

请求头格式：

```http
Authorization: Bearer <accessToken>
```

### 2.3 Token 约定

- `accessToken`
  - 用于访问受保护接口
  - 当前默认有效期来自 `dailyforge.security.jwt.access-token-ttl`
- `refreshToken`
  - 用于换发新的 token 对
  - 当前默认有效期来自 `dailyforge.security.jwt.refresh-token-ttl`
- 当前版本为无状态 JWT 方案
  - 不使用 Cookie
  - 不使用 Redis token 黑名单
  - `logout` 为服务端空操作，占位保留

### 2.4 角色与权益字段

- `platformRole`
  - `user`
  - `admin`
- `accountTier`
  - `basic`
  - `invited_ai`
  - `premium`

邀请码当前只允许提升 `accountTier`，不允许赋予管理权限。

## 3. 接口列表

| 编号 | 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|------|
| A1 | POST | `/api/auth/register` | 否 | 用户注册，可选直接输入邀请码 |
| A2 | POST | `/api/auth/login` | 否 | 用户登录，返回双令牌 |
| A3 | GET | `/api/auth/me` | 是 | 获取当前登录用户信息 |
| A4 | POST | `/api/auth/refresh-token` | 否 | 使用 refresh token 换发新 token 对 |
| A5 | POST | `/api/auth/logout` | 是 | 退出登录，占位接口 |
| A6 | POST | `/api/auth/redeem-invite-code` | 是 | 登录后兑换邀请码提升权益 |

## 4. 接口详情

### 4.1 注册

- 路径：`POST /api/auth/register`
- 认证：否
- 作用：创建用户账号，初始化 `user_profiles`，可选在注册事务内直接兑换邀请码

请求头：

```http
Content-Type: application/json
```

请求体：

```json
{
  "email": "user@example.com",
  "password": "PlainTextPassword123",
  "confirmPassword": "PlainTextPassword123",
  "userName": "daily_user",
  "inviteCode": "DAILYFORGE-AI-001"
}
```

请求参数说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `email` | `string` | 是 | 注册邮箱，唯一 |
| `password` | `string` | 是 | 明文密码，入库前 BCrypt 加密 |
| `confirmPassword` | `string` | 是 | 确认密码 |
| `userName` | `string` | 是 | 用户名，最大 64 字符 |
| `inviteCode` | `string` | 否 | 注册时可直接输入的邀请码，最大 64 字符 |

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "userName": "daily_user",
    "platformRole": "user",
    "accountTier": "invited_ai",
    "inviteCodeApplied": true
  }
}
```

响应字段说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `data.userId` | `number` | 是 | 新用户 ID |
| `data.email` | `string` | 是 | 注册邮箱 |
| `data.userName` | `string` | 是 | 用户名 |
| `data.platformRole` | `string` | 是 | 平台角色，当前默认 `user` |
| `data.accountTier` | `string` | 是 | 注册完成后的权益层级 |
| `data.inviteCodeApplied` | `boolean` | 是 | 是否成功应用邀请码 |

失败场景：

- `EMAIL_ALREADY_EXISTS`：邮箱已存在，HTTP 409
- `PASSWORD_CONFIRM_MISMATCH`：两次密码不一致，HTTP 400
- `INVITE_CODE_NOT_FOUND`：邀请码不存在，HTTP 404
- `INVITE_CODE_DISABLED`：邀请码已禁用，HTTP 400
- `INVITE_CODE_EXPIRED`：邀请码已过期，HTTP 400
- `INVITE_CODE_EXHAUSTED`：邀请码已用尽，HTTP 400
- `INVITE_CODE_GRANT_CONFLICT`：邀请码权益与当前账户规则冲突，HTTP 400

实现逻辑：

1. 校验请求参数和密码确认关系。
2. 校验邮箱唯一。
3. 如果传入 `inviteCode`，先做可用性校验。
4. 插入 `users`，默认 `platformRole=user`、`accountTier=basic`、`status=active`。
5. 同事务插入 `user_profiles` 初始化记录。
6. 如果传入 `inviteCode`，同事务内完成权益升级、使用记录写入和 `used_count + 1`。
7. 返回注册结果。

### 4.2 登录

- 路径：`POST /api/auth/login`
- 认证：否
- 作用：校验邮箱和密码，返回 `accessToken + refreshToken + user`

请求头：

```http
Content-Type: application/json
```

请求体：

```json
{
  "email": "user@example.com",
  "password": "PlainTextPassword123"
}
```

请求参数说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `email` | `string` | 是 | 登录邮箱 |
| `password` | `string` | 是 | 明文密码 |

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.access",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh",
    "expiresIn": 7200,
    "user": {
      "userId": 1,
      "email": "user@example.com",
      "userName": "daily_user",
      "platformRole": "user",
      "accountTier": "basic"
    }
  }
}
```

响应字段说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `data.accessToken` | `string` | 是 | 访问令牌 |
| `data.refreshToken` | `string` | 是 | 刷新令牌 |
| `data.expiresIn` | `number` | 是 | `accessToken` 过期秒数 |
| `data.user.userId` | `number` | 是 | 用户 ID |
| `data.user.email` | `string` | 是 | 用户邮箱 |
| `data.user.userName` | `string` | 是 | 用户名 |
| `data.user.platformRole` | `string` | 是 | 平台角色 |
| `data.user.accountTier` | `string` | 是 | 当前权益层级 |

失败场景：

- `USER_NOT_FOUND`：账号不存在，HTTP 404
- `INVALID_CREDENTIALS`：密码错误，HTTP 401
- `ACCOUNT_DISABLED`：账号被禁用，HTTP 403

实现逻辑：

1. 根据邮箱查询用户。
2. 校验用户存在且状态为 `active`。
3. 使用 `PasswordEncoder.matches` 校验密码。
4. 更新 `users.last_login_at`。
5. 签发新的 access/refresh token 对。
6. 返回 token 和用户摘要。

### 4.3 获取当前用户

- 路径：`GET /api/auth/me`
- 认证：是
- 作用：返回数据库最新用户身份信息

请求头：

```http
Authorization: Bearer <accessToken>
```

请求体：无

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "userName": "daily_user",
    "platformRole": "user",
    "accountTier": "invited_ai",
    "status": "active"
  }
}
```

响应字段说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `data.userId` | `number` | 是 | 当前用户 ID |
| `data.email` | `string` | 是 | 当前用户邮箱 |
| `data.userName` | `string` | 是 | 当前用户名 |
| `data.platformRole` | `string` | 是 | 当前平台角色 |
| `data.accountTier` | `string` | 是 | 当前权益层级 |
| `data.status` | `string` | 是 | 当前账户状态 |

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `TOKEN_INVALID` / `TOKEN_EXPIRED`：token 无效或过期，HTTP 401
- `USER_NOT_FOUND`：用户不存在，HTTP 404
- `ACCOUNT_DISABLED`：账号被禁用，HTTP 403

实现逻辑：

1. JWT 过滤器解析 access token，并写入 `SecurityContext`。
2. 从认证上下文中读取当前 `userId`。
3. 再次查询数据库中的最新用户记录。
4. 返回最新用户状态，不直接信任 token 内旧快照。

### 4.4 刷新令牌

- 路径：`POST /api/auth/refresh-token`
- 认证：否
- 作用：使用 refresh token 换发新的 token 对，并返回用户摘要

请求头：

```http
Content-Type: application/json
```

请求体：

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh"
}
```

请求参数说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `refreshToken` | `string` | 是 | 刷新令牌 |

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.new-access",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.new-refresh",
    "expiresIn": 7200,
    "user": {
      "userId": 1,
      "email": "user@example.com",
      "userName": "daily_user",
      "platformRole": "user",
      "accountTier": "basic"
    }
  }
}
```

失败场景：

- `INVALID_ARGUMENT`：`refreshToken` 缺失，HTTP 400
- `TOKEN_INVALID`：refresh token 非法，HTTP 401
- `TOKEN_EXPIRED`：refresh token 过期，HTTP 401
- `TOKEN_TYPE_MISMATCH`：传入了 access token，HTTP 401
- `USER_NOT_FOUND`：对应用户不存在，HTTP 404
- `ACCOUNT_DISABLED`：账号被禁用，HTTP 403

实现逻辑：

1. 解析并校验 refresh token。
2. 校验 `typ=refresh`。
3. 根据 token 中的 `userId` 查询用户。
4. 校验用户状态。
5. 重新签发新的 token 对。
6. 返回 token 对和用户摘要。

### 4.5 退出登录

- 路径：`POST /api/auth/logout`
- 认证：是
- 作用：占位接口，第一版仅校验登录态并返回成功

请求头：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

请求体：

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh"
}
```

请求参数说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `refreshToken` | `string` | 否 | 预留字段，当前版本不会做服务端失效处理 |

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": null
}
```

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `TOKEN_INVALID` / `TOKEN_EXPIRED`：token 无效或过期，HTTP 401

实现逻辑：

1. 校验当前请求存在有效 access token。
2. 读取当前用户身份。
3. 记录日志后直接返回成功。
4. 前端负责清理本地 token。

### 4.6 兑换邀请码

- 路径：`POST /api/auth/redeem-invite-code`
- 认证：是
- 作用：当前登录用户兑换邀请码，提升账户权益

请求头：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

请求体：

```json
{
  "code": "DAILYFORGE-AI-001"
}
```

请求参数说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `code` | `string` | 是 | 待兑换的邀请码 |

成功响应：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "userId": 1,
    "accountTier": "invited_ai",
    "inviteCode": "DAILYFORGE-AI-001"
  }
}
```

响应字段说明：

| 参数名 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| `data.userId` | `number` | 是 | 当前用户 ID |
| `data.accountTier` | `string` | 是 | 兑换后的账户权益层级 |
| `data.inviteCode` | `string` | 是 | 本次成功兑换的邀请码 |

失败场景：

- `UNAUTHORIZED`：未登录，HTTP 401
- `TOKEN_INVALID` / `TOKEN_EXPIRED`：token 无效或过期，HTTP 401
- `USER_NOT_FOUND`：用户不存在，HTTP 404
- `ACCOUNT_DISABLED`：用户被禁用，HTTP 403
- `INVITE_CODE_NOT_FOUND`：邀请码不存在，HTTP 404
- `INVITE_CODE_DISABLED`：邀请码禁用，HTTP 400
- `INVITE_CODE_EXPIRED`：邀请码过期，HTTP 400
- `INVITE_CODE_EXHAUSTED`：邀请码用尽，HTTP 400
- `INVITE_CODE_ALREADY_USED`：当前用户已经使用过该邀请码，HTTP 409
- `INVITE_CODE_GRANT_CONFLICT`：权益冲突或非升级型权益，HTTP 400

实现逻辑：

1. 通过登录态获取当前 `userId`。
2. 使用 `select ... for update` 锁定邀请码记录。
3. 校验邀请码状态、过期时间和剩余次数。
4. 校验用户是否已使用过该邀请码。
5. 根据 `grantType=account_tier` 和 `grantValue` 计算目标权益。
6. 更新 `users.account_tier`。
7. 写入 `user_invite_code_usages`。
8. 更新 `invite_codes.used_count`。
9. 返回兑换结果。

## 5. 鉴权与错误响应说明

### 5.1 401 响应

以下情况会返回 401：

- 未携带 access token 访问受保护接口
- token 非法
- token 过期
- token 类型错误

默认响应示例：

```json
{
  "code": "UNAUTHORIZED",
  "message": "authentication is required",
  "data": null
}
```

如果在 JWT 过滤器中已经识别出明确的 token 问题，则直接返回对应错误码，例如 `TOKEN_INVALID`、`TOKEN_EXPIRED`、`TOKEN_TYPE_MISMATCH`。

### 5.2 403 响应

以下情况会返回 403：

- 用户状态不是 `active`
- 已通过认证但没有权限访问受限资源

默认响应示例：

```json
{
  "code": "FORBIDDEN",
  "message": "permission denied",
  "data": null
}
```

## 6. Swagger 与调试约定

- `OpenApiConfig` 只注册 `bearerAuth` 安全方案，不做全局接口强制鉴权。
- 需要鉴权的接口由 Controller 上的 `@SecurityRequirement(name = "bearerAuth")` 单独声明。
- DTO/VO 已补充 `@Schema` 注解，包含字段说明和示例值。
- 关键 debug 日志已落在：
  - `AuthController`
  - `AuthApplicationService`
  - `InviteCodeApplicationService`
  - `JwtTokenService`
  - `JwtAuthenticationFilter`

日志约束：

- 不输出明文密码
- 不输出完整 access/refresh token
- 不输出 JWT secret

## 7. 当前实现备注

- `refresh-token` 接口当前返回 `user` 摘要，前端刷新令牌后无需额外立刻调用 `me` 获取基础身份信息。
- `logout` 当前不做服务端 token 失效控制，后续如果引入 Redis 或会话表，再扩展该接口语义。
- 登录接口当前对“用户不存在”和“密码错误”使用不同错误码：
  - 用户不存在：`USER_NOT_FOUND`
  - 密码错误：`INVALID_CREDENTIALS`
