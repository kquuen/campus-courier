package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.AppealRequest;
import com.campus.courier.dto.CompleteOrderRequest;
import com.campus.courier.dto.PublishOrderRequest;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.UserRole;
import com.campus.courier.security.RequireRole;
import com.campus.courier.service.OrderService;
import com.campus.courier.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/publish")
    public Result<?> publish(@Valid @RequestBody PublishOrderRequest request) {
        return orderService.publishOrder(UserContext.getUserId(), request);
    }

    @PostMapping("/{id}/accept")
    public Result<?> accept(@PathVariable Long id) {
        // 权限以数据库实时角色为准（避免 JWT 未刷新导致审核通过后仍被拒）
        return orderService.acceptOrder(UserContext.getUserId(), id);
    }

    @PostMapping("/{id}/pickup")
    public Result<?> pickup(@PathVariable Long id) {
        return orderService.startPickup(UserContext.getUserId(), id);
    }

    @PostMapping("/{id}/deliver")
    public Result<?> deliver(@PathVariable Long id) {
        return orderService.startDeliver(UserContext.getUserId(), id);
    }

    @PostMapping("/{id}/complete")
    public Result<?> complete(@PathVariable Long id,
                              @RequestBody CompleteOrderRequest request) {
        return orderService.completeOrder(
                UserContext.getUserId(), id, request.getImageUrl());
    }

    @PostMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id) {
        return orderService.cancelOrder(UserContext.getUserId(), id);
    }

    @PostMapping("/{id}/refund-cancel")
    public Result<?> refundCancel(@PathVariable Long id) {
        return paymentService.refundForOrder(UserContext.getUserId(), id);
    }

    @PostMapping("/{id}/appeal")
    public Result<?> appeal(@PathVariable Long id,
                            @Valid @RequestBody AppealRequest request) {
        return orderService.submitAppeal(UserContext.getUserId(), id, request.getReason());
    }

    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) BigDecimal minFee,
                             @RequestParam(required = false) BigDecimal maxFee,
                             @RequestParam(required = false) String since,
                             @RequestParam(required = false) String areaKeyword) {
        return orderService.listPendingOrders(page, size, UserContext.getUserId(), 
                minFee, maxFee, since, areaKeyword);
    }

    @GetMapping("/recommend")
    public Result<?> recommend(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        return orderService.smartRecommendOrders(page, size, UserContext.getUserId());
    }

    @GetMapping("/my-published")
    public Result<?> myPublished() {
        return orderService.myPublishedOrders(UserContext.getUserId());
    }

    @GetMapping("/my-courier")
    public Result<?> myCourier() {
        return orderService.myCourierOrders(UserContext.getUserId());
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return orderService.getDetail(id);
    }

    @GetMapping("/admin/list")
    @RequireRole(UserRole.ADMIN)
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) Integer status,
                               @RequestParam(required = false) BigDecimal minFee,
                               @RequestParam(required = false) BigDecimal maxFee,
                               @RequestParam(required = false) String since,
                               @RequestParam(required = false) String areaKeyword) {
        return orderService.adminListOrders(page, size, status, minFee, maxFee, since, areaKeyword);
    }
}
