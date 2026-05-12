# AI Gateway Platform - 企业级 AI 模型调度网关

Spring Boot 3.2 + MyBatis-Plus + Docker + Redis Lua 限流 + UUID Token 认证 + Token 级计费

---

## 🚀 Docker 一键部署（推荐）

适配 **macOS** (Apple Silicon / Intel)、**Linux** (Ubuntu / CentOS / Debian)、**Windows** (WSL2)

### 前置条件

| 依赖     | 版本   | 安装                                                                                     |
|--------|------|----------------------------------------------------------------------------------------|
| JDK    | 21+  | [Adoptium](https://adoptium.net/) 或 `brew install openjdk@21`                          |
| Maven  | 3.9+ | `brew install maven` 或 `apt install maven`                                             |
| Docker | 24+  | `brew install --cask docker` (Mac) 或 `curl -fsSL https://get.docker.com \| sh` (Linux) |

### 本机启动

```bash
./deploy.sh
```

**自动完成**: 编译 → 拉取基础镜像 (Docker Hub 被墙自动切 DaoCloud) → 构建应用镜像 → 导出 tar → 启动 MySQL+Redis+App 三容器

### 只编译 jar（不涉及 Docker）

```bash
./deploy.sh build
```

### 导出镜像包（传输到其他服务器）

```bash
./deploy.sh export
```

生成文件:

- `ai-gateway-images.tar` — 完整镜像包 (含 app + mysql:8.0 + redis:7-alpine)
- `docker-compose.yml` — 服务编排
- `docker-deploy.sh` — 目标机器一键部署脚本

### 传输到目标机器并部署

```bash
# 1. 传输文件
scp ai-gateway-images.tar user@TARGET_IP:/opt/ai-gateway/
scp docker-compose.yml  user@TARGET_IP:/opt/ai-gateway/
scp docker-deploy.sh    user@TARGET_IP:/opt/ai-gateway/
mkdir -p /tmp/sql && cp src/main/resources/sql/*.sql /tmp/sql/
scp -r /tmp/sql user@TARGET_IP:/opt/ai-gateway/

# 2. 目标机器执行
ssh user@TARGET_IP "cd /opt/ai-gateway && bash docker-deploy.sh"
```

> 目标机器**无需安装 JDK/Maven/MySQL/Redis**，只需能运行 Docker。

---

## 🖥️ 当前部署

| 环境                  | 地址                             | 说明                 |
|---------------------|--------------------------------|--------------------|
| **CentOS9 ARM 虚拟机** | `http://10.211.55.10:8080/api` | Docker 三容器运行中      |
| 虚拟机 SSH             | `ssh root@10.211.55.10`        | 密码 `9999`          |
| 部署目录                | `/opt/ai-gateway/`             | 镜像包 + compose + 脚本 |

### Docker 容器

| 容器               | 端口映射      | 账号                  |
|------------------|-----------|---------------------|
| ai-gateway-app   | 8080:8080 | superadmin/admin123 |
| ai-gateway-mysql | 3307→3306 | root/root123        |
| ai-gateway-redis | 6380→6379 | 密码 redis123         |

### Docker 常用命令

```bash
ssh root@10.211.55.10 "cd /opt/ai-gateway && docker compose ps"        # 查看状态
ssh root@10.211.55.10 "cd /opt/ai-gateway && docker compose logs -f app"  # 查看日志
ssh root@10.211.55.10 "cd /opt/ai-gateway && docker compose restart"    # 重启
ssh root@10.211.55.10 "cd /opt/ai-gateway && docker compose down"       # 停止
```

---

## 🔧 本地开发（非Docker）

```bash
# 直连虚拟机 MySQL/Redis，本地只跑应用
export JAVA_HOME=/path/to/jdk21
./start.sh     # 编译+启动
./stop.sh      # 停止
```

---

## 📋 测试账号

| 用户名        | 角色          | 密码       |
|------------|-------------|----------|
| superadmin | SUPER_ADMIN | admin123 |

> Docker 初始只有 superadmin。其他测试账号可通过注册接口创建。

---

## 📡 接口文档 & Apifox

- **接口文档**: [API_DOCUMENTATION.md](API_DOCUMENTATION.md) — 前端只需这一个文件
- **Apifox 一键导入**: 打开 Apifox → 导入 → OpenAPI → 选择 `API_TEST.apifox.json`
- **Postman**: 拖入项目目录下的 `API_TEST.postman_collection.json`（如有）

---

## 🧠 核心特性

- ✅ **多模型接入** — DeepSeek V4-Pro / V4-Flash，兼容 OpenAI 格式
- ✅ **UUID Token 认证** — 登录返回随机 UUID Token (Redis 24h)，不可伪造
- ✅ **Redis Lua 限流** — 滑动窗口按 API Key 粒度
- ✅ **Token 计费** — 预扣余额 + 调用后结算 + 差额退还
- ✅ **免费策略** — UNLIMITED / QUOTA_BASED / COUNT_LIMITED / TIME_LIMITED / MODEL_SPECIFIC
- ✅ **角色权限** — SUPER_ADMIN / ADMIN / USER 三级
- ✅ **SSE 流式** — 打字机效果逐字输出
- ✅ **Docker 部署** — 自动适配 arm64/amd64，DaoCloud 国内加速

---

## 🏗️ 项目结构

```
├── deploy.sh                     # 本机 Docker 一键部署
├── docker-compose.yml            # 服务编排
├── docker-deploy.sh              # 目标机器自动部署（deploy.sh 自动生成）
├── docker-stop.sh                # 停止 Docker 服务
├── Dockerfile                    # 应用镜像定义
├── .dockerignore                 # Docker 构建忽略
├── start.sh                      # 本地开发启动（非Docker）
├── stop.sh                       # 本地开发停止
├── check_env.sh                  # 环境连通性检查
├── init_db.sh                    # 数据库初始化
├── API_TEST.apifox.json          # Apifox 一键导入
├── API_DOCUMENTATION.md          # 接口文档
├── README.md
└── src/
    ├── main/java/com/ai/gateway/
    │   ├── common/               # Result/枚举/Constants
    │   ├── config/               # Security/Redis/MyBatis/WebMvc
    │   ├── controller/           # Auth / User / ApiKey / Admin / Chat
    │   ├── interceptor/          # ApiKeyAuthInterceptor + TokenAuthInterceptor
    │   ├── service/              # 计费/限流/对话/管理
    │   └── vo/dto/entity/        # 数据模型
    └── resources/
        ├── sql/                  # schema.sql + Docker 模型配置
        ├── lua/                  # Redis Lua: billing.lua / rate_limit.lua / settlement.lua
        ├── application.yml       # 本地配置（指向 10.211.55.10）
        └── application-docker.yml# Docker 配置
```

---

## 🛠 技术栈

Spring Boot 3.2 · MyBatis-Plus 3.5.7 · MySQL 8.0 · Redis 7 · DeepSeek API · Docker · BCrypt · Hutool · Lombok
