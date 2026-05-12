# Docker 零基础快速入门指南

> 📖 本指南专为 Docker 初学者设计，5分钟理解核心概念，10分钟完成首次部署

---

## 一、Docker 是什么？

### 通俗理解

想象你要把应用部署到不同的服务器：

**传统方式（痛苦）：**

```
服务器1: 安装 Java → 配置 MySQL → 安装 Redis → 部署应用 → 调试环境问题
服务器2: 安装 Java → 配置 MySQL → 安装 Redis → 部署应用 → 调试环境问题
服务器3: 安装 Java → 配置 MySQL → 安装 Redis → 部署应用 → 调试环境问题
```

**Docker 方式（轻松）：**

```
本地: 打包成镜像（包含所有依赖）
      ↓ 传输镜像
服务器1: 运行镜像 ✅
服务器2: 运行镜像 ✅
服务器3: 运行镜像 ✅
```

### 核心概念对比

| 概念        | 传统比喻        | Docker 对应      |
|-----------|-------------|----------------|
| **菜谱**    | 做菜的步骤说明     | Dockerfile     |
| **打包好的菜** | 预制菜/罐头      | Image（镜像）      |
| **正在吃的菜** | 加热后的菜品      | Container（容器）  |
| **厨房**    | 做菜的环境       | Docker Engine  |
| **菜单组合**  | 套餐（主菜+汤+甜点） | Docker Compose |

---

## 二、本项目 Docker 架构

```
┌─────────────────────────────────────────────┐
│           Docker Compose 管理                │
├──────────┬──────────┬───────────────────────┤
│  MySQL   │  Redis   │   Application         │
│  容器     │  容器     │   容器                 │
│          │          │                       │
│ 端口:3306│ 端口:6379│  端口:8080             │
│          │          │                       │
│ 数据卷    │ 数据卷    │  日志目录              │
└──────────┴──────────┴───────────────────────┘
         ↓
    Docker Network (内部通信)
```

**三个容器：**

1. **MySQL 容器** - 数据库服务
2. **Redis 容器** - 缓存服务
3. **App 容器** - Spring Boot 应用

**优势：**

- ✅ 每个容器独立运行，互不干扰
- ✅ 容器间通过内部网络通信
- ✅ 数据持久化到宿主机
- ✅ 一键启动/停止所有服务

---

## 三、文件说明

### 1. Dockerfile（应用镜像构建文件）

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY target/ai-gateway-platform-1.0.0.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75",
  "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```

**关键点：**

- 使用 `eclipse-temurin:21-jre-alpine` 轻量级 JRE 镜像
- 非 root 用户运行（安全性）
- 内置健康检查 wget → /actuator/health
- G1GC 垃圾回收器 + 75% 内存上限

### 2. docker-compose.yml（服务编排文件）

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot123"]

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass redis123 --appendonly yes
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "redis123", "ping"]

  app:
    image: ai-gateway-platform:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
```

**关键点：**

- `depends_on` + `healthcheck` 确保 MySQL/Redis 先启动好再启动 App
- `volumes` 挂载初始化 SQL：MySQL 容器首次启动自动建表+插入初始数据
- `SPRING_PROFILES_ACTIVE=docker` 激活 `application-docker.yml` 配置

### 3. .dockerignore（忽略文件）

类似 `.gitignore`，指定哪些文件不打包到镜像中：

```
.git
target/
logs/
*.md
```

**作用：**

- 减小镜像体积
- 加快构建速度
- 避免敏感信息泄露

### 4. deploy.sh（一键部署脚本）

自动化执行以下步骤：

1. 检查 Docker 环境
2. 构建镜像
3. 启动服务
4. 验证健康状态

---

## 四、常用命令速查表

### 基础命令

```bash
# 查看所有容器
docker ps                    # 运行中的
docker ps -a                 # 包括已停止的

# 查看镜像
docker images

# 查看日志
docker logs <container_id>
docker logs -f <container_id>  # 实时跟踪

# 进入容器
docker exec -it <container_id> sh

# 停止容器
docker stop <container_id>

# 删除容器
docker rm <container_id>

# 删除镜像
docker rmi <image_id>
```

### Docker Compose 命令

```bash
# 启动所有服务
docker compose up -d         # -d 表示后台运行

# 停止所有服务
docker compose down

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs          # 所有服务
docker compose logs app      # 只看 app
docker compose logs -f app   # 实时跟踪

# 重启服务
docker compose restart app

# 重新构建
docker compose build --no-cache

# 完全清理（包括数据）
docker compose down -v
```

### 实用技巧

```bash
# 查看容器资源占用
docker stats

# 查看容器详细信息
docker inspect <container_id>

# 复制文件到容器
docker cp local.txt <container_id>:/path/

# 从容器复制文件
docker cp <container_id>:/path/file.txt .

