package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CourierAuditStatus {

    NONE(0, "未申请"),
    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回");

    @EnumValue
    @JsonValue
    private final int code;
    private final String desc;

    CourierAuditStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
