# Backend DDD Template

Use this as a repository-adapted template, not as text to copy verbatim.

Naming convention:

- module API doc: `docs/backend/<module_name>/<module_name>_接口文档.md`
- module DDD doc: `docs/backend/<module_name>/<module_name>_DDD.md`

## Recommended Sections

### 1. 方案概述

- 模块目标
- 一期范围表
- 模块归属
- 核心约束

### 2. 数据库设计

- 本模块涉及的表
- 关键字段说明
- 唯一键 / 外键 / 并发控制点
- 实体关系图或关系说明

### 3. 核心业务规则与状态

- 领域状态枚举
- 生命周期或状态流转
- 关键规则
- 权限和权益规则

### 4. API 设计

- Base Path
- 鉴权要求
- 接口总览表
- 每个接口的核心实现步骤
- 事务边界和失败语义

### 5. 安全设计

- 认证链路
- token / session 策略
- 安全配置变更点
- 当前实现缺口

### 6. 错误码设计

- 模块新增错误码
- 与共享错误码的关系
- HTTP 状态语义

### 7. Java 代码结构设计

- 目标包结构
- 核心类职责
- DTO / VO 清单
- Assembler / Mapping 约定
- Swagger / OpenAPI 注解约定
- debug 日志设计
- Mapper / Repository 设计建议

### 8. 事务、一致性与幂等

- 哪些方法必须事务
- 并发冲突怎么控制
- 唯一键或锁如何参与兜底
- 哪些接口天然幂等

### 9. 配置与扩展点

- 新增配置项
- 外部依赖
- 后续演进方向

### 10. 测试设计

- 单元测试重点
- 集成测试重点
- 安全测试重点

### 11. 实施顺序

- 按依赖关系排序的开发步骤

## Writing Rules

- 优先引用真实路径、真实类名、真实表名、真实字段名。
- 如果某个实现尚不存在，明确写“待新增”或“建议新增”。
- 如果发现接口文档、DDL、现有代码三者不一致，必须在 DDD 中单独指出。
- 如果模块涉及鉴权，不要只写“使用 JWT”，要写 claims、过滤器位置、放行路径和失败语义。
- 如果模块涉及邀请码、库存、名额、状态竞争等资源消耗，必须单独写并发控制策略。
- 如果模块有对外 API，默认补齐 DTO / VO 清单、debug 日志设计、Swagger / OpenAPI 注解约定。
- debug 日志设计必须明确哪些字段允许记录、哪些字段必须脱敏或禁止输出。
- 如果用户提供了参考技术方案文档，优先模仿结构密度和表达风格。
