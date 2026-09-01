# DailyForge 账号管理模块详细设计文档（DDD）
> 版本：v0.1 | 日期：2026-09-02 | 模块归属：`backend` 单体应用 `com.dailyforge.modules.auth`

---

## 一、方案概述

### 1.1 模块目标

在现有 `auth` 模块（注册/登录/JWT/邀请码）基础上，补齐**账号管理**三项能力：

1. **修改用户名**：登录态下修改，唯一、不与他人重复（不区分大小写）。
2. **修改密码**：登录态下修改，需验证旧密码。
3. **密码找回（忘记密码）**：未登录，通过邮箱验证码重置密码。

本文档沿用 `auth_DDD.md` 的结构与风格，仅覆盖新增部分。

### 1.2 本期交付范围

| 序号 | 功能 | 说明 | 状态 |
|------|------|------|:---:|
| AU1 | 修改用户名 | `PUT /api/auth/username`，返回最新 `CurrentUserResponse` | 待开发 |
| AU2 | 修改密码 | `POST /api/auth/password/change` | 待开发 |
| AU3 | 发送找回验证码 | `POST /api/auth/password/forgot-code`（防枚举、60s 限流） | 待开发 |
| AU4 | 重置密码 | `POST /api/auth/password/reset`（验证码校验、错误计数） | 待开发 |
| M1 | 邮件服务 | QQ 邮箱 SMTP 发送验证码邮件 | 待开发 |
| D1 | V10 唯一索引 | `users.user_name` 加唯一索引 | 待开发 |

### 1.3 核心约束

- **修改用户名**：非空；长度 2~20；字符仅中文/字母/数字/下划线；唯一（不区分大小写，依赖 `utf8mb4_unicode_ci` + 唯一索引）。
- **修改密码**：需验旧密码；新密码 6~18 位；两次一致；新 ≠ 旧；本期不使其它会话失效（JWT 无状态）。
- **找回密码**：
  - 防枚举：邮箱不存在也返回成功，但不发邮件、不留验证码。
  - 同邮箱 60 秒内不重复发送。
  - 验证码错误最多 5 次，超限删除作废。
  - 验证码 10 分钟过期。
  - 发送失败 → `EMAIL_SEND_FAILED`，不落验证码。
- **验证码存储**：Redis，key=`forgot:{email}`，值含验证码与尝试计数，TTL 10 分钟。

### 1.4 与现有基础设施的关系

复用：

- `ApiResponse` / `ErrorCode` / `BusinessException` / `GlobalExceptionHandler`
- `SecurityConfig`（放行 `/auth/**`）
- `AuthSecurityUtils.getCurrentUserId()`
- `PasswordEncoder`（BCrypt）
- `PasswordPolicyService`
- `AuthAssembler`
- `spring-boot-starter-data-redis`（`StringRedisTemplate`）
- `application.yml` / `application-test.yml`

新增：

- `spring-boot-starter-mail`（`JavaMailSender`）
- `AccountManagementService`（或扩展 `AuthApplicationService`）
- `EmailSendService`
- 邮件配置占位（环境变量 `MAIL_*`）

---

## 二、关键决策

### 2.1 服务拆分决策

账号管理逻辑较独立，建议**新增 `AccountManagementService`**（独立应用服务），避免继续膨胀 `AuthApplicationService`；Controller 层仍走 `AuthController`（保持 `/auth/**` 路由集中）。

### 2.2 用户名唯一性决策

- 判重**不区分大小写**，通过 `selectByUserName` 查询（SQL 依赖 `utf8mb4_unicode_ci` 排序规则实现大小写不敏感），并在服务层排除自身。
- 兜底：V10 唯一索引 + 捕获 `DuplicateKeyException` 映射 `USERNAME_ALREADY_EXISTS`（并发写入保护）。

### 2.3 验证码存储决策

- 用 `StringRedisTemplate` 存 `forgot:{email}` → JSON 字符串 `{ code, attempts }`，TTL 10 分钟。
- 60s 限流：额外用 `forgot:{email}:cooldown` key（TTL 60s）或在主 key 存 `lastSentAt`。采用独立 cooldown key 更简单，不影响主验证码 TTL。

### 2.4 邮件发送决策

- 邮箱不存在时：**不调用邮件服务、不写 Redis**，但返回成功（防枚举）。
- 邮箱存在时：先写 Redis 验证码 + cooldown，再发邮件；发送失败 → `EMAIL_SEND_FAILED`（回滚删除刚写入的验证码与 cooldown，保证不留痕）。

---

## 三、数据设计

### 3.1 使用表

| 表名 | 用途 |
|------|------|
| `users` | 账户主表（`user_name` 唯一、`password_hash` 更新） |

无新表。`user_name` 增加唯一索引（V10）。

### 3.2 V10 迁移

```sql
ALTER TABLE users ADD UNIQUE KEY uk_users_user_name (user_name);
```

