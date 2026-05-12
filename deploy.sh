#!/bin/bash
# ============================================
# AI Gateway Platform - Docker 一键部署脚本
# ============================================
# 适配环境:
#   macOS (Apple Silicon / Intel)
#   Linux   (Ubuntu / CentOS / Debian)
#
# 使用方法:
#   ./deploy.sh          本机构建+启动（DockerHub 被墙时自动用 DaoCloud 镜像）
#   ./deploy.sh export   本机构建 arm64 镜像并导出tar + 生成VM部署包
#   ./deploy.sh build    仅编译 jar 包（不涉及Docker）
# ============================================
set -e

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

echo "========================================="
echo "AI Gateway Platform - Docker 部署"
echo "========================================="

# ======================== 检测平台 ========================
ARCH=$(uname -m)
OS=$(uname -s)
case "$ARCH" in
  arm64|aarch64) DOCKER_PLATFORM="linux/arm64" ;;
  x86_64|amd64)  DOCKER_PLATFORM="linux/amd64" ;;
  *) echo -e "${RED}不支持的架构: $ARCH${NC}"; exit 1 ;;
esac
echo "平台: $OS / $ARCH → Docker $DOCKER_PLATFORM"

# ======================== 检查 Java + Maven ========================
JDK=""
for candidate in "$JAVA_HOME" \
  "/Users/a1234/Library/Java/JavaVirtualMachines/corretto-21.0.10/Contents/Home" \
  "/usr/lib/jvm/java-21-openjdk" \
  "/usr/lib/jvm/java-17-openjdk"; do
  [ -f "$candidate/bin/java" ] && { JDK="$candidate"; break; }
done
[ -z "$JDK" ] && { echo -e "${RED}未找到 JDK 21+，请设置 JAVA_HOME${NC}"; exit 1; }
export JAVA_HOME="$JDK"
export PATH="$JAVA_HOME/bin:$PATH"

if ! command -v mvn &>/dev/null; then
  echo -e "${RED}❌ Maven 未安装，请安装后重试${NC}"; exit 1
fi

# ======================== 编译 ========================
echo -e "${YELLOW}[1/5] 编译项目...${NC}"
mvn clean package -DskipTests -q
echo -e "${GREEN}✅ 编译完成${NC}"

# ======================== 模式: 仅编译 ========================
if [ "$1" = "build" ]; then
  echo -e "${GREEN}✅ jar 位于: target/ai-gateway-platform-1.0.0.jar${NC}"
  exit 0
fi

# ======================== 检查 Docker ========================
if ! command -v docker &>/dev/null; then
  echo -e "${RED}❌ Docker 未安装${NC}"
  echo "  macOS: brew install --cask docker"
  echo "  Linux: curl -fsSL https://get.docker.com | sh"
  exit 1
fi

# ======================== 拉取基础镜像（DaoCloud 国内加速）=====================
IMAGE_MYSQL="mysql:8.0"
IMAGE_REDIS="redis:7-alpine"
IMAGE_JRE="eclipse-temurin:21-jre-alpine"

pull_with_fallback() {
  local name="$1" dao="$2"
  if docker pull "$name" 2>/dev/null | grep -q "Downloaded"; then
    echo -e "${GREEN}✅ $name (Docker Hub)${NC}"
  else
    echo -e "${YELLOW}Docker Hub 不可达，切换 DaoCloud 镜像...${NC}"
    docker pull "$dao" && docker tag "$dao" "$name" && echo -e "${GREEN}✅ $name (DaoCloud)${NC}"
  fi
}

echo -e "${YELLOW}[2/5] 拉取基础镜像...${NC}"
pull_with_fallback "$IMAGE_JRE"   "docker.m.daocloud.io/library/eclipse-temurin:21-jre-alpine"
pull_with_fallback "$IMAGE_MYSQL" "docker.m.daocloud.io/library/mysql:8.0"
pull_with_fallback "$IMAGE_REDIS" "docker.m.daocloud.io/library/redis:7-alpine"

# ======================== 构建应用镜像 ========================
echo -e "${YELLOW}[3/5] 构建应用镜像 ($DOCKER_PLATFORM)...${NC}"
docker build --platform "$DOCKER_PLATFORM" -f docker/Dockerfile -t ai-gateway-platform:latest .
echo -e "${GREEN}✅ 应用镜像构建完成${NC}"

