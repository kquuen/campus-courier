package com.campus.courier.entity;

import lombok.Getter;

@Getter
public enum PaymentEventType {

    CREATE("创建支付"),
    CALLBACK("支付回调"),
    REFUND("退款"),
    VERIFY_FAILED("验签失败");

    private final String desc;

    PaymentEventType(String desc) {
        this.desc = desc;
    }
}