> ⚠️ 上线前需确认现有 `users.user_name` 无重复，否则唯一索引创建失败。

### 3.3 Redis key 设计

| key | 值 | TTL | 用途 |
|------|------|------|------|
| `forgot:{email}` | `{ "code": "123456", "attempts": 0 }` | 10 分钟 | 验证码与尝试计数 |
| `forgot:{email}:cooldown` | `"1"` | 60 秒 | 发送限流 |

---

## 四、领域规则

### 4.1 用户名规则

`validateUserName(userName)`：

- 非空。
- 长度 2~20。
- 匹配 `^[\u4e00-\u9fa5a-zA-Z0-9_]+$`。

对应错误码：`USERNAME_INVALID`。

### 4.2 新密码规则

`PasswordPolicyService` 补充 `validateNewPassword(newPassword)`：

- 非空。
- 长度 6~18。

对应错误码：`INVALID_ARGUMENT`（长度非法）。

### 4.3 找回验证码规则

`ForgotCodeDomainService`（或内嵌于服务）：

- 60s 内重发 → `FORGOT_CODE_TOO_FREQUENT`。
- 验证码错误（累加 attempts）：
  - attempts < 5 → `FORGOT_CODE_INVALID`。
  - attempts >= 5 → 删除 key，`FORGOT_CODE_ATTEMPTS_EXCEEDED`。
- 验证码不存在/过期 → `FORGOT_CODE_EXPIRED`。

---

## 五、安全设计

### 5.1 鉴权

- AU1 / AU2 需登录：`@SecurityRequirement(name = "bearerAuth")`，服务层 `AuthSecurityUtils.getCurrentUserId()`。
- AU3 / AU4 匿名（`/auth/**` 已放行）。

### 5.2 防枚举

AU3 对不存在的邮箱返回成功，不暴露邮箱是否存在。

### 5.3 日志脱敏

- 禁止输出：明文密码、验证码、邮箱完整值（可掩码）、QQ 授权码。

---

## 六、接口设计

### 6.1 接口总览

| 编号 | 方法 | 路径 | 需要 access token | 请求体 | 说明 |
|------|------|------|:---:|:---:|------|
| AU1 | PUT | `/api/auth/username` | 是 | `UpdateUserNameRequest` | 修改用户名 |
| AU2 | POST | `/api/auth/password/change` | 是 | `ChangePasswordRequest` | 修改密码 |
| AU3 | POST | `/api/auth/password/forgot-code` | 否 | `ForgotPasswordCodeRequest` | 发送找回验证码 |
| AU4 | POST | `/api/auth/password/reset` | 否 | `ResetPasswordRequest` | 校验验证码并重置 |

### 6.2 AU1 修改用户名

1. 取当前用户 ID。
2. `validateUserName`。
3. `selectByUserName` 查重（排除自身）→ 重名抛 `USERNAME_ALREADY_EXISTS`。
4. 更新 `users.user_name`。
5. 返回 `CurrentUserResponse`。

### 6.3 AU2 修改密码

1. 取当前用户，`passwordEncoder.matches(oldPassword, hash)` → 不匹配抛 `PASSWORD_INCORRECT`。
2. `validateNewPassword`；两次一致（`PASSWORD_CONFIRM_MISMATCH`）。
3. 新密码与旧密码明文相同 → `PASSWORD_SAME_AS_OLD`。
4. 更新 `password_hash`。

### 6.4 AU3 发送找回验证码

1. 校验 email 格式（`INVALID_ARGUMENT`）。
2. 查用户：不存在 → 直接返回成功（防枚举）。
3. cooldown key 存在 → `FORGOT_CODE_TOO_FREQUENT`。
4. 生成 6 位数字码，写 `forgot:{email}`（attempts=0）+ cooldown。
5. 发送邮件；失败 → 删除刚写入的 key，抛 `EMAIL_SEND_FAILED`。

### 6.5 AU4 重置密码

1. 取 `forgot:{email}`：不存在 → `FORGOT_CODE_EXPIRED`。
2. 比对验证码：
   - 错：attempts+1；>=5 → 删除，`FORGOT_CODE_ATTEMPTS_EXCEEDED`；否则 `FORGOT_CODE_INVALID`。
   - 对：继续。
3. `validateNewPassword` + 两次一致。
4. 按 email 查用户更新 `password_hash`。
5. 删除验证码与 cooldown。

---

## 七、代码结构设计

### 7.1 新增文件

```text
com.dailyforge.modules.auth
├── application
│   └── service
│       ├── AccountManagementService.java        # 账号管理编排（AU1-AU4）
│       └── EmailSendService.java                # 邮件发送封装
├── domain
│   └── service
│       ├── PasswordPolicyService.java           # 修改：补 validateNewPassword
│       └── (UsernamePolicyService / ForgotCodeValidator 可内嵌)
├── infrastructure
│   └── persistence
│       └── mapper
│           └── UserMapper.java                  # 修改：增 selectByUserName
└── interfaces
    ├── dto
    │   ├── UpdateUserNameRequest.java
    │   ├── ChangePasswordRequest.java
    │   ├── ForgotPasswordCodeRequest.java
    │   └── ResetPasswordRequest.java
    └── rest
        └── AuthController.java                  # 修改：增 AU1-AU4
```

