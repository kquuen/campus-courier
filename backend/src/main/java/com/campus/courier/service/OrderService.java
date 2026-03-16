package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.Order;
import com.campus.courier.entity.User;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    /** 发布代取需求 */
    @Transactional
    public Result<Order> publishOrder(Long publisherId, String trackingNo,
                                      String expressCompany, String pickupAddress,
                                      String deliveryAddress, String remark,
                                      BigDecimal fee, LocalDateTime expectedTime) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setPublisherId(publisherId);
        order.setTrackingNo(trackingNo);
        order.setExpressCompany(expressCompany);
        order.setPickupAddress(pickupAddress);
        order.setDeliveryAddress(deliveryAddress);
        order.setRemark(remark);
        order.setFee(fee != null ? fee : new BigDecimal("2.00"));
        order.setStatus(0);  // 待接单
        order.setExpectedTime(expectedTime);

        orderMapper.insert(order);

        return Result.ok(order);
    }

    /** 代取员接单 */
    @Transactional
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
        courier.setBalance(courier.getBalance().add(order.getFee()));
        // 信用分加0.1（完成一单）
        courier.setCreditScore(courier.getCreditScore().add(new BigDecimal("0.1")));
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

    /** 查询待接单列表（供代取员浏览） */
    public Result<IPage<Order>> listPendingOrders(int page, int size) {
        IPage<Order> result = orderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 0)
                        .orderByDesc(Order::getCreatedAt)
        );
        return Result.ok(result);
    }

    /** 查询我发布的订单 */
    public Result<List<Order>> myPublishedOrders(Long publisherId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPublisherId, publisherId)
                        .orderByDesc(Order::getCreatedAt)
        );
        return Result.ok(orders);
    }

    /** 查询我接的订单（代取员） */
    public Result<List<Order>> myCourierOrders(Long courierId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getCourierId, courierId)
                        .orderByDesc(Order::getCreatedAt)
        );
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
        Order order = orderMapper.selectById(orderId);
        return order != null ? Result.ok(order) : Result.fail("订单不存在");
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
