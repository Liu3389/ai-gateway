#!/bin/bash
# CentOS9 虚拟机 Docker 部署脚本
# 在虚拟机上执行: ./docker-deploy.sh
set -e

echo "========================================="
echo "AI Gateway Platform - Docker 部署"
echo "========================================="

# 1. 检查/安装 Docker
if ! command -v docker &> /dev/null; then
    echo "[1/4] 安装 Docker..."
    dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
    systemctl enable --now docker
    echo "✅ Docker 安装完成"
else
    echo "[1/4] Docker 已安装"
    systemctl start docker 2>/dev/null || true
fi

# 2. 导入镜像
echo "[2/4] 导入镜像..."
docker load -i ai-gateway-images.tar
echo "✅ 镜像导入完成"

# 3. 初始化 SQL 目录
mkdir -p sql
# 如果没有 sql 文件，创建默认的
if [ ! -f "sql/schema.sql" ]; then
    echo "⚠️  缺少 sql/schema.sql，请确保已从本机拷贝"
fi

# 4. 启动
echo "[3/4] 启动服务..."
docker compose down 2>/dev/null || true
docker compose up -d

echo "[4/4] 等待就绪..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        echo ""
        echo "========================================="
        echo "✅ 部署成功！"
        echo "  地址: http://$(hostname -I | awk '{print $1}'):8080/api"
        echo "  登录: superadmin / admin123"
        echo "========================================="
        exit 0
    fi
    sleep 3
    echo -n "."
done
echo "❌ 超时，查看日志: docker compose logs app"
exit 1