### 7.2 类职责

| 类名 | 职责 |
|------|------|
| `AccountManagementService` | 编排修改用户名/密码、找回密码流程，注入 `UserMapper`/`PasswordEncoder`/`PasswordPolicyService`/`StringRedisTemplate`/`EmailSendService`/`AuthAssembler` |
| `EmailSendService` | 封装 `JavaMailSender` 发送验证码邮件 |
| `PasswordPolicyService` | 现有确认规则 + 新密码长度规则 |

### 7.3 Mapper 约定

`UserMapper` 新增：

```java
@Select("SELECT * FROM users WHERE user_name = #{userName} LIMIT 1")
UserEntity selectByUserName(String userName);
```

---

## 八、DTO / VO 清单

### 8.1 新增 Request DTO

| 类名 | 字段 |
|------|------|
| `UpdateUserNameRequest` | `userName` |
| `ChangePasswordRequest` | `oldPassword`、`newPassword`、`confirmPassword` |
| `ForgotPasswordCodeRequest` | `email` |
| `ResetPasswordRequest` | `email`、`code`、`newPassword`、`confirmPassword` |

### 8.2 响应 VO

- AU1 返回现有 `CurrentUserResponse`。
- AU2/AU3/AU4 返回 `ApiResponse<Void>`。

---

## 九、错误码设计

| 错误码 | HTTP 状态 | 含义 |
|------|------|------|
| `USERNAME_ALREADY_EXISTS` | 409 | 用户名已被占用 |
| `USERNAME_INVALID` | 400 | 用户名长度/字符非法 |
| `PASSWORD_INCORRECT` | 400 | 旧密码错误 |
| `PASSWORD_SAME_AS_OLD` | 400 | 新密码与旧密码相同 |
| `FORGOT_CODE_INVALID` | 400 | 验证码错误 |
| `FORGOT_CODE_EXPIRED` | 400 | 验证码过期/不存在 |
| `FORGOT_CODE_ATTEMPTS_EXCEEDED` | 400 | 验证码尝试超限 |
| `FORGOT_CODE_TOO_FREQUENT` | 400 | 发送过于频繁 |
| `EMAIL_SEND_FAILED` | 500 | 邮件发送失败 |

复用现有：`UNAUTHORIZED`、`INVALID_ARGUMENT`、`PASSWORD_CONFIRM_MISMATCH`。

---

## 十、邮件配置

### 10.1 pom

新增 `spring-boot-starter-mail`。

### 10.2 配置（application.yml，环境变量注入）

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.qq.com}
    port: ${MAIL_PORT:465}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.ssl.enable: true
    from: ${MAIL_FROM:}
```

> 本地未配置时发送失败返回 `EMAIL_SEND_FAILED`；测试用 mock `JavaMailSender`。

---

## 十一、事务与并发

### 11.1 事务边界

| 方法 | 是否事务 | 说明 |
|------|------|------|
| `updateUserName` | 否/是 | 单表更新；查重 + 更新建议同事务，捕获唯一索引冲突 |
| `changePassword` | 否 | 单表更新 |
| `resetPassword` | 否 | 校验码 + 单表更新 |

### 11.2 并发控制

- 用户名并发注册：唯一索引兜底，捕获 `DuplicateKeyException` → `USERNAME_ALREADY_EXISTS`。
- 验证码重置并发：Redis 操作非事务，但验证码比对 + 删除在单请求内完成；attempts 计数用 Redis 自增 + 判断。

---

## 十二、测试设计

### 12.1 单元测试

- `AccountManagementServiceTest`（mock mapper/encoder/redis/mail）：
  - 用户名：规则非法、重名（排除自身）、成功。
  - 改密：旧密码错、两次不一致、新旧相同、成功。
  - 找回：发送防枚举（不存在邮箱成功）、60s 限流、重置成功、错码 5 次作废、过期。
- `PasswordPolicyServiceTest`：补新密码长度规则。

### 12.2 集成测试

- AU1/AU2/AU3/AU4 的 HTTP 全链路（H2 + mock mail + mock redis）。

---

## 十三、验收标准

1. 修改用户名：成功更新；重名（不区分大小写）被拒；长度/字符非法被拒；成功后登录态同步。
2. 修改密码：旧密码错/两次不一致/新旧相同被拒；成功后新密码可登录、旧密码不可。
3. 找回：合法邮箱返回成功；不存在邮箱同样成功；60s 内重发被拒；错码 5 次作废；过期被拒。
4. 邮件：能通过 QQ SMTP 收到验证码。
5. `mvn test` 通过；契约联调校验通过。
