package com.campus.courier.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PublishOrderRequest {

    @NotBlank(message = "快递单号不能为空")
    @Size(max = 50, message = "快递单号长度不能超过50位")
    private String trackingNo;

    @Size(max = 50, message = "快递公司名称长度不能超过50位")
    private String expressCompany;

    @NotBlank(message = "取件地址不能为空")
    @Size(max = 200, message = "取件地址长度不能超过200位")
    private String pickupAddress;

    @NotBlank(message = "送达地址不能为空")
    @Size(max = 200, message = "送达地址长度不能超过200位")
    private String deliveryAddress;

    @Size(max = 300, message = "备注长度不能超过300位")
    private String remark;

    @DecimalMin(value = "0.01", message = "代取费用必须大于0")
    @DecimalMax(value = "999.99", message = "代取费用不能超过999.99")
    private BigDecimal fee;
}
