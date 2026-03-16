package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.Result;
import com.campus.courier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** POST /api/user/register */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        return userService.register(
                body.get("phone"),
                body.get("password"),
                body.get("studentId"),
                body.get("nickname")
        );
    }

    /** POST /api/user/login */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        return userService.login(body.get("phone"), body.get("password"));
    }

    /** POST /api/user/logout */
    @PostMapping("/logout")
    public Result<?> logout() {
        return userService.logout(UserContext.getUserId());
    }

    /** GET /api/user/profile */
    @GetMapping("/profile")
    public Result<?> profile() {
        return userService.getProfile(UserContext.getUserId());
    }

    /** POST /api/user/apply-courier - 申请成为代取员 */
    @PostMapping("/apply-courier")
    public Result<?> applyCourier(@RequestBody Map<String, String> body) {
        return userService.applyCourier(
                UserContext.getUserId(),
                body.get("realName"),
                body.get("studentId")
        );
    }

    /** PUT /api/user/{id}/status - 管理员禁用/启用用户 */
    @PutMapping("/{id}/status")
    public Result<?> setStatus(@PathVariable Long id,
                               @RequestParam Integer status) {
        if (UserContext.getRole() != 2) return Result.forbidden();
        return userService.setStatus(id, status);
    }

    /** GET /api/user/list - 管理员查询所有用户 */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {
        if (UserContext.getRole() != 2) return Result.forbidden();
        return userService.listAll(page, size);
    }
}
