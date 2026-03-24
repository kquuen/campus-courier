package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.Result;
import com.campus.courier.dto.ReviewRequest;
import com.campus.courier.entity.ReviewType;
import com.campus.courier.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/submit")
    public Result<?> submit(@Valid @RequestBody ReviewRequest request) {
        ReviewType type = ReviewType.values()[request.getType() - 1];
        return reviewService.submitReview(
                UserContext.getUserId(),
                request.getOrderId(),
                request.getScore(),
                request.getContent(),
                type);
    }

    @GetMapping("/order/{orderId}")
    public Result<?> byOrder(@PathVariable Long orderId) {
        return reviewService.getOrderReviews(orderId);
    }

    @GetMapping("/user/{userId}")
    public Result<?> byUser(@PathVariable Long userId) {
        return reviewService.getUserReviews(userId);
    }
}
