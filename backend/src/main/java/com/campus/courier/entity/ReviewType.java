package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ReviewType {

    USER_TO_COURIER(1, "用户评代取员"),
    COURIER_TO_USER(2, "代取员评用户");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    ReviewType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
