package com.checkout.backend.investment_portfolio.asset.quote.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset_quote {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false, unique = true)
    private Long assetId;
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal precio;
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal cierreAnterior;
    // Variación porcentual respecto al cierre anterior (ej. 1.2500 = 1.25%)
    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal variacionPct;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime actualizadoEn;
}
