-- 阶段 A：用户安全与违规记录字段（在 patch-v2 之后执行）
USE `campus_courier`;

ALTER TABLE `user`
    ADD COLUMN `violation_count` INT NOT NULL DEFAULT 0 COMMENT '违规次数' AFTER `campus_card_image_url`,
    ADD COLUMN `violation_remark` VARCHAR(500) NULL COMMENT '最近违规说明' AFTER `violation_count`;
