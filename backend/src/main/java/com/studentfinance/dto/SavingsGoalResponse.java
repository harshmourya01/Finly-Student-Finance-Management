package com.studentfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalResponse(
        Long id,
        String name,
        BigDecimal target,
        BigDecimal targetAmount,
        BigDecimal saved,
        BigDecimal currentAmount,
        String date,
        LocalDate targetDate
) {}
