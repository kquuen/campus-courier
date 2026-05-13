package com.campus.courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;

    @NotBlank(message = "学号不能为空")
    @Size(min = 5, max = 32, message = "学号长度须在5～32位")
    private String studentId;

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 30, message = "姓名长度须在2～30个字符")
    private String realName;

    @Size(max = 50, message = "昵称长度不能超过50位")
    private String nickname;
}
