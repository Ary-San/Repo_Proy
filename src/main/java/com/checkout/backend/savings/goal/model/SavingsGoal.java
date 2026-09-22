package com.checkout.backend.savings.goal.model;

import com.checkout.backend.savings.goal.contribution.model.SavingsGoalContribution;
import com.checkout.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "savings_goals",
        indexes = @Index(name = "idx_savings_goals_user_status_deadline",
                columnList = "user_id, status, deadline")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_savings_goals_user"))
    private User user;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotNull
    @DecimalMin(value = "0.01", message = "The target amount must be greater than zero")
    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    /**
     * Cache of the sum of contributions. The detail is the source of truth;
     * recalculate it inside the same transaction that adds a contribution.
     */
    @NotNull
    @DecimalMin("0")
    @Column(name = "accumulated_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal accumulatedAmount = BigDecimal.ZERO;

    @NotNull
    @Future(message = "The deadline must be in the future")
    @Column(nullable = false)
    private LocalDate deadline;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GoalStatus status = GoalStatus.IN_PROGRESS;

    /** Set once, when the goal is first reached. Stops the reward being paid twice. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "savingsGoal", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SavingsGoalContribution> contributions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Keeps both sides of the relation in step. Adding straight to the list
     * leaves savings_goal_id null and the insert fails on the constraint.
     */
    public void addContribution(SavingsGoalContribution contribution) {
        contributions.add(contribution);
        contribution.setSavingsGoal(this);
    }

    public void removeContribution(SavingsGoalContribution contribution) {
        contributions.remove(contribution);
        contribution.setSavingsGoal(null);
    }

    /** Identity is the primary key; two unsaved instances are only equal to themselves. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavingsGoal other)) return false;
        return id != null && id.equals(other.id);
    }

    /** Constant on purpose: the hash must not change when the id is assigned on persist. */
    @Override
    public int hashCode() {
        return SavingsGoal.class.hashCode();
    }
}
