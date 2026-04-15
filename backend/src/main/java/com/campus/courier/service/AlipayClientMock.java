package com.campus.courier.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AlipayClientMock implements ThirdPartyPayClient {

    private static final String MOCK_SECRET = "alipay_mock_secret_key_2024";

    @Override
    public Map<String, Object> createPrepay(String orderNo, String amount) {
        Map<String, Object> result = new HashMap<>();
        result.put("tradeNo", "alipay_trade_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        result.put("outTradeNo", orderNo);
        result.put("totalAmount", amount);
        result.put("subject", "校园代取订单");
        result.put("qrCode", "https://mock.alipay.com/qrcode/" + UUID.randomUUID());

        log.info("[支付宝Mock] 生成预支付订单 orderNo={}, amount={}", orderNo, amount);
        return result;
    }

    @Override
    public boolean verifyNotify(Map<String, String> params, String signature) {
        if (signature == null || signature.isEmpty()) {
            log.warn("[支付宝Mock] 签名为空");
            return false;
        }

        String expectedSign = generateSignForVerify(params);
        boolean valid = expectedSign.equals(signature);

        if (!valid) {
            log.warn("[支付宝Mock] 验签失败: expected={}, actual={}", expectedSign, signature);
        } else {
            log.info("[支付宝Mock] 验签成功");
        }

        return valid;
    }

    @Override
    public String getChannelName() {
        return "ALIPAY";
    }

    private String generateSignForVerify(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.entrySet().stream()
                .filter(e -> !"sign".equals(e.getKey()) && !"sign_type".equals(e.getKey()) && e.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append("&"));

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(MOCK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }
}