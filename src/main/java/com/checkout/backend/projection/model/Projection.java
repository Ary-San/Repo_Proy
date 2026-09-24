package com.checkout.backend.projection.model;

import com.checkout.backend.savings.goal.model.SavingsGoal;
import com.checkout.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A saved scenario of the savings calculator.
 *
 * Both results are stored because comparing simple against compound interest
 * is the educational point of the app; a single result could not express it.
 */
@Entity
@Table(
        name = "projections",
        indexes = @Index(name = "idx_projections_user_calculated",
                columnList = "user_id, calculated_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_projections_user"))
    private User user;

    /**
     * The goal this projection is planning for, when there is one. Optional on
     * purpose: the screen also works as a standalone calculator.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_goal_id",
            foreignKey = @ForeignKey(name = "fk_projections_goal"))
    private SavingsGoal savingsGoal;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotNull
    @DecimalMin("0")
    @Column(name = "initial_capital", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialCapital;

    @NotNull
    @DecimalMin("0")
    @Column(name = "monthly_contribution", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal monthlyContribution = BigDecimal.ZERO;

    /** TEA as a rate between 0 and 1. The service converts it to a monthly rate. */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "annual_rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal annualRate;

    @NotNull
    @Min(1)
    @Max(600)
    @Column(nullable = false)
    private Integer periods;

    @Column(name = "simple_final_amount", precision = 19, scale = 2)
    private BigDecimal simpleFinalAmount;

    @Column(name = "compound_final_amount", precision = 19, scale = 2)
    private BigDecimal compoundFinalAmount;

    @CreationTimestamp
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;

    /** Identity is the primary key; two unsaved instances are only equal to themselves. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Projection other)) return false;
        return id != null && id.equals(other.id);
    }

    /** Constant on purpose: the hash must not change when the id is assigned on persist. */
    @Override
    public int hashCode() {
        return Projection.class.hashCode();
    }
}
