# 2026-08-21 Changelog

## 今日概览

本轮完成生产部署资产并上线验证：新增 Docker 生产部署能力（Dockerfile + docker-compose.prod + 迁移脚本 + 一键部署脚本），在阿里云轻量服务器（Ubuntu 24.04）完成部署并通过注册功能验证；同时修复 `application-prod.yml` 的 MySQL 连接参数。

## 今日完成内容

### 1. 生产部署资产

- 后端 `backend/Dockerfile`（Maven 打包 → `eclipse-temurin:21-jre` 运行）与 `.dockerignore`。
- 前端 `frontend/Dockerfile`（`node:20-alpine` 构建 → `nginx:1.27-alpine` 托管）、`.dockerignore` 与 `nginx.conf`（SPA fallback + `/api/` 反代）。
- `deploy/docker-compose.prod.yml`：nginx + backend + mysql + redis 四服务编排，仅 nginx 暴露 80 端口。
- `deploy/.env.example`：数据库密码 / JWT 密钥 / AI Key 环境变量模板。
- `deploy/run-migrations.sh`：按序执行 V1–V7（Flyway 已关闭，需手动迁移）。
- `deploy/deploy.sh`：一键部署脚本（起基础设施 → 等健康 → 跑迁移 → 构建启动）。
- `deploy/DEPLOY.md`：从零到跑通的部署说明（Ubuntu 24.04 + IP 访问）。

### 2. 配置修复

- 修复 `application-prod.yml` 的 DB URL，补 `allowPublicKeyRetrieval=true&useSSL=false`（否则 MySQL 8 的 `caching_sha2_password` 连不上），并移除 `docker-compose.prod.yml` 里对应的 `SPRING_DATASOURCE_URL` 覆盖。

### 3. 上线验证

- 在阿里云轻量服务器（2C4G / 50G，Ubuntu 24.04）完成部署，公网访问注册功能测试通过。

## 注意事项

- 大陆服务器直连 GitHub 与 Docker Hub 均会被重置：GitHub 用 SSH over 443 解决，Docker Hub 用镜像加速源解决（均为服务器侧配置，不进仓库）。

## 总结

本轮打通了「拉仓库 → 一键部署 → 公网访问」的完整链路，项目具备了对朋友开放试用、持续迭代的基础；后续重点是邀请码发放、数据库备份与监控告警。
