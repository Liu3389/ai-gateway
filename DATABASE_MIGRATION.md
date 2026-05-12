# 数据库迁移指南

## 📋 概述

本文档说明如何将现有数据库升级到支持管理员系统的版本。

---

## 🔍 迁移内容

### 新增字段（user表）

| 字段名                      | 类型            | 默认值    | 说明        |
|--------------------------|---------------|--------|-----------|
| role                     | VARCHAR(32)   | 'USER' | 用户角色      |
| free_api_strategy        | VARCHAR(32)   | NULL   | 免费策略类型    |
| free_quota               | DECIMAL(10,4) | 0.0000 | 免费额度      |
| daily_call_limit         | INT           | NULL   | 每日调用限制    |
| monthly_call_limit       | INT           | NULL   | 每月调用限制    |
| free_strategy_start_time | DATETIME      | NULL   | 策略开始时间    |
| free_strategy_end_time   | DATETIME      | NULL   | 策略结束时间    |
| allowed_free_models      | TEXT          | NULL   | 允许免费的模型列表 |

### 新增数据

- 超级管理员账号: `superadmin` (密码: admin123)
- 可选管理员账号: `admin` (密码: admin123)

---

## 🚀 迁移步骤

### ⚠️ 常见错误解决

**如果报错 "Table 'user' doesn't exist"**

这是因为 user 表还未创建。请按以下步骤操作：

```bash
# 方法1：完整初始化（推荐）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ai_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p ai_gateway < src/main/resources/sql/schema.sql

# 方法2：使用安全迁移脚本（自动检查）
mysql -u root -p ai_gateway < src/main/resources/sql/migration_add_admin_safe.sql
```

### 方法一：使用安全迁移脚本（推荐）

这个脚本会自动检查表和字段是否存在，避免重复执行错误。

```bash
# 1. 备份数据库（重要！）
mysqldump -u root -p ai_gateway > backup_before_admin_$(date +%Y%m%d_%H%M%S).sql

# 2. 执行安全迁移脚本
mysql -u root -p ai_gateway < src/main/resources/sql/migration_add_admin_safe.sql

# 3. 验证迁移结果
mysql -u root -p ai_gateway -e "SELECT id, username, role FROM user WHERE role != 'USER';"
```

### 方法二：手动执行SQL

#### 步骤1: 添加字段

```sql
-- 切换到数据库
USE ai_gateway;

-- 添加角色字段
ALTER TABLE `user` 
ADD COLUMN `role` varchar(32) NOT NULL DEFAULT 'USER' 
COMMENT '用户角色：USER-普通用户，ADMIN-管理员，SUPER_ADMIN-超级管理员' 
AFTER `status`;

-- 添加免费策略字段
ALTER TABLE `user` 
ADD COLUMN `free_api_strategy` varchar(32) DEFAULT NULL 
COMMENT 'API免费策略' 
AFTER `role`;

ALTER TABLE `user` 
ADD COLUMN `free_quota` decimal(10,4) DEFAULT '0.0000' 
COMMENT '免费额度（美元）' 
AFTER `free_api_strategy`;

ALTER TABLE `user` 
ADD COLUMN `daily_call_limit` int DEFAULT NULL 
COMMENT '每日调用次数限制' 
AFTER `free_quota`;

ALTER TABLE `user` 
ADD COLUMN `monthly_call_limit` int DEFAULT NULL 
COMMENT '每月调用次数限制' 
AFTER `daily_call_limit`;

ALTER TABLE `user` 
ADD COLUMN `free_strategy_start_time` datetime DEFAULT NULL 
COMMENT '免费策略开始时间' 
AFTER `monthly_call_limit`;

ALTER TABLE `user` 
ADD COLUMN `free_strategy_end_time` datetime DEFAULT NULL 
COMMENT '免费策略结束时间' 
AFTER `free_strategy_start_time`;

ALTER TABLE `user` 
ADD COLUMN `allowed_free_models` text 
COMMENT '允许免费的模型列表（JSON格式）' 
AFTER `free_strategy_end_time`;
```

#### 步骤2: 创建超级管理员

```sql
-- 创建超级管理员账号
INSERT INTO `user` 
(`username`, `password`, `email`, `balance`, `status`, `role`) 
VALUES 
('superadmin', '0192023a7bbd73250516f069df18b500', 'superadmin@aiplatform.com', 0.0000, 1, 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'SUPER_ADMIN';

-- 可选：创建普通管理员账号
INSERT INTO `user` 
(`username`, `password`, `email`, `balance`, `status`, `role`) 
VALUES 
('admin', '0192023a7bbd73250516f069df18b500', 'admin@aiplatform.com', 0.0000, 1, 'ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'ADMIN';
```

