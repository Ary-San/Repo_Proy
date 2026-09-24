package com.checkout.backend.minigame.session.controller;

import com.checkout.backend.minigame.session.dto.MinigameSessionRequest;
import com.checkout.backend.minigame.session.dto.MinigameSessionResponse;
import com.checkout.backend.minigame.session.service.MinigameSessionService;
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
 * Partidas jugadas por el usuario autenticado.
 *
 * No va anidado bajo /minigames/{id} aunque cada partida pertenezca a un
 * minijuego: el historial que el usuario quiere ver es el suyo completo, de
 * todos los juegos, y anidarlo obligaria a pedirlo juego por juego.
 *
 * No hay DELETE. Una partida es un asiento del historial y mueve fichas;
 * borrarla dejaria el saldo sin explicacion.
 */
@RestController
@RequestMapping("/minigame-sessions")
public class MinigameSessionController {

    private final MinigameSessionService sessionService;
    private final CurrentUserProvider currentUser;

    public MinigameSessionController(MinigameSessionService sessionService,
                                     CurrentUserProvider currentUser) {
        this.sessionService = sessionService;
        this.currentUser = currentUser;
    }

    /** GET /api/v1/minigame-sessions */
    @GetMapping
    public ResponseEntity<List<MinigameSessionResponse>> list() {
        return ResponseEntity.ok(sessionService.list(currentUser.requireCurrentUser()));
    }

    /** GET /api/v1/minigame-sessions/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<MinigameSessionResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.get(currentUser.requireCurrentUser(), id));
    }

    /**
     * POST /api/v1/minigame-sessions
     *
     * Registra una partida: cobra el coste del juego y acredita la recompensa.
     *
     * La puntuacion llega del cliente y no se puede verificar, porque el juego
     * corre ahi. Lo que acota el dano es que la recompensa nunca supera el
     * maxTokenReward del catalogo, que solo edita un ADMIN.
     */
    @PostMapping
    public ResponseEntity<MinigameSessionResponse> play(
            @Valid @RequestBody MinigameSessionRequest request) {

        MinigameSessionResponse played =
                sessionService.play(currentUser.requireCurrentUser(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(played.getId())
                .toUri();

        return ResponseEntity.created(location).body(played);
    }

}
