# DailyForge Frontend Shared 基础设施说明

> 版本：v1.1  
> 日期：2026-08-23  
> 模块归属：`frontend/src/shared`

---

## 1. 模块目标

`shared` 模块用于承载跨业务模块复用的基础能力。当前已落地通用 HTTP 请求层与通用 UUID 工具，二者是后续所有前端业务模块的共同依赖。

当前文件：

- `api/http.ts`
- `lib/uuid.ts`

---

## 2. http.ts 设计说明

### 2.1 作用

`request<T>` 是对浏览器原生 `fetch` 的轻量封装。

当前负责：

- 拼接统一后端前缀 `/api`
- 统一 JSON 请求头
- 统一 Bearer Token 附加方式
- 统一解析后端 `ApiResponse<T>`
- 支持 query 参数自动拼接
- 将错误转换为带 `code` / `status` 的 `ApiRequestError`

### 2.2 入参设计

```ts
type QueryParams = Record<
  string,
  string | number | boolean | null | undefined | Array<string | number | boolean>
>;

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  accessToken?: string;
  query?: QueryParams;
};
```

说明：

- `method`：HTTP 方法，默认 `GET`
- `body`：请求体，会自动 JSON 序列化
- `accessToken`：如果存在，会自动写入 `Authorization` 请求头
- `query`：查询参数，自动拼接到 URL；`null` / `undefined` 会被跳过，数组会重复追加

### 2.3 响应处理策略

当前逻辑：

1. 通过 `buildApiUrl(path, query)` 拼接 `/api` 前缀与查询参数
2. 发起 `fetch`
3. 如果状态不是 `2xx`
4. 尝试解析错误 JSON
5. 抛出 `ApiRequestError`，优先带后端返回的 `message`，并保留 `code` 与 `status`
6. 如果解析失败，则回退到 `请求失败: status`
7. 如果是 `204`，返回 `undefined`
8. 其余情况按 `SuccessPayload<T>` 解析，返回 `data`

### 2.4 结构化错误

非 `2xx` 时会抛出 `ApiRequestError`（继承自 `Error`），并保留：

- `code`：后端返回的错误码（可选）
- `status`：HTTP 状态码

业务模块可直接通过 `error.code` 做可控分支，例如 `profile` 删除记录时区分 `BODY_METRIC_NOT_FOUND` 与 `BODY_METRIC_LATEST_ALREADY_DELETED`。

### 2.5 当前优点

- 足够轻量
- 类型签名简单
- 与后端统一响应模型直接对接
- 模块 API 层不需要重复处理基础逻辑
- 支持 query 参数拼接，满足分页等常见场景
- 结构化错误保留 `code`，便于页面做业务分支

### 2.6 当前缺点

当前通用层还比较薄：

1. 没有超时控制
2. 没有取消请求机制
3. 没有重试能力
4. 没有文件上传能力

---

## 3. uuid.ts 设计说明

### 3.1 作用

`generateUuid()` 用于生成 UUID v4，当前主要供 AI 任务请求的 `clientRequestId` 使用。

### 3.2 非安全上下文兼容

`crypto.randomUUID()` 仅在安全上下文（HTTPS / localhost）可用；通过纯 HTTP 或 IP 访问时它是 `undefined`。

`generateUuid` 的策略：

1. 优先使用 `crypto.randomUUID()`
2. 不可用时回退到 `crypto.getRandomValues()` 手动构造 UUID v4

这样无论访问方式如何，都能稳定生成合法 UUID，避免纯 HTTP 访问下抛出 `crypto.randomUUID is not a function`。

---

## 4. 后续扩展建议

建议后续把 `shared` 基础设施继续扩展为以下几个方向：

### 4.1 API 基础能力

- 自动注入 trace id
- 支持 token 刷新
- 支持 `FormData` 文件上传
- 支持超时控制与请求取消

### 4.2 UI 基础能力

- 通用按钮
- 卡片
- 文本输入框
- 状态提示组件
- 页面空态组件

### 4.3 通用工具

- 日期格式化
- 数字格式化
- 表单字段校验工具
- 本地存储工具

---

## 5. 当前与业务模块的关系

`shared` 当前被以下业务模块复用：

- `features/auth/api/auth.ts` —— 使用 `request`
- `features/profile/api/profile.ts` —— 使用 `request`
- `features/ai-coach` 的 `TemplateGenerationPage` / `CycleSummaryPage` —— 使用 `generateUuid` 生成任务 `clientRequestId`

未来任何业务模块都应该优先复用这个基础层，而不是直接在页面中写裸 `fetch` 或自行实现 UUID。

