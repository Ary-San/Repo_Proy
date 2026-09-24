package com.checkout.backend.minigame.session.repository;

import com.checkout.backend.minigame.session.model.MinigameSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso al historial de partidas. */
public interface MinigameSessionRepository extends JpaRepository<MinigameSession, Long> {

    List<MinigameSession> findByUserIdOrderByPlayedAtDesc(Long userId);

    Optional<MinigameSession> findByIdAndUserId(Long id, Long userId);
}
