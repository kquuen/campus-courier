package com.campus.courier.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围为1-5")
    @Max(value = 5, message = "评分范围为1-5")
    private Integer score;

    @Size(max = 500, message = "评价内容不能超过500字")
    private String content;

    @NotNull(message = "评价类型不能为空")
    @Min(value = 1, message = "评价类型无效")
    @Max(value = 2, message = "评价类型无效")
    private Integer type;
}
