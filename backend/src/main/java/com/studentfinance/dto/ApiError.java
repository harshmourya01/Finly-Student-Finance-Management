package com.studentfinance.dto;
import java.time.LocalDateTime;
public record ApiError(String message, int status, LocalDateTime timestamp) { }
