# AI Gateway Platform - 企业级 AI 模型调度网关

Spring Boot 3.2 + DeepSeek V4-Pro + Redis Lua 限流 + Token 级计费 + Docker 部署

---

## 🚀 快速启动（Docker 一键部署）

```bash
# 前置：安装 Docker Desktop
# macOS: brew install --cask docker （首次需打开 Docker.app 启动服务）
# Linux: curl -fsSL https://get.docker.com | sh

./deploy.sh
```

一行命令启动 MySQL + Redis + 后端应用，3-5 分钟后访问 `http://localhost:8080/api`。

---

## 📦 移植到 CentOS9 虚拟机

```bash
# 1. 在本机生成 arm64 镜像和部署包
./deploy.sh export

# 2. 传输到 CentOS9 虚拟机
scp ai-gateway-platform.tar root@你的虚拟机IP:/opt/ai-gateway/
scp centos9-deploy.sh root@你的虚拟机IP:/opt/ai-gateway/
scp docker-compose.yml root@你的虚拟机IP:/opt/ai-gateway/
scp -r src/main/resources/sql/ root@你的虚拟机IP:/opt/ai-gateway/src/main/resources/sql/

# 3. 在 CentOS9 上执行
ssh root@你的虚拟机IP "cd /opt/ai-gateway && ./centos9-deploy.sh"
```

---

## 📋 测试账号（密码统一 `admin123`）

| 用户名        | 角色          | userId | 说明   |
|------------|-------------|--------|------|
| superadmin | SUPER_ADMIN | 1      | 最高权限 |

> Docker 环境初始只有 superadmin，其他测试账号需通过 API 注册或导入 `testdata.sql`。

---

## 🔧 本地开发（非 Docker）

```bash
# 1. 初始化数据库（替换连接信息）
mysql -h YOUR_HOST -u YOUR_USER -p < src/main/resources/sql/schema.sql

# 2. 编译运行（需 JDK 21+）
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean package -DskipTests
java -jar target/ai-gateway-platform-1.0.0.jar
```

---

## 📡 接口文档 & Apifox 导入

- **接口文档**：[API_DOCUMENTATION.md](API_DOCUMENTATION.md) — 前端只需读这一个文件
- **Apifox 一键导入**：打开 Apifox → 导入 → OpenAPI/Swagger → 选择 `API_TEST.apifox.json`

---

## 🐳 Docker 服务说明

| 服务        | 端口   | 账号                  |
|-----------|------|---------------------|
| 后端应用      | 8080 | superadmin/admin123 |
| MySQL 8.0 | 3306 | root/root123        |
| Redis 7   | 6379 | 密码 redis123         |

---

## 🏗️ 项目结构

```
├── Dockerfile                    # Docker 镜像定义
├── docker-compose.yml            # 服务编排（app+mysql+redis）
├── deploy.sh                     # 一键部署 / 导出 CentOS9 镜像
├── API_TEST.apifox.json          # Apifox 一键导入
├── API_DOCUMENTATION.md          # 接口文档（前端唯一参考）
├── README.md                     # 本文件
└── src/
    ├── main/java/com/ai/gateway/
    │   ├── common/               # Result/枚举/常量
    │   ├── config/               # Security/Redis/MyBatis/WebMvc
    │   ├── controller/           # 5个控制器（Auth/User/ApiKey/Admin/Chat）
    │   ├── interceptor/          # API Key 鉴权 + Token 认证拦截器
    │   ├── service/              # 业务层（含计费/限流/对话）
    │   └── vo/dto/entity/        # 数据模型
    └── resources/
        ├── sql/                  # schema.sql + Docker 模型配置
        ├── lua/                  # Redis Lua 脚本（限流/计费/结算）
        ├── application.yml       # 本地配置
        └── application-docker.yml# Docker 环境配置
```

---

## 🛠 技术栈

Spring Boot 3.2 · MyBatis-Plus 3.5.7 · MySQL 8.0 · Redis 7 · DeepSeek API · Docker · Hutool · BCrypt
