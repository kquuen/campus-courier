package com.campus.courier.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditCourierRequest {

    @NotNull(message = "approve 不能为空")
    private Boolean approve;

    /** 驳回时可选说明 */
    private String rejectReason;
}
