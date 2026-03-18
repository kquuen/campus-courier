package com.campus.courier.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String studentId;

    @JsonIgnore
    private String password;

    private String nickname;

    private String avatar;

    /** 0普通用户 1代取员 2管理员 */
    private Integer role;

    private BigDecimal creditScore;

    private BigDecimal balance;

    /** 1正常 0禁用 */
    private Integer status;

    private String realName;

    private Integer idVerified;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
