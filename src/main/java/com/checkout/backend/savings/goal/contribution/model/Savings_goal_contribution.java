package com.checkout.backend.savings.goal.contribution.model;

import com.checkout.backend.savings.goal.contribution.enums.OrigenAporte;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Savings_goal_contribution {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false)
    private Long savingsGoalId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenAporte origen;
    private Long referenciaId;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
