package com.campus.courier.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    @Value("${app.jwt.refreshExpiration}")
    private long refreshExpiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(Long userId, Integer role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析令牌
     */
    public Claims parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token不能为空");
        }

        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token已过期", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("无效的Token", e);
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public Integer getRole(String token) {
        return parseToken(token).get("role", Integer.class);
    }

    /**
     * 验证令牌有效性
     */
    public boolean isValidAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 检查是否为访问令牌（包含role claim）
            return claims.get("role") != null;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 验证刷新令牌有效性
     */
    public boolean isValidRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 刷新令牌不包含role claim
            return claims.get("role") == null;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 检查令牌是否即将过期（剩余时间小于30分钟）
     */
    public boolean isTokenExpiring(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            return remainingTime < TimeUnit.MINUTES.toMillis(30);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 验证令牌有效性（通用方法）
     */
    public boolean isValid(String token) {
        return isValidAccessToken(token);
    }
}