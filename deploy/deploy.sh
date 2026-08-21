#!/usr/bin/env bash
# 一键部署：启动 MySQL/Redis -> 等健康 -> 跑迁移 -> 构建并启动 backend/nginx。
# 需要 Docker Compose v2.20+（Ubuntu 24.04 的 docker-compose-v2 满足）。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE="docker compose -f $SCRIPT_DIR/docker-compose.prod.yml"

if [ ! -f "$SCRIPT_DIR/.env" ]; then
  echo "缺少 $SCRIPT_DIR/.env，请先执行：cp $SCRIPT_DIR/.env.example $SCRIPT_DIR/.env 并填写" >&2
  exit 1
fi

echo "==> [1/4] 启动 MySQL & Redis（等待健康）"
$COMPOSE up -d --wait mysql redis

echo "==> [2/4] 应用数据库迁移"
"$SCRIPT_DIR/run-migrations.sh"

echo "==> [3/4] 构建并启动 backend + nginx"
$COMPOSE up -d --build backend nginx

echo ""
echo "==> 部署完成，浏览器访问 http://<服务器公网IP>/ 即可。"
