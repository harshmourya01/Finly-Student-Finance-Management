package com.studentfinance.dto;

import com.studentfinance.entity.PaymentMethod;
import com.studentfinance.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        BigDecimal amount,
        LocalDate transactionDate,
        String description,
        TransactionType type,
        PaymentMethod paymentMethod,
        CategoryDto category,
        String notes
) {}
