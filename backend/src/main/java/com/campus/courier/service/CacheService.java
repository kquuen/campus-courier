package com.campus.courier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    private static final String USER_PROFILE_CACHE = "user-profile";
    private static final String ORDER_DETAILS_CACHE = "order-details";
    private static final String ORDER_LIST_CACHE = "order-list";
    private static final String USER_LIST_CACHE = "users";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存用户信息
     */
    @Cacheable(value = USER_PROFILE_CACHE, key = "#userId")
    public Object cacheUserProfile(Long userId) {
        // 实际实现中会从数据库获取用户信息
        return null;
    }

    /**
     * 更新用户信息缓存
     */
    @CachePut(value = USER_PROFILE_CACHE, key = "#userId")
    public Object updateUserProfileCache(Long userId, Object userProfile) {
        return userProfile;
    }

    /**
     * 清除用户信息缓存
     */
    @CacheEvict(value = USER_PROFILE_CACHE, key = "#userId")
    public void evictUserProfileCache(Long userId) {
        // 清除缓存
    }

    /**
     * 缓存订单详情
     */
    @Cacheable(value = ORDER_DETAILS_CACHE, key = "#orderId")
    public Object cacheOrderDetails(Long orderId) {
        // 实际实现中会从数据库获取订单详情
        return null;
    }

    /**
     * 更新订单详情缓存
     */
    @CachePut(value = ORDER_DETAILS_CACHE, key = "#orderId")
    public Object updateOrderDetailsCache(Long orderId, Object orderDetails) {
        return orderDetails;
    }

    /**
     * 清除订单详情缓存
     */
    @CacheEvict(value = ORDER_DETAILS_CACHE, key = "#orderId")
    public void evictOrderDetailsCache(Long orderId) {
        // 清除缓存
    }

    /**
     * 缓存订单列表
     */
    @Cacheable(value = ORDER_LIST_CACHE, key = "#userId + ':' + #page + ':' + #size")
    public Object cacheOrderList(Long userId, int page, int size) {
        // 实际实现中会从数据库获取订单列表
        return null;
    }

    /**
     * 清除订单列表缓存
     */
    @CacheEvict(value = ORDER_LIST_CACHE, allEntries = true)
    public void evictOrderListCache() {
        // 清除所有订单列表缓存
    }

    /**
     * 手动设置缓存（用于复杂查询结果）
     */
    public void setCache(String key, Object value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 获取缓存
     */
    public Object getCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public void deleteCache(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 清除所有相关缓存
     */
    public void clearAllCaches() {
        redisTemplate.delete(redisTemplate.keys("campus:courier:*"));
    }
}