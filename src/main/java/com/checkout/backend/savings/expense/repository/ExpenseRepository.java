package com.checkout.backend.savings.expense.repository;

import com.checkout.backend.savings.expense.model.Expense;
import com.checkout.backend.savings.expense.model.ExpenseCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla expenses.
 *
 * Igual que en incomes, el orden por fecha descendente aprovecha
 * idx_expenses_user_date.
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdOrderByDateDesc(Long userId);

    List<Expense> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate from, LocalDate to);

    List<Expense> findByUserIdAndCategoryOrderByDateDesc(Long userId, ExpenseCategory category);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

}
