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

    /**
     * 0待接单 1已接单 2取件中 3已完成 4已取消 5异常
     */
    private Integer status;

    private LocalDateTime expectedTime;

    private LocalDateTime acceptedAt;

    private LocalDateTime pickedAt;

    private LocalDateTime completedAt;

    private String imageUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
