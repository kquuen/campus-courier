package com.campus.courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyCourierRequest {

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 20, message = "真实姓名长度不能超过20位")
    private String realName;

    @NotBlank(message = "学号不能为空")
    @Size(max = 30, message = "学号长度不能超过30位")
    private String studentId;
}
