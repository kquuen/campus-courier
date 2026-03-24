package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.CompleteOrderRequest;
import com.campus.courier.dto.PublishOrderRequest;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.UserRole;
import com.campus.courier.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/publish")
    public Result<?> publish(@Valid @RequestBody PublishOrderRequest request) {
        return orderService.publishOrder(UserContext.getUserId(), request);
    }

    @PostMapping("/{id}/accept")
    public Result<?> accept(@PathVariable Long id) {
        if (!UserContext.getRole().isAtLeast(UserRole.COURIER)) {
            return Result.fail(403, "需要代取员权限");
        }
        return orderService.acceptOrder(UserContext.getUserId(), id);
    }

    @PostMapping("/{id}/pickup")
    public Result<?> pickup(@PathVariable Long id) {
        return orderService.startPickup(UserContext.getUserId(), id);
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

    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size) {
        return orderService.listPendingOrders(page, size);
    }

    @GetMapping("/recommend")
    public Result<?> recommend(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        if (!UserContext.getRole().isAtLeast(UserRole.COURIER)) return Result.forbidden();
        return orderService.smartRecommendOrders(page, size);
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
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) Integer status) {
        if (UserContext.getRole() != UserRole.ADMIN) return Result.forbidden();
        return orderService.adminListOrders(page, size, status);
    }
}
