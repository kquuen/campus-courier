NEW_FILE_CODE
-- 支付流水事件表
CREATE TABLE IF NOT EXISTS payment_event (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             payment_id BIGINT NOT NULL,
                                             event_type VARCHAR(50) NOT NULL,
    payload TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_id (payment_id)
    );

-- 结算表
CREATE TABLE IF NOT EXISTS settlement (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          order_id BIGINT NOT NULL,
                                          courier_id BIGINT NOT NULL,
                                          order_fee DECIMAL(10, 2) NOT NULL,
    platform_rate DECIMAL(5, 4) NOT NULL,
    platform_fee DECIMAL(10, 2) NOT NULL,
    courier_earn DECIMAL(10, 2) NOT NULL,
    settled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_courier_id (courier_id),
    INDEX idx_order_id (order_id)
    );