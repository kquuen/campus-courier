package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.Order;
import com.campus.courier.entity.OrderStatus;
import com.campus.courier.entity.Review;
import com.campus.courier.entity.ReviewType;
import com.campus.courier.entity.User;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.ReviewMapper;
import com.campus.courier.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final CreditScoreCalculator creditScoreCalculator;

    @Transactional
    public Result<?> submitReview(Long reviewerId, Long orderId, Integer score,
                                  String content, ReviewType type) {
        if (score < 1 || score > 5) return Result.fail("评分范围为1-5");

        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (order.getStatus() != OrderStatus.COMPLETED) return Result.fail("只有已完成的订单才能评价");

        if (type == ReviewType.USER_TO_COURIER && !order.getPublisherId().equals(reviewerId)) {
            return Result.fail("无权评价");
        }
        if (type == ReviewType.COURIER_TO_USER) {
            Long courierId = order.getCourierId();
            if (courierId == null || !courierId.equals(reviewerId)) {
                return Result.fail("无权评价");
            }
        }

        Long exists = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .eq(Review::getType, type));
        if (exists > 0) return Result.fail("已评价，不能重复提交");

        Long revieweeId = type == ReviewType.USER_TO_COURIER
                ? order.getCourierId()
                : order.getPublisherId();

        Review review = new Review();
        review.setOrderId(orderId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setScore(score);
        review.setContent(content);
        review.setType(type);
        reviewMapper.insert(review);

        updateCreditScore(revieweeId);
        return Result.ok("评价提交成功");
    }

    private void updateCreditScore(Long userId) {
        User user = userMapper.selectById(userId);
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getRevieweeId, userId));

        if (reviews.isEmpty()) return;

        List<Integer> scores = reviews.stream().map(Review::getScore).toList();
        
        long totalOrders = ordersByUser(userId);
        long completedOrders = reviews.size();
        long onTimeOrders = completedOrders;

        BigDecimal newCreditScore = creditScoreCalculator.calculateFromReviews(
                scores, (int) totalOrders, (int) completedOrders, (int) onTimeOrders);

        user.setCreditScore(newCreditScore);
        userMapper.updateById(user);
    }

    private long ordersByUser(Long userId) {
        return orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPublisherId, userId)
                        .or()
                        .eq(Order::getCourierId, userId));
    }

    public Result<List<Review>> getOrderReviews(Long orderId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderId, orderId));
        return Result.ok(reviews);
    }

    public Result<List<Review>> getUserReviews(Long userId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getRevieweeId, userId)
                        .orderByDesc(Review::getCreatedAt));
        return Result.ok(reviews);
    }

    public Result<Map<String, Object>> getCreditProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.fail("用户不存在");

        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getRevieweeId, userId)
                        .orderByDesc(Review::getCreatedAt));

        double avgScore = reviews.isEmpty() ? 0.0 :
                reviews.stream().mapToInt(Review::getScore).average().orElse(0.0);

        List<Review> recentReviews = reviews.stream().limit(10).toList();

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", userId);
        profile.put("nickname", user.getNickname());
        profile.put("creditScore", user.getCreditScore());
        profile.put("avgScore", avgScore);
        profile.put("totalReviews", reviews.size());
        profile.put("recentReviews", recentReviews);

        return Result.ok(profile);
    }
}
