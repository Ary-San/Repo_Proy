package com.checkout.backend.savings.income.repository;

import com.checkout.backend.savings.income.model.Income;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla incomes.
 *
 * El orden y el filtro por rango se apoyan en idx_incomes_user_date, que es un
 * indice compuesto por (user_id, date).
 */
public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findByUserIdOrderByDateDesc(Long userId);

    List<Income> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate from, LocalDate to);

    Optional<Income> findByIdAndUserId(Long id, Long userId);

}
