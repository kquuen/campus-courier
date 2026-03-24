package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long reviewerId;

    private Long revieweeId;

    private Integer score;

    private String content;

    private ReviewType type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
