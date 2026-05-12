#!/bin/bash
# ============================================
# AI Gateway Platform - 快速启动脚本（本地开发/非Docker）
# ============================================
# 功能: 编译 + 启动应用（直连虚拟机 MySQL/Redis）
# 使用: ./start.sh
# ============================================

echo "========================================="
echo "AI Gateway Platform - 快速启动"
echo "========================================="
echo ""

# 检查JDK (需要21+)
for jdk in "$JAVA_HOME" \
  "/Users/a1234/Library/Java/JavaVirtualMachines/corretto-21.0.10/Contents/Home" \
  "/usr/lib/jvm/java-21-openjdk"; do
  [ -f "$jdk/bin/java" ] && { export JAVA_HOME="$jdk"; export PATH="$JAVA_HOME/bin:$PATH"; break; }
done

if ! java -version 2>&1 | grep -qE "version \"(21|22|23|24|25)"; then
  echo "❌ 需要 JDK 21+，当前: $(java -version 2>&1 | head -1)"
  echo "手动指定: export JAVA_HOME=/path/to/jdk21 && ./start.sh"
  exit 1
fi

# 检查Maven
if ! command -v mvn &>/dev/null; then
  echo "❌ Maven 未安装"; exit 1
fi

# 编译
echo "1/3 编译项目..."
mvn clean package -DskipTests -q
echo "✅ 编译完成"

JAR="target/ai-gateway-platform-1.0.0.jar"
[ ! -f "$JAR" ] && { echo "❌ JAR 未生成"; exit 1; }

# 停旧进程
echo "2/3 检查旧进程..."
PID=$(lsof -ti:8080 2>/dev/null)
[ -n "$PID" ] && { echo "停止旧进程 PID=$PID"; kill -9 "$PID" 2>/dev/null; sleep 2; }

# 启动
echo "3/3 启动应用..."
mkdir -p logs
nohup java -jar "$JAR" > logs/app.log 2>&1 &
NEW_PID=$!

for i in $(seq 1 20); do
  if curl -s http://localhost:8080/api/actuator/health 2>/dev/null | grep -q UP; then
    echo ""
    echo "========================================="
    echo "✅ 启动成功 PID=$NEW_PID"
    echo "  地址: http://localhost:8080/api"
    echo "  日志: tail -f logs/app.log"
    echo "  停止: ./stop.sh"
    echo "========================================="
    exit 0
  fi
  sleep 3; echo -n "."
done
echo "❌ 启动超时，查看日志: tail -30 logs/app.log"; exit 1
