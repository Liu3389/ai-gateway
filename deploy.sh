#!/bin/bash

# ============================================
# AI Gateway Platform - 一键部署脚本（Docker）
# ============================================
# 使用方法：
#   部署到本机：  ./deploy.sh
#   导出CentOS9： ./deploy.sh export
# ============================================

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================="
echo "AI Gateway Platform - Docker 一键部署"
echo "========================================="
echo ""

# ========== 检查 Docker ==========
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装${NC}"
    echo "  macOS: brew install --cask docker"
    echo "  Linux: curl -fsSL https://get.docker.com | sh"
    exit 1
fi

DOCKER_VERSION=$(docker --version)
COMPOSE_VERSION=$(docker compose version 2>/dev/null || echo "未安装")
echo -e "${GREEN}✅ $DOCKER_VERSION${NC}"
echo -e "${GREEN}✅ $COMPOSE_VERSION${NC}"

# ========== 模式：导出镜像给 CentOS9 ==========
if [ "$1" = "export" ]; then

    echo ""
    echo -e "${YELLOW}========== 导出 Docker 镜像（移植到 CentOS9）==========${NC}"
    echo ""

    # 先编译
    echo -e "${YELLOW}[1/4] 编译项目...${NC}"
    export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "$JAVA_HOME")
    mvn clean package -DskipTests -q
    echo -e "${GREEN}✅ 编译完成${NC}"

    # 构建 arm64 镜像（Mac Apple Silicon + CentOS9 ARM虚拟机）
    echo -e "${YELLOW}[2/4] 构建 arm64 Docker 镜像...${NC}"
    docker buildx build --platform linux/arm64 -t ai-gateway-platform:latest --load .
    echo -e "${GREEN}✅ 镜像构建完成${NC}"

    # 导出为 tar
    echo -e "${YELLOW}[3/4] 导出镜像文件...${NC}"
    docker save ai-gateway-platform:latest -o ai-gateway-platform.tar
    FILE_SIZE=$(du -h ai-gateway-platform.tar | cut -f1)
    echo -e "${GREEN}✅ 镜像已导出: ai-gateway-platform.tar (${FILE_SIZE})${NC}"

    # 生成 CentOS9 一键导入脚本
    echo -e "${YELLOW}[4/4] 生成 CentOS9 部署脚本...${NC}"
    cat > centos9-deploy.sh << 'CENTOS_SCRIPT'
#!/bin/bash
# ============================================
# AI Gateway Platform - CentOS9 一键部署
# ============================================
# 前置条件：CentOS9已安装Docker，本机已有 ai-gateway-platform.tar
# 使用方式：./centos9-deploy.sh
# ============================================

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================="
echo "AI Gateway Platform - CentOS9 部署"
echo "========================================="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装${NC}"
    echo "CentOS9 安装Docker:"
    echo "  sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo"
    echo "  sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin"
    echo "  sudo systemctl enable --now docker"
    exit 1
fi

# 导入镜像
echo -e "${YELLOW}[1/3] 导入镜像...${NC}"
if [ ! -f "ai-gateway-platform.tar" ]; then
    echo -e "${RED}❌ 未找到 ai-gateway-platform.tar${NC}"
    echo "请确保此文件和 centos9-deploy.sh 在同一目录"
    exit 1
fi
docker load -i ai-gateway-platform.tar
echo -e "${GREEN}✅ 镜像导入完成${NC}"

# 启动服务
echo -e "${YELLOW}[2/3] 启动服务...${NC}"
docker compose up -d
echo -e "${GREEN}✅ 服务已启动${NC}"

# 等待就绪
echo -e "${YELLOW}[3/3] 等待服务就绪...${NC}"
for i in {1..60}; do
    if curl -s http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}=========================================${NC}"
        echo -e "${GREEN}✅ AI Gateway Platform 部署成功！${NC}"
        echo -e "${GREEN}=========================================${NC}"
        echo ""
        echo "  访问地址: http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'YOUR_VM_IP'):8080/api"
        echo "  登录账号: superadmin / admin123"
        echo ""
        echo "  查看日志: docker compose logs -f app"
        echo "  停止服务: docker compose down"
        exit 0
    fi
    echo -n "."
    sleep 3
done

echo -e "${RED}❌ 启动超时，请检查日志: docker compose logs${NC}"
exit 1
CENTOS_SCRIPT

    chmod +x centos9-deploy.sh

    echo ""
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}✅ 导出完成！${NC}"
    echo -e "${GREEN}=========================================${NC}"
    echo ""
    echo "生成的文件："
    echo "  1. ai-gateway-platform.tar  — Docker 镜像（${FILE_SIZE})"
    echo "  2. centos9-deploy.sh         — CentOS9 一键部署脚本"
    echo "  3. docker-compose.yml        — 服务编排配置"
    echo ""
    echo "移植到 CentOS9 虚拟机步骤："
    echo "  1. scp ai-gateway-platform.tar user@centos9:/path/"
    echo "  2. scp centos9-deploy.sh user@centos9:/path/"
    echo "  3. scp docker-compose.yml user@centos9:/path/"
    echo "  4. scp src/main/resources/sql/ user@centos9:/path/src/main/resources/sql/"
    echo "  5. ssh user@centos9 'cd /path && ./centos9-deploy.sh'"
    echo ""
    exit 0
fi

# ========== 检查依赖文件 ==========
echo -e "${YELLOW}步骤 2: 检查配置文件...${NC}"
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}❌ docker-compose.yml 不存在${NC}"
    exit 1
fi
if [ ! -f "Dockerfile" ]; then
    echo -e "${RED}❌ Dockerfile 不存在${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 配置文件存在${NC}"

# ========== 清理旧容器 ==========
echo -e "${YELLOW}步骤 3: 清理旧容器...${NC}"
docker compose down 2>/dev/null || true
echo -e "${GREEN}✅ 清理完成${NC}"

# ========== 构建并启动 ==========
echo -e "${YELLOW}步骤 4: 构建并启动所有服务...${NC}"
echo "首次构建需下载Docker镜像，约3-5分钟..."

docker compose up -d --build

# ========== 等待就绪 ==========
echo -e "${YELLOW}步骤 5: 等待服务就绪...${NC}"
MAX_WAIT=120
WAIT_COUNT=0

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if docker compose ps app 2>/dev/null | grep -q "healthy"; then
        echo ""
        echo -e "${GREEN}=========================================${NC}"
        echo -e "${GREEN}✅ 部署成功！${NC}"
        echo -e "${GREEN}=========================================${NC}"
        echo ""
        echo "  访问地址: http://localhost:8080/api"
        echo "  登录账号: superadmin / admin123"
        echo ""
        echo "  查看日志: docker compose logs -f app"
        echo "  停止服务: docker compose down"
        echo ""
        exit 0
    fi
    WAIT_COUNT=$((WAIT_COUNT + 5))
    echo -n "."
    sleep 5
done

echo ""
echo -e "${RED}❌ 启动超时，查看日志: docker compose logs${NC}"
exit 1
