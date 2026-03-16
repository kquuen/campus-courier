package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.Result;
import com.campus.courier.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** POST /api/order/publish - 发布代取需求 */
    @PostMapping("/publish")
    public Result<?> publish(@RequestBody Map<String, Object> body) {
        String fee = (String) body.getOrDefault("fee", "2.00");
        return orderService.publishOrder(
                UserContext.getUserId(),
                (String) body.get("trackingNo"),
                (String) body.get("expressCompany"),
                (String) body.get("pickupAddress"),
                (String) body.get("deliveryAddress"),
                (String) body.get("remark"),
                new BigDecimal(fee),
                null   // expectedTime 可选，暂简化
        );
    }

    /** POST /api/order/{id}/accept - 代取员接单 */
    @PostMapping("/{id}/accept")
    public Result<?> accept(@PathVariable Long id) {
        if (UserContext.getRole() < 1) return Result.fail(403, "需要代取员权限");
        return orderService.acceptOrder(UserContext.getUserId(), id);
    }

    /** POST /api/order/{id}/pickup - 更新为取件中 */
    @PostMapping("/{id}/pickup")
    public Result<?> pickup(@PathVariable Long id) {
        return orderService.startPickup(UserContext.getUserId(), id);
    }

    /** POST /api/order/{id}/complete - 完成订单（上传凭证） */
    @PostMapping("/{id}/complete")
    public Result<?> complete(@PathVariable Long id,
                              @RequestBody Map<String, String> body) {
        return orderService.completeOrder(
                UserContext.getUserId(), id, body.get("imageUrl"));
    }

    /** POST /api/order/{id}/cancel - 取消订单 */
    @PostMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id) {
        return orderService.cancelOrder(UserContext.getUserId(), id);
    }

    /** GET /api/order/pending - 待接单列表（代取员浏览） */
    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size) {
        return orderService.listPendingOrders(page, size);
    }

    /** GET /api/order/my-published - 我发布的订单 */
    @GetMapping("/my-published")
    public Result<?> myPublished() {
        return orderService.myPublishedOrders(UserContext.getUserId());
    }

    /** GET /api/order/my-courier - 我接的订单 */
    @GetMapping("/my-courier")
    public Result<?> myCourier() {
        return orderService.myCourierOrders(UserContext.getUserId());
    }

    /** GET /api/order/{id} - 订单详情 */
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return orderService.getDetail(id);
    }

    /** GET /api/order/admin/list - 管理员查看所有订单 */
    @GetMapping("/admin/list")
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) Integer status) {
        if (UserContext.getRole() != 2) return Result.forbidden();
        return orderService.adminListOrders(page, size, status);
    }
}
