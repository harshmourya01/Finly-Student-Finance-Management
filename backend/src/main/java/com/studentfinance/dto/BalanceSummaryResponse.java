package com.studentfinance.dto;

import java.math.BigDecimal;

public record BalanceSummaryResponse(
        BigDecimal totalBalance,
        BigDecimal reservedSavings,
        BigDecimal spendableBalance
) {}
