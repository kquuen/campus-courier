package com.campus.courier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;

    /** 将令牌加入黑名单（使用剩余过期时间） */
    public void addToBlacklist(String token, long expirationTime) {
        if (token == null || token.isEmpty()) {
            return;
        }

        long remainingTime = expirationTime - System.currentTimeMillis();
        if (remainingTime > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token, "blacklisted",
                    remainingTime, TimeUnit.MILLISECONDS);
        }
    }

    /** 将令牌加入黑名单（默认24小时过期） */
    public void addToBlacklist(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token, "blacklisted",
                24, TimeUnit.HOURS);
    }

    /** 检查令牌是否在黑名单中 */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
