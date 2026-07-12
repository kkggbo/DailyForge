# DailyForge Frontend Home 模块详细设计

> 版本：v1.0  
> 日期：2026-07-12  
> 模块归属：`frontend/src/features/home`

---

## 1. 模块目标

`home` 模块当前承担两类页面：

- 未登录用户看到的项目首页
- 已登录用户看到的临时控制台首页

这个模块目前仍偏占位性质，但它承担了前端第一版“入口引导”和“联调落点”的职责。

---

## 2. 模块结构

```text
src/features/home
└─ pages
   ├─ LandingPage.tsx
   └─ HomePage.tsx
```

---

## 3. LandingPage

对应文件：

- [LandingPage.tsx](/D:/Computer%20Science/DailyForge/frontend/src/features/home/pages/LandingPage.tsx)

### 3.1 页面目标

当前首页的核心目标不是营销，而是：

- 展示 DailyForge 当前产品定位
- 说明前端第一版已经接通的能力
- 为新用户和已登录用户提供不同入口

### 3.2 页面结构

页面主要由两部分组成：

1. 顶部主视觉区
2. 下方三张功能说明卡片

顶部主视觉区又分左右布局：

- 左侧：品牌标题、价值说明、主 CTA
- 右侧：当前已接通能力列表

### 3.3 动态行为

页面通过 `useAuth()` 读取 `isAuthenticated`，决定主按钮目标：

- 已登录：跳转 `/app`
- 未登录：跳转 `/register`

### 3.4 `pillars` 数据

页面内定义了三个当前产品方向：

- 训练计划
- 训练打卡
- AI 建议

这是当前产品能力的轻量表达，也能作为后续首页继续扩展的基础。

### 3.5 当前定位

当前 `LandingPage` 更像“产品占位首页 + 联调入口页”，不是最终营销首页。

---

## 4. HomePage

对应文件：

- [HomePage.tsx](/D:/Computer%20Science/DailyForge/frontend/src/features/home/pages/HomePage.tsx)

### 4.1 页面目标

登录后的 `/app` 当前主要承担：

- 作为登录成功后的跳转落点
- 显示当前用户基本信息
- 提示下一阶段业务模块的开发方向

### 4.2 页面结构

页面分为两个大区域：

1. 顶部双栏摘要区
2. 下方双栏行动区

顶部双栏摘要区：

- 左侧：欢迎说明和当前前端状态说明
- 右侧：用户摘要卡片区

下方双栏行动区：

- 左侧：后续建议开发模块列表
- 右侧：邀请码升级入口

### 4.3 SummaryCard 组件

页面内部定义了一个局部组件 `SummaryCard`，用于展示：

- 用户 ID
- 邮箱
- 平台角色
- 账户层级
- 状态

当前没有抽成共享组件，是因为还只在一个页面使用。

### 4.4 当前作用边界

当前控制台首页还不是正式业务首页，不承载：

- 训练计划概览
- 今日训练入口
- 身体指标摘要
- 统计图表

它只是用来证明“登录后受保护页面链路可用”。

---

## 5. 模块与鉴权的关系

`home` 模块两个页面都依赖 `useAuth()`：

- `LandingPage` 读取 `isAuthenticated`
- `HomePage` 读取 `currentUser`

因此它与 `app/providers/AuthProvider` 是紧耦合的应用入口层模块。

---

## 6. 后续演进建议

### 6.1 LandingPage

后续可以增强为：

- 更完整的产品首页
- 功能预览区
- MVP 路线图
- 体验申请入口

### 6.2 HomePage

后续建议逐步演进为真实工作台：

1. 今日训练状态
2. 当前循环模板摘要
3. 最近身体指标
4. 训练完成率
5. AI 建议入口

---

## 7. 当前已知问题

当前页面源码中也存在部分中文乱码，需要后续统一清理，否则控制台和首页文案会受影响。

