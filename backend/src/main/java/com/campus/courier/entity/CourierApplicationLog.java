package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("courier_application_log")
public class CourierApplicationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** SUBMITTED / APPROVED / REJECTED */
    private String eventType;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
