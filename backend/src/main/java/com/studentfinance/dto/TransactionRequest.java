package com.studentfinance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.studentfinance.entity.PaymentMethod;
import com.studentfinance.entity.TransactionType;

public record TransactionRequest(
    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    BigDecimal amount,

    @NotNull
    LocalDate transactionDate,

    @NotBlank
    String description,

    @NotNull
    TransactionType type,

    @NotNull
    PaymentMethod paymentMethod,

    @NotNull
    Long categoryId,

    String notes
) {}
