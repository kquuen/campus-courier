package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.User;
import com.campus.courier.mapper.UserMapper;
import com.campus.courier.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CacheService cacheService;

    /** 注册 */
    public Result<?> register(String phone, String password, String studentId, String nickname) {
        if (phone == null || password == null || studentId == null) {
            return Result.fail(400, "必填参数不能为空");
        }

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (count > 0) {
            return Result.fail("该手机号已注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setStudentId(studentId);
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : "用户" + phone.substring(Math.max(0, phone.length() - 4)));
        user.setRole(0);
        user.setStatus(1);

        userMapper.insert(user);
        return Result.ok("注册成功");
    }

    /** 登录 */
    public Result<?> login(String phone, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        if (user == null) {
            return Result.fail(400, "用户不存在");
        }
        if (user.getStatus() == 0) {
            return Result.fail(403, "账号已被禁用");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail(400, "密码错误");
        }

        String token = jwtUtil.generateAccessToken(user.getId(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("role", user.getRole());
        data.put("nickname", user.getNickname());
        return Result.ok(data);
    }

    /** 注销 */
    public Result<?> logout(Long userId) {
        return Result.ok("已退出登录");
    }

    /** 获取个人信息 */
    public Result<User> getProfile(Long userId) {
        // 先从缓存获取
        User cachedUser = (User) cacheService.getCache("user-profile:" + userId);
        if (cachedUser != null) {
            return Result.ok(cachedUser);
        }

        // 缓存未命中，从数据库获取
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");

        // 更新缓存
        cacheService.setCache("user-profile:" + userId, user, 3600, TimeUnit.SECONDS);
        return Result.ok(user);
    }

    /** 申请成为代取员 */
    public Result<?> applyCourier(Long userId, String realName, String studentId) {
        if (realName == null || realName.isBlank()) {
            return Result.fail(400, "真实姓名不能为空");
        }
        if (studentId == null || studentId.isBlank()) {
            return Result.fail(400, "学号不能为空");
        }

        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");
        if (user.getRole() >= 1) return Result.fail("已是代取员");

        user.setRealName(realName);
        user.setStudentId(studentId);
        user.setRole(1);
        user.setIdVerified(1);
        userMapper.updateById(user);
        return Result.ok("申请成功，您已成为代取员");
    }

    /** 管理员禁用/启用用户 */
    public Result<?> setStatus(Long targetId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail(400, "状态值无效");
        }

        User user = userMapper.selectById(targetId);
        if (user == null) return Result.fail("用户不存在");

        user.setStatus(status);
        userMapper.updateById(user);
        return Result.ok(status == 1 ? "已启用" : "已禁用");
    }

    public Result<IPage<User>> listAll(int page, int size) {
        IPage<User> result = userMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        return Result.ok(result);
    }

    public User findById(Long userId) {
        return userMapper.selectById(userId);
    }
}