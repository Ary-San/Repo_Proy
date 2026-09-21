package com.checkout.backend.token_wallet.tktransaction.enums;

/**
 * Motivo del movimiento de fichas (cantidad > 0 = crédito, cantidad < 0 = débito).
 * referenciaId apunta a:
 * INGRESO_REGISTRADO -> Income, GASTO_REGISTRADO -> Expense, META_COMPLETADA -> Savings_goal,
 * COSTO_MINIJUEGO / PREMIO_MINIJUEGO -> Minigame_session, COMPRA_ACTIVO / VENTA_ACTIVO -> Trade_order,
 * MISION_DIARIA / AJUSTE_ADMIN -> null.
 */
public enum MotivoFicha {
    INGRESO_REGISTRADO,
    GASTO_REGISTRADO,
    META_COMPLETADA,
    MISION_DIARIA,
    COSTO_MINIJUEGO,
    PREMIO_MINIJUEGO,
    COMPRA_ACTIVO,
    VENTA_ACTIVO,
    AJUSTE_ADMIN
}
