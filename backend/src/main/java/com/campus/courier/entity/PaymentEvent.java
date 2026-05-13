package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("payment_event")
public class PaymentEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paymentId;

    private String eventType;

    private String payload;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
