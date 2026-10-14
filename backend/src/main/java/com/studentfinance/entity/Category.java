package com.studentfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity @Table(name = "categories", uniqueConstraints = @UniqueConstraint(name = "uk_category_name_type", columnNames = {"name", "type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, length = 80) private String name;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private TransactionType type;

    public Long getId() { return id; }
    public String getName() { return name; }
    public TransactionType getType() { return type; }
}
