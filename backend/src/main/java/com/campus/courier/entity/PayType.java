package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PayType {

    WECHAT(1, "微信"),
    ALIPAY(2, "支付宝"),
    BALANCE(3, "余额"),
    CAMPUS_CARD(4, "校园卡");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    PayType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
