package com.campus.courier.controller;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.Result;
import com.campus.courier.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courier/earnings")
@RequiredArgsConstructor
public class CourierEarningsController {

    private final SettlementService settlementService;

    @GetMapping("/summary")
    public Result<?> summary() {
        return settlementService.getEarningsSummary(UserContext.getUserId());
    }

    @GetMapping("/ledger")
    public Result<?> ledger(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size) {
        return settlementService.getEarningsLedger(UserContext.getUserId(), page, size);
    }
}
