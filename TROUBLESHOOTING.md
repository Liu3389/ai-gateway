# 数据库迁移故障排除指南

## ❌ 常见错误及解决方案

---

### 错误1: Table 'ai_gateway.user' doesn't exist

**错误信息:**

```
ERROR 1146 (42S02): Table 'ai_gateway.user' doesn't exist
```

**原因:** user 表还未创建

**解决方案:**

```bash
# 步骤1: 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ai_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 步骤2: 执行完整建表脚本
mysql -u root -p ai_gateway < src/main/resources/sql/schema.sql

# 步骤3: 验证表已创建
mysql -u root -p ai_gateway -e "SHOW TABLES;"

# 步骤4: 再执行迁移脚本
mysql -u root -p ai_gateway < src/main/resources/sql/migration_add_admin_safe.sql
```

---

### 错误2: Unknown database 'ai_gateway'

**错误信息:**

```
ERROR 1049 (42000): Unknown database 'ai_gateway'
```

**原因:** 数据库不存在

**解决方案:**

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ai_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 然后重新执行迁移
mysql -u root -p ai_gateway < src/main/resources/sql/migration_add_admin_safe.sql
```

---

### 错误3: Duplicate column name 'role'

**错误信息:**

```
ERROR 1060 (42S21): Duplicate column name 'role'
```

**原因:** 字段已经存在（重复执行迁移脚本）

**解决方案:**

使用安全迁移脚本，它会自动检查字段是否存在：

```bash
mysql -u root -p ai_gateway < src/main/resources/sql/migration_add_admin_safe.sql
```

或者手动跳过已存在的字段：

```sql
-- 检查哪些字段已存在
DESCRIBE user;

-- 只添加缺失的字段
ALTER TABLE `user` ADD COLUMN `free_api_strategy` varchar(32) DEFAULT NULL AFTER `role`;
-- ... 其他缺失的字段
```

---

### 错误4: Access denied for user

**错误信息:**

```
ERROR 1045 (28000): Access denied for user 'root'@'localhost'
```

**原因:** MySQL 密码错误或权限不足

**解决方案:**

```bash
# 方法1: 确认密码正确
mysql -u root -p你的密码

# 方法2: 使用 sudo（Mac/Linux）
sudo mysql -u root

# 方法3: 重置MySQL密码
# Mac: brew services stop mysql && mysqld_safe --skip-grant-tables &
# Linux: sudo systemctl stop mysql && sudo mysqld_safe --skip-grant-tables &
```

---

### 错误5: Can't connect to MySQL server

**错误信息:**

```
ERROR 2002 (HY000): Can't connect to local MySQL server through socket
```

**原因:** MySQL 服务未启动

**解决方案:**

```bash
# Mac (Homebrew)
brew services start mysql

# Mac (官方安装包)
sudo /usr/local/mysql/support-files/mysql.server start

# Linux (systemd)
sudo systemctl start mysql

# Linux (service)
sudo service mysql start

# 检查状态
mysqladmin -u root -p status
```

---

## 🔍 诊断步骤

### 步骤1: 检查MySQL是否运行

```bash
# 检查MySQL进程
ps aux | grep mysql

# 检查端口
lsof -i :3306

# 尝试连接
mysql -u root -p -e "SELECT 1;"
```

### 步骤2: 检查数据库是否存在

```bash
mysql -u root -p -e "SHOW DATABASES;" | grep ai_gateway
```

如果不存在：

```bash
mysql -u root -p -e "CREATE DATABASE ai_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 步骤3: 检查表是否存在

```bash
mysql -u root -p ai_gateway -e "SHOW TABLES;"
```

应该看到：

```
+----------------------+
| Tables_in_ai_gateway |
+----------------------+
| api_key              |
| billing_record       |
| call_log             |
| model_config         |
| user                 |
+----------------------+
```

### 步骤4: 检查表结构

```bash
mysql -u root -p ai_gateway -e "DESCRIBE user;"
```

应该看到所有字段，包括新增的：

- role
- free_api_strategy
- free_quota
- daily_call_limit
- monthly_call_limit
- free_strategy_start_time
- free_strategy_end_time
- allowed_free_models

### 步骤5: 检查管理员账号

```bash
mysql -u root -p ai_gateway -e "SELECT id, username, role FROM user WHERE role IN ('ADMIN', 'SUPER_ADMIN');"
```

应该看到：

```
+----+------------+-------------+
| id | username   | role        |
+----+------------+-------------+
|  1 | superadmin | SUPER_ADMIN |
|  2 | admin      | ADMIN       |
+----+------------+-------------+
```

---

## 🛠️ 完整初始化流程

如果是全新安装，按以下步骤操作：

