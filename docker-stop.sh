#!/bin/bash

# ============================================
# AI Gateway Platform - Docker 停止脚本
# ============================================
# 使用方法：
#   ./docker-stop.sh

echo "========================================="
echo "AI Gateway Platform - 停止 Docker 服务"
echo "========================================="
echo ""

# 停止并移除容器
echo "正在停止服务..."
docker compose down

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 服务已停止"
    echo ""
    echo "如果需要完全清理（包括数据卷）："
    echo "  docker compose down -v"
else
    echo ""
    echo "❌ 停止失败"
    exit 1
fi
