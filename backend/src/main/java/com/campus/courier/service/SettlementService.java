package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.Order;
import com.campus.courier.entity.Settlement;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.SettlementMapper;
import com.campus.courier.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementMapper settlementMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Value("${app.settlement.platform-rate:0.1}")
    private BigDecimal platformRate;

    @Transactional
    public BigDecimal settleOrder(Long orderId, Long courierId) {
        // 幂等检查：防止重复结算
        Long existingCount = settlementMapper.selectCount(
                new LambdaQueryWrapper<Settlement>().eq(Settlement::getOrderId, orderId));
        if (existingCount > 0) {
            throw new IllegalStateException("订单已结算，禁止重复操作");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        BigDecimal orderFee = order.getFee();
        if (orderFee == null) {
            throw new IllegalStateException("订单费用未设置，无法结算");
        }
        BigDecimal platformFee = orderFee.multiply(platformRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal courierEarn = orderFee.subtract(platformFee);
            
        if (courierEarn.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("平台抽成比例过高，代取员收益不能为负");
        }

        Settlement settlement = new Settlement();
        settlement.setOrderId(orderId);
        settlement.setCourierId(courierId);
        settlement.setOrderFee(orderFee);
        settlement.setPlatformRate(platformRate);
        settlement.setPlatformFee(platformFee);
        settlement.setCourierEarn(courierEarn);
        settlement.setSettledAt(LocalDateTime.now());
        settlementMapper.insert(settlement);
        return courierEarn;
    }

    public Result<Map<String, Object>> getEarningsSummary(Long courierId) {
        Long totalOrders = settlementMapper.selectCount(
                new LambdaQueryWrapper<Settlement>()
                        .eq(Settlement::getCourierId, courierId));

        BigDecimal totalEarn = BigDecimal.ZERO;
        if (totalOrders > 0) {
            List<Settlement> settlements = settlementMapper.selectList(
                    new LambdaQueryWrapper<Settlement>()
                            .select(Settlement::getCourierEarn)
                            .eq(Settlement::getCourierId, courierId));
            totalEarn = settlements.stream()
                    .map(Settlement::getCourierEarn)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalEarn", totalEarn);
        summary.put("totalOrders", totalOrders);
        summary.put("platformRate", platformRate);

        return Result.ok(summary);
    }

    public Result<Page<Settlement>> getEarningsLedger(Long courierId, int page, int size) {
        Page<Settlement> result = settlementMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Settlement>()
                        .eq(Settlement::getCourierId, courierId)
                        .orderByDesc(Settlement::getSettledAt));
        return Result.ok(result);
    }
}
