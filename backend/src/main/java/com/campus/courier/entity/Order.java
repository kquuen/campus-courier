package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long publisherId;

    private Long courierId;

    private String trackingNo;

    private String expressCompany;

    private String pickupAddress;

    private String deliveryAddress;

    private String remark;

    private BigDecimal fee;

    private OrderStatus status;

    private LocalDateTime expectedTime;

    private LocalDateTime acceptedAt;

    private LocalDateTime pickedAt;

    private LocalDateTime completedAt;

    private String imageUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 智能匹配得分（不持久化，仅用于排序） */
    @TableField(exist = false)
    private transient BigDecimal matchScore;
}
