package com.checkout.backend.projection.repository;

import com.checkout.backend.projection.model.Projection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla projections. El filtro por usuario aisla las cuentas. */
public interface ProjectionRepository extends JpaRepository<Projection, Long> {

    List<Projection> findByUserIdOrderByCalculatedAtDesc(Long userId);

    Optional<Projection> findByIdAndUserId(Long id, Long userId);
}
