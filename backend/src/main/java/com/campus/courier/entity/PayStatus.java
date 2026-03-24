package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PayStatus {

    UNPAID(0, "待支付"),
    PAID(1, "已支付"),
    REFUNDED(2, "已退款");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    PayStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
