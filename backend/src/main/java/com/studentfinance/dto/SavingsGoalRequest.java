package com.studentfinance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SavingsGoalRequest(
        @NotBlank(message = "Goal name is required")
        String name,

        @NotNull(message = "Target amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Target amount must be greater than zero")
        BigDecimal targetAmount,

        @DecimalMin(value = "0.00", inclusive = true, message = "Current saved amount cannot be negative")
        BigDecimal currentAmount,

        @NotBlank(message = "Target date is required")
        String targetDate
) {}