```bash
#!/bin/bash
# complete_setup.sh - 完整的数据库初始化脚本

echo "=== AI Gateway 数据库完整初始化 ==="

# 1. 创建数据库
echo "1. 创建数据库..."
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS ai_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if [ $? -eq 0 ]; then
    echo "✓ 数据库创建成功"
else
    echo "✗ 数据库创建失败"
    exit 1
fi

# 2. 执行建表脚本
echo "2. 创建数据表..."
mysql -u root -p ai_gateway < src/main/resources/sql/schema.sql

if [ $? -eq 0 ]; then
    echo "✓ 数据表创建成功"
else
    echo "✗ 数据表创建失败"
    exit 1
fi

# 3. 执行管理员系统迁移
echo "3. 执行管理员系统迁移..."
mysql -u root -p ai_gateway < src/main/resources/sql/migration_add_admin_safe.sql

if [ $? -eq 0 ]; then
    echo "✓ 管理员系统迁移成功"
else
    echo "✗ 管理员系统迁移失败"
    exit 1
fi

# 4. 验证
echo "4. 验证安装..."
echo "数据库列表:"
mysql -u root -p -e "SHOW DATABASES;" | grep ai_gateway

echo "数据表列表:"
mysql -u root -p ai_gateway -e "SHOW TABLES;"

echo "管理员账号:"
mysql -u root -p ai_gateway -e "SELECT id, username, role FROM user WHERE role IN ('ADMIN', 'SUPER_ADMIN');"

echo ""
echo "=== 初始化完成！==="
echo "默认账号:"
echo "  超级管理员: superadmin / admin123"
echo "  管理员:     admin / admin123"
echo ""
echo "请立即修改默认密码！"
```

使用方法：

```bash
chmod +x complete_setup.sh
./complete_setup.sh
```

---

## 📋 快速检查清单

遇到问题时，按顺序检查：

- [ ] MySQL 服务是否运行？
- [ ] 能否连接到 MySQL？
- [ ] ai_gateway 数据库是否存在？
- [ ] user 表是否存在？
- [ ] 表结构是否正确（8个新字段）？
- [ ] 管理员账号是否创建成功？
- [ ] 应用配置中的数据库连接是否正确？

---

## 🔧 手动修复示例

### 场景1: 只缺少某个字段

```sql
-- 检查字段是否存在
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'ai_gateway' 
AND TABLE_NAME = 'user' 
AND COLUMN_NAME = 'role';

-- 如果返回空，说明字段不存在，手动添加
ALTER TABLE `user` ADD COLUMN `role` varchar(32) NOT NULL DEFAULT 'USER' AFTER `status`;
```

### 场景2: 需要重置整个数据库

```bash
# ⚠️ 警告：这会删除所有数据！

# 1. 备份（如果需要）
mysqldump -u root -p ai_gateway > backup_$(date +%Y%m%d).sql

# 2. 删除数据库
mysql -u root -p -e "DROP DATABASE ai_gateway;"

# 3. 重新创建
mysql -u root -p -e "CREATE DATABASE ai_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 4. 执行完整初始化
mysql -u root -p ai_gateway < src/main/resources/sql/schema.sql
mysql -u root -p ai_gateway < src/main/resources/sql/migration_add_admin_safe.sql
```

### 场景3: 只想创建管理员账号

```sql
USE ai_gateway;

-- 创建超级管理员
INSERT INTO `user` (`username`, `password`, `email`, `balance`, `status`, `role`) 
VALUES ('superadmin', '0192023a7bbd73250516f069df18b500', 'superadmin@aiplatform.com', 0.0000, 1, 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'SUPER_ADMIN';

-- 创建管理员
INSERT INTO `user` (`username`, `password`, `email`, `balance`, `status`, `role`) 
VALUES ('admin', '0192023a7bbd73250516f069df18b500', 'admin@aiplatform.com', 0.0000, 1, 'ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'ADMIN';
```

---

## 📞 获取帮助

如果以上方法都无法解决问题：

1. **查看MySQL错误日志**
   ```bash
   # Mac (Homebrew)
   tail -f /usr/local/var/mysql/*.err
   
   # Linux
   tail -f /var/log/mysql/error.log
   ```

2. **检查应用日志**
   ```bash
   tail -f logs/application.log
   ```

3. **提供以下信息寻求帮助**
    - MySQL 版本: `mysql --version`
    - 操作系统: `uname -a`
    - 完整错误信息
    - 已执行的命令
    - 当前的表结构: `DESCRIBE user;`

---

## ✅ 验证成功

迁移成功后，应该能够：

```bash
# 1. 登录超级管理员
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123"}'

# 2. 查看系统统计
curl -X GET http://localhost:8080/admin/stats \
  -H "X-User-Id: 1"

# 3. 查看所有用户
curl -X GET http://localhost:8080/admin/users \
  -H "X-User-Id: 1"
```

如果这些命令都成功执行，说明迁移完全成功！🎉

---

**最后更新**: 2026-05-12  
**版本**: v1.0.0
