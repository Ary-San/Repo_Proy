package com.checkout.backend.minigame.repository;

import com.checkout.backend.minigame.model.Minigame;
import com.checkout.backend.minigame.model.MinigameStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso al catalogo de minijuegos.
 */
public interface MinigameRepository extends JpaRepository<Minigame, Long> {

    /**
     * Los minijuegos de un estado concreto.
     *
     * Existe para que el catalogo publico solo devuelva los PUBLISHED: un
     * borrador tiene costes y recompensas que todavia se estan ajustando, y
     * dejarlo ver permitiria jugarlo con parametros que nadie aprobo.
     */
    List<Minigame> findByStatusOrderByTitleAsc(MinigameStatus status);

    Optional<Minigame> findByIdAndStatus(Long id, MinigameStatus status);

    boolean existsByTitleIgnoreCase(String title);
}
