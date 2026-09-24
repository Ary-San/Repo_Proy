package com.checkout.backend.investment_portfolio.trade_order.model;

public enum OrderStatus {
    PENDING,
    EXECUTED,
    REJECTED,
    /** Withdrawn by the user before the simulator executed it. */
    CANCELLED
}
