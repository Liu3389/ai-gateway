DELETE
FROM billing_record;
DELETE
FROM call_log;
DELETE
FROM api_key;
DELETE
FROM user
WHERE id NOT IN (4, 6);
ALTER TABLE billing_record
    AUTO_INCREMENT = 1;
ALTER TABLE call_log
    AUTO_INCREMENT = 1;
ALTER TABLE api_key
    AUTO_INCREMENT = 1;

INSERT IGNORE INTO model_config (model_name, provider, base_url, api_key, input_price, output_price, status,
                                 create_time, update_time)
VALUES ('deepseek-v4-flash', 'deepseek', 'https://api.deepseek.com/v1/chat/completions',
        'sk-5d6ddbf624fe45939fa6cb244ea326c6', 0.00014, 0.00028, 1, NOW(), NOW());

INSERT INTO user (id, username, password, email, balance, status, role, free_api_strategy)
VALUES (7, 'zhangsan', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'zhangsan@ai.com', 50.0000, 1,
        'ADMIN', NULL),
       (8, 'lisi', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'lisi@ai.com', 100.0000, 1, 'USER',
        NULL),
       (9, 'wangwu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'wangwu@ai.com', 20.0000, 1,
        'USER', 'UNLIMITED'),
       (10, 'zhaoliu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'zhaoliu@ai.com', 5.5000, 1,
        'USER', 'COUNT_LIMITED'),
       (11, 'sunqi', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'sunqi@ai.com', 0.0000, 0, 'USER',
        NULL),
       (12, 'devteam', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'dev@ai.com', 200.0000, 1,
        'ADMIN', 'QUOTA_BASED');

UPDATE user
SET balance=100.0000
WHERE id = 6;
UPDATE user
SET daily_call_limit=100,
    monthly_call_limit=3000
WHERE id = 10;
UPDATE user
SET free_quota=50.0000
WHERE id = 12;

INSERT INTO api_key (id, api_key, user_id, name, status, rate_limit, create_time)
VALUES (1, 'sk-2adbdb4f5ae04cacb9ea0466ac4b23f0', 6, '生产环境Key', 1, 500, NOW()),
       (2, 'sk-a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6', 7, 'zhangsan-Key', 1, 200, NOW()),
       (3, 'sk-b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7', 8, '测试Key-1', 1, 100, NOW()),
       (4, 'sk-c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8', 9, '免费Key', 1, 300, DATE_ADD(NOW(), INTERVAL 30 DAY)),
       (5, 'sk-d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9', 10, '限次Key', 1, 50, NOW()),
       (6, 'sk-e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0', 12, '开发团队Key', 0, 1000, NOW()),
       (7, 'sk-f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1', 6, '测试开发Key', 1, 200, NOW());

INSERT INTO call_log (id, api_key, user_id, model, input_tokens, output_tokens, cost, duration, status, error_message,
                      create_time)
VALUES (1, 'sk-2adbdb4f5ae04cacb9ea0466ac4b23f0', 6, 'deepseek-v4-pro', 25, 80, 0.000028, 1200, 1, NULL,
        '2026-05-12 08:00:00'),
       (2, 'sk-2adbdb4f5ae04cacb9ea0466ac4b23f0', 6, 'deepseek-v4-pro', 15, 45, 0.000015, 980, 1, NULL,
        '2026-05-12 09:30:00'),
       (3, 'sk-2adbdb4f5ae04cacb9ea0466ac4b23f0', 6, 'deepseek-v4-pro', 8, 24, 0.000008, 560, 1, NULL,
        '2026-05-12 10:15:00'),
       (4, 'sk-a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6', 7, 'deepseek-v4-flash', 40, 120, 0.000048, 1500, 1, NULL,
        '2026-05-12 08:45:00'),
       (5, 'sk-a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6', 7, 'deepseek-v4-pro', 30, 60, 0.000024, 1100, 1, NULL,
        '2026-05-12 11:00:00'),
       (6, 'sk-b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7', 8, 'deepseek-chat', 10, 35, 0.000012, 720, 0, 'timeout',
        '2026-05-12 13:00:00'),
       (7, 'sk-b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7', 8, 'deepseek-v4-pro', 20, 55, 0.000020, 1300, 1, NULL,
        '2026-05-12 14:30:00'),
       (8, 'sk-c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8', 9, 'deepseek-v4-pro', 12, 40, 0.000014, 890, 1, NULL,
        '2026-05-12 15:00:00'),
       (9, 'sk-d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9', 10, 'deepseek-v4-flash', 18, 50, 0.000020, 1050, 1, NULL,
        '2026-05-12 16:00:00'),
       (10, 'sk-2adbdb4f5ae04cacb9ea0466ac4b23f0', 6, 'deepseek-v4-pro', 5, 15, 0.000005, 400, 1, NULL,
        '2026-05-11 18:00:00'),
       (11, 'sk-a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6', 7, 'deepseek-v4-pro', 35, 90, 0.000036, 1600, 1, NULL,
        '2026-05-11 20:00:00'),
       (12, 'sk-b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7', 8, 'deepseek-chat', 22, 65, 0.000024, 950, 1, NULL,
        '2026-05-11 22:00:00');

INSERT INTO billing_record (id, user_id, call_log_id, amount, type, balance_before, balance_after, create_time)
VALUES (1, 6, 1, 0.000028, 1, 100.0000, 99.9000, '2026-05-12 08:00:01'),
       (2, 6, 2, 0.000015, 1, 99.9000, 99.8850, '2026-05-12 09:30:01'),
       (3, 6, 3, 0.000008, 1, 99.8850, 99.8770, '2026-05-12 10:15:01'),
       (4, 7, 4, 0.000048, 1, 50.0000, 49.9520, '2026-05-12 08:45:01'),
       (5, 7, 5, 0.000024, 1, 49.9520, 49.9280, '2026-05-12 11:00:01'),
       (6, 8, 6, 0.000000, 1, 100.0000, 100.0000, '2026-05-12 13:00:01'),
       (7, 8, 7, 0.000020, 1, 100.0000, 99.9800, '2026-05-12 14:30:01'),
       (8, 9, 8, 0.000014, 1, 20.0000, 20.0000, '2026-05-12 15:00:01'),
       (9, 10, 9, 0.000020, 1, 5.5000, 5.4800, '2026-05-12 16:00:01'),
       (10, 6, 10, 0.000005, 1, 99.8770, 99.8720, '2026-05-11 18:00:01'),
       (11, 7, 11, 0.000036, 1, 49.9280, 49.8920, '2026-05-11 20:00:01'),
       (12, 8, 12, 0.000024, 1, 99.9800, 99.9560, '2026-05-11 22:00:01');

SELECT 'TEST DATA READY' AS status;
