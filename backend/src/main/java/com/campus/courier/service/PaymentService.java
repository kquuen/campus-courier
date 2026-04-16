package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.*;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.PaymentEventMapper;
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
    private final PaymentEventMapper paymentEventMapper;
    private final WxPayClientMock wxPayClientMock;
    private final AlipayClientMock alipayClientMock;

    /**
     * 发起支付（模拟）。返回 paymentNo；微信/支付宝需再调 callback 完成模拟支付，订单才会进入「已支付」。
     */
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Result<?> pay(Long userId, Long orderId, Integer payTypeCode) {
        if (payTypeCode == null || payTypeCode < 1 || payTypeCode > 4) {
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
        } else if (payType == PayType.CAMPUS_CARD) {
            User user = userMapper.selectById(userId);
            if (user == null) return Result.fail("用户不存在");
            if (user.getStudentId() == null || user.getStudentId().isEmpty()) {
                return Result.fail("未绑定学号，无法使用校园卡支付");
            }
        }

        boolean isImmediatePay = payType == PayType.BALANCE || payType == PayType.CAMPUS_CARD;

        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPayType(payType);
        payment.setPayStatus(isImmediatePay ? PayStatus.PAID : PayStatus.UNPAID);
        payment.setThirdPartyNo(isImmediatePay ? null : "MOCK_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        payment.setPaidAt(isImmediatePay ? LocalDateTime.now() : null);
        paymentMapper.insert(payment);

        recordPaymentEvent(payment.getId(), PaymentEventType.CREATE, 
                String.format("{\"payType\":\"%s\",\"amount\":\"%s\"}", payType.getDesc(), amount));

        Map<String, Object> data = new HashMap<>();
        data.put("paymentNo", payment.getPaymentNo());
        data.put("paid", isImmediatePay);
        data.put("needCallback", !isImmediatePay);
        
        if (!isImmediatePay) {
            ThirdPartyPayClient client = payType == PayType.WECHAT ? wxPayClientMock : alipayClientMock;
            Map<String, Object> prepayResult = client.createPrepay(payment.getPaymentNo(), amount.toString());
            data.put("prepayParams", prepayResult);
            data.put("channel", client.getChannelName());
        }
        
        return Result.ok(data);
    }

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
        
        recordPaymentEvent(payment.getId(), PaymentEventType.CALLBACK, 
                String.format("{\"callbackTime\":\"%s\"}", LocalDateTime.now()));
        
        return Result.ok("支付成功（模拟回调）");
    }

    @Transactional
    public Result<?> wechatNotify(Map<String, String> params, String signature) {
        String paymentNo = params.get("out_trade_no");
        if (paymentNo == null) {
            return Result.fail(400, "缺少订单号");
        }

        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) {
            return Result.fail(404, "支付记录不存在");
        }

        if (!wxPayClientMock.verifyNotify(params, signature)) {
            recordPaymentEvent(payment.getId(), PaymentEventType.VERIFY_FAILED, 
                    "{\"reason\":\"invalid_signature\"}");
            return Result.fail(401, "验签失败");
        }

        if (payment.getPayStatus() == PayStatus.PAID) {
            return Result.ok("已处理");
        }

        payment.setPayStatus(PayStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.updateById(payment);
        
        recordPaymentEvent(payment.getId(), PaymentEventType.CALLBACK, 
                String.format("{\"channel\":\"WECHAT\",\"time\":\"%s\"}", LocalDateTime.now()));

        return Result.ok("success");
    }

    @Transactional
    public Result<?> alipayNotify(Map<String, String> params, String signature) {
        String paymentNo = params.get("out_trade_no");
        if (paymentNo == null) {
            return Result.fail(400, "缺少订单号");
        }

        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) {
            return Result.fail(404, "支付记录不存在");
        }

        if (!alipayClientMock.verifyNotify(params, signature)) {
            recordPaymentEvent(payment.getId(), PaymentEventType.VERIFY_FAILED, 
                    "{\"reason\":\"invalid_signature\"}");
            return Result.fail(401, "验签失败");
        }

        if (payment.getPayStatus() == PayStatus.PAID) {
            return Result.ok("已处理");
        }

        payment.setPayStatus(PayStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.updateById(payment);
        
        recordPaymentEvent(payment.getId(), PaymentEventType.CALLBACK, 
                String.format("{\"channel\":\"ALIPAY\",\"time\":\"%s\"}", LocalDateTime.now()));

        return Result.ok("success");
    }

    @Transactional
    public Result<?> refundForOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (!order.getPublisherId().equals(userId)) return Result.fail("无权操作");
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.ACCEPTED) {
            return Result.fail("仅待接单或已接单状态可申请退款取消");
        }

        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getPayStatus, PayStatus.PAID));
        if (payment == null) return Result.fail("订单未支付，无需退款");
        if (payment.getPayStatus() == PayStatus.REFUNDED) return Result.fail("订单已退款");

        if (payment.getPayType() == PayType.BALANCE) {
            User user = userMapper.selectById(userId);
            if (user == null) return Result.fail("用户不存在");
            BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
            user.setBalance(currentBalance.add(payment.getAmount()));
            userMapper.updateById(user);
        }
        // CAMPUS_CARD 支付未从 balance 扣款，不退回余额

        payment.setPayStatus(PayStatus.REFUNDED);
        paymentMapper.updateById(payment);
        
        recordPaymentEvent(payment.getId(), PaymentEventType.REFUND, 
                String.format("{\"refundTime\":\"%s\",\"amount\":\"%s\"}", LocalDateTime.now(), payment.getAmount()));

        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);

        return Result.ok("退款成功，订单已取消");
    }

    private void recordPaymentEvent(Long paymentId, PaymentEventType eventType, String payload) {
        PaymentEvent event = new PaymentEvent();
        event.setPaymentId(paymentId);
        event.setEventType(eventType.name());
        event.setPayload(payload);
        paymentEventMapper.insert(event);
    }

    /** 查询订单支付状态 */
    public Result<?> queryStatus(Long orderId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId));
        if (payment == null) return Result.ok("未支付");
        return Result.ok(payment.getPayStatus().getDesc());
    }

    /** 检查订单是否已支付 */
    public boolean isOrderPaid(Long orderId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getPayStatus, PayStatus.PAID));
        return payment != null;
    }

    private String generatePaymentNo() {
        return "PAY" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
