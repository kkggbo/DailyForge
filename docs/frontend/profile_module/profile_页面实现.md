# DailyForge Frontend Profile 页面实现说明

> 版本：v1.1  
> 日期：2026-07-14  
> 模块归属：`frontend/src/features/profile/pages`

---

## 1. 文档目标

本文档从页面实现角度说明 `profile` 模块当前前端落地方式，重点描述：

- 页面结构
- 表单行为
- 请求时机
- 状态刷新链路
- 本轮新增的体验优化

---

## 2. `ProfilePage`

路由：

- `/profile`

### 2.1 页面目标

这是用户日常维护个人资料的主页面。

### 2.2 页面结构

页面由以下区域组成：

1. 标题与说明区
2. 完成度摘要区
3. Tab 切换区
4. 当前 Tab 内容区

### 2.3 标题与说明区

展示：

- 页面标题 `个人资料`
- 当前模块说明文案
- `ProfileTabNav`

### 2.4 完成度摘要区

通过 `CompletionSummaryBanner` 展示：

- 基础档案是否可用于 AI
- 是否已有体重记录
- 当前体重
- 缺失的关键字段

### 2.5 基础档案 Tab

组件：

- `BasicProfileForm`

当前实现规则：

- 生日默认回填为 `2000-01-01`
- 日期使用原生日期控件
- 日期控件采用浅色方案，暗色背景下图标可见
- 身高必须为整数
- 身高输入隐藏浏览器默认增减按钮
- 下拉框统一深色样式

### 2.6 身体指标 Tab

组件：

1. `BodyMetricSummaryCard`
2. `BodyMetricForm`
3. `BodyMetricHistoryList`

当前实现规则：

- 新增记录表单默认回填当前身体指标快照。
- 用户如果只想改体重或少数几个字段，不需要重复抄写上次数据。
- 表单旁提供“清空”按钮，允许从空白状态重新填写。

### 2.7 身体指标录入表单行为

当前实现细节：

- 页面上不展示 `recordDate`。
- 提交时自动使用用户本地当天日期作为 `recordDate`。
- 小数字段步进统一为 `0.1`。
- `bodyAge` 仍然按整数输入。
- 当所有指标字段都为空时，不显示备注栏。
- 一旦任一指标有值，备注栏自动出现。

### 2.8 历史记录区

展示：

- 最近记录分页列表
- `isLatest=true` 的记录显示“最新记录”标签
- 仅最新记录显示删除入口

### 2.9 删除最新记录交互

组件：

- `DeleteLatestMetricDialog`

当前实现规则：

1. 点击“删除最新记录”后先打开确认弹窗。
2. 确认后调用 `DELETE /api/profile/body-metrics/latest`。
3. 成功后关闭弹窗并刷新摘要、快照、历史、基础档案。
4. 失败时：
   - 页面级错误区会显示错误
   - 弹窗内部也会显示错误

当前已处理的业务错误码：

- `BODY_METRIC_LATEST_ALREADY_DELETED`
- `BODY_METRIC_NOT_FOUND`

---

## 3. `ProfileOnboardingPage`

路由：

- `/profile/onboarding`

### 3.1 页面目标

注册成功并自动登录后，首次进入应用的资料引导页。

### 3.2 页面结构

当前为两步：

1. 欢迎说明 + 基础档案
2. 身体指标录入

### 3.3 第一步行为

使用：

- `BasicProfileForm`

当前实现规则：

- 不再提供“下一步，暂不保存”
- 只保留两个动作：
  - `跳过并进入应用`
  - `保存并继续`
- 按钮区域右对齐

### 3.4 第二步行为

使用：

- `BodyMetricForm`

当前实现规则：

- 提供 `上一步`
- 提供 `跳过并进入应用`
- 即使一个指标都不填，也允许点击“保存并完成”直接结束引导

说明：

- 这个“允许空提交”只对 onboarding 第二步生效
- 不会影响普通资料页和 AI 补录页

---

## 4. `ProfileAiCompletionPage`

路由：

- `/profile/ai-completion`

### 4.1 页面目标

在 AI 场景真正需要用户资料时，进行场景化补录。

### 4.2 页面结构

当前也采用两步：

1. 基础档案
2. 身体指标

顶部增加：

- 当前场景说明
- 当前缺失字段列表
- 当前资料是否已满足该场景

### 4.3 页面行为

第一步提供：

- `稍后补充`
- `下一步，先看身体指标`

第二步提供：

- `上一步`
- `稍后补充`

完成后：

- 优先跳转 `redirect`
- 没有 `redirect` 时回 `/profile`

### 4.4 与 onboarding 的差异

- onboarding 第二步允许空提交
- AI 补录页第二步不允许空提交，仍需至少填写一个指标字段

---

## 5. 请求与刷新链路

### 5.1 `ProfilePage` 首屏请求

并行请求：

1. `GET /api/profile/basic`
2. `GET /api/profile/completion-summary`
3. `GET /api/profile/body-metrics/current`
4. `GET /api/profile/body-metrics?page=1&pageSize=20`

### 5.2 保存基础档案后

刷新：

- `basicProfile`
- `completionSummary`

### 5.3 新增身体指标后

刷新：

- `basicProfile`
- `completionSummary`
- `snapshot`
- `history`

### 5.4 删除最新记录后

刷新：

- `basicProfile`
- `completionSummary`
- `snapshot`
- `history`

---

## 6. 页面级体验优化汇总

本轮已落地的体验优化：

### 6.1 注册与首次引导相关

- 注册成功后自动登录，不再跳回登录页让用户手动再登一次。
- 自动登录成功后进入 `/app`，再由现有逻辑接到 `/profile/onboarding`。

### 6.2 基础档案表单

- 下拉框暗色主题可读性修复
- 出生日期默认值 `2000-01-01`
- 日期图标可见性修复
- 身高只允许整数输入

### 6.3 身体指标表单

- `recordDate` 隐藏并自动生成
- 备注栏条件显示
- 小数步进统一为 `0.1`
- 资料页默认回填上次快照值
- 提供“清空”按钮

### 6.4 删除失败反馈

- 删除最新记录失败时，错误提示不再被弹窗遮挡
- 业务错误会直接显示在当前删除弹窗中

---

## 7. 当前实现注意事项

1. `BodyMetricForm` 是三处页面共用组件，但三处行为不完全相同，后续改造要特别注意入口差异。
2. 当前“回填上次指标”的逻辑只在 `/profile` 主资料页启用，不能误带到 onboarding 与 AI 补录页。
3. 删除最新记录的错误处理已经是页面级错误 + 弹窗内错误双通道，后续不要只保留其中一层。
