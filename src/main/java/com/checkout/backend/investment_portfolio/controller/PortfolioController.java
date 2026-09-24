package com.checkout.backend.investment_portfolio.controller;

import com.checkout.backend.investment_portfolio.dto.PortfolioResponse;
import com.checkout.backend.investment_portfolio.position.dto.PositionResponse;
import com.checkout.backend.investment_portfolio.service.PortfolioService;
import com.checkout.backend.user.service.CurrentUserProvider;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cartera simulada del usuario autenticado. Solo lectura, como el monedero.
 *
 * Una posicion no se crea ni se edita por API: aparece porque se ejecuto una
 * orden de compra y desaparece cuando se vende entera. Permitir editarla seria
 * poder darse activos sin pagarlos, que es la version de inversion del mismo
 * agujero que evita el monedero de solo lectura.
 *
 * Un usuario tiene una sola cartera, asi que la ruta no lleva id.
 */
@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CurrentUserProvider currentUser;

    public PortfolioController(PortfolioService portfolioService,
                               CurrentUserProvider currentUser) {
        this.portfolioService = portfolioService;
        this.currentUser = currentUser;
    }

    /**
     * GET /api/v1/portfolio
     *
     * Trae las posiciones ya valoradas con la cotizacion del momento, mas el
     * resultado no realizado: lo que valen hoy frente a lo que costo abrirlas.
     */
    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio() {
        return ResponseEntity.ok(portfolioService.getSummary(currentUser.requireCurrentUser()));
    }

    /**
     * GET /api/v1/portfolio/positions
     *
     * Las mismas posiciones sin el resumen, para la pantalla que solo lista.
     */
    @GetMapping("/positions")
    public ResponseEntity<List<PositionResponse>> listPositions() {
        return ResponseEntity.ok(portfolioService.listPositions(currentUser.requireCurrentUser()));
    }

}
