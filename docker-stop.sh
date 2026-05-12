#!/bin/bash
# ============================================
# AI Gateway Platform - Docker 停止脚本
# ============================================
# 用途: 停止 Docker 容器（含可选清理数据卷）
# 使用:
#   ./docker-stop.sh        停止容器
#   ./docker-stop.sh clean  停止并清理数据卷（重置数据库）
# ============================================

echo "========================================="
echo "AI Gateway Platform - 停止 Docker 服务"
echo "========================================="

if [ "$1" = "clean" ]; then
  echo "⚠️  将删除所有数据卷（MySQL/Redis 数据将丢失）"
  read -p "确认? (y/n): " -n 1 -r
  echo
  [[ $REPLY =~ ^[Yy]$ ]] || { echo "取消"; exit 0; }
  docker compose down -v
  echo "✅ 容器已停止，数据卷已清理"
else
  docker compose down
  echo "✅ 容器已停止（数据卷保留）"
  echo "如需清数据: ./docker-stop.sh clean"
fi
