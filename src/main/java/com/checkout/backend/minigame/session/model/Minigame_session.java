package com.checkout.backend.minigame.session.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Minigame_session {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long minigameId;
    @Column(nullable = false)
    private Integer puntaje;
    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal fichasGanadas = BigDecimal.ZERO;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime jugadoEn;
}
