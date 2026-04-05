-- v2 增量补丁：在已有 campus_courier 库上执行（init 已执行过后运行本脚本一次）
USE `campus_courier`;

ALTER TABLE `user`
    ADD COLUMN `courier_audit_status` TINYINT NOT NULL DEFAULT 0
        COMMENT '代取员审核: 0未申请 1待审核 2已通过 3已驳回' AFTER `id_verified`,
    ADD COLUMN `campus_card_image_url` VARCHAR(500) NULL COMMENT '校园卡照片URL' AFTER `courier_audit_status`;

ALTER TABLE `order`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本' AFTER `image_url`,
    ADD COLUMN `appeal_reason` VARCHAR(500) NULL COMMENT '申诉原因' AFTER `version`,
    ADD COLUMN `arbitrate_remark` VARCHAR(500) NULL COMMENT '仲裁说明' AFTER `appeal_reason`;

-- 已有代取员视为已审核通过
UPDATE `user` SET `courier_audit_status` = 2 WHERE `role` = 1;
