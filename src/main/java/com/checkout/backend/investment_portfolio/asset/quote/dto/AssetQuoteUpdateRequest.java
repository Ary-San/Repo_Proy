package com.checkout.backend.investment_portfolio.asset.quote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Nuevo precio de un activo.
 *
 * Solo lleva el precio. previousClose y changePercent son datos derivados y los
 * calcula el servicio: dejar que lleguen de fuera permitiria que el porcentaje
 * diga una cosa y los precios otra.
 *
 * @param price precio actual, siempre mayor que cero
 */
public record AssetQuoteUpdateRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "The price must be greater than zero")
        BigDecimal price
) {
}
