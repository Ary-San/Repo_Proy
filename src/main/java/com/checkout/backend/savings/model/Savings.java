package com.checkout.backend.savings.model;

import com.checkout.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Real-money savings balance, in soles. One per user. */
@Entity
@Table(
        name = "savings",
        uniqueConstraints = @UniqueConstraint(name = "uk_savings_user", columnNames = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Savings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_savings_user"))
    private User user;

    @NotNull
    @Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Identity is the primary key; two unsaved instances are only equal to themselves. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Savings other)) return false;
        return id != null && id.equals(other.id);
    }

    /** Constant on purpose: the hash must not change when the id is assigned on persist. */
    @Override
    public int hashCode() {
        return Savings.class.hashCode();
    }
}
