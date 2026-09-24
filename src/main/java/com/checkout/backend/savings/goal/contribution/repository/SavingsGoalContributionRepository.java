package com.checkout.backend.savings.goal.contribution.repository;

import com.checkout.backend.savings.goal.contribution.model.SavingsGoalContribution;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la tabla savings_goal_contributions.
 */
public interface SavingsGoalContributionRepository
        extends JpaRepository<SavingsGoalContribution, Long> {

    /**
     * Aportes de una meta, del mas reciente al mas antiguo.
     *
     * El filtro por meta basta para aislar usuarios: quien llama ya resolvio la
     * meta con findByIdAndUserId, de modo que una meta ajena nunca llega hasta
     * aqui.
     */
    List<SavingsGoalContribution> findBySavingsGoalIdOrderByCreatedAtDesc(Long savingsGoalId);

}
