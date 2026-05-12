#!/bin/bash

# ============================================
# AI Gateway Platform - 一键部署脚本（Docker）
# ============================================
# 功能说明：
#   1. 检查 Docker 环境
#   2. 构建镜像
#   3. 启动所有服务
#   4. 验证服务状态
# 使用方法：
#   ./deploy.sh
# ============================================

set -e  # 遇到错误立即退出

echo "========================================="
echo "AI Gateway Platform - Docker 一键部署"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ========== 步骤1：检查 Docker 环境 ==========
echo -e "${YELLOW}步骤 1: 检查 Docker 环境...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装${NC}"
    echo "请先安装 Docker："
    echo "  macOS: brew install --cask docker"
    echo "  Linux: curl -fsSL https://get.docker.com | sh"
    echo "  Windows: 下载 Docker Desktop"
    exit 1
fi

if ! command -v docker compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose 未安装${NC}"
    echo "请安装 Docker Compose V2"
    exit 1
fi

DOCKER_VERSION=$(docker --version)
COMPOSE_VERSION=$(docker compose version)
echo -e "${GREEN}✅ $DOCKER_VERSION${NC}"
echo -e "${GREEN}✅ $COMPOSE_VERSION${NC}"
echo ""

# ========== 步骤2：检查配置文件 ==========
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
echo ""

# ========== 步骤3：询问 OpenAI API Key ==========
echo -e "${YELLOW}步骤 3: 配置 OpenAI API Key${NC}"
echo "提示：可以直接按回车使用默认值，稍后在 .env 文件中修改"
read -p "请输入 OpenAI API Key (或按回车跳过): " OPENAI_KEY

if [ -z "$OPENAI_KEY" ]; then
    echo "使用默认占位符，稍后可在 .env 文件中配置"
else
    # 创建或更新 .env 文件
    echo "OPENAI_API_KEY=$OPENAI_KEY" > .env
    echo -e "${GREEN}✅ API Key 已保存到 .env 文件${NC}"
fi
echo ""

# ========== 步骤4：停止旧容器（如果存在）==========
echo -e "${YELLOW}步骤 4: 清理旧容器...${NC}"
docker compose down 2>/dev/null || true
echo -e "${GREEN}✅ 清理完成${NC}"
echo ""

# ========== 步骤5：构建镜像 ==========
echo -e "${YELLOW}步骤 5: 构建 Docker 镜像...${NC}"
echo "这可能需要几分钟时间（首次构建需要下载依赖）..."
docker compose build --no-cache

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 镜像构建失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 镜像构建成功${NC}"
echo ""

# ========== 步骤6：启动服务 ==========
echo -e "${YELLOW}步骤 6: 启动所有服务...${NC}"
docker compose up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 服务启动失败${NC}"
    echo "查看日志: docker compose logs"
    exit 1
fi

echo -e "${GREEN}✅ 服务已启动${NC}"
echo ""

# ========== 步骤7：等待服务就绪 ==========
echo -e "${YELLOW}步骤 7: 等待服务就绪...${NC}"
MAX_WAIT=120
WAIT_COUNT=0

echo "正在检查服务状态..."
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    # 检查应用健康状态
    if docker compose ps app | grep -q "healthy" 2>/dev/null; then
        echo ""
        echo -e "${GREEN}=========================================${NC}"
        echo -e "${GREEN}✅ 所有服务启动成功！${NC}"
        echo -e "${GREEN}=========================================${NC}"
        echo ""
        echo "访问地址: http://localhost:8080/api"
        echo "健康检查: http://localhost:8080/api/actuator/health"
        echo ""
        echo "常用命令："
        echo "  查看日志:     docker compose logs -f app"
        echo "  查看状态:     docker compose ps"
        echo "  停止服务:     docker compose down"
        echo "  重启服务:     docker compose restart"
        echo "  进入容器:     docker exec -it ai-gateway-app sh"
        echo ""
        echo "数据库连接信息："
        echo "  MySQL: localhost:3306 (root/123456)"
        echo "  Redis: localhost:6379 (密码: 123321)"
        echo ""
        exit 0
    fi
    
    WAIT_COUNT=$((WAIT_COUNT + 5))
    echo -n "."
    sleep 5
done

echo ""
echo -e "${RED}❌ 服务启动超时${NC}"
echo ""
echo "查看日志排查问题："
echo "  docker compose logs app"
echo "  docker compose logs mysql"
echo "  docker compose logs redis"
echo ""
exit 1
