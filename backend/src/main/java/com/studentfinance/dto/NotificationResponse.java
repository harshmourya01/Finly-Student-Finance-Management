package com.studentfinance.dto;

import com.studentfinance.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt,
        Long relatedEntityId
) {}
