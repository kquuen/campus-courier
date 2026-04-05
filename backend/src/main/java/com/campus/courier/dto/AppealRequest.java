package com.campus.courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppealRequest {

    @NotBlank(message = "申诉原因不能为空")
    @Size(max = 500, message = "申诉原因过长")
    private String reason;
}
