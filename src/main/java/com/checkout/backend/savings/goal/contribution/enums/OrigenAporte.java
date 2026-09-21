package com.checkout.backend.savings.goal.contribution.enums;

/**
 * Origen del aporte a una meta. referenciaId apunta a:
 * INGRESO -> Income que financió el aporte; MANUAL y AJUSTE -> null.
 */
public enum OrigenAporte {
    MANUAL, INGRESO, AJUSTE
}
