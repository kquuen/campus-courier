package com.campus.courier.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CourierApplicationTimelineVo {

    private int currentStatus;
    private String currentStatusDesc;
    private List<EventItem> events;

    @Data
    @Builder
    public static class EventItem {
        private String eventType;
        private String remark;
        private String createdAt;
    }
}
