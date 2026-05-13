package com.campus.courier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.courier.entity.CourierApplicationLog;
import com.campus.courier.mapper.CourierApplicationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourierAuditLogService {

    private final CourierApplicationLogMapper logMapper;

    public static final String EVT_SUBMITTED = "SUBMITTED";
    public static final String EVT_APPROVED = "APPROVED";
    public static final String EVT_REJECTED = "REJECTED";

    @Transactional
    public void record(Long userId, String eventType, String remark) {
        CourierApplicationLog row = new CourierApplicationLog();
        row.setUserId(userId);
        row.setEventType(eventType);
        row.setRemark(remark);
        logMapper.insert(row);
    }

    public List<CourierApplicationLog> listByUserIdAsc(Long userId) {
        return logMapper.selectList(
                new LambdaQueryWrapper<CourierApplicationLog>()
                        .eq(CourierApplicationLog::getUserId, userId)
                        .orderByAsc(CourierApplicationLog::getCreatedAt));
    }
}
