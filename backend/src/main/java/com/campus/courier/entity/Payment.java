package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    /** 1微信 2支付宝 3余额 */
    private Integer payType;

    /** 0待支付 1已支付 2已退款 */
    private Integer payStatus;

    private String thirdPartyNo;

    private LocalDateTime paidAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
