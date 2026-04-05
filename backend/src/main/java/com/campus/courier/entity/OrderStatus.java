package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING(0, "待接单"),
    ACCEPTED(1, "已接单"),
    PICKING(2, "取件中"),
    DELIVERING(6, "配送中"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    ERROR(5, "异常");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
