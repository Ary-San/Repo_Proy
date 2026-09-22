package com.checkout.backend.investment_portfolio.position.model;

import com.checkout.backend.investment_portfolio.asset.model.Asset;
import com.checkout.backend.investment_portfolio.model.InvestmentPortfolio;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * How much of one asset a portfolio holds, and at what weighted average cost.
 *
 * Quantities are fractional: a token balance in the hundreds cannot buy a whole
 * share of an asset priced in the hundreds of dollars.
 */
@Entity
@Table(
        name = "portfolio_positions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_positions_portfolio_asset",
                columnNames = {"portfolio_id", "asset_id"}),
        indexes = @Index(name = "idx_positions_portfolio", columnList = "portfolio_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_positions_portfolio"))
    private InvestmentPortfolio portfolio;

    /** No cascade: the asset catalogue is shared and outlives any position. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_positions_asset"))
    private Asset asset;

    @NotNull
    @DecimalMin("0")
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    /** Weighted average: a sale lowers the quantity but leaves this untouched. */
    @NotNull
    @DecimalMin("0")
    @Column(name = "average_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal averageCost;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Identity is the primary key; two unsaved instances are only equal to themselves. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortfolioPosition other)) return false;
        return id != null && id.equals(other.id);
    }

    /** Constant on purpose: the hash must not change when the id is assigned on persist. */
    @Override
    public int hashCode() {
        return PortfolioPosition.class.hashCode();
    }
}
