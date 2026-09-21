package com.checkout.backend.investment_portfolio.asset.model;

import com.checkout.backend.investment_portfolio.asset.enums.TipoActivo;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false, unique = true)
    private String simbolo;
    @Column(nullable = false)
    private String nombre;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoActivo tipo;
    // Código ISO 4217 (ej. PEN, USD)
    @Column(nullable = false, length = 3)
    private String moneda;
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
