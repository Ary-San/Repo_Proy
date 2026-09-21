package com.checkout.backend.investment_portfolio.model;

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
public class Investment_portfolio {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false, unique = true)
    private Long userId;
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal fichasInvertidas = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal valorSimulado = BigDecimal.ZERO;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime actualizadoEn;
}
