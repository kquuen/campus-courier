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

    /** 1用户评代取员 2代取员评用户 */
    private Integer type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
