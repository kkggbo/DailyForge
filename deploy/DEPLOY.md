# DailyForge 生产部署说明（Ubuntu 24.04 / IP + HTTP）

本文档描述如何在阿里云轻量应用服务器（Ubuntu 24.04）上把 DailyForge 部署起来，先用「IP + 端口」访问，供朋友试用。

## 1. 架构概览

```text
浏览器 http://<公网IP>/
        │
        ▼
   nginx (80) ── 静态托管前端 dist/
        │  location /api/ 反代
        ▼
   backend (8080, context-path /api)
        │
        ├──► mysql (内部, 3306)
        └──► redis (内部, 6379)
```

- 只有 `nginx` 暴露 80 端口；`mysql` / `redis` / `backend` 只在 Docker 内部网络，不暴露公网。
- 数据持久化在 Docker 命名卷 `mysql_data` / `redis_data`。
- 后端通过环境变量注入密码 / JWT 密钥 / AI Key，不写死在代码里。

## 2. 前置准备

### 2.1 防火墙 / 安全组

在轻量服务器控制台的防火墙放行：

- `22`（SSH，仅自己访问）
- `80`（HTTP）

**不要**放行 3306 / 6379，MySQL 和 Redis 不对公网开放。

### 2.2 SSH 登录

```bash
ssh root@<公网IP>
```

## 3. 安装 Docker + Docker Compose

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

重新登录一次（或执行 `newgrp docker`）让 docker 组生效，然后验证：

```bash
docker --version
docker compose version
```

> 说明：`deploy.sh` 用到了 `docker compose up --wait`，需要 Compose v2.20+，Ubuntu 24.04 的 `docker-compose-v2` 满足。

## 4. 拉取代码

```bash
git clone https://github.com/kkggbo/DailyForge.git
cd DailyForge
```

## 5. 配置 .env

```bash
cp deploy/.env.example deploy/.env
openssl rand -base64 48   # 生成 JWT 密钥
```

编辑 `deploy/.env`，填入真实值：

| 变量 | 说明 |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `MYSQL_PASSWORD` | 应用账号 `dailyforge` 的密码 |
| `JWT_SECRET` | 用上面 `openssl` 生成的强随机值（≥ 256 bit） |
| `AI_API_KEY` | DeepSeek API Key；暂不用 AI 可保持占位 |

> `deploy/.env` 已被 `.gitignore` 忽略，不会误提交到仓库。

## 6. 一键部署

```bash
chmod +x deploy/*.sh
./deploy/deploy.sh
```

首次构建会下载 Maven / npm 依赖，需要几分钟。

## 7. 分步部署（便于排查）

与 `deploy.sh` 等价：

```bash
# 1) 启动基础设施并等待健康
docker compose -f deploy/docker-compose.prod.yml up -d --wait mysql redis

# 2) 应用 V1–V7 迁移（Flyway 已关闭，需手动执行）
./deploy/run-migrations.sh

# 3) 构建并启动后端 + nginx
docker compose -f deploy/docker-compose.prod.yml up -d --build backend nginx
```

## 8. 验证

```bash
docker compose -f deploy/docker-compose.prod.yml ps
docker compose -f deploy/docker-compose.prod.yml logs -f backend
curl -I http://localhost/
```

然后在浏览器打开 `http://<公网IP>/`。

## 9. 常用运维命令

```bash
# 查看日志
docker compose -f deploy/docker-compose.prod.yml logs -f

# 重启
docker compose -f deploy/docker-compose.prod.yml restart

# 更新代码后重新部署（重新构建镜像）
git pull
./deploy/deploy.sh

# 备份数据库（手动，输出到当前目录）
docker compose -f deploy/docker-compose.prod.yml exec -T mysql \
  mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" dailyforge > backup_$(date +%F).sql
```

## 10. 注意事项

- **首次部署**：空库时 `run-migrations.sh` 会自动建表 + 灌种子数据；已建库后重复执行迁移可能报「表已存在」，属正常（迁移是一次性的）。
- **后续新增迁移**（如 V8）：重新执行 `./deploy/run-migrations.sh` 即可。
- **DB 连接**：`application-prod.yml` 的 DB URL 已包含 `allowPublicKeyRetrieval=true&useSSL=false`，避免 MySQL 8 的 `caching_sha2_password` 连接失败；数据库密码通过 compose 的 `SPRING_DATASOURCE_PASSWORD` 环境变量注入。
- **当前是 HTTP + IP 访问**：上域名 + HTTPS 时需改 `frontend/nginx.conf` 并完成 ICP 备案。
- **安全**：正式对外前，建议配置 SSH 密钥登录、开启服务器基础监控和账单预警。

## 11. 通过 SSH 隧道用 IDEA 连接数据库

MySQL 只绑定了 `127.0.0.1:3306`（不暴露公网）。本地用 IDEA 的 Database 模块通过 SSH 隧道连接：

1. 服务器上更新代码并重建 mysql：

   ```bash
   docker compose -f deploy/docker-compose.prod.yml up -d mysql
   ```

2. IDEA：`Database` 工具窗口 → `+` → `Data Source` → `MySQL`。

3. 填连接参数：
   - Host `127.0.0.1`、Port `3306`、Database `dailyforge`
   - User `dailyforge`（或 `root`）、Password 对应 `.env` 里的 `MYSQL_PASSWORD`（或 `MYSQL_ROOT_PASSWORD`）

4. 切到 `SSH/SSL` 标签页 → 勾选 `Use SSH tunnel`：
   - Host 填服务器公网 IP、Port `22`、User `root`、认证方式选密码或密钥

5. `Test Connection`，通过即可用。

其他工具（DBeaver / Navicat）同理；若工具无内置 SSH 隧道，手动开隧道：

```bash
ssh -N -L 3307:127.0.0.1:3306 root@<服务器IP>
```

然后客户端连 `localhost:3307`。
