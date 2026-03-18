package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.Order;
import com.campus.courier.entity.User;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.UserMapper;
import com.campus.courier.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final CacheService cacheService;

    /** 发布代取需求 - 清除缓存 */
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Result<Order> publishOrder(Long publisherId, String trackingNo,
                                      String expressCompany, String pickupAddress,
                                      String deliveryAddress, String remark,
                                      BigDecimal fee, LocalDateTime expectedTime) {
        if (trackingNo == null || trackingNo.isBlank()) {
            return Result.fail(400, "快递单号不能为空");
        }
        if (pickupAddress == null || pickupAddress.isBlank()) {
            return Result.fail(400, "取件地址不能为空");
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            return Result.fail(400, "送达地址不能为空");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setPublisherId(publisherId);
        order.setTrackingNo(trackingNo);
        order.setExpressCompany(expressCompany);
        order.setPickupAddress(pickupAddress);
        order.setDeliveryAddress(deliveryAddress);
        order.setRemark(remark);
        order.setFee(fee != null ? fee : new BigDecimal("2.00"));
        order.setStatus(0);
        order.setExpectedTime(expectedTime);

        orderMapper.insert(order);

        return Result.ok(order);
    }

    /** 代取员接单 - 清除缓存 */
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Result<?> acceptOrder(Long courierId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (order.getStatus() != 0) return Result.fail("该订单已被接单或已完成");
        if (order.getPublisherId().equals(courierId)) return Result.fail("不能接自己发布的订单");

        // 验证代取员身份
        User courier = userMapper.selectById(courierId);
        if (courier == null || courier.getRole() < 1) return Result.fail("您不是代取员");

        order.setCourierId(courierId);
        order.setStatus(1);  // 已接单
        order.setAcceptedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok("接单成功");
    }

    /** 代取员更新为取件中 */
    @Transactional
    public Result<?> startPickup(Long courierId, Long orderId) {
        Order order = getAndCheckCourier(courierId, orderId);
        if (order == null) return Result.fail("无权操作或订单不存在");
        if (order.getStatus() != 1) return Result.fail("当前订单状态不允许此操作");

        order.setStatus(2);  // 取件中
        order.setPickedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok("已更新为取件中");
    }

    /** 代取员完成取件，上传凭证 */
    @Transactional
    public Result<?> completeOrder(Long courierId, Long orderId, String imageUrl) {
        Order order = getAndCheckCourier(courierId, orderId);
        if (order == null) return Result.fail("无权操作或订单不存在");
        if (order.getStatus() != 2) return Result.fail("当前订单状态不允许此操作");

        order.setStatus(3);  // 已完成
        order.setCompletedAt(LocalDateTime.now());
        order.setImageUrl(imageUrl);
        orderMapper.updateById(order);

        // 将费用结算到代取员余额
        User courier = userMapper.selectById(courierId);
        if (courier == null) {
            return Result.fail("代取员不存在");
        }

        BigDecimal currentBalance = courier.getBalance() != null ? courier.getBalance() : BigDecimal.ZERO;
        BigDecimal currentCredit = courier.getCreditScore() != null ? courier.getCreditScore() : new BigDecimal("100.0");

        courier.setBalance(currentBalance.add(order.getFee()));
        courier.setCreditScore(currentCredit.add(new BigDecimal("0.1")));
        userMapper.updateById(courier);

        return Result.ok("订单已完成，收益已到账");
    }

    /** 取消订单（发布者取消，待接单状态才允许） */
    @Transactional
    public Result<?> cancelOrder(Long publisherId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (!order.getPublisherId().equals(publisherId)) return Result.fail("无权操作");
        if (order.getStatus() >= 1) return Result.fail("订单已在处理中，无法取消");

        order.setStatus(4);  // 已取消
        orderMapper.updateById(order);
        return Result.ok("订单已取消");
    }

    /** 查询待接单列表（供代取员浏览） - 缓存1分钟 */
    @Cacheable(value = "orders", key = "#page + '-' + #size", unless = "#result == null || #result.getRecords().isEmpty()")
    public Result<IPage<Order>> listPendingOrders(int page, int size) {
        IPage<Order> result = orderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 0)
                        .orderByDesc(Order::getCreatedAt)
        );
        return Result.ok(result);
    }

    /**
     * 智能推荐订单 - 综合评分后排序推荐
     * 匹配规则：
     * 1. 价格优先：费率高的订单优先
     * 2. 信用优先：发布人信用分高的订单优先
     * 3. 时间优先：发布时间近的订单优先
     * 综合得分 = 费率权重(30%) + 信用分权重(40%) + 时间权重(30%)
     */
    @Cacheable(value = "orders", key = "'recommend-' + #page + '-' + #size", unless = "#result == null || #result.getRecords().isEmpty()")
    public Result<IPage<Order>> smartRecommendOrders(int page, int size) {
        IPage<Order> rawOrders = orderMapper.selectPage(
                new Page<>(page, size * 2),  // 查更多数据用于排序
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 0)
                        .orderByDesc(Order::getCreatedAt)
        );

        List<Order> scoredOrders = new ArrayList<>();
        for (Order order : rawOrders.getRecords()) {
            User publisher = userMapper.selectById(order.getPublisherId());
            if (publisher == null) continue;

            // 费率得分（假设标准费率为2元，越高越好）
            BigDecimal feeRateScore = order.getFee().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(30));

            // 信用分得分（0-100映射到0-40分）
            BigDecimal creditScore = publisher.getCreditScore() != null
                    ? publisher.getCreditScore() : new BigDecimal("100");
            BigDecimal creditScoreWeighted = creditScore.multiply(BigDecimal.valueOf(0.4));

            // 时间得分（最近24小时内发布的得分更高）
            long hoursSincePublish = java.time.Duration.between(order.getCreatedAt(), LocalDateTime.now()).toHours();
            BigDecimal timeScore = BigDecimal.valueOf(40).subtract(
                    BigDecimal.valueOf(Math.min(hoursSincePublish, 48) / 48 * 30)
            );

            // 综合得分
            BigDecimal totalScore = feeRateScore.add(creditScoreWeighted).add(timeScore);
            order.setMatchScore(totalScore);
            scoredOrders.add(order);
        }

        // 按综合得分排序
        List<Order> sortedOrders = scoredOrders.stream()
                .sorted(Comparator.comparing(Order::getMatchScore).reversed())
                .limit(size)
                .collect(Collectors.toList());

        // 分页返回
        IPage<Order> result = new Page<>(page, size);
        result.setRecords(sortedOrders);
        result.setTotal((long) sortedOrders.size());
        result.setSize(size);
        result.setCurrent((long) page);

        return Result.ok(result);
    }

    /** 查询我发布的订单 */
    public Result<List<Order>> myPublishedOrders(Long publisherId) {
        // 先从缓存获取
        List<Order> cachedOrders = (List<Order>) cacheService.getCache("user-orders:" + publisherId);
        if (cachedOrders != null) {
            return Result.ok(cachedOrders);
        }

        // 缓存未命中，从数据库获取
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPublisherId, publisherId)
                        .orderByDesc(Order::getCreatedAt)
        );

        // 更新缓存
        cacheService.setCache("user-orders:" + publisherId, orders, 1800, TimeUnit.SECONDS);
        return Result.ok(orders);
    }

    /** 查询我接的订单（代取员） */
    public Result<List<Order>> myCourierOrders(Long courierId) {
        // 先从缓存获取
        List<Order> cachedOrders = (List<Order>) cacheService.getCache("courier-orders:" + courierId);
        if (cachedOrders != null) {
            return Result.ok(cachedOrders);
        }

        // 缓存未命中，从数据库获取
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getCourierId, courierId)
                        .orderByDesc(Order::getCreatedAt)
        );

        // 更新缓存
        cacheService.setCache("courier-orders:" + courierId, orders, 1800, TimeUnit.SECONDS);
        return Result.ok(orders);
    }

    /** 管理员查看所有订单 */
    public Result<IPage<Order>> adminListOrders(int page, int size, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreatedAt);
        if (status != null) wrapper.eq(Order::getStatus, status);
        return Result.ok(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /** 订单详情 */
    public Result<Order> getDetail(Long orderId) {
        // 先从缓存获取
        Order cachedOrder = (Order) cacheService.getCache("order-details:" + orderId);
        if (cachedOrder != null) {
            return Result.ok(cachedOrder);
        }

        // 缓存未命中，从数据库获取
        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");

        // 更新缓存
        cacheService.setCache("order-details:" + orderId, order, 1800, TimeUnit.SECONDS);
        return Result.ok(order);
    }

    // --- 内部工具方法 ---

    private String generateOrderNo() {
        return "CC" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private Order getAndCheckCourier(Long courierId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) return null;
        if (!courierId.equals(order.getCourierId())) return null;
        return order;
    }
}