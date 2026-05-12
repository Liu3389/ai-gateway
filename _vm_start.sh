#!/bin/bash
APP_DIR="/opt/ai-gateway"
LOG_DIR="$APP_DIR/logs"
mkdir -p "$LOG_DIR"

PID=$(pgrep -f "ai-gateway-platform" || true)
if [ -n "$PID" ]; then
    echo "旧进程 PID=$PID，正在停止..."
    kill "$PID" 2>/dev/null
    sleep 3
    pgrep -f "ai-gateway-platform" > /dev/null && kill -9 "$PID"
    echo "已停止"
fi

nohup java -Xmx512m -XX:+UseG1GC \
  -Djava.security.egd=file:/dev/./urandom \
  -jar "$APP_DIR/app.jar" \
  > "$LOG_DIR/app.log" 2>&1 &

sleep 5
NEW_PID=$(pgrep -f "ai-gateway-platform" || true)
if [ -n "$NEW_PID" ]; then
    echo "✅ 启动成功 PID=$NEW_PID"
    echo "查看日志: tail -f $LOG_DIR/app.log"
else
    echo "❌ 启动失败，查看日志: cat $LOG_DIR/app.log"
    exit 1
fi
