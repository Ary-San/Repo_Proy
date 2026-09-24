package com.checkout.backend.token_wallet.tktransaction.model;

import com.checkout.backend.token_wallet.model.TokenWallet;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ledger entry explaining every change to a TokenWallet balance.
 *
 * Without it, screens such as "you earned 40 tokens this week" cannot be
 * computed and a user's claim cannot be audited.
 */
@Entity
@Table(
        name = "token_transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_token_tx_reason_reference",
                columnNames = {"reason", "reference_id"}),
        indexes = @Index(name = "idx_token_tx_wallet_created",
                columnList = "token_wallet_id, created_at")
)
@Check(name = "ck_token_tx_amount_not_zero", constraints = "amount <> 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token_wallet_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_token_tx_wallet"))
    private TokenWallet tokenWallet;

    /** Positive when tokens are granted, negative when they are spent. */
    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenReason reason;

    /**
     * Soft reference to the goal, minigame session or order that caused it. It
     * cannot be a foreign key because the target changes with the reason, so
     * the pair (reason, referenceId) is unique instead: a retry of the same
     * operation hits the constraint rather than charging twice. It stays null
     * for a manual ADJUSTMENT, and SQL treats nulls as distinct, so several
     * adjustments are still allowed.
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * Wallet balance once this entry was applied. Makes the ledger auditable at
     * any point in time and lets a drift from TokenWallet.tokenBalance be
     * detected by reading a single row.
     */
    @NotNull
    @DecimalMin("0")
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * A ledger row worth zero tokens is noise: it pollutes the history and
     * breaks any aggregation that assumes real movement. @DecimalMin is no use
     * here because a spend is legitimately negative, so the rule is expressed
     * as a range check and mirrored by the @Check constraint on the table.
     */
    @AssertTrue(message = "The amount of a token movement cannot be zero")
    private boolean isAmountNonZero() {
        return amount == null || amount.signum() != 0;
    }

    /** Identity is the primary key; two unsaved instances are only equal to themselves. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenTransaction other)) return false;
        return id != null && id.equals(other.id);
    }

    /** Constant on purpose: the hash must not change when the id is assigned on persist. */
    @Override
    public int hashCode() {
        return TokenTransaction.class.hashCode();
    }
}
