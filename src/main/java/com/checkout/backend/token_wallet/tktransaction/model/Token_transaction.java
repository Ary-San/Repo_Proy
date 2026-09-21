package com.checkout.backend.token_wallet.tktransaction.model;

import com.checkout.backend.token_wallet.tktransaction.enums.MotivoFicha;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(check = @CheckConstraint(name = "ck_token_transaction_cantidad_no_cero", constraint = "cantidad <> 0"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token_transaction {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private Long tokenWalletId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal cantidad;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoFicha motivo;
    private Long referenciaId;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
