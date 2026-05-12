CREATE DATABASE IF NOT EXISTS ai_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_gateway;

CREATE TABLE `user`
(
    `id`                       bigint         NOT NULL AUTO_INCREMENT,
    `username`                 varchar(64)    NOT NULL,
    `password`                 varchar(128)   NOT NULL,
    `email`                    varchar(128)            DEFAULT NULL,
    `balance`                  decimal(10, 4) NOT NULL DEFAULT '0.0000',
    `status`                   tinyint        NOT NULL DEFAULT '1',
    `role`                     varchar(32)    NOT NULL DEFAULT 'USER',
    `free_api_strategy`        varchar(32)             DEFAULT NULL,
    `free_quota`               decimal(10, 4)          DEFAULT '0.0000',
    `daily_call_limit`         int                     DEFAULT NULL,
    `monthly_call_limit`       int                     DEFAULT NULL,
    `free_strategy_start_time` datetime                DEFAULT NULL,
    `free_strategy_end_time`   datetime                DEFAULT NULL,
    `allowed_free_models`      text,
    `create_time`              datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`              datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `api_key`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT,
    `api_key`     varchar(64) NOT NULL,
    `user_id`     bigint      NOT NULL,
    `name`        varchar(64)          DEFAULT NULL,
    `status`      tinyint     NOT NULL DEFAULT '1',
    `rate_limit`  int         NOT NULL DEFAULT '100',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expire_time` datetime             DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_key` (`api_key`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `model_config`
(
    `id`           bigint         NOT NULL AUTO_INCREMENT,
    `model_name`   varchar(64)    NOT NULL,
    `provider`     varchar(32)    NOT NULL,
    `base_url`     varchar(256)   NOT NULL,
    `api_key`      varchar(128)   NOT NULL,
    `input_price`  decimal(10, 6) NOT NULL,
    `output_price` decimal(10, 6) NOT NULL,
    `status`       tinyint        NOT NULL DEFAULT '1',
    `create_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_name` (`model_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `call_log`
(
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `api_key`       varchar(64)    NOT NULL,
    `user_id`       bigint         NOT NULL,
    `model`         varchar(64)    NOT NULL,
    `input_tokens`  int            NOT NULL DEFAULT '0',
    `output_tokens` int            NOT NULL DEFAULT '0',
    `cost`          decimal(10, 6) NOT NULL DEFAULT '0.000000',
    `duration`      int            NOT NULL DEFAULT '0',
    `status`        tinyint        NOT NULL,
    `error_message` text,
    `create_time`   datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_api_key` (`api_key`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `billing_record`
(
    `id`             bigint         NOT NULL AUTO_INCREMENT,
    `user_id`        bigint         NOT NULL,
    `call_log_id`    bigint         NOT NULL,
    `amount`         decimal(10, 6) NOT NULL,
    `type`           tinyint        NOT NULL,
    `balance_before` decimal(10, 4) NOT NULL,
    `balance_after`  decimal(10, 4) NOT NULL,
    `create_time`    datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_call_log_id` (`call_log_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO `model_config`
VALUES (1, 'gpt-3.5-turbo', 'openai', 'https://api.openai.com/v1/chat/completions', 'sk-test', 0.001000, 0.002000, 1,
        NOW(), NOW()),
       (2, 'gpt-4o', 'openai', 'https://api.openai.com/v1/chat/completions', 'sk-test', 0.005000, 0.015000, 1, NOW(),
        NOW());

INSERT INTO `user`
VALUES (1, 'superadmin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'superadmin@aiplatform.com',
        0.0000, 1, 'SUPER_ADMIN', NULL, 0.0000, NULL, NULL, NULL, NULL, NULL, NOW(), NOW()),
       (2, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@aiplatform.com', 0.0000, 1,
        'ADMIN', NULL, 0.0000, NULL, NULL, NULL, NULL, NULL, NOW(), NOW());
