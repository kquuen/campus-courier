package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.PayRequest;
import com.campus.courier.dto.Result;
import com.campus.courier.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public Result<?> pay(@Valid @RequestBody PayRequest request) {
        return paymentService.pay(
                UserContext.getUserId(),
                request.getOrderId(),
                request.getPayType());
    }

    @PostMapping("/callback/{paymentNo}")
    public Result<?> callback(@PathVariable String paymentNo) {
        return paymentService.mockCallback(paymentNo);
    }

    @GetMapping("/status/{orderId}")
    public Result<?> status(@PathVariable Long orderId) {
        return paymentService.queryStatus(orderId);
    }
}
