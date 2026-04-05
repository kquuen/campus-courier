package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.*;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.PaymentMapper;
import com.campus.courier.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    /**
     * 发起支付（模拟）。返回 paymentNo；微信/支付宝需再调 callback 完成模拟支付，订单才会进入「已支付」。
     */
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Result<?> pay(Long userId, Long orderId, Integer payTypeCode) {
        if (payTypeCode == null || payTypeCode < 1 || payTypeCode > 3) {
            return Result.fail(400, "支付方式无效");
        }
        PayType payType = PayType.values()[payTypeCode - 1];

        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (!order.getPublisherId().equals(userId)) return Result.fail("无权支付");
        if (order.getStatus() == OrderStatus.CANCELLED) return Result.fail("订单已取消");

        Long existing = paymentMapper.selectCount(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getPayStatus, PayStatus.PAID));
        if (existing > 0) return Result.fail("该订单已支付");

        BigDecimal amount = order.getFee();

        if (payType == PayType.BALANCE) {
            User user = userMapper.selectById(userId);
            if (user == null) return Result.fail("用户不存在");

            BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
            if (balance.compareTo(amount) < 0) {
                return Result.fail("余额不足，请充值或选择其他支付方式");
            }
            user.setBalance(balance.subtract(amount));
            userMapper.updateById(user);
        }

        boolean isBalance = payType == PayType.BALANCE;

        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPayType(payType);
        payment.setPayStatus(isBalance ? PayStatus.PAID : PayStatus.UNPAID);
        payment.setThirdPartyNo(isBalance ? null : "MOCK_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        payment.setPaidAt(isBalance ? LocalDateTime.now() : null);
        paymentMapper.insert(payment);

        Map<String, Object> data = new HashMap<>();
        data.put("paymentNo", payment.getPaymentNo());
        data.put("paid", isBalance);
        data.put("needCallback", !isBalance);
        if (!isBalance) {
            data.put("mockPayUrl", "http://mock-pay.example.com/pay?no=" + payment.getPaymentNo()
                    + "&amount=" + amount + "&type=" + payType.getDesc());
        }
        return Result.ok(data);
    }

    /** 模拟支付回调 */
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Result<?> mockCallback(String paymentNo) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) return Result.fail("支付记录不存在");
        if (payment.getPayStatus() == PayStatus.PAID) return Result.ok("已支付，勿重复操作");

        payment.setPayStatus(PayStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.updateById(payment);
        return Result.ok("支付成功（模拟回调）");
    }

    /** 订单是否已有成功支付记录（用于接单/履约前校验） */
    public boolean isOrderPaid(Long orderId) {
        Long count = paymentMapper.selectCount(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getPayStatus, PayStatus.PAID));
        return count != null && count > 0;
    }

    /** 查询订单支付状态 */
    public Result<?> queryStatus(Long orderId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId));
        if (payment == null) return Result.ok("未支付");
        return Result.ok(payment.getPayStatus().getDesc());
    }

    private String generatePaymentNo() {
        return "PAY" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
