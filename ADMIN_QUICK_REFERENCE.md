# 管理员系统 - 快速参考卡

## 🔑 默认账号

```
超级管理员: superadmin / admin123
管理员:     admin / admin123
```

## 📋 角色权限对比

| 功能      | SUPER_ADMIN | ADMIN | USER |
|---------|-------------|-------|------|
| 任命管理员   | ✅           | ❌     | ❌    |
| 取消管理员   | ✅           | ❌     | ❌    |
| 设置免费策略  | ✅           | ❌     | ❌    |
| 查看管理员列表 | ✅           | ❌     | ❌    |
| 查看所有用户  | ✅           | ✅     | ❌    |
| 查看用户详情  | ✅           | ✅     | ❌    |
| 禁用/启用用户 | ✅           | ✅     | ❌    |
| 查看系统统计  | ✅           | ✅     | ❌    |
| 使用API   | ✅           | ✅     | ✅    |

## 🎁 免费策略类型

| 策略   | 代码             | 说明     |
|------|----------------|--------|
| 完全免费 | UNLIMITED      | 无限制    |
| 额度免费 | QUOTA_BASED    | 每月固定额度 |
| 限次免费 | COUNT_LIMITED  | 限制调用次数 |
| 限时免费 | TIME_LIMITED   | 特定时间段  |
| 模型限定 | MODEL_SPECIFIC | 仅特定模型  |

## 🚀 快速命令

### 数据库迁移

```bash
mysql -u root -p ai_gateway < src/main/resources/sql/migration_add_admin.sql
```

### 启动应用

```bash
./start.sh
```

### 运行测试

```bash
./test_admin.sh
```

## 📡 API速查

### 登录

```bash
POST /auth/login
{"username":"superadmin","password":"admin123"}
```

### 分配管理员

```bash
POST /admin/assign-role
Headers: X-User-Id: 1
{"userId":2,"role":"ADMIN"}
```

### 设置完全免费

```bash
POST /admin/set-free-strategy
Headers: X-User-Id: 1
{"userId":2,"freeApiStrategy":"UNLIMITED"}
```

### 设置额度免费($10)

```bash
POST /admin/set-free-strategy
Headers: X-User-Id: 1
{"userId":2,"freeApiStrategy":"QUOTA_BASED","freeQuota":10.0}
```

### 设置限次免费

```bash
POST /admin/set-free-strategy
Headers: X-User-Id: 1
{"userId":2,"freeApiStrategy":"COUNT_LIMITED","dailyCallLimit":100,"monthlyCallLimit":3000}
```

### 查看用户列表

```bash
GET /admin/users
Headers: X-User-Id: 1
```

### 查看系统统计

```bash
GET /admin/stats
Headers: X-User-Id: 1
```

### 禁用用户

```bash
POST /admin/user/status
Headers: X-User-Id: 1
{"userId":2,"status":0}
```

## ⚠️ 安全提醒

1. ✅ 首次登录后立即修改密码
2. ✅ 生产环境使用HTTPS
3. ✅ 实施IP白名单
4. ✅ 定期审计操作日志
5. ✅ 遵循最小权限原则

## 📊 统计指标

系统统计包含：

- 👥 用户总数、活跃数、管理员数
- 📞 API总调用、今日调用
- 💰 总收入、今日收入、平均费用
- 🎁 免费策略用户数
- 🤖 模型总数、启用数

## 🔧 常见问题

**Q: 如何重置密码？**
A: 直接更新数据库password字段（MD5加密）

**Q: 如何取消免费策略？**
A: 将freeApiStrategy设置为null

**Q: 管理员能看其他管理员吗？**
A: 可以，但只有超级管理员能修改角色

**Q: 免费策略何时生效？**
A: 设置后立即生效

## 📚 详细文档

- 完整指南: [ADMIN_GUIDE.md](ADMIN_GUIDE.md)
- 功能总结: [ADMIN_FEATURES.md](ADMIN_FEATURES.md)
- 实现细节: [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)

---
**提示**: 将此卡片保存为书签，方便快速查阅！
