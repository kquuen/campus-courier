package com.campus.courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^(1[3-9]\\d{9}|[a-zA-Z0-9_]{3,20})$",
             message = "请输入有效的手机号或用户名（3-20位字母、数字、下划线）")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;
}
