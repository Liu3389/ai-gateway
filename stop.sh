#!/bin/bash

# ============================================
# AI Gateway Platform - 停止脚本
# ============================================
# 功能说明：
#   1. 查找应用进程
#   2. 优雅停止应用
#   3. 如果优雅停止失败，强制终止
# 使用方法：
#   ./stop.sh
# ============================================

echo "========================================="
echo "AI Gateway Platform - 停止应用"
echo "========================================="

# 定义应用名称和JAR文件
APP_NAME="ai-gateway-platform"
JAR_FILE="target/ai-gateway-platform-1.0.0.jar"

# 查找进程
echo ""
echo "1. 查找应用进程..."

# 方法1: 通过 JAR 文件名查找
PID=$(ps aux | grep "$JAR_FILE" | grep -v grep | awk '{print $2}')

# 方法2: 如果方法1失败，通过应用名称查找
if [ -z "$PID" ]; then
    PID=$(ps aux | grep "$APP_NAME" | grep -v grep | awk '{print $2}')
fi

# 方法3: 如果还是没找到，检查端口占用
if [ -z "$PID" ]; then
    PORT_PID=$(lsof -ti:8080 2>/dev/null)
    if [ ! -z "$PORT_PID" ]; then
        echo "⚠️  未找到应用进程，但端口 8080 被占用"
        echo "   占用进程 PID: $PORT_PID"
        read -p "是否停止该进程? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            PID=$PORT_PID
        else
            echo "已取消"
            exit 0
        fi
    fi
fi

if [ -z "$PID" ]; then
    echo "✅ 应用未运行"
    exit 0
fi

echo "✅ 找到应用进程: PID=$PID"

# 显示进程信息
echo ""
echo "进程详情："
ps -p $PID -o pid,ppid,user,%cpu,%mem,etime,command

# 询问是否停止
echo ""
read -p "是否停止应用? (y/n): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

# 优雅停止（SIGTERM）
echo ""
echo "2. 正在优雅停止应用..."
kill $PID

# 等待进程结束
echo "   等待进程退出..."
MAX_WAIT=15
for i in $(seq 1 $MAX_WAIT); do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "✅ 应用已优雅停止"
        echo ""
        echo "========================================="
        echo "应用已成功停止"
        echo "========================================="
        exit 0
    fi
    
    # 每3秒显示一次进度
    if [ $((i % 3)) -eq 0 ]; then
        echo -n "."
    fi
    
    sleep 1
done

# 强制停止（SIGKILL）
echo ""
echo "⚠️  优雅停止超时，尝试强制停止..."
kill -9 $PID

# 验证是否停止成功
sleep 2
if ! ps -p $PID > /dev/null 2>&1; then
    echo "✅ 应用已强制停止"
else
    echo "❌ 停止失败，请手动执行: kill -9 $PID"
    exit 1
fi

echo ""
echo "========================================="
echo "应用已成功停止"
echo "========================================="
