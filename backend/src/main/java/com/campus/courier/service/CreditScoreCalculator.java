package com.campus.courier.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Component
public class CreditScoreCalculator {

    private static final double COMPLETION_RATE_WEIGHT = 0.3;
    private static final double AVG_SCORE_WEIGHT = 0.4;
    private static final double ON_TIME_RATE_WEIGHT = 0.3;

    public BigDecimal calculate(double completionRate, double avgScore, double onTimeRate) {
        double normalizedAvgScore = avgScore / 5.0;

        double score = COMPLETION_RATE_WEIGHT * completionRate
                + AVG_SCORE_WEIGHT * normalizedAvgScore
                + ON_TIME_RATE_WEIGHT * onTimeRate;

        score = Math.min(1.0, Math.max(0.0, score));

        BigDecimal result = BigDecimal.valueOf(score * 100).setScale(1, RoundingMode.HALF_UP);

        log.debug("淇＄敤鍒嗚绠? 瀹屾垚鐜?{}, 鍧囧垎={}, 鍑嗘椂鐜?{}, 鏈€缁堝垎={}",
                completionRate, avgScore, onTimeRate, result);

        return result;
    }

    public BigDecimal calculateFromReviews(List<Integer> scores, int totalOrders, int completedOrders, int onTimeOrders) {
        if (totalOrders == 0) {
            return new BigDecimal("100.0");
        }

        double completionRate = (double) completedOrders / totalOrders;
        double avgScore = scores.isEmpty() ? 3.0 :
                scores.stream().mapToInt(Integer::intValue).average().orElse(3.0);
        double onTimeRate = (double) onTimeOrders / Math.max(completedOrders, 1);

        return calculate(completionRate, avgScore, onTimeRate);
    }
}
