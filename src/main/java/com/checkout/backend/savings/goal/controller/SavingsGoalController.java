package com.checkout.backend.savings.goal.controller;

import com.checkout.backend.savings.goal.dto.SavingsGoalRequest;
import com.checkout.backend.savings.goal.dto.SavingsGoalResponse;
import com.checkout.backend.savings.goal.service.SavingsGoalService;
import com.checkout.backend.user.service.CurrentUserProvider;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Metas de ahorro del usuario autenticado.
 *
 * Convenciones que sigue este controller y que valen para el resto de la API:
 *
 * <ul>
 *   <li>La ruta nombra el recurso en plural y sin verbos: /savings-goals, nunca
 *       /getSavingsGoals. El verbo ya lo dice el metodo HTTP.</li>
 *   <li>POST devuelve 201 con la cabecera Location apuntando al recurso creado,
 *       para que el cliente no tenga que componer la URL a mano.</li>
 *   <li>DELETE devuelve 204 sin cuerpo: no hay nada que describir.</li>
 *   <li>Ningun id de usuario viaja en la URL. El dueno sale del token, no del
 *       cliente; si viajara en la ruta, cambiarlo seria leer datos ajenos.</li>
 * </ul>
 */
@RestController
@RequestMapping("/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService goalService;
    private final CurrentUserProvider currentUser;

    public SavingsGoalController(SavingsGoalService goalService, CurrentUserProvider currentUser) {
        this.goalService = goalService;
        this.currentUser = currentUser;
    }

    /** GET /api/v1/savings-goals */
    @GetMapping
    public ResponseEntity<List<SavingsGoalResponse>> list() {
        return ResponseEntity.ok(goalService.list(currentUser.requireCurrentUser()));
    }

    /** GET /api/v1/savings-goals/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.get(currentUser.requireCurrentUser(), id));
    }

    /** POST /api/v1/savings-goals */
    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(@Valid @RequestBody SavingsGoalRequest request) {
        SavingsGoalResponse created = goalService.create(currentUser.requireCurrentUser(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * PUT /api/v1/savings-goals/{id}
     *
     * Es PUT y no PATCH porque el cuerpo trae la meta completa: los tres campos
     * editables son obligatorios y reemplazan a los anteriores. Un PATCH
     * prometeria actualizacion parcial, que este endpoint no hace.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody SavingsGoalRequest request) {
        return ResponseEntity.ok(goalService.update(currentUser.requireCurrentUser(), id, request));
    }

    /** DELETE /api/v1/savings-goals/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goalService.delete(currentUser.requireCurrentUser(), id);
        return ResponseEntity.noContent().build();
    }

}
