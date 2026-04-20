package com.campus.courier.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayRequest {

    @NotNull(message = "\u8ba2\u5355ID\u4e0d\u80fd\u4e3a\u7a7a")
    private Long orderId;

    @NotNull(message = "\u652f\u4ed8\u65b9\u5f0f\u4e0d\u80fd\u4e3a\u7a7a")
    @Min(value = 1, message = "\u652f\u4ed8\u65b9\u5f0f\u65e0\u6548")
    @Max(value = 4, message = "\u652f\u4ed8\u65b9\u5f0f\u65e0\u6548")
    private Integer payType;
}
