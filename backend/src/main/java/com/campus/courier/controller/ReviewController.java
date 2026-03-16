package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.Result;
import com.campus.courier.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** POST /api/review/submit */
    @PostMapping("/submit")
    public Result<?> submit(@RequestBody Map<String, Object> body) {
        Long orderId  = Long.valueOf(body.get("orderId").toString());
        Integer score = (Integer) body.get("score");
        String content = (String) body.get("content");
        Integer type  = (Integer) body.get("type");  // 1用户评代取员 2代取员评用户
        return reviewService.submitReview(
                UserContext.getUserId(), orderId, score, content, type);
    }

    /** GET /api/review/order/{orderId} - 查询订单评价 */
    @GetMapping("/order/{orderId}")
    public Result<?> byOrder(@PathVariable Long orderId) {
        return reviewService.getOrderReviews(orderId);
    }

    /** GET /api/review/user/{userId} - 查询用户收到的评价 */
    @GetMapping("/user/{userId}")
    public Result<?> byUser(@PathVariable Long userId) {
        return reviewService.getUserReviews(userId);
    }
}
