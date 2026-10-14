package com.studentfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for returning subscription data to the client.
 */
public record SubscriptionResponse(
        Long id,
        String name,
        String category,
        BigDecimal amount,
        com.studentfinance.entity.SubscriptionFrequency frequency,
        LocalDate renewal,
        com.studentfinance.entity.SubscriptionStatus status
) {}
