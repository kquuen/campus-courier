package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.Order;
import com.campus.courier.entity.Review;
import com.campus.courier.entity.User;
import com.campus.courier.mapper.OrderMapper;
import com.campus.courier.mapper.ReviewMapper;
import com.campus.courier.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    /**
     * 提交评价（双向）
     * type=1：用户评代取员；type=2：代取员评用户
     */
    @Transactional
    public Result<?> submitReview(Long reviewerId, Long orderId, Integer score,
                                  String content, Integer type) {
        if (score < 1 || score > 5) return Result.fail("评分范围为1-5");

        Order order = orderMapper.selectById(orderId);
        if (order == null) return Result.fail("订单不存在");
        if (order.getStatus() != 3) return Result.fail("只有已完成的订单才能评价");

        // 检查权限
        if (type == 1 && !order.getPublisherId().equals(reviewerId)) {
            return Result.fail("无权评价");
        }
        if (type == 2 && !order.getCourierId().equals(reviewerId)) {
            return Result.fail("无权评价");
        }

        // 防止重复评价
        Long exists = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .eq(Review::getType, type));
        if (exists > 0) return Result.fail("已评价，不能重复提交");

        Long revieweeId = type == 1 ? order.getCourierId() : order.getPublisherId();

        Review review = new Review();
        review.setOrderId(orderId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setScore(score);
        review.setContent(content);
        review.setType(type);
        reviewMapper.insert(review);

        // 更新被评人信用分（加权平均算法）
        updateCreditScore(revieweeId);

        return Result.ok("评价提交成功");
    }

    /**
     * 加权信用分更新：
     * 新信用分 = 历史均分 * 0.7 + 本次评分 * 20 * 0.3
     * 范围限制在 [0, 100]
     */
    private void updateCreditScore(Long userId) {
        User user = userMapper.selectById(userId);
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getRevieweeId, userId));

        if (reviews.isEmpty()) return;

        double avg = reviews.stream()
                .mapToInt(Review::getScore)
                .average()
                .orElse(3.0);

        // 评分1-5 映射到 0-100
        double newScore = user.getCreditScore().doubleValue() * 0.7 + (avg * 20) * 0.3;
        newScore = Math.min(100, Math.max(0, newScore));

        user.setCreditScore(BigDecimal.valueOf(newScore).setScale(1, RoundingMode.HALF_UP));
        userMapper.updateById(user);
    }

    /** 查询某订单的所有评价 */
    public Result<List<Review>> getOrderReviews(Long orderId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderId, orderId));
        return Result.ok(reviews);
    }

    /** 查询某用户收到的所有评价 */
    public Result<List<Review>> getUserReviews(Long userId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getRevieweeId, userId)
                        .orderByDesc(Review::getCreatedAt));
        return Result.ok(reviews);
    }
}