---

## ✅ 验证迁移

### 检查字段是否添加成功

```sql
-- 查看user表结构
DESCRIBE user;

-- 应该看到新增的8个字段
```

### 检查默认账号

```sql
-- 查看所有管理员账号
SELECT id, username, email, role, status 
FROM user 
WHERE role IN ('ADMIN', 'SUPER_ADMIN');

-- 预期输出：
-- +----+------------+---------------------------+-------------+--------+
-- | id | username   | email                     | role        | status |
-- +----+------------+---------------------------+-------------+--------+
-- |  1 | superadmin | superadmin@aiplatform.com | SUPER_ADMIN |      1 |
-- |  2 | admin      | admin@aiplatform.com      | ADMIN       |      1 |
-- +----+------------+---------------------------+-------------+--------+
```

### 检查现有用户

```sql
-- 所有现有用户的角色应该都是USER
SELECT id, username, role 
FROM user 
WHERE role = 'USER';
```

---

## ⚠️ 注意事项

### 1. 备份数据库

**迁移前务必备份数据库！**

```bash
mysqldump -u root -p ai_gateway > backup_$(date +%Y%m%d).sql
```

### 2. 密码安全

默认密码是 `admin123`，首次登录后请立即修改：

```sql
-- 修改超级管理员密码（新密码: YourNewPassword123）
UPDATE user 
SET password = MD5('YourNewPassword123') 
WHERE username = 'superadmin';
```

### 3. 现有用户

迁移后，所有现有用户的角色默认为 `USER`，需要手动提升为管理员：

```sql
-- 将某个用户提升为管理员
UPDATE user 
SET role = 'ADMIN' 
WHERE username = 'your_username';
```

### 4. 应用重启

迁移完成后，需要重启应用使更改生效：

```bash
./stop.sh
./start.sh
```

---

## 🔄 回滚方案

如果迁移出现问题，可以回滚：

### 方法一：从备份恢复

```bash
mysql -u root -p ai_gateway < backup_YYYYMMDD_HHMMSS.sql
```

### 方法二：删除新增字段

```sql
-- 删除新增字段（谨慎操作！）
ALTER TABLE `user` DROP COLUMN `allowed_free_models`;
ALTER TABLE `user` DROP COLUMN `free_strategy_end_time`;
ALTER TABLE `user` DROP COLUMN `free_strategy_start_time`;
ALTER TABLE `user` DROP COLUMN `monthly_call_limit`;
ALTER TABLE `user` DROP COLUMN `daily_call_limit`;
ALTER TABLE `user` DROP COLUMN `free_quota`;
ALTER TABLE `user` DROP COLUMN `free_api_strategy`;
ALTER TABLE `user` DROP COLUMN `role`;

-- 删除管理员账号
DELETE FROM user WHERE username IN ('superadmin', 'admin');
```

---

## 🐛 常见问题

### Q1: 迁移脚本报错 "Column already exists"

**原因**: 字段已经存在  
**解决**: 跳过该字段的添加，继续执行后续SQL

### Q2: 无法登录超级管理员

**原因**: 密码错误或账号未创建  
**解决**:

```sql
-- 检查账号是否存在
SELECT * FROM user WHERE username = 'superadmin';

-- 重置密码
UPDATE user SET password = MD5('admin123') WHERE username = 'superadmin';
```

### Q3: 现有用户数据会丢失吗？

**答案**: 不会！迁移只添加新字段，不会影响现有数据

### Q4: 如何批量设置某些用户为管理员？

```sql
-- 批量设置
UPDATE user 
SET role = 'ADMIN' 
WHERE username IN ('user1', 'user2', 'user3');
```

### Q5: 迁移后应用启动失败

**原因**: 可能是实体类与数据库不同步  
**解决**:

1. 检查User.java是否包含新字段
2. 清理并重新编译: `mvn clean package`
3. 重启应用

---

## 📊 迁移检查清单

- [ ] 已备份数据库
- [ ] 已执行迁移脚本
- [ ] 已验证新字段存在
- [ ] 已验证超级管理员账号创建成功
- [ ] 已测试登录功能
- [ ] 已修改默认密码（生产环境）
- [ ] 已重启应用
- [ ] 已测试管理员接口

---

## 📞 技术支持

如遇到问题，请检查：

1. 数据库版本（建议MySQL 5.7+）
2. 应用日志文件
3. 数据库连接配置

详细文档请参考：

- [ADMIN_GUIDE.md](ADMIN_GUIDE.md)
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)

---

**最后更新**: 2026-05-12  
**版本**: v1.0.0
