package com.campus.courier.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CreditScoreCalculatorTest {

    private CreditScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CreditScoreCalculator();
    }

    @Test
    void testCalculate_allPerfect() {
        BigDecimal result = calculator.calculate(1.0, 5.0, 1.0);
        assertEquals(new BigDecimal("100.0"), result);
    }

    @Test
    void testCalculate_allZero() {
        BigDecimal result = calculator.calculate(0.0, 0.0, 0.0);
        assertEquals(new BigDecimal("0.0"), result);
    }

    @Test
    void testCalculate_midValues() {
        BigDecimal result = calculator.calculate(0.8, 4.0, 0.9);
        // completion:0.8*0.3=0.24, avg:4/5=0.8*0.4=0.32, onTime:0.9*0.3=0.27 total=0.83*100=83.0
        assertEquals(new BigDecimal("83.0"), result);
    }

    @Test
    void testCalculate_clampsToZero() {
        BigDecimal result = calculator.calculate(-0.5, -1.0, -0.2);
        // Should clamp to 0
        assertEquals(new BigDecimal("0.0"), result);
    }

    @Test
    void testCalculate_clampsToOne() {
        BigDecimal result = calculator.calculate(1.5, 6.0, 2.0);
        // Should clamp to 1.0 * 100 = 100.0
        assertEquals(new BigDecimal("100.0"), result);
    }

    @Test
    void testCalculateFromReviews_normalCase() {
        List<Integer> scores = Arrays.asList(4, 5, 3);
        int totalOrders = 10;
        int completedOrders = 8;
        int onTimeOrders = 7;

        BigDecimal result = calculator.calculateFromReviews(scores, totalOrders, completedOrders, onTimeOrders);
        // completion: 8/10=0.8, avg: (4+5+3)/3=4.0, onTime: 7/8=0.875
        // normalized avg: 4/5=0.8
        // score: 0.8*0.3 + 0.8*0.4 + 0.875*0.3 = 0.24 + 0.32 + 0.2625 = 0.8225 * 100 = 82.25 rounded to 82.3
        assertEquals(new BigDecimal("82.3"), result);
    }

    @Test
    void testCalculateFromReviews_noOrders() {
        List<Integer> scores = Arrays.asList();
        BigDecimal result = calculator.calculateFromReviews(scores, 0, 0, 0);
        assertEquals(new BigDecimal("100.0"), result);
    }

    @Test
    void testCalculateFromReviews_noScores() {
        List<Integer> scores = Arrays.asList();
        int totalOrders = 5;
        int completedOrders = 3;
        int onTimeOrders = 2;
        BigDecimal result = calculator.calculateFromReviews(scores, totalOrders, completedOrders, onTimeOrders);
        // completion: 3/5=0.6, avg: default 3.0 -> normalized 3/5=0.6, onTime: 2/3≈0.6667
        // 0.6*0.3 + 0.6*0.4 + 0.6667*0.3 = 0.18 + 0.24 + 0.2000 = 0.6200 * 100 = 62.0
        assertEquals(new BigDecimal("62.0"), result);
    }

    @Test
    void testCalculateFromReviews_completedZero() {
        List<Integer> scores = Arrays.asList(5, 5);
        int totalOrders = 5;
        int completedOrders = 0;
        int onTimeOrders = 0;
        BigDecimal result = calculator.calculateFromReviews(scores, totalOrders, completedOrders, onTimeOrders);
        // completion: 0/5=0, avg: 5.0 normalized 1.0, onTime: 0/1=0
        // 0*0.3 + 1.0*0.4 + 0*0.3 = 0.4 * 100 = 40.0
        assertEquals(new BigDecimal("40.0"), result);
    }
}