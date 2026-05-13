package com.campus.courier.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRoleRequest {

    /** 0 普通用户 1 代取员 2 管理员 */
    @NotNull(message = "role 不能为空")
    @Min(0)
    @Max(2)
    private Integer role;
}
