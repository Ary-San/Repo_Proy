package com.checkout.backend.savings.goal.contribution.model;

import com.checkout.backend.savings.goal.model.SavingsGoal;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ledger entry behind SavingsGoal.accumulatedAmount. Without it, the progress
 * of a goal is a number nobody can audit.
 */
@Entity
@Table(
        name = "savings_goal_contributions",
        indexes = @Index(name = "idx_goal_contributions_goal_created",
                columnList = "savings_goal_id, created_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "savings_goal_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_goal_contributions_goal"))
    private SavingsGoal savingsGoal;

    @NotNull
    @DecimalMin(value = "0.01", message = "The contribution must be greater than zero")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributionSource source;

    /** Soft reference to the income that funded the contribution, when any. */
    @Column(name = "reference_id")
    private Long referenceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Identity is the primary key; two unsaved instances are only equal to themselves. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavingsGoalContribution other)) return false;
        return id != null && id.equals(other.id);
    }

    /** Constant on purpose: the hash must not change when the id is assigned on persist. */
    @Override
    public int hashCode() {
        return SavingsGoalContribution.class.hashCode();
    }
}
