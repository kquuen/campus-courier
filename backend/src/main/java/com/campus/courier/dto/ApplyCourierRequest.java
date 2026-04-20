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

    /** 校园卡照片 URL（可先接对象存储，或开发阶段填可访问的图片链接） */
    @Size(max = 500, message = "图片地址过长")
    private String campusCardImageUrl;
}
