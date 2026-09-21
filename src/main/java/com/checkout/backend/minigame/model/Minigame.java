package com.checkout.backend.minigame.model;

import com.checkout.backend.minigame.enums.EstadoJuego;
import com.checkout.backend.minigame.enums.TipoJuego;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Minigame {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable=false)
    private String titulo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoJuego tipo;
    @Column(nullable = false)
    private String tema;
    @Column(nullable = false)
    private Integer costoFichas;
    @Column(nullable = false)
    private Integer fichasRecompensaMax;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoJuego estado;
}
