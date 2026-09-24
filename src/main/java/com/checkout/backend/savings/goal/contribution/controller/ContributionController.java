package com.checkout.backend.savings.goal.contribution.controller;

import com.checkout.backend.savings.goal.contribution.dto.ContributionRequest;
import com.checkout.backend.savings.goal.contribution.dto.ContributionResponse;
import com.checkout.backend.savings.goal.contribution.service.ContributionService;
import com.checkout.backend.user.service.CurrentUserProvider;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Aportes de una meta.
 *
 * La ruta va anidada bajo la meta porque un aporte no existe sin ella: no tiene
 * sentido pedir /contributions/{id} sin decir a que meta pertenece. El anidado
 * ademas hace imposible aportar a una meta sin nombrarla.
 *
 * No hay DELETE. Un aporte es un asiento del historial, y borrarlo dejaria el
 * acumulado de la meta sin explicacion. Revertir uno es una operacion de negocio
 * con su propio registro, no un borrado.
 */
@RestController
@RequestMapping("/savings-goals/{goalId}/contributions")
public class ContributionController {

    private final ContributionService contributionService;
    private final CurrentUserProvider currentUser;

    public ContributionController(ContributionService contributionService,
                                  CurrentUserProvider currentUser) {
        this.contributionService = contributionService;
        this.currentUser = currentUser;
    }

    /** GET /api/v1/savings-goals/{goalId}/contributions */
    @GetMapping
    public ResponseEntity<List<ContributionResponse>> list(@PathVariable Long goalId) {
        return ResponseEntity.ok(contributionService.list(currentUser.requireCurrentUser(), goalId));
    }

    /** POST /api/v1/savings-goals/{goalId}/contributions */
    @PostMapping
    public ResponseEntity<ContributionResponse> create(@PathVariable Long goalId,
                                                       @Valid @RequestBody ContributionRequest request) {
        ContributionResponse created =
                contributionService.create(currentUser.requireCurrentUser(), goalId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

}
