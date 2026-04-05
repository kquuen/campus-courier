package com.campus.courier.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称过长")
    private String nickname;

    @Size(max = 200, message = "头像地址过长")
    private String avatar;
}
