package com.studentfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity @Table(name = "subscriptions") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @NotBlank @Column(nullable = false, length = 120) private String name;
    @NotBlank @Column(nullable = false, length = 80) private String category;
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SubscriptionFrequency frequency;
    @NotNull @Column(nullable = false) private LocalDate nextRenewalDate;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SubscriptionStatus status;
}
