package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserRole {

    USER(0, "普通用户"),
    COURIER(1, "代取员"),
    ADMIN(2, "管理员");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    UserRole(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public boolean isAtLeast(UserRole required) {
        return this.code >= required.code;
    }
}
