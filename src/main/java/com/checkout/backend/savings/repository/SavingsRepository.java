package com.checkout.backend.savings.repository;

import com.checkout.backend.savings.model.Savings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla savings, que guarda un unico saldo por usuario.
 */
public interface SavingsRepository extends JpaRepository<Savings, Long> {

    /**
     * La tabla tiene UNIQUE sobre user_id, asi que esto devuelve como mucho una
     * fila. Es Optional y no Savings porque el registro se crea la primera vez
     * que el usuario lo necesita, no al registrarse.
     */
    Optional<Savings> findByUserId(Long userId);

}
