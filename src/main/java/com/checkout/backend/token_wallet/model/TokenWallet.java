package com.checkout.backend.token_wallet.model;

import com.checkout.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Game currency balance. One per user.
 *
 * The balance is a running total of TokenTransaction rows and must never be
 * assigned directly outside the transaction that records the movement.
 */
@Entity
@Table(
        name = "token_wallets",
        uniqueConstraints = @UniqueConstraint(name = "uk_token_wallets_user", columnNames = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_token_wallets_user"))
    private User user;

    /**
     * Decimal, not integer: buying 0.41200000 of an asset priced at 548.90
     * spends 226.14 tokens.
     */
    @NotNull
    @DecimalMin(value = "0", message = "The token balance can never go negative")
    @Column(name = "token_balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal tokenBalance = BigDecimal.ZERO;

    /**
     * Optimistic lock against double spending. Two concurrent purchases read
     * the same balance; without this the second one silently overwrites the
     * first, with it the second flush fails and can be retried.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Identity is the primary key; two unsaved instances are only equal to themselves. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenWallet other)) return false;
        return id != null && id.equals(other.id);
    }

    /** Constant on purpose: the hash must not change when the id is assigned on persist. */
    @Override
    public int hashCode() {
        return TokenWallet.class.hashCode();
    }
}
