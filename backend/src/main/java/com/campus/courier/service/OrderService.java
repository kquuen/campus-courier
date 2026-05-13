package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.courier.dto.PublishOrderRequest;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.CourierAuditStatus;
import com.campus.courier.entity.Order;
import com.campus.courier.entity.OrderStatus;
import com.campus.courier.entity.PayStatus;
import com.campus.courier.entity.User;
import com.campus.courier.entity.UserRole;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.UserMapper;
import com.campus.courier.config.OrderStateMachine;
import com.campus.courier.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final CacheService cacheService;
    private final PaymentService paymentService;
    private final OrderStateMachine orderStateMachine;
    private final SettlementService settlementService;

    private static final BigDecimal STANDARD_FEE = new BigDecimal("2.00");
    private static final BigDecimal DEFAULT_CREDIT = new BigDecimal("100.0");
    private static final BigDecimal CREDIT_INCREMENT = new BigDecimal("0.1");

    /** 发布代取需求 */
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Result<Order> publishOrder(Long publisherId, PublishOrderRequest request) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setPublisherId(publisherId);
        order.setTrackingNo(request.getTrackingNo());
        order.setExpressCompany(request.getExpressCompany());
        order.setPickupAddress(request.getPickupAddress());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setRemark(request.getRemark());
        order.setFee(request.getFee() != null ? request.getFee() : STANDARD_FEE);
        order.setExpectedTime(request.getExpectedTime());
        order.setStatus(OrderStatus.PENDING);

        orderMapper.insert(order);
        return Result.ok(order);
    }

    /** 代取员接单 */
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Result<?> acceptOrder(Long courierId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (order.getStatus() != OrderStatus.PENDING) return Result.fail("该订单已被接单或已完成");
        if (order.getPublisherId().equals(courierId)) return Result.fail("不能接自己发布的订单");
        if (!paymentService.isOrderPaid(orderId)) {
            return Result.fail("发单方尚未完成支付，无法接单");
        }

        User courier = userMapper.selectById(courierId);
        if (courier == null || courier.getRole() != UserRole.COURIER) {
            return Result.fail("您不是代取员");
        }
        if (courier.getCourierAuditStatus() != CourierAuditStatus.APPROVED) {
            return Result.fail("代取员资质未通过审核，无法接单");
        }

        try {
            orderStateMachine.validate(order.getStatus(), OrderStatus.ACCEPTED);
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }

        order.setCourierId(courierId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setAcceptedAt(LocalDateTime.now());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            return Result.fail("接单失败，订单可能已被他人接单，请刷新列表");
        }
        evictOrderRelatedCaches(order);
        return Result.ok("接单成功");
    }

    @Transactional
    public Result<?> startPickup(Long courierId, Long orderId) {
        Order order = getAndCheckCourier(courierId, orderId);
        if (order == null) return Result.fail("无权操作或订单不存在");
        
        try {
            orderStateMachine.validate(order.getStatus(), OrderStatus.PICKING);
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }

        order.setStatus(OrderStatus.PICKING);
        order.setPickedAt(LocalDateTime.now());
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            return Result.fail("状态已变更，请刷新后重试");
        }
        evictOrderRelatedCaches(order);
        return Result.ok("已更新为取件中");
    }

    @Transactional
    public Result<?> startDeliver(Long courierId, Long orderId) {
        Order order = getAndCheckCourier(courierId, orderId);
        if (order == null) return Result.fail("无权操作或订单不存在");
        
        try {
            orderStateMachine.validate(order.getStatus(), OrderStatus.DELIVERING);
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }

        order.setStatus(OrderStatus.DELIVERING);
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            return Result.fail("状态已变更，请刷新后重试");
        }
        evictOrderRelatedCaches(order);
        return Result.ok("已开始配送");
    }

    @Transactional
    public Result<?> completeOrder(Long courierId, Long orderId, String imageUrl) {
        Order order = getAndCheckCourier(courierId, orderId);
        if (order == null) return Result.fail("无权操作或订单不存在");
        
        try {
            orderStateMachine.validate(order.getStatus(), OrderStatus.COMPLETED);
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
        
        if (!paymentService.isOrderPaid(orderId)) {
            return Result.fail("订单未支付，无法完成履约结算");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setImageUrl(imageUrl);
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            return Result.fail("状态已变更，请刷新后重试");
        }

        BigDecimal courierEarn;
        try {
            courierEarn = settlementService.settleOrder(orderId, courierId);
        } catch (Exception e) {
            return Result.fail("结算失败: " + e.getMessage());
        }

        User courier = userMapper.selectById(courierId);
        if (courier == null) {
            return Result.fail("代取员不存在");
        }

        BigDecimal currentBalance = courier.getBalance() != null ? courier.getBalance() : BigDecimal.ZERO;
        BigDecimal currentCredit = courier.getCreditScore() != null ? courier.getCreditScore() : DEFAULT_CREDIT;

        courier.setBalance(currentBalance.add(courierEarn));
        courier.setCreditScore(currentCredit.add(CREDIT_INCREMENT));
        userMapper.updateById(courier);

        cacheService.deleteCache("user-profile:" + courierId);
        evictOrderRelatedCaches(order);
        return Result.ok("订单已完成，收益已到账");
    }

    @Transactional
    public Result<?> cancelOrder(Long publisherId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (!order.getPublisherId().equals(publisherId)) return Result.fail("无权操作");
        
        try {
            orderStateMachine.validate(order.getStatus(), OrderStatus.CANCELLED);
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
        
        if (paymentService.isOrderPaid(orderId)) {
            return Result.fail("订单已支付，请使用退款功能取消订单");
        }

        order.setStatus(OrderStatus.CANCELLED);
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            return Result.fail("操作失败，请刷新后重试");
        }
        evictOrderRelatedCaches(order);
        return Result.ok("订单已取消");
    }

    /** 查询待接单列表（供已认证代取员浏览） */
    @Cacheable(value = "orders", key = "#page + '-' + #size + '-' + #viewerId + '-' + (#minFee != null ? #minFee.toString() : 'null') + '-' + (#maxFee != null ? #maxFee.toString() : 'null') + '-' + (#since != null ? #since : 'null') + '-' + (#areaKeyword != null ? #areaKeyword : 'null')", unless = "#result == null || #result.data == null || #result.code != 200")
    public Result<IPage<Order>> listPendingOrders(int page, int size, Long viewerId,
                                                   BigDecimal minFee, BigDecimal maxFee,
                                                   String since, String areaKeyword) {
        size = Math.min(size, 100);
        if (!isApprovedCourier(viewerId)) {
            return Result.fail(403, "需要已通过资质审核的代取员");
        }
        
        LocalDateTime sinceTime = null;
        if (since != null) {
            try {
                sinceTime = LocalDateTime.parse(since);
            } catch (Exception e) {
                return Result.fail(400, "时间格式错误，请使用ISO 8601格式");
            }
        }

        IPage<Order> result = queryPendingOrdersPage(page, size, minFee, maxFee, sinceTime, areaKeyword, true);
        // Demo fallback: if there is no paid pending order, show all pending orders temporarily.
        if (result.getRecords().isEmpty()) {
            result = queryPendingOrdersPage(page, size, minFee, maxFee, sinceTime, areaKeyword, false);
        }
        return Result.ok(result);
    }

    /**
     * 智能推荐订单 — 综合评分后排序推荐
     * 综合得分 = 费率权重(30%) + 信用分权重(40%) + 时间权重(30%)
     */
    @Cacheable(value = "orders", key = "'recommend-' + #page + '-' + #size + '-' + #viewerId", unless = "#result == null || #result.data == null || #result.code != 200")
    public Result<IPage<Order>> smartRecommendOrders(int page, int size, Long viewerId) {
        size = Math.min(size, 100);
        if (!isApprovedCourier(viewerId)) {
            return Result.fail(403, "需要已通过资质审核的代取员");
        }
        IPage<Order> rawOrders = queryPendingOrdersPage(page, size * 2, null, null, null, null, true);
        // Demo fallback: if there is no paid pending order, recommend from all pending orders.
        if (rawOrders.getRecords().isEmpty()) {
            rawOrders = queryPendingOrdersPage(page, size * 2, null, null, null, null, false);
        }
        if (rawOrders.getRecords().isEmpty()) {
            IPage<Order> empty = new Page<>(page, size);
            empty.setRecords(Collections.emptyList());
            empty.setTotal(0);
            return Result.ok(empty);
        }

        // 批量查询发布者信息，避免 N+1
        Set<Long> publisherIds = rawOrders.getRecords().stream()
                .map(Order::getPublisherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = publisherIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(publisherIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Order> scoredOrders = new ArrayList<>();
        for (Order order : rawOrders.getRecords()) {
            if (order.getFee() == null || order.getCreatedAt() == null) {
                continue;
            }
            User publisher = userMap.get(order.getPublisherId());
            if (publisher == null) continue;

            // 费率 0~30：相对标准费，封顶 2 倍映射满 30 分
            double feeMult = order.getFee()
                    .divide(STANDARD_FEE, 4, RoundingMode.HALF_UP)
                    .doubleValue();
            double feeNorm = Math.min(feeMult, 2.0) / 2.0;
            BigDecimal feePart = BigDecimal.valueOf(feeNorm * 30);

            // 信用分 0~40：信用分满 100 对应 40 分
            BigDecimal creditVal = publisher.getCreditScore() != null
                    ? publisher.getCreditScore() : DEFAULT_CREDIT;
            BigDecimal creditPart = creditVal.multiply(BigDecimal.valueOf(0.4));

            // 时间 0~30：发布时间越新越高（48 小时内线性衰减）
            long hoursSincePublish = Duration.between(order.getCreatedAt(), LocalDateTime.now()).toHours();
            double timeT = Math.min(hoursSincePublish, 48) / 48.0;
            BigDecimal timePart = BigDecimal.valueOf((1.0 - timeT) * 30);

            BigDecimal totalScore = feePart.add(creditPart).add(timePart);
            order.setMatchScore(totalScore);
            scoredOrders.add(order);
        }

        List<Order> sortedOrders = scoredOrders.stream()
                .sorted(Comparator.comparing(Order::getMatchScore).reversed())
                .limit(size)
                .toList();

        IPage<Order> result = new Page<>(page, size);
        result.setRecords(sortedOrders);
        result.setTotal(rawOrders.getTotal());
        result.setSize(size);
        result.setCurrent(page);

        return Result.ok(result);
    }

    /** 查询我发布的订单 */
    public Result<List<Order>> myPublishedOrders(Long publisherId) {
        List<Order> cachedOrders = (List<Order>) cacheService.getCache("user-orders:" + publisherId);
        if (cachedOrders != null) {
            return Result.ok(cachedOrders);
        }

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPublisherId, publisherId)
                        .orderByDesc(Order::getCreatedAt)
                        .last("LIMIT 200")
        );

        cacheService.setCache("user-orders:" + publisherId, orders, 1800, TimeUnit.SECONDS);
        return Result.ok(orders);
    }

    /** 查询我接的订单（代取员） */
    public Result<List<Order>> myCourierOrders(Long courierId) {
        List<Order> cachedOrders = (List<Order>) cacheService.getCache("courier-orders:" + courierId);
        if (cachedOrders != null) {
            return Result.ok(cachedOrders);
        }

        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getCourierId, courierId)
                        .orderByDesc(Order::getCreatedAt)
                        .last("LIMIT 200")
        );

        cacheService.setCache("courier-orders:" + courierId, orders, 1800, TimeUnit.SECONDS);
        return Result.ok(orders);
    }

    /** 查询我的订单（发单+接单） */
    public Result<IPage<Order>> listMyOrders(int page, int size, Long userId,
                                              Integer status, String role) {
        size = Math.min(size, 100);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>();

        // 根据 role 筛选：publisher=我发布的, courier=我接单的, 默认两者都查
        if ("publisher".equalsIgnoreCase(role)) {
            wrapper.eq(Order::getPublisherId, userId);
        } else if ("courier".equalsIgnoreCase(role)) {
            wrapper.eq(Order::getCourierId, userId);
        } else {
            wrapper.and(w -> w.eq(Order::getPublisherId, userId)
                    .or().eq(Order::getCourierId, userId));
        }

        if (status != null) {
            for (OrderStatus s : OrderStatus.values()) {
                if (s.getCode() == status) {
                    wrapper.eq(Order::getStatus, s);
                    break;
                }
            }
        }

        wrapper.orderByDesc(Order::getCreatedAt);
        return Result.ok(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 管理员查看所有订单 */
    public Result<IPage<Order>> adminListOrders(int page, int size, Integer status,
                                                 BigDecimal minFee, BigDecimal maxFee,
                                                 String since, String areaKeyword) {
        size = Math.min(size, 100);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>();
        
        if (status != null) {
            for (OrderStatus s : OrderStatus.values()) {
                if (s.getCode() == status) {
                    wrapper.eq(Order::getStatus, s);
                    break;
                }
            }
        }
        
        if (minFee != null) {
            wrapper.ge(Order::getFee, minFee);
        }
        if (maxFee != null) {
            wrapper.le(Order::getFee, maxFee);
        }
        if (since != null) {
            try {
                LocalDateTime sinceTime = LocalDateTime.parse(since);
                wrapper.ge(Order::getCreatedAt, sinceTime);
            } catch (Exception e) {
                return Result.fail(400, "时间格式错误，请使用ISO 8601格式");
            }
        }
        if (areaKeyword != null && !areaKeyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Order::getPickupAddress, areaKeyword)
                    .or().like(Order::getDeliveryAddress, areaKeyword));
        }
        
        wrapper.orderByDesc(Order::getCreatedAt);
        return Result.ok(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 发单方/接单方：异常申诉（进入异常状态，由管理员仲裁） */
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Result<?> submitAppeal(Long userId, Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        boolean publisher = userId.equals(order.getPublisherId());
        boolean courier = order.getCourierId() != null && userId.equals(order.getCourierId());
        if (!publisher && !courier) return Result.fail("无权发起申诉");

        OrderStatus st = order.getStatus();
        if (st == OrderStatus.PENDING || st == OrderStatus.CANCELLED || st == OrderStatus.ERROR) {
            return Result.fail("当前状态不可发起申诉");
        }

        try {
            orderStateMachine.validate(st, OrderStatus.ERROR);
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }

        order.setStatus(OrderStatus.ERROR);
        order.setAppealReason(reason);
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            return Result.fail("操作失败，请刷新后重试");
        }
        evictOrderRelatedCaches(order);
        return Result.ok("申诉已提交，等待管理员处理");
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Result<?> arbitrateOrder(Long orderId, Integer targetStatusCode, String remark) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (order.getStatus() != OrderStatus.ERROR) {
            return Result.fail("仅异常/申诉中的订单可仲裁");
        }
        OrderStatus next = null;
        for (OrderStatus s : OrderStatus.values()) {
            if (s.getCode() == targetStatusCode) {
                next = s;
                break;
            }
        }
        if (next != OrderStatus.COMPLETED && next != OrderStatus.CANCELLED) {
            return Result.fail("仲裁目标状态仅支持：已完成(" + OrderStatus.COMPLETED.getCode() + ") 或 已取消(" + OrderStatus.CANCELLED.getCode() + ")");
        }
        
        try {
            orderStateMachine.validate(order.getStatus(), next);
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
        
        order.setStatus(next);
        order.setArbitrateRemark(remark);
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            return Result.fail("操作失败，请刷新后重试");
        }
        evictOrderRelatedCaches(order);
        return Result.ok("仲裁处理完成");
    }

    /** 订单详情（含权限校验） */
    public Result<Order> getDetail(Long userId, Long orderId) {
        Order cachedOrder = (Order) cacheService.getCache("order-details:" + orderId);
        if (cachedOrder != null) {
            if (!canViewOrderDetail(userId, cachedOrder)) {
                return Result.fail(403, "无权查看此订单");
            }
            return Result.ok(cachedOrder);
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");

        if (!canViewOrderDetail(userId, order)) {
            return Result.fail(403, "无权查看此订单");
        }

        cacheService.setCache("order-details:" + orderId, order, 1800, TimeUnit.SECONDS);
        return Result.ok(order);
    }

    private String generateOrderNo() {
        return "CC" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private Order getAndCheckCourier(Long courierId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !courierId.equals(order.getCourierId())) return null;
        return order;
    }

    /** 仅展示「已支付」的待接单订单，与接单规则一致 */
    private LambdaQueryWrapper<Order> paidPendingWrapper() {
        return pendingWrapper()
                .apply("EXISTS (SELECT 1 FROM payment p WHERE p.order_id = `order`.id AND p.pay_status = {0})",
                        PayStatus.PAID.getCode());
    }

    private LambdaQueryWrapper<Order> pendingWrapper() {
        return new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING);
    }

    private IPage<Order> queryPendingOrdersPage(int page, int size,
                                                BigDecimal minFee, BigDecimal maxFee,
                                                LocalDateTime sinceTime, String areaKeyword,
                                                boolean paidOnly) {
        LambdaQueryWrapper<Order> wrapper = paidOnly ? paidPendingWrapper() : pendingWrapper();
        if (minFee != null) {
            wrapper.ge(Order::getFee, minFee);
        }
        if (maxFee != null) {
            wrapper.le(Order::getFee, maxFee);
        }
        if (sinceTime != null) {
            wrapper.ge(Order::getCreatedAt, sinceTime);
        }
        if (areaKeyword != null && !areaKeyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Order::getPickupAddress, areaKeyword)
                    .or().like(Order::getDeliveryAddress, areaKeyword));
        }

        return orderMapper.selectPage(new Page<>(page, size), wrapper.orderByDesc(Order::getCreatedAt));
    }

    private boolean isApprovedCourier(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null
                && u.getRole() == UserRole.COURIER
                && u.getCourierAuditStatus() == CourierAuditStatus.APPROVED;
    }

    private boolean isAdmin(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null && u.getRole() == UserRole.ADMIN;
    }

    private boolean canViewOrderDetail(Long userId, Order order) {
        if (userId == null || order == null) {
            return false;
        }
        if (userId.equals(order.getPublisherId())
                || userId.equals(order.getCourierId())
                || isAdmin(userId)) {
            return true;
        }
        return order.getStatus() == OrderStatus.PENDING && isApprovedCourier(userId);
    }
    private void evictOrderRelatedCaches(Order order) {
        if (order.getId() != null) {
            cacheService.deleteCache("order-details:" + order.getId());
        }
        if (order.getPublisherId() != null) {
            cacheService.deleteCache("user-orders:" + order.getPublisherId());
        }
        if (order.getCourierId() != null) {
            cacheService.deleteCache("courier-orders:" + order.getCourierId());
        }
    }
}
