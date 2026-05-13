package com.campus.courier.dto;


import lombok.Data;

@Data
public class ViolationRequest {
    private String violationType;
    private String description;
}
