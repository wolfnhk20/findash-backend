package com.findash.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
public class CategoryAnalytics {
    private BigDecimal income;
    private BigDecimal expense;
}