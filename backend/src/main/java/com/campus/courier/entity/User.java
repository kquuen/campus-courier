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

    private UserRole role;

    private BigDecimal creditScore;

    private BigDecimal balance;

    /** 1正常 0禁用 */
    private Integer status;

    private String realName;

    private Integer idVerified;

    /** 代取员资质审核状态 */
    private CourierAuditStatus courierAuditStatus;

    /** 校园卡照片（URL，可由对象存储或后端上传接口生成） */
    private String campusCardImageUrl;

    private Integer violationCount;

    private String violationRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
