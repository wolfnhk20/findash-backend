package com.findash.backend.dto;

import com.findash.backend.model.Category;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class AnalyticsResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netBalance;

    private Map<Category, CategoryAnalytics> categoryTotals;

    private Map<String, BigDecimal> monthlySpend;

    private BigDecimal averageMonthlySpend;
}