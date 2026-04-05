package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.ApplyCourierRequest;
import com.campus.courier.dto.ChangePasswordRequest;
import com.campus.courier.dto.ForgotPasswordRequest;
import com.campus.courier.dto.LoginRequest;
import com.campus.courier.dto.RegisterRequest;
import com.campus.courier.dto.ResetPasswordRequest;
import com.campus.courier.dto.Result;
import com.campus.courier.dto.UpdateProfileRequest;
import com.campus.courier.entity.UserRole;
import com.campus.courier.security.RequireRole;
import com.campus.courier.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(
                request.getPhone(),
                request.getPassword(),
                request.getStudentId(),
                request.getRealName(),
                request.getNickname()
        );
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request.getPhone(), request.getPassword());
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return userService.logout(token);
    }

    @GetMapping("/profile")
    public Result<?> profile() {
        return userService.getProfile(UserContext.getUserId());
    }

    @PutMapping("/profile")
    public Result<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(UserContext.getUserId(), request);
    }

    @PutMapping("/password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(
                UserContext.getUserId(),
                request.getOldPassword(),
                request.getNewPassword());
    }

    @PostMapping("/password/forgot")
    public Result<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return userService.requestPasswordResetOtp(request.getPhone());
    }

    @PostMapping("/password/reset")
    public Result<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPasswordByOtp(
                request.getPhone(),
                request.getCode(),
                request.getNewPassword());
    }

    @PostMapping("/apply-courier")
    public Result<?> applyCourier(@Valid @RequestBody ApplyCourierRequest request) {
        return userService.applyCourier(
                UserContext.getUserId(),
                request.getRealName(),
                request.getStudentId(),
                request.getCampusCardImageUrl()
        );
    }

    /** 代取资质申请审核进度（时间线） */
    @GetMapping("/courier-application/timeline")
    public Result<?> courierApplicationTimeline() {
        return userService.getCourierApplicationTimeline(UserContext.getUserId());
    }

    @PutMapping("/{id}/status")
    @RequireRole(UserRole.ADMIN)
    public Result<?> setStatus(@PathVariable Long id,
                               @RequestParam Integer status) {
        return userService.setStatus(id, status);
    }

    @GetMapping("/list")
    @RequireRole(UserRole.ADMIN)
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return userService.listAll(page, size);
    }
}
