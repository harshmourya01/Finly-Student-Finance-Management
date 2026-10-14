package com.studentfinance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transaction entity representing an income or expense record.
 */
@Entity
@Table(name = "transactions")
public class Transaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String description;

    @NotNull
    @Column(nullable = false)
    private LocalDate transactionDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 1000)
    private String notes;

    // Default constructor needed by JPA
    public Transaction() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Builder pattern for Transaction (used in tests)
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private User user;
        private Category category;
        private TransactionType type;
        private java.math.BigDecimal amount;
        private String description;
        private java.time.LocalDate transactionDate;
        private com.studentfinance.entity.PaymentMethod paymentMethod;
        private String notes;

        public Builder user(User user) { this.user = user; return this; }
        public Builder category(Category category) { this.category = category; return this; }
        public Builder type(TransactionType type) { this.type = type; return this; }
        public Builder amount(java.math.BigDecimal amount) { this.amount = amount; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder transactionDate(java.time.LocalDate transactionDate) { this.transactionDate = transactionDate; return this; }
        public Builder paymentMethod(com.studentfinance.entity.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }

        public Transaction build() {
            Transaction tx = new Transaction();
            tx.setUser(this.user);
            tx.setCategory(this.category);
            tx.setType(this.type);
            tx.setAmount(this.amount);
            tx.setDescription(this.description);
            tx.setTransactionDate(this.transactionDate);
            tx.setPaymentMethod(this.paymentMethod);
            tx.setNotes(this.notes);
            return tx;
        }
    }

}
