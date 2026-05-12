# AI Gateway Platform - AI 模型调度网关

Spring Boot 3.2 + DeepSeek V4-Pro + Redis Lua 限流 + Token 级计费

## 快速启动

```bash
# 1. 初始化数据库
mysql -h 10.211.55.10 -u root -p123456 < src/main/resources/sql/schema.sql

# 2. 插入测试数据
mysql -h 10.211.55.10 -u root -p123456 ai_gateway < testdata.sql

# 3. 编译运行
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean package -DskipTests
java -jar target/ai-gateway-platform-1.0.0.jar
```

## 测试账号（密码统一 `admin123`）

| 用户名        | 角色          | userId | 说明             |
|------------|-------------|--------|----------------|
| superadmin | SUPER_ADMIN | 4      | 最高权限           |
| zhangsan   | ADMIN       | 7      | 管理员            |
| lisi       | USER        | 8      | 普通用户           |
| wangwu     | USER        | 9      | 完全免费 UNLIMITED |
| devteam    | ADMIN       | 12     | 额度免费           |

## Apifox 一键导入

打开 Apifox → 导入 → OpenAPI/Swagger → 选择 `API_TEST.apifox.json`  
所有 20+ 接口自动加载，Header/参数/示例值均预填好。

## 文档

- **接口文档**：[API_DOCUMENTATION.md](API_DOCUMENTATION.md) — 前端只需读这一个文件即可开发
- **测试数据**：`testdata.sql`

## 技术栈

Spring Boot 3.2 · MyBatis-Plus 3.5.7 · MySQL 8.0 · Redis 6.2 · DeepSeek API · Hutool

## 项目结构

```
src/main/java/com/ai/gateway/
├── common/        # Result/枚举
├── config/        # Security/Redis/MyBatis
├── controller/    # 5个控制器
├── dto/           # 请求体
├── entity/        # 5张数据表
├── interceptor/   # API Key鉴权+限流+预扣
├── mapper/        # MyBatis Mapper
├── service/       # 业务层
└── vo/            # 响应体
src/main/resources/
├── lua/           # Redis Lua脚本(限流/计费/结算)
├── sql/           # schema.sql
└── application.yml
```
