package com.checkout.backend.investment_portfolio.trade_order.model;

import com.checkout.backend.investment_portfolio.trade_order.enums.EstadoOrden;
import com.checkout.backend.investment_portfolio.trade_order.enums.LadoOrden;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade_order {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long assetId;
    @Column(nullable = false, unique = true, updatable = false)
    private UUID clientOrderId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LadoOrden lado;
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal cantidad;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoOrden estado = EstadoOrden.PENDIENTE;
    @Column(precision = 19, scale = 8)
    private BigDecimal precioEjecucion;
    @Column(precision = 19, scale = 2)
    private BigDecimal fichasMovidas;
    private String motivoRechazo;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadaEn;
    private LocalDateTime ejecutadaEn;
}
