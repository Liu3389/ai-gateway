-- Docker环境模型配置（不含API Key占位符，需替换真实Key）
INSERT INTO model_config (id, model_name, provider, base_url, api_key, input_price, output_price, status, create_time,
                          update_time)
VALUES (1, 'deepseek-v4-pro', 'deepseek', 'https://api.deepseek.com/v1/chat/completions', 'sk-your-deepseek-key',
        0.00014, 0.00028, 1, NOW(), NOW()),
       (2, 'deepseek-v4-flash', 'deepseek', 'https://api.deepseek.com/v1/chat/completions', 'sk-your-deepseek-key',
        0.00007, 0.00014, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE model_name=VALUES(model_name);

-- 初始管理员（密码admin123, BCrypt）
INSERT INTO user (id, username, password, email, balance, status, role)
VALUES (1, 'superadmin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'superadmin@aiplatform.com',
        100.0000, 1, 'SUPER_ADMIN')
ON DUPLICATE KEY UPDATE username=VALUES(username);
