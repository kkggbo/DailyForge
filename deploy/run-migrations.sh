#!/usr/bin/env bash
# 按顺序把 V1–V7 迁移脚本应用到 dailyforge 库（Flyway 已关闭，需手动执行）。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.prod.yml"
MIGRATIONS_DIR="$REPO_ROOT/backend/src/main/resources/db/migration"
DB_NAME="${MYSQL_DATABASE:-dailyforge}"

if [ -f "$SCRIPT_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$SCRIPT_DIR/.env"
  set +a
fi

ROOT_PW="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD 未设置，请先复制 deploy/.env.example 为 deploy/.env 并填写}"

echo "==> 应用迁移脚本到数据库 '$DB_NAME'（目录 $MIGRATIONS_DIR）"

for f in "$MIGRATIONS_DIR"/V*.sql; do
  name="$(basename "$f")"
  echo "==> applying $name"
  docker compose -f "$COMPOSE_FILE" exec -T mysql \
    mysql --default-character-set=utf8mb4 -uroot -p"$ROOT_PW" "$DB_NAME" < "$f"
done

echo "==> 迁移完成"
