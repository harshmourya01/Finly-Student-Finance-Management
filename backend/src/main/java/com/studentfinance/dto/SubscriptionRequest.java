package com.studentfinance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating/updating a subscription.
 */
public record SubscriptionRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Category is required") String category,
        @NotNull @DecimalMin(value = "0.01", inclusive = false, message = "Amount must be greater than zero") BigDecimal amount,
        @NotNull(message = "Frequency is required") com.studentfinance.entity.SubscriptionFrequency frequency,
        @NotNull(message = "Next renewal date is required") LocalDate renewal,
        @NotNull(message = "Status is required") com.studentfinance.entity.SubscriptionStatus status
) {}
