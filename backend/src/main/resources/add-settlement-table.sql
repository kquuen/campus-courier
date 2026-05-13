CREATE TABLE IF NOT EXISTS `settlement` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `courier_id` BIGINT NOT NULL,
    `order_fee` DECIMAL(8,2) NOT NULL,
    `platform_rate` DECIMAL(5,4) NOT NULL,
    `platform_fee` DECIMAL(8,2) NOT NULL,
    `courier_earn` DECIMAL(8,2) NOT NULL,
    `settled_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_settlement_order` (`order_id`),
    INDEX `idx_settlement_courier` (`courier_id`),
    INDEX `idx_settlement_settled_at` (`settled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
