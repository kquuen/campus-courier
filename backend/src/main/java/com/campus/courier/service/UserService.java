package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.CourierAuditStatus;
import com.campus.courier.entity.User;
import com.campus.courier.entity.UserRole;
import com.campus.courier.mapper.UserMapper;
import com.campus.courier.util.JwtUtil;
import com.campus.courier.campus.CampusIdentityVerifier;
import com.campus.courier.dto.CourierApplicationTimelineVo;
import com.campus.courier.dto.UpdateProfileRequest;
import com.campus.courier.entity.CourierApplicationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Value("${app.security.expose-password-reset-code:false}")
    private boolean exposePasswordResetCode;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CacheService cacheService;
    private final TokenBlacklistService tokenBlacklistService;
    private final CampusIdentityVerifier campusIdentityVerifier;
    private final CourierAuditLogService courierAuditLogService;

    /** 注册（校园身份：学号 + 真实姓名） */
    public Result<?> register(String phone, String password, String studentId, String realName, String nickname) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (count > 0) {
            return Result.fail("该手机号已注册");
        }

        String campusErr = campusIdentityVerifier.verifyStudentProfile(studentId, realName);
        if (campusErr != null) {
            return Result.fail(campusErr);
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setStudentId(studentId);
        user.setRealName(realName.trim());
        user.setNickname(nickname != null && !nickname.isBlank()
                ? nickname
                : "用户" + phone.substring(Math.max(0, phone.length() - 4)));
        user.setRole(UserRole.USER);
        user.setCourierAuditStatus(CourierAuditStatus.NONE);
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

        String token = jwtUtil.generateAccessToken(user.getId(), user.getRole().getCode());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("role", user.getRole());
        data.put("nickname", user.getNickname());
        return Result.ok(data);
    }

    /** 注销 — 将 Token 加入黑名单 */
    public Result<?> logout(String token) {
        if (token != null && !token.isEmpty()) {
            tokenBlacklistService.addToBlacklist(token);
            try {
                Long uid = jwtUtil.getUserId(token);
                cacheService.deleteCache("user-profile:" + uid);
            } catch (Exception e) {
                log.warn("注销时解析用户ID失败（token可能已失效）: {}", e.getMessage());
            }
        }
        return Result.ok("已退出登录");
    }

    /** 获取个人信息 */
    public Result<User> getProfile(Long userId) {
        User cachedUser = (User) cacheService.getCache("user-profile:" + userId);
        if (cachedUser != null) {
            return Result.ok(cachedUser);
        }

        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");

        cacheService.setCache("user-profile:" + userId, user, 3600, TimeUnit.SECONDS);
        return Result.ok(user);
    }

    /** 申请成为代取员（提交后待管理员审核，通过后方可接单） */
    public Result<?> applyCourier(Long userId, String realName, String studentId, String campusCardImageUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");
        if (user.getRole() == UserRole.COURIER
                && user.getCourierAuditStatus() == CourierAuditStatus.APPROVED) {
            return Result.fail("您已是已认证代取员");
        }
        if (user.getCourierAuditStatus() == CourierAuditStatus.PENDING) {
            return Result.fail("您已有审核中的申请，请耐心等待");
        }

        String campusErr = campusIdentityVerifier.verifyStudentProfile(studentId, realName);
        if (campusErr != null) {
            return Result.fail(campusErr);
        }

        user.setRealName(realName);
        user.setStudentId(studentId);
        if (campusCardImageUrl != null && !campusCardImageUrl.isBlank()) {
            user.setCampusCardImageUrl(campusCardImageUrl);
        }
        user.setCourierAuditStatus(CourierAuditStatus.PENDING);
        user.setRole(UserRole.USER);
        userMapper.updateById(user);

        courierAuditLogService.record(userId, CourierAuditLogService.EVT_SUBMITTED, "用户提交代取员资质申请");

        cacheService.deleteCache("user-profile:" + userId);
        return Result.ok("申请已提交，请等待管理员审核");
    }

    /** 管理员：待审核代取员申请列表 */
    public Result<IPage<User>> listCourierPendingApplications(int page, int size) {
        IPage<User> result = userMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<User>()
                        .eq(User::getCourierAuditStatus, CourierAuditStatus.PENDING)
                        .orderByDesc(User::getUpdatedAt));
        return Result.ok(result);
    }

    /** 管理员：通过 / 驳回代取员申请 */
    public Result<?> auditCourierApplication(Long targetUserId, boolean approve, String rejectReason) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) return Result.fail("用户不存在");
        if (user.getCourierAuditStatus() != CourierAuditStatus.PENDING) {
            return Result.fail("该用户没有待审核的代取员申请");
        }
        if (approve) {
            user.setRole(UserRole.COURIER);
            user.setCourierAuditStatus(CourierAuditStatus.APPROVED);
            user.setIdVerified(1);
        } else {
            user.setRole(UserRole.USER);
            user.setCourierAuditStatus(CourierAuditStatus.REJECTED);
        }
        userMapper.updateById(user);
        if (approve) {
            courierAuditLogService.record(targetUserId, CourierAuditLogService.EVT_APPROVED, "管理员审核通过");
        } else {
            courierAuditLogService.record(
                    targetUserId,
                    CourierAuditLogService.EVT_REJECTED,
                    rejectReason != null && !rejectReason.isBlank() ? rejectReason : "管理员驳回");
        }
        cacheService.deleteCache("user-profile:" + targetUserId);
        String msg = approve ? "已通过代取员认证" : "已驳回申请";
        if (!approve && rejectReason != null && !rejectReason.isBlank()) {
            msg = msg + "：" + rejectReason;
        }
        return Result.ok(msg);
    }

    /** 当前用户代取申请审核进度时间线 */
    public Result<CourierApplicationTimelineVo> getCourierApplicationTimeline(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");
        CourierAuditStatus st = user.getCourierAuditStatus();
        int code = st != null ? st.getCode() : CourierAuditStatus.NONE.getCode();
        String desc = st != null ? st.getDesc() : CourierAuditStatus.NONE.getDesc();

        List<CourierApplicationLog> logs = courierAuditLogService.listByUserIdAsc(userId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<CourierApplicationTimelineVo.EventItem> items = logs.stream()
                .map(l -> CourierApplicationTimelineVo.EventItem.builder()
                        .eventType(l.getEventType())
                        .remark(l.getRemark())
                        .createdAt(l.getCreatedAt() != null ? l.getCreatedAt().format(fmt) : "")
                        .build())
                .collect(Collectors.toList());

        return Result.ok(CourierApplicationTimelineVo.builder()
                .currentStatus(code)
                .currentStatusDesc(desc)
                .events(items)
                .build());
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

        cacheService.deleteCache("user-profile:" + targetId);
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

    /** 修改昵称、头像（至少填一项） */
    public Result<?> updateProfile(Long userId, UpdateProfileRequest req) {
        boolean hasNick = req.getNickname() != null && !req.getNickname().isBlank();
        boolean hasAvatar = req.getAvatar() != null && !req.getAvatar().isBlank();
        if (!hasNick && !hasAvatar) {
            return Result.fail(400, "请至少填写昵称或头像地址中的一项");
        }
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");
        if (hasNick) {
            user.setNickname(req.getNickname().trim());
        }
        if (hasAvatar) {
            user.setAvatar(req.getAvatar().trim());
        }
        userMapper.updateById(user);
        cacheService.deleteCache("user-profile:" + userId);
        return Result.ok("资料已更新");
    }

    /** 登录态下修改密码 */
    public Result<?> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.fail("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        cacheService.deleteCache("user-profile:" + userId);
        return Result.ok("密码已修改，请妥善保管");
    }

    /** 忘记密码：下发 6 位验证码到 Redis（模拟短信） */
    public Result<Map<String, Object>> requestPasswordResetOtp(String phone) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            return Result.fail("该手机号未注册");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail("账号已被禁用");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        cacheService.setCache("pwd-reset:" + phone, code, 10, TimeUnit.MINUTES);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "验证码已发送（模拟短信，10分钟内有效）");
        if (exposePasswordResetCode) {
            data.put("debugCode", code);
        }
        return Result.ok(data);
    }

    /** 验证码重置密码 */
    public Result<?> resetPasswordByOtp(String phone, String code, String newPassword) {
        Object cached = cacheService.getCache("pwd-reset:" + phone);
        if (cached == null || !code.equals(String.valueOf(cached))) {
            return Result.fail("验证码无效或已过期");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) return Result.fail("用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        cacheService.deleteCache("pwd-reset:" + phone);
        cacheService.deleteCache("user-profile:" + user.getId());
        return Result.ok("密码已重置，请使用新密码登录");
    }

    /** 管理员分配角色 */
    public Result<?> assignUserRole(Long targetUserId, UserRole newRole) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) return Result.fail("用户不存在");

        long adminCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRole.ADMIN));
        if (target.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN
                && adminCount <= 1) {
            return Result.fail("不能撤销系统中唯一的管理员");
        }

        target.setRole(newRole);
        if (newRole == UserRole.USER) {
            target.setCourierAuditStatus(CourierAuditStatus.NONE);
        } else if (newRole == UserRole.COURIER) {
            target.setCourierAuditStatus(CourierAuditStatus.APPROVED);
            target.setIdVerified(1);
        }
        userMapper.updateById(target);
        cacheService.deleteCache("user-profile:" + targetUserId);
        return Result.ok("角色已更新");
    }

    public Result<?> recordViolation(Long targetUserId, String violationType, String description) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) return Result.fail("用户不存在");

        int currentCount = user.getViolationCount() != null ? user.getViolationCount() : 0;
        int newCount = currentCount + 1;
        
        String remark = user.getViolationRemark();
        String newRemark = String.format("[%s] %s (累计%d次)", 
                violationType, description, newCount);
        if (remark != null && !remark.isEmpty()) {
            newRemark = remark + "\n" + newRemark;
        }

        user.setViolationCount(newCount);
        user.setViolationRemark(newRemark);

        if (newCount >= 3) {
            user.setStatus(0);
            userMapper.updateById(user);
            cacheService.deleteCache("user-profile:" + targetUserId);
            return Result.ok(String.format("违规记录已添加，累计%d次，账号已自动禁用", newCount));
        }

        userMapper.updateById(user);
        cacheService.deleteCache("user-profile:" + targetUserId);
        return Result.ok(String.format("违规记录已添加，累计%d次", newCount));
    }
}
