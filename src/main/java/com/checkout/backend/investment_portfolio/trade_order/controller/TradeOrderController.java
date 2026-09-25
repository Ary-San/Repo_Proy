package com.checkout.backend.investment_portfolio.trade_order.controller;

import com.checkout.backend.investment_portfolio.trade_order.dto.TradeOrderRequest;
import com.checkout.backend.investment_portfolio.trade_order.dto.TradeOrderResponse;
import com.checkout.backend.investment_portfolio.trade_order.service.TradeOrderService;
import com.checkout.backend.user.service.CurrentUserProvider;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Ordenes de compra y venta del usuario autenticado.
 *
 * Es la unica escritura del modulo de inversion: la cartera y las posiciones se
 * derivan de lo que pasa aqui. Concentrar la escritura en un solo sitio es lo
 * que permite que el resto sea de solo lectura.
 *
 * El cliente manda un clientOrderId y la ruta es idempotente: reintentar la
 * misma orden devuelve la que ya se ejecuto en vez de comprar otra vez. Sin eso,
 * un toque doble o una red inestable cuestan dinero al usuario.
 */
@RestController
@RequestMapping("/orders")
public class TradeOrderController {

    private final TradeOrderService orderService;
    private final CurrentUserProvider currentUser;

    public TradeOrderController(TradeOrderService orderService,
                                CurrentUserProvider currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    /** GET /api/v1/orders */
    @GetMapping
    public ResponseEntity<List<TradeOrderResponse>> list() {
        return ResponseEntity.ok(orderService.list(currentUser.requireCurrentUser()));
    }

    /** GET /api/v1/orders/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<TradeOrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.get(currentUser.requireCurrentUser(), id));
    }

    /**
     * POST /api/v1/orders
     *
     * Coloca la orden y la resuelve al instante contra la cotizacion vigente.
     *
     * Responde 201 tambien cuando la orden sale rechazada: el recurso se creo y
     * queda en el historial con su motivo. Que no se ejecutara es el resultado de
     * la operacion, no un fallo de la peticion, y el cliente lo lee en el campo
     * status.
     */
    @PostMapping
    public ResponseEntity<TradeOrderResponse> place(@Valid @RequestBody TradeOrderRequest request) {
        TradeOrderResponse placed = orderService.place(currentUser.requireCurrentUser(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(placed.getId())
                .toUri();

        return ResponseEntity.created(location).body(placed);
    }

    /**
     * DELETE /api/v1/orders/{id}
     *
     * Cancela una orden pendiente. Devuelve el recurso y no 204 porque el cambio
     * de estado es justo lo que el cliente necesita ver.
     *
     * Una orden ya ejecutada no se cancela: deshacerla significaria revertir
     * fichas y posiciones a precios que ya cambiaron.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<TradeOrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(currentUser.requireCurrentUser(), id));
    }

}
