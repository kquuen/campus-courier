package com.campus.courier.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderAdminMapper {

    Page<Map<String, Object>> selectOrdersWithDetails(
            Page<Map<String, Object>> page,
            @Param("status") Integer status,
            @Param("minFee") BigDecimal minFee,
            @Param("maxFee") BigDecimal maxFee,
            @Param("since") LocalDateTime since,
            @Param("areaKeyword") String areaKeyword);

    List<Map<String, Object>> selectDailyOrderStats(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<Map<String, Object>> selectTopCouriers(
            @Param("limit") int limit,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<Map<String, Object>> selectPaymentSummary(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
