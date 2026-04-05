package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.Order;
import com.campus.courier.entity.OrderStatus;
import com.campus.courier.entity.PayStatus;
import com.campus.courier.entity.Payment;
import com.campus.courier.mapper.OrderAdminMapper;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.PaymentMapper;
import com.campus.courier.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final OrderAdminMapper orderAdminMapper;

    public Result<Map<String, Object>> dashboardStats() {
        long userCount = userMapper.selectCount(null);
        long orderCount = orderMapper.selectCount(null);
        long paidCount = paymentMapper.selectCount(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPayStatus, PayStatus.PAID));

        BigDecimal totalPaid = paymentMapper.selectList(
                        new LambdaQueryWrapper<Payment>().eq(Payment::getPayStatus, PayStatus.PAID))
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userCount);
        data.put("orderCount", orderCount);
        data.put("paidOrderCount", paidCount);
        data.put("totalPaidAmount", totalPaid);
        return Result.ok(data);
    }

    public Result<Map<String, Object>> getDetailedStats(LocalDateTime from, LocalDateTime to) {
        List<Map<String, Object>> dailyStats = orderAdminMapper.selectDailyOrderStats(from, to);
        List<Map<String, Object>> topCouriers = orderAdminMapper.selectTopCouriers(10, from, to);
        List<Map<String, Object>> paymentSummary = orderAdminMapper.selectPaymentSummary(from, to);

        long totalOrders = dailyStats.stream()
                .mapToLong(s -> ((Number) s.get("order_count")).longValue())
                .sum();
        
        long completedOrders = dailyStats.stream()
                .mapToLong(s -> ((Number) s.get("completed_count")).longValue())
                .sum();

        BigDecimal gmv = dailyStats.stream()
                .map(s -> (BigDecimal) s.get("total_fee"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long errorOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.ERROR));

        double errorRate = totalOrders > 0 ? (double) errorOrders / totalOrders * 100 : 0;

        long activeUsers = paymentSummary.stream()
                .mapToLong(s -> ((Number) s.get("payment_count")).longValue())
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("completedOrders", completedOrders);
        stats.put("errorOrders", errorOrders);
        stats.put("errorRate", String.format("%.2f%%", errorRate));
        stats.put("gmv", gmv);
        stats.put("activeUsers", activeUsers);
        stats.put("dailyStats", dailyStats);
        stats.put("topCouriers", topCouriers);
        stats.put("paymentSummary", paymentSummary);
        stats.put("periodFrom", from);
        stats.put("periodTo", to);

        return Result.ok(stats);
    }

    public Result<Page<Map<String, Object>>> listOrdersWithDetails(int page, int size,
                                                                     Integer status,
                                                                     java.math.BigDecimal minFee,
                                                                     java.math.BigDecimal maxFee,
                                                                     LocalDateTime since,
                                                                     String areaKeyword) {
        Page<Map<String, Object>> result = orderAdminMapper.selectOrdersWithDetails(
                new Page<>(page, size),
                status, minFee, maxFee, since, areaKeyword);
        return Result.ok(result);
    }
}
