package com.checkout.backend.token_wallet.refresh_token.repository;

import com.checkout.backend.token_wallet.refresh_token.model.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a la tabla refresh_tokens.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca por el hash, nunca por el token en claro: en la tabla solo esta el
     * hash. Quien llama calcula el hash del token que recibio y busca por el.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserId(Long userId);

    /**
     * Revoca de golpe todos los tokens vivos de un usuario. Es lo que hace un
     * cierre de sesion en todos los dispositivos, y tambien la reaccion cuando
     * se detecta que un token robado se esta reutilizando.
     *
     * Los dos flags no son decorativos. Una consulta de actualizacion en JPQL va
     * directa a la base y no pasa por el contexto de persistencia: las entidades
     * que Hibernate ya tenga cargadas conservan su estado anterior. Sin
     * clearAutomatically, releer un token recien revocado por esta consulta lo
     * devuelve con revokedAt en null y la revocacion parece no haber ocurrido.
     * flushAutomatically asegura lo contrario, que los cambios pendientes en la
     * sesion se escriban antes de que la actualizacion masiva los pise.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken t
            set t.revokedAt = :now
            where t.user.id = :userId and t.revokedAt is null
            """)
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

}
