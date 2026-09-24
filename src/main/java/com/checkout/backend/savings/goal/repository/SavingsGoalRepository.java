package com.checkout.backend.savings.goal.repository;

import com.checkout.backend.savings.goal.model.GoalStatus;
import com.checkout.backend.savings.goal.model.SavingsGoal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a la tabla savings_goals.
 *
 * Todas las consultas llevan el userId. No es una comodidad: es lo que impide
 * que un usuario lea o modifique la meta de otro cambiando el id de la URL. Un
 * findById suelto en el service abriria justo ese agujero.
 */
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdOrderByDeadlineAsc(Long userId);

    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    /**
     * Suma de lo acumulado en las metas vivas del usuario.
     *
     * Es el "comprometido" del saldo: segun la decision tomada en la auditoria
     * del modelo, una meta es un sobre virtual sobre el mismo dinero de savings,
     * no una bolsa aparte. Por eso se descuenta del saldo disponible en vez de
     * sumarse al total.
     *
     * COALESCE deja la consulta en cero cuando el usuario no tiene metas; sin el
     * la suma devuelve null y quien llama tendria que comprobarlo.
     */
    @Query("""
            select coalesce(sum(g.accumulatedAmount), 0)
            from SavingsGoal g
            where g.user.id = :userId and g.status = :status
            """)
    BigDecimal sumAccumulatedByUserIdAndStatus(@Param("userId") Long userId,
                                               @Param("status") GoalStatus status);

}
