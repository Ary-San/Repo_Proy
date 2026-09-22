package com.checkout.backend.savings.expense.model;

import com.checkout.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "expenses",
        indexes = @Index(name = "idx_expenses_user_date", columnList = "user_id, date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_expenses_user"))
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    @NotNull
    @DecimalMin(value = "0.01", message = "The amount must be greater than zero")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @PastOrPresent(message = "The date cannot be in the future")
    @Column(nullable = false)
    private LocalDate date;

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Identity is the primary key; two unsaved instances are only equal to themselves. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expense other)) return false;
        return id != null && id.equals(other.id);
    }

    /** Constant on purpose: the hash must not change when the id is assigned on persist. */
    @Override
    public int hashCode() {
        return Expense.class.hashCode();
    }
}
