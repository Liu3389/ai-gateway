-- 安全的管理员系统数据库迁移脚本
-- 包含错误检查和表存在性验证

-- 第一步：检查数据库是否存在
SELECT DATABASE()
INTO @current_db;
SELECT IF(@current_db IS NULL, '请先选择数据库: USE ai_gateway', '当前数据库: ' || @current_db) AS status;

-- 第二步：检查user表是否存在
SELECT COUNT(*)
INTO @table_exists
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'user';

-- 如果表不存在，给出提示
SELECT IF(@table_exists = 0,
          '错误: user表不存在！请先执行 schema.sql 创建基础表',
          'user表存在，开始迁移...') AS migration_status;

-- 第三步：执行迁移（仅在表存在时）
-- 注意：以下语句在表不存在时会报错，这是正常的

-- 添加角色字段
SET @dbname = DATABASE();
SET @tablename = 'user';
SET @columnname = 'role';
SET @preparedStatement = (SELECT IF(
                                         (SELECT COUNT(*)
                                          FROM INFORMATION_SCHEMA.COLUMNS
                                          WHERE (table_name = @tablename)
                                            AND (table_schema = @dbname)
                                            AND (column_name = @columnname)) > 0,
                                         'SELECT 1',
                                         CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname,
                                                '` varchar(32) NOT NULL DEFAULT \'USER\' COMMENT \'用户角色：USER-普通用户，ADMIN-管理员，SUPER_ADMIN-超级管理员\' AFTER `status`')
                                 ));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加免费策略字段
SET @columnname = 'free_api_strategy';
SET @preparedStatement = (SELECT IF(
                                         (SELECT COUNT(*)
                                          FROM INFORMATION_SCHEMA.COLUMNS
                                          WHERE (table_name = @tablename)
                                            AND (table_schema = @dbname)
                                            AND (column_name = @columnname)) > 0,
                                         'SELECT 1',
                                         CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname,
                                                '` varchar(32) DEFAULT NULL COMMENT \'API免费策略\' AFTER `role`')
                                 ));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加免费额度字段
SET @columnname = 'free_quota';
SET @preparedStatement = (SELECT IF(
                                         (SELECT COUNT(*)
                                          FROM INFORMATION_SCHEMA.COLUMNS
                                          WHERE (table_name = @tablename)
                                            AND (table_schema = @dbname)
                                            AND (column_name = @columnname)) > 0,
                                         'SELECT 1',
                                         CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname,
                                                '` decimal(10,4) DEFAULT \'0.0000\' COMMENT \'免费额度（美元）\' AFTER `free_api_strategy`')
                                 ));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加每日调用限制字段
SET @columnname = 'daily_call_limit';
SET @preparedStatement = (SELECT IF(
                                         (SELECT COUNT(*)
                                          FROM INFORMATION_SCHEMA.COLUMNS
                                          WHERE (table_name = @tablename)
                                            AND (table_schema = @dbname)
                                            AND (column_name = @columnname)) > 0,
                                         'SELECT 1',
                                         CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname,
                                                '` int DEFAULT NULL COMMENT \'每日调用次数限制\' AFTER `free_quota`')
                                 ));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加每月调用限制字段
SET @columnname = 'monthly_call_limit';
SET @preparedStatement = (SELECT IF(
                                         (SELECT COUNT(*)
                                          FROM INFORMATION_SCHEMA.COLUMNS
                                          WHERE (table_name = @tablename)
                                            AND (table_schema = @dbname)
                                            AND (column_name = @columnname)) > 0,
                                         'SELECT 1',
                                         CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname,
                                                '` int DEFAULT NULL COMMENT \'每月调用次数限制\' AFTER `daily_call_limit`')
                                 ));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加策略开始时间字段
SET @columnname = 'free_strategy_start_time';
SET @preparedStatement = (SELECT IF(
                                         (SELECT COUNT(*)
                                          FROM INFORMATION_SCHEMA.COLUMNS
                                          WHERE (table_name = @tablename)
                                            AND (table_schema = @dbname)
                                            AND (column_name = @columnname)) > 0,
                                         'SELECT 1',
                                         CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname,
                                                '` datetime DEFAULT NULL COMMENT \'免费策略开始时间\' AFTER `monthly_call_limit`')
                                 ));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加策略结束时间字段
SET @columnname = 'free_strategy_end_time';
SET @preparedStatement = (SELECT IF(
                                         (SELECT COUNT(*)
                                          FROM INFORMATION_SCHEMA.COLUMNS
                                          WHERE (table_name = @tablename)
                                            AND (table_schema = @dbname)
                                            AND (column_name = @columnname)) > 0,
                                         'SELECT 1',
                                         CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname,
                                                '` datetime DEFAULT NULL COMMENT \'免费策略结束时间\' AFTER `free_strategy_start_time`')
                                 ));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加允许免费模型字段
SET @columnname = 'allowed_free_models';
SET @preparedStatement = (SELECT IF(
                                         (SELECT COUNT(*)
                                          FROM INFORMATION_SCHEMA.COLUMNS
                                          WHERE (table_name = @tablename)
                                            AND (table_schema = @dbname)
                                            AND (column_name = @columnname)) > 0,
                                         'SELECT 1',
                                         CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @columnname,
                                                '` text COMMENT \'允许免费的模型列表（JSON格式）\' AFTER `free_strategy_end_time`')
                                 ));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 第四步：创建或更新超级管理员账号
INSERT INTO `user` (`username`, `password`, `email`, `balance`, `status`, `role`)
VALUES ('superadmin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'superadmin@aiplatform.com',
        0.0000, 1, 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'SUPER_ADMIN';

-- 可选：创建测试管理员账号
INSERT INTO `user` (`username`, `password`, `email`, `balance`, `status`, `role`)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@aiplatform.com', 0.0000, 1,
        'ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'ADMIN';

-- 第五步：验证迁移结果
SELECT '迁移完成！' AS status;
SELECT id, username, email, role, status
FROM `user`
WHERE role IN ('ADMIN', 'SUPER_ADMIN');
