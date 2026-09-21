package com.checkout.backend.projection.model;

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
public class Projection {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal capitalInicial;
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal aporteMensual = BigDecimal.ZERO;
    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal tasaAnual;
    @Column(nullable = false)
    private Integer periodos;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montoFinalSimple;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montoFinalCompuesto;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime calculadaEn;
}
