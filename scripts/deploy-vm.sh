#!/bin/bash
# ============================================
# AI Gateway Platform - 部署到 CentOS9 ARM 虚拟机
# ============================================
# 使用方式: ./deploy-vm.sh
# 前置条件: 虚拟机已有 JDK 21 + MySQL + Redis 在运行
# 如提示输入密码，密码为 9999
# 可选先行免密: ssh-copy-id root@10.211.55.10
# ============================================
set -e

VM_IP="10.211.55.10"
VM_USER="root"
VM_PASS="9999"
VM_DIR="/opt/ai-gateway"
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 自动检测是否有 sshpass，没有则尝试免密
if command -v sshpass &> /dev/null; then
    SSH_CMD="sshpass -p ${VM_PASS} ssh -o StrictHostKeyChecking=no"
    SCP_CMD="sshpass -p ${VM_PASS} scp -o StrictHostKeyChecking=no"
else
    SSH_CMD="ssh -o StrictHostKeyChecking=no"
    SCP_CMD="scp -o StrictHostKeyChecking=no"
fi

echo "========================================="
echo "AI Gateway Platform - 部署到 ${VM_IP}"
echo "========================================="

# 1. 编译
echo -e "${YELLOW}[1/5] 编译工程...${NC}"
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || echo "$JAVA_HOME")
mvn clean package -DskipTests -q
echo -e "${GREEN}✅ 编译完成${NC}"

# 2. 创建虚拟机部署包
echo -e "${YELLOW}[2/5] 打包部署文件...${NC}"
DEPLOY_DIR="/tmp/ai-gateway-deploy"
rm -rf "$DEPLOY_DIR"
mkdir -p "$DEPLOY_DIR"

cp target/ai-gateway-platform-1.0.0.jar "$DEPLOY_DIR/app.jar"

# 生成虚拟机启动脚本
cat > "$DEPLOY_DIR/start.sh" << 'STARTSCRIPT'
#!/bin/bash
APP_DIR="/opt/ai-gateway"
LOG_DIR="$APP_DIR/logs"
mkdir -p "$LOG_DIR"

PID=$(pgrep -f "ai-gateway-platform" || true)
if [ -n "$PID" ]; then
    echo "应用已在运行 PID=$PID"
    exit 0
fi

nohup java -Xmx512m -XX:+UseG1GC \
  -Djava.security.egd=file:/dev/./urandom \
  -jar "$APP_DIR/app.jar" \
  > "$LOG_DIR/app.log" 2>&1 &

sleep 5
NEW_PID=$(pgrep -f "ai-gateway-platform" || true)
if [ -n "$NEW_PID" ]; then
    echo "✅ 启动成功 PID=$NEW_PID"
    echo "日志: tail -f $LOG_DIR/app.log"
else
    echo "❌ 启动失败，查看日志: cat $LOG_DIR/app.log"
    exit 1
fi
STARTSCRIPT

cat > "$DEPLOY_DIR/stop.sh" << 'STOPSCRIPT'
#!/bin/bash
PID=$(pgrep -f "ai-gateway-platform" || true)
if [ -z "$PID" ]; then
    echo "应用未运行"
    exit 0
fi
kill "$PID"
sleep 3
if pgrep -f "ai-gateway-platform" > /dev/null; then
    kill -9 "$PID"
fi
echo "✅ 已停止 PID=$PID"
STOPSCRIPT

cat > "$DEPLOY_DIR/status.sh" << 'STATUSSCRIPT'
#!/bin/bash
PID=$(pgrep -f "ai-gateway-platform" || true)
if [ -n "$PID" ]; then
    echo "✅ 运行中 PID=$PID"
    curl -s http://localhost:8080/api/actuator/health
else
    echo "❌ 未运行"
fi
STATUSSCRIPT

chmod +x "$DEPLOY_DIR"/*.sh
echo -e "${GREEN}✅ 部署包已就绪: $DEPLOY_DIR${NC}"
ls -lh "$DEPLOY_DIR/"

# 3. 检查虚拟机 JDK
echo -e "${YELLOW}[3/5] 检查虚拟机 JDK...${NC}"
if ${SSH_CMD} "${VM_USER}@${VM_IP}" "java -version 2>&1" | grep -q "21"; then
    echo -e "${GREEN}✅ 虚拟机 JDK 21 已就绪${NC}"
else
    echo -e "${YELLOW}⚠️  虚拟机需安装 JDK 21，正在安装...${NC}"
    ${SSH_CMD} "${VM_USER}@${VM_IP}" "dnf install -y java-21-openjdk" || echo "请手动安装: dnf install -y java-21-openjdk"
fi

# 4. 传输文件
echo -e "${YELLOW}[4/5] 传输文件到虚拟机...${NC}"
${SSH_CMD} "${VM_USER}@${VM_IP}" "mkdir -p ${VM_DIR}/logs"
${SCP_CMD} "$DEPLOY_DIR/app.jar" "${VM_USER}@${VM_IP}:${VM_DIR}/"
${SCP_CMD} "$DEPLOY_DIR/start.sh" "${VM_USER}@${VM_IP}:${VM_DIR}/"
${SCP_CMD} "$DEPLOY_DIR/stop.sh" "${VM_USER}@${VM_IP}:${VM_DIR}/"
${SCP_CMD} "$DEPLOY_DIR/status.sh" "${VM_USER}@${VM_IP}:${VM_DIR}/"
echo -e "${GREEN}✅ 传输完成${NC}"

# 5. 重启应用
echo -e "${YELLOW}[5/5] 启动应用...${NC}"
${SSH_CMD} "${VM_USER}@${VM_IP}" "cd ${VM_DIR} && ./stop.sh 2>/dev/null; ./start.sh"

# 等待
echo "等待应用就绪..."
sleep 8
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "http://${VM_IP}:8080/api/actuator/health" 2>/dev/null || echo "000")
if [ "$HEALTH" = "200" ]; then
    echo ""
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}✅ 部署成功！${NC}"
    echo -e "${GREEN}=========================================${NC}"
    echo ""
    echo "  地址: http://${VM_IP}:8080/api"
    echo "  登录: superadmin / admin123"
    echo ""
    echo "  管理命令 (在虚拟机上):"
    echo "    cd ${VM_DIR}"
    echo "    ./start.sh   启动"
    echo "    ./stop.sh    停止"
    echo "    ./status.sh  状态"
    echo "    tail -f logs/app.log  查看日志"
else
    echo -e "${YELLOW}⚠️  应用可能还在启动，请到虚拟机上检查: ssh ${VM_USER}@${VM_IP} 'cd ${VM_DIR} && tail -30 logs/app.log'${NC}"
fi

rm -rf "$DEPLOY_DIR"
