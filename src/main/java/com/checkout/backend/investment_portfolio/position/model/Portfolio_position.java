package com.checkout.backend.investment_portfolio.position.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"portfolioId", "assetId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio_position {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private Long portfolioId;
    @Column(nullable = false)
    private Long assetId;
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal cantidad;
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal costoPromedio;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime actualizadaEn;
}
