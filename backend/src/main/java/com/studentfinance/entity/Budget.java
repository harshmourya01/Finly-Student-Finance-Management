package com.studentfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity @Table(name = "budgets", uniqueConstraints = @UniqueConstraint(name = "uk_budget_user_category_month", columnNames = {"user_id", "category_id", "budget_month"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Budget extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false) private Category category;
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @NotNull @Column(name = "budget_month", nullable = false) private LocalDate month;
}
