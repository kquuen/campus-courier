package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.Order;
import com.campus.courier.entity.Payment;
import com.campus.courier.entity.User;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.PaymentMapper;
import com.campus.courier.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    /**
     * 发起支付（模拟）
     * payType: 1=微信 2=支付宝 3=余额
     */
    @Transactional
    public Result<?> pay(Long userId, Long orderId, Integer payType) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (!order.getPublisherId().equals(userId)) return Result.fail("无权支付");
        if (order.getStatus() == 4) return Result.fail("订单已取消");

        // 检查是否已有待支付记录
        Long existing = paymentMapper.selectCount(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getPayStatus, 1));
        if (existing > 0) return Result.fail("该订单已支付");

        BigDecimal amount = order.getFee();

        if (payType == 3) {
            // 余额支付
            User user = userMapper.selectById(userId);
            if (user.getBalance().compareTo(amount) < 0) {
                return Result.fail("余额不足，请充值或选择其他支付方式");
            }
            user.setBalance(user.getBalance().subtract(amount));
            userMapper.updateById(user);
        }

        // 创建支付记录
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPayType(payType);
        payment.setPayStatus(payType == 3 ? 1 : 0);  // 余额支付直接成功，其他等回调
        payment.setThirdPartyNo(payType != 3 ? "MOCK_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase() : null);
        payment.setPaidAt(payType == 3 ? LocalDateTime.now() : null);
        paymentMapper.insert(payment);

        if (payType == 3) {
            return Result.ok("余额支付成功");
        } else {
            // 模拟返回支付跳转链接（实际项目中接入真实SDK）
            String mockPayUrl = "http://mock-pay.example.com/pay?no=" + payment.getPaymentNo()
                    + "&amount=" + amount + "&type=" + (payType == 1 ? "wechat" : "alipay");
            return Result.ok(mockPayUrl);
        }
    }

    /**
     * 模拟支付回调（实际项目中由第三方平台主动回调）
     */
    @Transactional
    public Result<?> mockCallback(String paymentNo) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) return Result.fail("支付记录不存在");
        if (payment.getPayStatus() == 1) return Result.ok("已支付，勿重复操作");

        payment.setPayStatus(1);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.updateById(payment);
        return Result.ok("支付成功（模拟回调）");
    }

    /** 查询订单支付状态 */
    public Result<?> queryStatus(Long orderId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId));
        if (payment == null) return Result.ok("未支付");
        return Result.ok(payment.getPayStatus() == 1 ? "已支付" : "待支付");
    }

    private String generatePaymentNo() {
        return "PAY" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
