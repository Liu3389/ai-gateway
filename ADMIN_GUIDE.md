# 管理员后台系统使用指南

## 📋 目录

- [系统概述](#系统概述)
- [角色体系](#角色体系)
- [API免费策略](#api免费策略)
- [快速开始](#快速开始)
- [API接口说明](#api接口说明)
- [使用示例](#使用示例)

---

## 系统概述

管理员后台系统提供了完整的用户管理、角色权限管理和API免费策略管理功能。支持三级角色体系，超级管理员可以任命/取消管理员，并为任何用户分配多种API免费使用策略。

## 角色体系

### 1. 超级管理员 (SUPER_ADMIN)

**权限：**

- ✅ 任命/取消管理员
- ✅ 分配API免费使用策略
- ✅ 查看所有用户和管理员
- ✅ 禁用/启用用户
- ✅ 查看用户详细信息

### 2. 管理员 (ADMIN)

**权限：**

- ✅ 查看所有用户
- ✅ 查看用户详细信息
- ✅ 禁用/启用用户
- ❌ 不能任命/取消管理员
- ❌ 不能分配免费策略

### 3. 普通用户 (USER)

**权限：**

- ✅ 使用API服务
- ✅ 查看个人信息
- ❌ 无管理权限

## API免费策略

系统支持5种免费策略，超级管理员可以为用户分配：

### 1. 完全免费 (UNLIMITED)

- 不限制调用次数
- 不限制Token数量
- 所有模型均可使用

### 2. 额度免费 (QUOTA_BASED)

- 每月赠送固定额度（如$10）
- 用完后需要充值或等待下月重置
- 适合测试用户或合作伙伴

### 3. 限次免费 (COUNT_LIMITED)

- 每日调用次数限制
- 每月调用次数限制
- 适合防止滥用

### 4. 限时免费 (TIME_LIMITED)

- 在特定时间段内免费
- 可设置开始和结束时间
- 适合促销活动

### 5. 模型限定免费 (MODEL_SPECIFIC)

- 仅对特定模型免费
- 可配置允许免费的模型列表
- 适合推广新模型

## 快速开始

### 1. 数据库迁移

执行迁移脚本添加管理员相关字段：

```bash
mysql -u root -p ai_gateway < src/main/resources/sql/migration_add_admin.sql
```

或者手动执行SQL：

```sql
-- 添加角色字段
ALTER TABLE `user` ADD COLUMN `role` varchar(32) NOT NULL DEFAULT 'USER' COMMENT '用户角色' AFTER `status`;

-- 添加免费策略相关字段
ALTER TABLE `user` ADD COLUMN `free_api_strategy` varchar(32) DEFAULT NULL AFTER `role`;
ALTER TABLE `user` ADD COLUMN `free_quota` decimal(10,4) DEFAULT '0.0000' AFTER `free_api_strategy`;
ALTER TABLE `user` ADD COLUMN `daily_call_limit` int DEFAULT NULL AFTER `free_quota`;
ALTER TABLE `user` ADD COLUMN `monthly_call_limit` int DEFAULT NULL AFTER `daily_call_limit`;
ALTER TABLE `user` ADD COLUMN `free_strategy_start_time` datetime DEFAULT NULL AFTER `monthly_call_limit`;
ALTER TABLE `user` ADD COLUMN `free_strategy_end_time` datetime DEFAULT NULL AFTER `free_strategy_start_time`;
ALTER TABLE `user` ADD COLUMN `allowed_free_models` text AFTER `free_strategy_end_time`;
```

### 2. 创建超级管理员

迁移脚本会自动创建超级管理员账号：

- **用户名**: superadmin
- **密码**: admin123
- **角色**: SUPER_ADMIN

⚠️ **重要**: 首次登录后请立即修改密码！

### 3. 启动应用

```bash
./start.sh
```

## API接口说明

### 认证相关

#### 1. 用户登录

```http
POST /auth/login
Content-Type: application/json

{
  "username": "superadmin",
  "password": "admin123"
}
```

响应：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "token_1",
    "userInfo": {
      "id": 1,
      "username": "superadmin",
      "email": "superadmin@aiplatform.com",
      "balance": 0.0000,
      "status": 1,
      "role": "SUPER_ADMIN",
      "freeApiStrategy": null
    }
  }
}
```

### 管理员接口

**注意**: 所有管理员接口需要在请求头中携带 `X-User-Id` 标识操作员ID

#### 2. 分配管理员角色（仅超级管理员）

```http
POST /admin/assign-role
X-User-Id: 1
Content-Type: application/json

{
  "userId": 2,
  "role": "ADMIN"
}
```

可选角色：

- `ADMIN` - 普通管理员
- `SUPER_ADMIN` - 超级管理员

#### 3. 取消管理员角色（仅超级管理员）

```http
POST /admin/revoke-role
X-User-Id: 1
Content-Type: application/json

{
  "userId": 2
}
```

#### 4. 设置API免费策略（仅超级管理员）

**完全免费策略：**

```http
POST /admin/set-free-strategy
X-User-Id: 1
Content-Type: application/json

{
  "userId": 2,
  "freeApiStrategy": "UNLIMITED"
}
```

**额度免费策略：**

```http
POST /admin/set-free-strategy
X-User-Id: 1
Content-Type: application/json

{
  "userId": 2,
  "freeApiStrategy": "QUOTA_BASED",
  "freeQuota": 10.0000
}
```

**限次免费策略：**

```http
POST /admin/set-free-strategy
X-User-Id: 1
Content-Type: application/json

{
  "userId": 2,
  "freeApiStrategy": "COUNT_LIMITED",
  "dailyCallLimit": 100,
  "monthlyCallLimit": 3000
}
```

**限时免费策略：**

```http
POST /admin/set-free-strategy
X-User-Id: 1
Content-Type: application/json

{
  "userId": 2,
  "freeApiStrategy": "TIME_LIMITED",
  "freeStrategyStartTime": "2026-05-12T00:00:00",
  "freeStrategyEndTime": "2026-06-12T23:59:59"
}
```

**模型限定免费策略：**

```http
POST /admin/set-free-strategy
X-User-Id: 1
Content-Type: application/json

{
  "userId": 2,
  "freeApiStrategy": "MODEL_SPECIFIC",
  "allowedFreeModels": "[\"gpt-3.5-turbo\",\"gpt-4o\"]"
}
```

#### 5. 获取所有管理员列表（仅超级管理员）

```http
GET /admin/admins
X-User-Id: 1
```

#### 6. 获取所有用户列表（管理员及以上）

```http
GET /admin/users
X-User-Id: 1
```

#### 7. 获取用户详细信息（管理员及以上）

```http
GET /admin/user/{userId}
X-User-Id: 1
```

#### 8. 禁用/启用用户（管理员及以上）

```http
POST /admin/user/status
X-User-Id: 1
Content-Type: application/json

{
  "userId": 2,
  "status": 0
}
```

状态值：

- `0` - 禁用
- `1` - 启用

## 使用示例

### 场景1：超级管理员任命普通管理员

```bash
# 1. 超级管理员登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123"}'

# 假设返回的userId为1

# 2. 为用户2分配管理员角色
curl -X POST http://localhost:8080/admin/assign-role \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"userId":2,"role":"ADMIN"}'
```

### 场景2：为用户设置完全免费策略

```bash
# 为用户3设置完全免费
curl -X POST http://localhost:8080/admin/set-free-strategy \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "userId":3,
    "freeApiStrategy":"UNLIMITED"
  }'
```

### 场景3：为测试用户设置月度额度

```bash
# 为用户4设置每月$10免费额度
curl -X POST http://localhost:8080/admin/set-free-strategy \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "userId":4,
    "freeApiStrategy":"QUOTA_BASED",
    "freeQuota":10.0000
  }'
```

### 场景4：查看和管理用户

```bash
# 查看所有用户
curl -X GET http://localhost:8080/admin/users \
  -H "X-User-Id: 1"

# 查看特定用户详情
curl -X GET http://localhost:8080/admin/user/2 \
  -H "X-User-Id: 1"

# 禁用违规用户
curl -X POST http://localhost:8080/admin/user/status \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"userId":2,"status":0}'
```

## 安全建议

1. **修改默认密码**: 首次登录后立即修改超级管理员密码
2. **最小权限原则**: 只授予必要的权限
3. **定期审计**: 定期检查管理员操作日志
4. **密码强度**: 使用强密码策略
5. **HTTPS**: 生产环境务必使用HTTPS
6. **Token机制**: 建议实现JWT Token替代简单的userId传递

## 后续优化建议

1. **JWT认证**: 实现完整的JWT Token认证机制
2. **操作日志**: 记录所有管理员操作
3. **权限拦截器**: 实现基于角色的权限拦截
4. **可视化界面**: 开发前端管理界面
5. **数据统计**: 添加用户活跃度、API调用统计等
6. **告警机制**: 异常操作实时告警

## 常见问题

**Q: 如何重置超级管理员密码？**
A: 直接在数据库中更新password字段（使用MD5加密）

**Q: 免费策略何时生效？**
A: 设置后立即生效，下次API调用时会根据策略判断

**Q: 如何取消用户的免费策略？**
A: 将freeApiStrategy设置为null即可

**Q: 管理员可以看其他管理员的信息吗？**
A: 可以，管理员可以查看所有用户信息，但只有超级管理员可以修改角色

## 技术支持

如有问题，请查看项目文档或联系开发团队。
