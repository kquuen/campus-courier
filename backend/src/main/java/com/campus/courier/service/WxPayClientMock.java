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
public class WxPayClientMock implements ThirdPartyPayClient {

    private static final String MOCK_SECRET = "wx_mock_secret_key_2024";

    @Override
    public Map<String, Object> createPrepay(String orderNo, String amount) {
        Map<String, Object> result = new HashMap<>();
        result.put("prepayId", "wx_prepay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        result.put("appId", "wx_mock_appid");
        result.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        result.put("nonceStr", UUID.randomUUID().toString().replace("-", ""));
        result.put("package", "prepay_id=wx_mock");
        result.put("signType", "HMAC-SHA256");

        String sign = generateSign(result);
        result.put("paySign", sign);

        log.info("[微信支付Mock] 生成预支付订单 orderNo={}, amount={}", orderNo, amount);
        return result;
    }

    @Override
    public boolean verifyNotify(Map<String, String> params, String signature) {
        if (signature == null || signature.isEmpty()) {
            log.warn("[微信支付Mock] 签名为空");
            return false;
        }

        String expectedSign = generateSignForVerify(params);
        boolean valid = expectedSign.equals(signature);

        if (!valid) {
            log.warn("[微信支付Mock] 验签失败: expected={}, actual={}", expectedSign, signature);
        } else {
            log.info("[微信支付Mock] 验签成功");
        }

        return valid;
    }

    @Override
    public String getChannelName() {
        return "WECHAT";
    }

    private String generateSign(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        params.entrySet().stream()
                .filter(e -> !"paySign".equals(e.getKey()) && e.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append("&"));
        sb.append("key=").append(MOCK_SECRET);

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

    private String generateSignForVerify(Map<String, String> params) {
        Map<String, Object> paramMap = new HashMap<>(params);
        return generateSign(paramMap);
    }
}