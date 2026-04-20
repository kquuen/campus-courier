-- 阶段 B：代取资质审核进度日志表（在 patch-v3 之后执行）
USE `campus_courier`;

CREATE TABLE IF NOT EXISTS `courier_application_log` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `event_type`  VARCHAR(32) NOT NULL COMMENT '事件: SUBMITTED/APPROVED/REJECTED',
    `remark`      VARCHAR(500) NULL COMMENT '备注/驳回原因',
    `created_at`  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_cal_user` (`user_id`),
    INDEX `idx_cal_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代取员申请审核进度';
