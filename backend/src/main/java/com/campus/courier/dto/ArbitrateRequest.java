package com.campus.courier.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArbitrateRequest {

    /**
     * 仲裁后的订单状态：3=已完成 4=已取消（与 OrderStatus 编码一致）
     */
    @NotNull(message = "目标状态不能为空")
    private Integer targetStatus;

    @Size(max = 500, message = "仲裁说明过长")
    private String remark;
}
