# DailyForge Frontend Auth 页面实现说明

> 版本：v1.1  
> 日期：2026-08-23  
> 模块归属：`frontend/src/features/auth/pages`

---

## 1. 文档目标

本文档从页面视角说明当前 `auth` 模块的实现细节，包括：

- 页面布局
- 表单字段
- 本地状态
- 提交行为
- 错误处理
- 页面跳转

---

## 2. LoginPage

对应文件：

- [LoginPage.tsx](/D:/Computer%20Science/DailyForge/frontend/src/features/auth/pages/LoginPage.tsx)

### 2.1 页面目标

登录页现在是未登录用户的首页（即 `/`，`/login` 会重定向到 `/`），既承载产品介绍，也承载登录表单。

### 2.2 页面结构

当前页面分为左右两栏：

1. 左栏：产品介绍区（眉题 `DailyForge` + 主标语 + 副文案 + 三张特性卡：训练计划 / 训练打卡 / AI 建议）
2. 右栏：登录表单区域

### 2.3 本地状态

| 状态 | 类型 | 作用 |
|------|------|------|
| `email` | `string` | 邮箱输入值 |
| `password` | `string` | 密码输入值 |
| `errorMessage` | `string \| null` | 登录失败提示 |
| `isSubmitting` | `boolean` | 提交中状态 |

### 2.4 提交逻辑

1. 阻止原生表单提交
2. 清空旧错误
3. 设置 `isSubmitting=true`
4. 调用 `login({ email, password })`
5. 成功则跳转 `/app`
6. 失败则展示错误文案
7. 最后恢复 `isSubmitting=false`

### 2.5 交互细节

- 提交按钮在提交中会禁用
- 按钮文案会从“登录”切换为“登录中...”
- 页面底部提供到注册页的跳转链接

### 2.6 当前适合作为后续模板的点

这个页面已经形成了一个基础模板，后续其它表单页可以复用其处理方式：

- `useState` 管理表单态
- 提交前清空错误
- 异步提交期间禁用按钮
- catch 中统一展示接口错误

---

## 3. RegisterPage

对应文件：

- [RegisterPage.tsx](/D:/Computer%20Science/DailyForge/frontend/src/features/auth/pages/RegisterPage.tsx)

### 3.1 页面目标

用于创建新账号，并支持在注册时直接输入邀请码。

### 3.2 页面结构

页面同样为双栏布局：

1. 左栏：轻量注册说明（含「邀请码可留空」提示）
2. 右栏：注册表单

### 3.3 表单字段

当前字段包括：

- `email`
- `userName`
- `password`
- `confirmPassword`
- `inviteCode`

### 3.4 本地状态

整个表单用一个 `form` 对象维护：

```ts
{
  email: "",
  userName: "",
  password: "",
  confirmPassword: "",
  inviteCode: ""
}
```

另有：

- `errorMessage`
- `isSubmitting`

### 3.5 updateField 设计

页面内部提供泛型 `updateField`，用于统一更新表单字段。

优点：

- 避免每个字段都单独写一个 setter 包装函数
- 代码更紧凑
- 对新增字段更友好

### 3.6 提交逻辑

1. 阻止原生提交
2. 前端先校验 `password === confirmPassword`
3. 若不一致，则直接提示错误并结束
4. 调用 `register`
5. 成功跳转 `/login`
6. 失败展示错误信息

### 3.7 交互细节

- 邀请码是可选字段
- 两次密码不一致的校验在前端先做
- 注册成功后不自动登录，而是跳回登录页

### 3.8 当前实现的业务意义

这版页面已经把注册期邀请码的产品意图体现出来了：

- 没邀请码也能注册普通账号
- 有邀请码可以在注册当下直接升级账号层级

---

## 4. RedeemInviteCodePage

对应文件：

- [RedeemInviteCodePage.tsx](/D:/Computer%20Science/DailyForge/frontend/src/features/auth/pages/RedeemInviteCodePage.tsx)

### 4.1 页面目标

用于已登录用户在个人使用流程中后补兑换邀请码。

### 4.2 页面结构

当前页面是双栏布局：

1. 左栏：说明邀请码用途，并展示当前账户层级（友好标签 + 层级说明）
2. 右栏：邀请码输入与提交表单

### 4.3 本地状态

| 状态 | 类型 | 作用 |
|------|------|------|
| `code` | `string` | 邀请码输入值 |
| `message` | `string \| null` | 成功提示 |
| `errorMessage` | `string \| null` | 失败提示 |
| `isSubmitting` | `boolean` | 提交中状态 |

### 4.4 提交逻辑

1. 阻止原生提交
2. 清空旧成功提示和错误提示
3. 设置提交中状态
4. 调用 `redeemInviteCode({ code })`（返回新 `accountTier`）
5. 成功后用返回的层级展示「已升级到 X」提示，并清空输入框
6. 失败时展示错误

### 4.5 与全局状态的关系

兑换成功后，`AuthProvider` 会更新 `currentUser.accountTier`。页面用 `account-tier.ts` 的映射把层级显示为中文友好标签（普通用户 / AI 体验版 / 尊享版）。

### 4.6 当前定位

这不是最终的“个人中心权益页”，而是一个用于承接邀请码升级流程的独立页面，后续可以并入 profile / account 模块。

---

## 5. auth 页面统一实现模式

当前三个页面已经形成统一模式：

### 5.1 页面层只处理 UI 和交互

页面负责：

- 字段输入
- 提交事件
- 加载状态
- 成功失败提示
- 页面跳转

不负责：

- token 存储
- 登录态恢复
- 鉴权全局状态管理

### 5.2 业务流程交给 Provider 或模块 API

- 登录、退出、邀请码兑换走 `AuthProvider`
- 注册暂时也是从 Provider 进入

### 5.3 错误展示策略

当前统一做法是：

- catch 异步错误
- 优先显示后端 message
- 如果拿不到 message，则展示兜底文案

---

## 6. 后续优化建议

1. 抽出通用表单卡片组件，减少重复布局代码。
2. 抽出通用输入框组件，统一 label、错误态和 disabled 态。
3. 为登录和注册增加更清晰的字段级校验提示。

