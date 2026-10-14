package com.studentfinance.dto;

public record UserResponse(
        Long id,
        String fullName,
        String email
) { }
