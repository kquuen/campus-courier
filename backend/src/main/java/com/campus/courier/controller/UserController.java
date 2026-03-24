package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.ApplyCourierRequest;
import com.campus.courier.dto.LoginRequest;
import com.campus.courier.dto.RegisterRequest;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.UserRole;
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

    @PostMapping("/apply-courier")
    public Result<?> applyCourier(@Valid @RequestBody ApplyCourierRequest request) {
        return userService.applyCourier(
                UserContext.getUserId(),
                request.getRealName(),
                request.getStudentId()
        );
    }

    @PutMapping("/{id}/status")
    public Result<?> setStatus(@PathVariable Long id,
                               @RequestParam Integer status) {
        if (UserContext.getRole() != UserRole.ADMIN) return Result.forbidden();
        return userService.setStatus(id, status);
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {
        if (UserContext.getRole() != UserRole.ADMIN) return Result.forbidden();
        return userService.listAll(page, size);
    }
}
