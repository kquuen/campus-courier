package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.Result;
import com.campus.courier.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** POST /api/payment/pay */
    @PostMapping("/pay")
    public Result<?> pay(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Integer payType = (Integer) body.get("payType");
        return paymentService.pay(UserContext.getUserId(), orderId, payType);
    }

    /** POST /api/payment/callback/{paymentNo} - 模拟支付回调（不需要登录） */
    @PostMapping("/callback/{paymentNo}")
    public Result<?> callback(@PathVariable String paymentNo) {
        return paymentService.mockCallback(paymentNo);
    }

    /** GET /api/payment/status/{orderId} */
    @GetMapping("/status/{orderId}")
    public Result<?> status(@PathVariable Long orderId) {
        return paymentService.queryStatus(orderId);
    }
}
