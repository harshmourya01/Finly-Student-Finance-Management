package com.studentfinance.dto;

import com.studentfinance.entity.TransactionType;

public record CategoryResponse(
        Long id,
        String name,
        TransactionType type
) {}