# 暂停/恢复容器
docker pause <container_id>
docker unpause <container_id>
```

---

## 五、首次部署实战

### Step 1: 安装 Docker

**macOS:**

```bash
brew install --cask docker
# 启动 Docker Desktop
open /Applications/Docker.app
```

**Linux (Ubuntu):**

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 重新登录后生效
```

**Windows:**
下载 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)

### Step 2: 验证安装

```bash
docker --version
# 输出: Docker version 24.x.x

docker compose version
# 输出: Docker Compose version v2.x.x

docker run hello-world
# 输出: Hello from Docker! （表示安装成功）
```

### Step 3: 部署项目

```bash
# 1. 进入项目目录
cd ~/Documents/Aiplatform/Aiplatform-demo

# 2. 本机启动（自动编译+拉镜像+构建+启动）
./deploy.sh

# 或导出镜像到其他服务器
./deploy.sh export

# 3. 等待启动完成（约 2-3 分钟）
# 看到 "✅ 部署成功！" 即可
```

### Step 4: 验证部署

```bash
# 1. 查看服务状态
docker compose ps

# 应该看到三个容器都是 "Up" 状态

# 2. 访问应用
curl http://localhost:8080/api/actuator/health

# 应该返回: {"status":"UP"}

# 3. 浏览器访问
# http://localhost:8080/api
```

### Step 5: 测试功能

使用 Apifox 导入 `API_TEST.apifox.json` 测试接口，或：

```bash
# 测试注册接口
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","email":"test@example.com"}'
```

---

## 六、常见问题 FAQ

### Q1: Docker 和虚拟机有什么区别？

| 特性   | Docker    | 虚拟机      |
|------|-----------|----------|
| 启动速度 | 秒级        | 分钟级      |
| 资源占用 | 轻量（共享内核）  | 重量（独立系统） |
| 性能   | 接近原生      | 有损耗      |
| 隔离性  | 进程级       | 系统级      |
| 适用场景 | 微服务、CI/CD | 完整系统隔离   |

### Q2: 容器删除后数据会丢失吗？

**不会！** 我们使用了 Docker Volumes：

- MySQL 数据保存在 `mysql-data` 卷
- Redis 数据保存在 `redis-data` 卷
- 应用日志保存在 `./logs` 目录

即使删除容器，数据依然存在。

### Q3: 如何更新应用？

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建并启动
docker compose up -d --build

# 3. 查看日志确认启动成功
docker compose logs -f app
```

### Q4: 如何备份数据？

```bash
# 备份 MySQL 数据
docker exec ai-gateway-mysql mysqldump -uroot -p123456 ai_gateway > backup.sql

# 备份 Redis 数据
docker exec ai-gateway-redis redis-cli -a 123321 SAVE

# 备份数据卷
docker run --rm -v ai-gateway-platform_mysql-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz /data
```

### Q5: 内存不足怎么办？

```bash
# 1. 查看资源占用
docker stats

# 2. 限制容器内存（编辑 docker-compose.yml）
services:
  app:
    deploy:
      resources:
        limits:
          memory: 1G

# 3. 调整 JVM 参数
environment:
  JAVA_OPTS: -Xms256m -Xmx512m
```

### Q6: 如何在服务器上后台运行？

```bash
# 使用 nohup 或 systemd

# 方法1: nohup
nohup docker compose up -d &

# 方法2: systemd（推荐）
# 创建 /etc/systemd/system/ai-gateway.service
[Unit]
Description=AI Gateway Platform
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/ai-gateway
ExecStart=/usr/bin/docker compose up -d
ExecStop=/usr/bin/docker compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target

# 启用服务
sudo systemctl enable ai-gateway
sudo systemctl start ai-gateway
```

---

## 七、进阶学习路线

### Level 1: 基础（已完成 ✅）

- [x] 理解 Docker 基本概念
- [x] 会使用 Docker Compose
- [x] 能部署本项目

### Level 2: 进阶

- [ ] 编写自己的 Dockerfile
- [ ] 理解 Docker 网络模式
- [ ] 掌握数据卷管理
- [ ] 学习 Docker 安全最佳实践

### Level 3: 高级

- [ ] Docker Swarm 集群
- [ ] Kubernetes 编排
- [ ] CI/CD 集成
- [ ] 监控和日志收集

### 学习资源

- **官方文档**: https://docs.docker.com/
- **交互式教程**: https://play-with-docker.com/
- **最佳实践**: https://github.com/docker/awesome-compose
- **视频课程**: B站搜索"Docker 入门"

---

## 八、总结

### 你已经学会：

1. ✅ Docker 的核心概念（Image、Container、Dockerfile）
2. ✅ 如何使用 Docker Compose 管理多容器应用
3. ✅ 如何部署 AI Gateway Platform
4. ✅ 常用的 Docker 命令
5. ✅ 故障排查方法

### 下一步：

1. 尝试修改 `docker-compose.yml` 配置
2. 学习编写简单的 Dockerfile
3. 探索 Docker 的高级功能

---

**🎉 恭喜！你已经从 Docker 零基础迈出了重要的一步！**

有任何问题，随时查阅本文档或官方文档。
