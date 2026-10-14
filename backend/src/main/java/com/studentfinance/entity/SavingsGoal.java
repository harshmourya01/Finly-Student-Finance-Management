package com.studentfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity @Table(name = "savings_goals") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavingsGoal extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @NotBlank @Column(nullable = false, length = 120) private String name;
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Column(nullable = false, precision = 19, scale = 2) private BigDecimal targetAmount;
    @NotNull @DecimalMin(value = "0.00") @Column(nullable = false, precision = 19, scale = 2) private BigDecimal currentAmount;
    @NotNull @Column(nullable = false) private LocalDate targetDate;
}