# ======================== 导出 tar（本机和 export 都需要）=======================
echo -e "${YELLOW}[4/5] 导出镜像包...${NC}"
docker save ai-gateway-platform:latest "$IMAGE_MYSQL" "$IMAGE_REDIS" -o ai-gateway-images.tar
SIZE=$(du -h ai-gateway-images.tar | cut -f1)
echo -e "${GREEN}✅ ai-gateway-images.tar (${SIZE})${NC}"

# ======================== 生成 VM 部署脚本（通用）=======================
cat > docker-deploy.sh << 'DEPLOY'
#!/bin/bash
set -e
echo "========================================="
echo "AI Gateway Platform - Docker 部署"
echo "========================================="

# 安装 Docker（如未安装）
if ! command -v docker &>/dev/null; then
  echo "[安装 Docker...]"
  if [ -f /etc/os-release ]; then
    . /etc/os-release
    case "$ID" in
      centos|rhel|fedora)
        dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo 2>/dev/null || true
        dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin ;;
      ubuntu|debian)
        apt-get update && apt-get install -y docker.io docker-compose-v2 ;;
      *) curl -fsSL https://get.docker.com | sh ;;
    esac
    systemctl enable --now docker
  fi
  echo "✅ Docker 安装完成"
else
  systemctl start docker 2>/dev/null || true
fi

echo "[导入镜像...]"
docker load -i ai-gateway-images.tar
echo "[启动服务...]"
docker compose down 2>/dev/null || true
docker compose up -d

echo "等待服务就绪..."
for i in $(seq 1 30); do
  if curl -s http://localhost:8080/api/actuator/health 2>/dev/null | grep -q UP; then
    IP=$(hostname -I 2>/dev/null | awk '{print $1}')
    echo ""
    echo "========================================="
    echo "✅ 部署成功！"
    echo "  地址: http://${IP:-localhost}:8080/api"
    echo "  登录: superadmin / admin123"
    echo "========================================="
    exit 0
  fi
  sleep 3; echo -n "."
done
echo "❌ 超时，查看日志: docker compose logs app"; exit 1
DEPLOY
chmod +x docker-deploy.sh

# ======================== 模式: export → 仅出包不启动 ========================
if [ "$1" = "export" ]; then
  # 准备 SQL 文件
  mkdir -p /tmp/ai-gateway-sql
  cp src/main/resources/sql/schema.sql src/main/resources/sql/docker-model-config.sql /tmp/ai-gateway-sql/
  echo ""
  echo -e "${GREEN}=========================================${NC}"
  echo -e "${GREEN}✅ 导出完成 ($DOCKER_PLATFORM)${NC}"
  echo -e "${GREEN}=========================================${NC}"
  echo ""
  echo "生成的文件:"
  echo "  1. ai-gateway-images.tar  镜像包 (${SIZE})"
  echo "  2. docker/docker-compose.yml     服务编排"
  echo "  3. docker/deploy-vm.sh       VM 一键部署"
  echo ""
  echo "传输到 CentOS9 虚拟机 (10.211.55.10):"
  echo "  scp ai-gateway-images.tar root@10.211.55.10:/opt/ai-gateway/"
  echo "  scp docker/docker-compose.yml root@10.211.55.10:/opt/ai-gateway/docker/"
  echo "  scp docker/deploy-vm.sh root@10.211.55.10:/opt/ai-gateway/docker/"
  echo "  mkdir -p /tmp/sql && cp src/main/resources/sql/schema.sql src/main/resources/sql/docker-model-config.sql /tmp/sql/"
  echo "  scp -r /tmp/sql root@10.211.55.10:/opt/ai-gateway/"
  echo "  ssh root@10.211.55.10 'cd /opt/ai-gateway && bash docker/deploy-vm.sh'"
  exit 0
fi

# ======================== 模式: 本机启动 ========================
echo -e "${YELLOW}[5/5] 启动服务...${NC}"
docker compose down 2>/dev/null || true
docker compose up -d

echo "等待服务就绪..."
for i in $(seq 1 30); do
  if docker compose ps app 2>/dev/null | grep -q "healthy"; then
    echo ""
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}✅ 部署成功！${NC}"
    echo -e "${GREEN}=========================================${NC}"
    echo "  地址: http://localhost:8080/api"
    echo "  登录: superadmin / admin123"
    exit 0
  fi
  sleep 3; echo -n "."
done
echo -e "${RED}❌ 超时: docker compose logs app${NC}"; exit 1
