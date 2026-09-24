package com.checkout.backend.savings.controller;

import com.checkout.backend.savings.dto.SavingsResponse;
import com.checkout.backend.savings.service.SavingsService;
import com.checkout.backend.user.service.CurrentUserProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Saldo de ahorro del usuario autenticado.
 *
 * La ruta no lleva id y no hay coleccion que listar: un usuario tiene un solo
 * ahorro, asi que /savings es un recurso singular, no /savings/{id}. Exponerlo
 * por id obligaria al cliente a conocer un identificador que no elige y que solo
 * puede tener un valor valido para el.
 *
 * El prefijo /api/v1 lo pone ApiVersioningConfig; aqui solo va la ruta propia.
 */
@RestController
@RequestMapping("/savings")
public class SavingsController {

    private final SavingsService savingsService;
    private final CurrentUserProvider currentUser;

    public SavingsController(SavingsService savingsService, CurrentUserProvider currentUser) {
        this.savingsService = savingsService;
        this.currentUser = currentUser;
    }

    /** GET /api/v1/savings */
    @GetMapping
    public ResponseEntity<SavingsResponse> getSavings() {
        return ResponseEntity.ok(savingsService.getSummary(currentUser.requireCurrentUser()));
    }

}
