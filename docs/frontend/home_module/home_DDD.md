# DailyForge Frontend Home 模块详细设计

> 版本：v1.1  
> 日期：2026-08-23  
> 模块归属：`frontend/src/features/home`

---

## 1. 模块目标

`home` 模块当前只承载一个页面：登录后控制台首页 `HomePage`（`/app`）。

原来的未登录项目首页 `LandingPage` 已删除；未登录用户现在看到的是登录页 `/`（`auth` 模块的 `LoginPage`），产品介绍已并入该登录页。

---

## 2. 模块结构

```text
src/features/home
└─ pages
   └─ HomePage.tsx
```

---

## 3. 已删除：LandingPage

`LandingPage` 已删除，不再作为未登录首页存在。

未登录用户现在看到的首页是登录页 `/`（`auth` 模块的 `LoginPage`），原 `LandingPage` 的产品介绍内容已并入该登录页（含产品介绍 + 三张特性卡）。

因此 `home` 模块不再承担未登录入口职责。

---

## 4. HomePage

对应文件：

- [HomePage.tsx](/D:/Computer%20Science/DailyForge/frontend/src/features/home/pages/HomePage.tsx)

### 4.1 页面目标

登录后的 `/app` 控制台首页，负责：

- 作为登录成功后的跳转落点
- 展示欢迎信息与 AI 解锁入口
- 根据当前训练状态渲染不同的后续行动内容

### 4.2 页面结构

页面顶部为双栏区域：

- 左侧：欢迎卡（`WelcomeCard`），显示「控制台」标签、`你好，{userName}` 问候语与一句引导文案
- 右侧：AI 解锁卡（`AiUnlockCard`），仅当 `currentUser.accountTier` 不是 AI 层级（`invited_ai` / `premium`）时显示，引导用户去 `/invite-code` 兑换邀请码

顶部之下按 `workspace.workspaceState` 三态条件渲染，并在加载与出错时分别显示加载占位 / 错误面板（含重新加载按钮）：

1. `no_active_template`（无激活循环）→ 快速入门卡（`QuickStart`）
2. `active` → 今天训练卡（`TodayCard`）
3. `cycle_completed` → 循环完成卡（`CycleCompleteCard`）

### 4.3 快速入门卡（QuickStart）

4 步引导：

1. 完善个人资料（→ `/profile`）
2. 创建并启用训练模板（两个按钮：手动创建模板 → `/cycle-templates/create`，AI 生成模板 → `/ai-coach/template-generation`）
3. 开始训练打卡（→ `/workout`）
4. 用 AI 教练（→ `/ai-coach/cycle-summary`）

### 4.4 今天训练卡（TodayCard）

展示当前训练日信息：

- 模板名（`workspace.templateName`）+ 轮次（`workspace.runNo`，若存在）
- `Day {currentDayIndex} · {dayName}`，休息日追加「· 休息日」
- 非休息日：显示今日动作列表（`exerciseName`），并提供「进入训练工作台」按钮（→ `/workout`）
- 休息日：显示「今天休息，没有计划动作」，并提供「完成今日打卡」按钮（调用 `completeSession` 以空动作打卡）与「进入训练工作台」按钮
- 打卡成功后显示「今日休息日打卡完成」提示并重新加载工作台

### 4.5 循环完成卡（CycleCompleteCard）

显示「这一轮训练已完成」标题与模板名，说明当前循环已结束，引导用户去 `/workout` 沿用当前模板重新开始、切换模板或进行 AI 周期总结。

### 4.6 数据加载

页面通过 `getWorkspace` 读取工作台状态；当 `workspaceState === "active"` 时再调用 `initializeCurrentDay` 获取今日训练日。若 `initializeCurrentDay` 抛出 `WORKOUT_CYCLE_COMPLETED`，则重新拉取 `getWorkspace` 得到 `cycle_completed` 状态。

---

## 5. 模块与鉴权的关系

`HomePage` 依赖 `useAuth()`，读取：

- `currentUser`：欢迎卡用户名与 AI 层级判断
- `accessToken`：用于调用工作台相关接口（`getWorkspace` / `initializeCurrentDay` / `completeSession`）

因此它与 `app/providers/AuthProvider` 是紧耦合的应用入口层模块。

---

## 6. 后续演进建议

### 6.1 HomePage

`HomePage` 已具备今日训练状态、当前循环模板摘要与 AI 解锁入口，后续可继续演进为更完整的工作台：

1. 最近身体指标
2. 训练完成率统计
3. 更丰富的 AI 建议入口
4. 多循环 / 历史循环切换

---

## 7. 当前已知问题

此前的源码中文乱码问题已解决，当前 `HomePage` 文案均为正常中文，无已知乱码问题。

