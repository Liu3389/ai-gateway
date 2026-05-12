# ============================================
# AI Gateway Platform - Docker 构建文件
# ============================================
# 说明：
#   1. 使用多阶段构建减小镜像体积
#   2. 第一阶段：编译 Java 应用
#   3. 第二阶段：运行应用（仅包含 JRE）
# ============================================

# ---------- 第一阶段：构建阶段 ----------
# 使用 Maven 官方镜像进行编译
FROM maven:3.9-eclipse-temurin-21 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml（利用 Docker 缓存层）
COPY pom.xml .

# 下载依赖（如果 pom.xml 没变化，这一步会缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 编译打包（跳过测试加快速度）
RUN mvn clean package -DskipTests -B

# ---------- 第二阶段：运行阶段 ----------
# 使用轻量级的 JRE 镜像
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 安装必要的系统工具（curl 用于健康检查）
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/target/ai-gateway-platform-1.0.0.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

# 设置 JVM 参数（优化容器环境）
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
