package com.campus.courier.controller;

import com.campus.courier.dto.*;
import com.campus.courier.entity.UserRole;
import com.campus.courier.security.RequireRole;
import com.campus.courier.service.AdminService;
import com.campus.courier.service.OrderService;
import com.campus.courier.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@RequireRole(UserRole.ADMIN)
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final OrderService orderService;

    @GetMapping("/stats")
    public Result<?> stats() {
        return adminService.dashboardStats();
    }

    @GetMapping("/stats/detail")
    public Result<?> statsDetail(@RequestParam(required = false) String from,
                                  @RequestParam(required = false) String to) {
        LocalDateTime fromTime = from != null ? LocalDateTime.parse(from) : null;
        LocalDateTime toTime = to != null ? LocalDateTime.parse(to) : null;
        return adminService.getDetailedStats(fromTime, toTime);
    }

    @GetMapping("/orders/with-details")
    public Result<?> ordersWithDetails(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) java.math.BigDecimal minFee,
                                        @RequestParam(required = false) java.math.BigDecimal maxFee,
                                        @RequestParam(required = false) String since,
                                        @RequestParam(required = false) String areaKeyword) {
        LocalDateTime sinceTime = since != null ? LocalDateTime.parse(since) : null;
        return adminService.listOrdersWithDetails(page, size, status, minFee, maxFee, sinceTime, areaKeyword);
    }

    @GetMapping("/courier-applications")
    public Result<?> courierApplications(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return userService.listCourierPendingApplications(page, size);
    }

    @PostMapping("/courier-applications/{userId}")
    public Result<?> auditCourier(@PathVariable Long userId,
                                  @Valid @RequestBody AuditCourierRequest request) {
        return userService.auditCourierApplication(
                userId,
                Boolean.TRUE.equals(request.getApprove()),
                request.getRejectReason());
    }

    @PutMapping("/users/{userId}/role")
    public Result<?> assignRole(@PathVariable Long userId,
                                @Valid @RequestBody AssignRoleRequest request) {
        int roleCode = request.getRole();
        if (roleCode < 0 || roleCode >= UserRole.values().length) {
            return Result.fail(400, "无效的角色");
        }
        UserRole role = UserRole.values()[roleCode];
        return userService.assignUserRole(userId, role);
    }

    @PostMapping("/orders/{orderId}/arbitrate")
    public Result<?> arbitrate(@PathVariable Long orderId,
                               @Valid @RequestBody ArbitrateRequest request) {
        return orderService.arbitrateOrder(orderId, request.getTargetStatus(), request.getRemark());
    }

    @PostMapping("/users/{userId}/violation")
    public Result<?> recordViolation(@PathVariable Long userId,
                                      @RequestBody ViolationRequest request) {
        return userService.recordViolation(userId, request.getViolationType(), request.getDescription());
    }
}
