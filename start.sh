#!/bin/bash

# ============================================
# AI Gateway Platform - 快速启动脚本
# ============================================
# 功能说明：
#   1. 检查 Java 和 Maven 环境
#   2. 编译打包项目
#   3. 启动应用并验证健康状态
# 使用方法：
#   ./start.sh
# ============================================

echo "========================================="
echo "AI Gateway Platform - 快速启动脚本"
echo "========================================="
echo ""

# 检查Java版本
echo "1. 检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误: Java未安装"
    echo "   请安装 JDK 21 或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "   检测到 Java 版本: $JAVA_VERSION"

# 提取主版本号
JAVA_MAJOR_VERSION=$(echo $JAVA_VERSION | cut -d. -f1)
if [ "$JAVA_MAJOR_VERSION" -lt 21 ]; then
    echo "❌ 错误: 需要 JDK 21 或更高版本，当前版本: $JAVA_VERSION"
    echo "   请下载 JDK 21: https://openjdk.org/projects/jdk/21/"
    exit 1
fi
echo "✅ Java 版本检查通过"

# 检查Maven
echo ""
echo "2. 检查Maven环境..."
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: Maven未安装"
    echo "   请安装 Maven 3.6+: https://maven.apache.org/download.cgi"
    exit 1
fi
MVN_VERSION=$(mvn -version 2>&1 | head -n 1)
echo "✅ $MVN_VERSION"

# 清理并编译
echo ""
echo "3. 清理并编译项目..."
echo "   执行: mvn clean package -DskipTests"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ 编译失败，请检查错误信息"
    exit 1
fi
echo "✅ 编译成功"

# 检查JAR文件
JAR_FILE="target/ai-gateway-platform-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR文件不存在: $JAR_FILE"
    exit 1
fi
echo "✅ JAR文件存在: $JAR_FILE"

# 检查配置文件
echo ""
echo "4. 检查配置文件..."
CONFIG_FILE="src/main/resources/application.yml"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "❌ 配置文件不存在: $CONFIG_FILE"
    exit 1
fi
echo "✅ 配置文件存在"

# 检查关键配置项
echo ""
echo "5. 检查配置项..."
if grep -q "sk-your-openai-api-key-here" "$CONFIG_FILE"; then
    echo "⚠️  警告: OpenAI API Key 未配置（使用占位符）"
    echo "   如需测试对话功能，请修改 application.yml 中的 openai.api-key"
else
    echo "✅ OpenAI API Key 已配置"
fi

# 询问是否启动
echo ""
read -p "是否现在启动应用? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消启动"
    echo "你可以稍后运行: java -jar $JAR_FILE"
    exit 0
fi

# 检查端口占用
echo ""
echo "6. 检查端口占用..."
PORT=8080
if lsof -i:$PORT > /dev/null 2>&1; then
    echo "⚠️  警告: 端口 $PORT 已被占用"
    PID=$(lsof -ti:$PORT)
    echo "   占用进程 PID: $PID"
    read -p "是否强制停止旧进程? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        kill -9 $PID
        echo "✅ 已停止旧进程"
        sleep 2
    else
        echo "已取消启动"
        exit 0
    fi
fi

# 启动应用
echo ""
echo "7. 启动应用..."
LOG_FILE="logs/ai-gateway.log"

# 确保日志目录存在
mkdir -p logs

echo "   日志文件: $LOG_FILE"
nohup java -jar $JAR_FILE > $LOG_FILE 2>&1 &
APP_PID=$!
echo "✅ 应用已启动，PID: $APP_PID"

# 等待应用启动
echo ""
echo "等待应用启动..."
MAX_WAIT=60
for i in $(seq 1 $MAX_WAIT); do
    # 检查健康端点
    if curl -s http://localhost:$PORT/api/actuator/health > /dev/null 2>&1; then
        echo ""
        echo "========================================="
        echo "✅ 应用启动成功！"
        echo "========================================="
        echo "访问地址: http://localhost:$PORT/api"
        echo "健康检查: http://localhost:$PORT/api/actuator/health"
        echo "查看日志: tail -f $LOG_FILE"
        echo "停止应用: ./stop.sh 或 kill $APP_PID"
        echo "========================================="
        exit 0
    fi
    
    # 每5秒显示一次进度
    if [ $((i % 5)) -eq 0 ]; then
        echo -n "."
    fi
    
    sleep 1
done

echo ""
echo "❌ 应用启动超时（${MAX_WAIT}秒）"
echo "请检查日志: tail -f $LOG_FILE"
echo ""
echo "常见错误："
echo "  1. 数据库连接失败 - 检查 MySQL 是否运行"
echo "  2. Redis 连接失败 - 检查 Redis 是否运行"
echo "  3. 端口被占用 - 检查 8080 端口"
echo ""
exit 1
