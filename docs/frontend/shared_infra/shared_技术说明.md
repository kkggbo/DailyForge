# DailyForge Frontend Shared 基础设施说明

> 版本：v1.0  
> 日期：2026-07-12  
> 模块归属：`frontend/src/shared`

---

## 1. 模块目标

`shared` 模块用于承载跨业务模块复用的基础能力。当前仅落地了通用 HTTP 请求层，但这个层是后续所有前端业务模块的共同依赖。

当前文件：

- `api/http.ts`

---

## 2. http.ts 设计说明

### 2.1 作用

`request<T>` 是对浏览器原生 `fetch` 的轻量封装。

当前负责：

- 拼接统一后端前缀 `/api`
- 统一 JSON 请求头
- 统一 Bearer Token 附加方式
- 统一解析后端 `ApiResponse<T>`
- 将错误转换为可展示的 `Error`

### 2.2 入参设计

```ts
type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  accessToken?: string;
};
```

说明：

- `method`：HTTP 方法，默认 `GET`
- `body`：请求体，会自动 JSON 序列化
- `accessToken`：如果存在，会自动写入 `Authorization` 请求头

### 2.3 响应处理策略

当前逻辑：

1. 发起 `fetch("/api" + path)`
2. 如果状态不是 `2xx`
3. 尝试解析错误 JSON
4. 优先抛出后端返回的 `message`
5. 如果解析失败，则回退到 `请求失败: status`
6. 如果是 `204`，返回 `undefined`
7. 其余情况按 `SuccessPayload<T>` 解析，返回 `data`

### 2.4 当前优点

- 足够轻量
- 类型签名简单
- 与后端统一响应模型直接对接
- 模块 API 层不需要重复处理基础逻辑

### 2.5 当前缺点

当前通用层还比较薄：

1. 没有保留后端 `code`
2. 没有超时控制
3. 没有取消请求机制
4. 没有重试能力
5. 没有文件上传能力
6. 没有 query 参数拼接工具

---

## 3. 后续扩展建议

建议后续把 `shared` 基础设施继续扩展为以下几个方向：

### 3.1 API 基础能力

- `request` 升级为支持错误码对象
- 自动注入 trace id
- 支持 token 刷新
- 支持 `FormData`

### 3.2 UI 基础能力

- 通用按钮
- 卡片
- 文本输入框
- 状态提示组件
- 页面空态组件

### 3.3 通用工具

- 日期格式化
- 数字格式化
- 表单字段校验工具
- 本地存储工具

---

## 4. 当前与业务模块的关系

`shared/api/http.ts` 当前被：

- `features/auth/api/auth.ts`

直接依赖。

未来任何业务模块都应该优先复用这个基础层，而不是直接在页面中写裸 `fetch`。

