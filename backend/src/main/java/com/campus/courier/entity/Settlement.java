package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement")
public class Settlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long courierId;

    private BigDecimal orderFee;

    private BigDecimal platformRate;

    private BigDecimal platformFee;

    private BigDecimal courierEarn;

    private LocalDateTime settledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
