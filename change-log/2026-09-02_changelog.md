# 2026-09-02 Changelog

## 今日概览

新增**用户账号管理**：修改用户名（唯一）、修改密码（需旧密码）、密码找回（QQ 邮箱验证码）。同时完成站点合规（ICP 备案号页脚）与品牌 favicon。

## 今日完成内容

### 1. 账号管理（auth_account）

**修改用户名**
- 登录态 `PUT /api/auth/username`：2~20 位、中文/字母/数字/下划线，**唯一**（不区分大小写）。
- 依赖 `V10` 迁移：`users.user_name` 增加唯一索引；并发抢名捕获 `DuplicateKeyException` 转 409。
- 前端「账号设置」页，成功后同步登录态用户名。

**修改密码**
- 登录态 `POST /api/auth/password/change`：需旧密码、新密码 6~18 位、两次一致、新旧不同。
- 本期不强制其它会话失效（JWT 无状态，后续可加 token 版本）。

**密码找回**
- 无需登录两步：`POST /api/auth/password/forgot-code`（6 位验证码存 Redis，TTL 10 分钟，QQ SMTP 发信，60 秒防刷、防枚举）+ `POST /api/auth/password/reset`（验码 5 次作废 → 重置密码）。
- 前端登录页「忘记密码」→ `/forgot-password` 两步流程。
- 新增 `spring-boot-starter-mail`，SMTP 配置走环境变量（`MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD/MAIL_FROM`）。
- OTP 用 `SecureRandom`；新码重置尝试计数；大小写不敏感判重。

**错误码**：新增 `USERNAME_ALREADY_EXISTS`、`PASSWORD_INCORRECT`、`PASSWORD_SAME_AS_OLD`、`FORGOT_CODE_*`、`EMAIL_SEND_FAILED`（移除死码 `USERNAME_INVALID`）。

### 2. 站点合规与品牌

- AppShell 底部新增 **ICP 备案号页脚**（`湘ICP备2026036952号`，链接 beian.miit.gov.cn）。
- 新增品牌 **favicon**（琥珀渐变方块 + 深色 "DF"，浏览器标签显示）。

### 3. 部署

- nginx 启用 **HTTPS**：80→443 跳转 + 443 ssl 监听，`frontend/nginx.conf` + `docker-compose.prod.yml`（443:443 + 证书挂载 `/etc/ssl/cert`）。

## 总结

账号管理（改用户名 / 改密码 / 找回密码）前后端 + 文档（PRD / 接口 / 前后端 DDD）交付，`user_name` 唯一索引 V10 迁移；顺带完成 ICP 备案号页脚、品牌 favicon 与 nginx HTTPS。后端 `mvn test` 186 用例通过，前端 `tsc` 通过（vitest 受本地沙箱限制交由 CI）。
