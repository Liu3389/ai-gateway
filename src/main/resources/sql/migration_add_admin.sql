-- 管理员系统数据库迁移脚本
-- 为现有user表添加角色和免费策略相关字段

-- 检查user表是否存在
-- 如果报错 "Table doesn't exist"，请先执行 schema.sql 创建基础表

-- 添加角色字段（如果不存在）
ALTER TABLE `user`
    ADD COLUMN `role` varchar(32) NOT NULL DEFAULT 'USER' COMMENT '用户角色：USER-普通用户，ADMIN-管理员，SUPER_ADMIN-超级管理员' AFTER `status`;

-- 添加API免费策略字段
ALTER TABLE `user`
    ADD COLUMN `free_api_strategy` varchar(32) DEFAULT NULL COMMENT 'API免费策略：UNLIMITED-完全免费，QUOTA_BASED-额度免费，COUNT_LIMITED-限次免费等' AFTER `role`;

ALTER TABLE `user`
    ADD COLUMN `free_quota` decimal(10, 4) DEFAULT '0.0000' COMMENT '免费额度（美元），仅当freeApiStrategy为QUOTA_BASED时有效' AFTER `free_api_strategy`;

ALTER TABLE `user`
    ADD COLUMN `daily_call_limit` int DEFAULT NULL COMMENT '每日调用次数限制，仅当freeApiStrategy为COUNT_LIMITED时有效' AFTER `free_quota`;

ALTER TABLE `user`
    ADD COLUMN `monthly_call_limit` int DEFAULT NULL COMMENT '每月调用次数限制，仅当freeApiStrategy为COUNT_LIMITED时有效' AFTER `daily_call_limit`;

ALTER TABLE `user`
    ADD COLUMN `free_strategy_start_time` datetime DEFAULT NULL COMMENT '免费策略开始时间' AFTER `monthly_call_limit`;

ALTER TABLE `user`
    ADD COLUMN `free_strategy_end_time` datetime DEFAULT NULL COMMENT '免费策略结束时间' AFTER `free_strategy_start_time`;

ALTER TABLE `user`
    ADD COLUMN `allowed_free_models` text COMMENT '允许免费的模型列表（JSON格式），仅当freeApiStrategy为MODEL_SPECIFIC时有效' AFTER `free_strategy_end_time`;

-- 创建第一个超级管理员账号（密码: admin123，使用BCrypt加密）
-- 注意：实际使用时应该修改为更安全的密码
INSERT INTO `user` (`username`, `password`, `email`, `balance`, `status`, `role`)
VALUES ('superadmin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'superadmin@aiplatform.com',
        0.0000, 1, 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'SUPER_ADMIN';

-- 可选：创建一个测试管理员账号（密码: admin123）
INSERT INTO `user` (`username`, `password`, `email`, `balance`, `status`, `role`)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@aiplatform.com', 0.0000, 1,
        'ADMIN')
ON DUPLICATE KEY UPDATE `role` = 'ADMIN';
