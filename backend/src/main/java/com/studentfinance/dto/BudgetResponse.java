package com.studentfinance.dto;

import java.math.BigDecimal;

public record BudgetResponse(
        Long id,
        Long categoryId,
        String category,
        BigDecimal limit,
        BigDecimal amount,
        BigDecimal spent,
        String month
) {}
