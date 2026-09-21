package com.checkout.backend.savings.goal.model;

import com.checkout.backend.savings.goal.enums.EstadoMeta;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Savings_goal {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montoObjetivo;
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal montoAcumulado = BigDecimal.ZERO;
    @Column(nullable = false)
    private LocalDate fechaLimite;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoMeta estado = EstadoMeta.EN_PROGRESO;
    private LocalDateTime completadaEn;
}
