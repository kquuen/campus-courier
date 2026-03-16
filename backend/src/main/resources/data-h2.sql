-- ============================================
-- 测试数据
-- ============================================

-- 管理员账号
-- 密码：admin123
INSERT INTO `user` (`phone`, `password`, `nickname`, `role`, `id_verified`) 
VALUES ('admin', 'admin123', '系统管理员', 2, 1);

-- 普通用户
-- 密码：user123
INSERT INTO `user` (`phone`, `password`, `nickname`, `student_id`, `role`, `credit_score`, `balance`) 
VALUES ('13800138000', 'user123', '张三', '20210001', 0, 100.0, 50.00);

-- 代取员
-- 密码：courier123
INSERT INTO `user` (`phone`, `password`, `nickname`, `student_id`, `role`, `credit_score`, `balance`, `real_name`, `id_verified`) 
VALUES ('13900139000', 'courier123', '李四', '20210002', 1, 98.5, 120.50, '李四', 1);

-- 测试订单
INSERT INTO `order` (`order_no`, `publisher_id`, `tracking_no`, `express_company`, `pickup_address`, `delivery_address`, `remark`, `fee`, `status`, `expected_time`) 
VALUES ('CO202403150001', 2, 'YT1234567890', '圆通速递', '学校北门快递站', '3号楼502宿舍', '快递比较大，请帮忙小心搬运', 5.00, 0, DATEADD('HOUR', 2, CURRENT_TIMESTAMP));

INSERT INTO `order` (`order_no`, `publisher_id`, `courier_id`, `tracking_no`, `express_company`, `pickup_address`, `delivery_address`, `fee`, `status`, `accepted_at`) 
VALUES ('CO202403150002', 2, 3, 'SF9876543210', '顺丰速运', '学校南门快递点', '图书馆一楼储物柜', 8.00, 1, DATEADD('MINUTE', -30, CURRENT_TIMESTAMP));

INSERT INTO `order` (`order_no`, `publisher_id`, `courier_id`, `tracking_no`, `express_company`, `pickup_address`, `delivery_address`, `fee`, `status`, `accepted_at`, `picked_at`) 
VALUES ('CO202403150003', 2, 3, 'ZT555666777', '中通快递', '学校东门菜鸟驿站', '实验楼308办公室', 3.00, 2, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), DATEADD('MINUTE', -30, CURRENT_TIMESTAMP));

-- 支付记录
INSERT INTO `payment` (`payment_no`, `order_id`, `user_id`, `amount`, `pay_type`, `pay_status`, `paid_at`) 
VALUES ('PAY202403150001', 2, 2, 8.00, 1, 1, DATEADD('MINUTE', -25, CURRENT_TIMESTAMP));

-- 评价记录
INSERT INTO `review` (`order_id`, `reviewer_id`, `reviewee_id`, `score`, `content`, `type`) 
VALUES (2, 2, 3, 5, '代取员服务很好，非常准时！', 1);

INSERT INTO `review` (`order_id`, `reviewer_id`, `reviewee_id`, `score`, `content`, `type`) 
VALUES (2, 3, 2, 4, '用户很有礼貌，沟通顺畅', 2);