-- ============================================
-- 校园快递代取系统 H2数据库初始化脚本
-- ============================================

-- ============ 用户表 ============
CREATE TABLE IF NOT EXISTS `user` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `phone`        VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    `student_id`   VARCHAR(30) COMMENT '学号',
    `password`     VARCHAR(100) NOT NULL COMMENT '密码(bcrypt加密)',
    `nickname`     VARCHAR(50) COMMENT '昵称',
    `avatar`       VARCHAR(200) COMMENT '头像URL',
    `role`         TINYINT NOT NULL DEFAULT 0 COMMENT '角色: 0普通用户 1代取员 2管理员',
    `credit_score` DECIMAL(4,1) NOT NULL DEFAULT 100.0 COMMENT '信用分',
    `balance`      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '钱包余额',
    `status`       TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    `real_name`    VARCHAR(30) COMMENT '真实姓名（代取员认证用）',
    `id_verified`  TINYINT NOT NULL DEFAULT 0 COMMENT '是否实名认证',
    `created_at`   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS `idx_user_phone` ON `user`(`phone`);
CREATE INDEX IF NOT EXISTS `idx_user_role` ON `user`(`role`);

-- ============ 订单表 ============
CREATE TABLE IF NOT EXISTS `order` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    `order_no`        VARCHAR(32) NOT NULL UNIQUE COMMENT '订单编号',
    `publisher_id`    BIGINT NOT NULL COMMENT '发布人ID（普通用户）',
    `courier_id`      BIGINT COMMENT '代取员ID',
    `tracking_no`     VARCHAR(50) NOT NULL COMMENT '快递单号',
    `express_company` VARCHAR(50) COMMENT '快递公司',
    `pickup_address`  VARCHAR(200) NOT NULL COMMENT '取件地址（快递站）',
    `delivery_address` VARCHAR(200) NOT NULL COMMENT '送达地址（宿舍等）',
    `remark`          VARCHAR(300) COMMENT '备注',
    `fee`             DECIMAL(8,2) NOT NULL DEFAULT 2.00 COMMENT '代取费用',
    `status`          TINYINT NOT NULL DEFAULT 0
                      COMMENT '状态: 0待接单 1已接单 2取件中 3已完成 4已取消 5异常',
    `expected_time`   TIMESTAMP COMMENT '期望取件时间',
    `accepted_at`     TIMESTAMP COMMENT '接单时间',
    `picked_at`       TIMESTAMP COMMENT '取件时间',
    `completed_at`    TIMESTAMP COMMENT '完成时间',
    `image_url`       VARCHAR(200) COMMENT '取件凭证图片',
    `created_at`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS `idx_order_publisher` ON `order`(`publisher_id`);
CREATE INDEX IF NOT EXISTS `idx_order_courier` ON `order`(`courier_id`);
CREATE INDEX IF NOT EXISTS `idx_order_status` ON `order`(`status`);
CREATE INDEX IF NOT EXISTS `idx_order_no` ON `order`(`order_no`);

-- ============ 支付记录表 ============
CREATE TABLE IF NOT EXISTS `payment` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
    `payment_no`     VARCHAR(32) NOT NULL UNIQUE COMMENT '支付流水号',
    `order_id`       BIGINT NOT NULL COMMENT '关联订单ID',
    `user_id`        BIGINT NOT NULL COMMENT '支付用户ID',
    `amount`         DECIMAL(8,2) NOT NULL COMMENT '支付金额',
    `pay_type`       TINYINT NOT NULL COMMENT '支付方式: 1微信 2支付宝 3余额',
    `pay_status`     TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已退款',
    `third_party_no` VARCHAR(64) COMMENT '第三方支付流水号（模拟）',
    `paid_at`        TIMESTAMP COMMENT '支付时间',
    `created_at`     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============ 评价表 ============
CREATE TABLE IF NOT EXISTS `review` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id`    BIGINT NOT NULL COMMENT '订单ID',
    `reviewer_id` BIGINT NOT NULL COMMENT '评价人ID',
    `reviewee_id` BIGINT NOT NULL COMMENT '被评价人ID',
    `score`       TINYINT NOT NULL COMMENT '评分 1-5',
    `content`     VARCHAR(500) COMMENT '评价内容',
    `type`        TINYINT NOT NULL COMMENT '评价类型: 1用户评代取员 2代取员评用户',
    `created_at`  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS `uk_review_order_type` ON `review`(`order_id`, `type`);

-- ============ 代取员提现记录 ============
CREATE TABLE IF NOT EXISTS `withdrawal` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `courier_id` BIGINT NOT NULL COMMENT '代取员ID',
    `amount`     DECIMAL(8,2) NOT NULL COMMENT '提现金额',
    `status`     TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已到账 2已拒绝',
    `remark`     VARCHAR(200) COMMENT '备注',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `processed_at` TIMESTAMP
);