package com.checkout.backend.savings.goal.contribution.service;

import com.checkout.backend.exceptions.InvalidRequestException;
import com.checkout.backend.savings.goal.contribution.dto.ContributionRequest;
import com.checkout.backend.savings.goal.contribution.dto.ContributionResponse;
import com.checkout.backend.savings.goal.contribution.model.SavingsGoalContribution;
import com.checkout.backend.savings.goal.contribution.repository.SavingsGoalContributionRepository;
import com.checkout.backend.savings.goal.model.GoalStatus;
import com.checkout.backend.savings.goal.model.SavingsGoal;
import com.checkout.backend.savings.goal.service.SavingsGoalService;
import com.checkout.backend.savings.service.SavingsService;
import com.checkout.backend.user.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aportes a una meta de ahorro.
 *
 * Un aporte no mueve dinero: compromete dinero que ya estaba en el saldo. Sube
 * el acumulado de la meta y, con el, el monto comprometido, de modo que baja el
 * disponible mientras currentBalance no cambia. Por eso aqui no se llama a
 * credit ni a debit.
 */
@Service
public class ContributionService {

    private final SavingsGoalContributionRepository contributionRepository;
    private final SavingsGoalService goalService;
    private final SavingsService savingsService;
    private final ModelMapper mapper;

    public ContributionService(SavingsGoalContributionRepository contributionRepository,
                               SavingsGoalService goalService,
                               SavingsService savingsService,
                               ModelMapper mapper) {
        this.contributionRepository = contributionRepository;
        this.goalService = goalService;
        this.savingsService = savingsService;
        this.mapper = mapper;
    }

    /**
     * Registra un aporte.
     *
     * Las tres reglas que se comprueban, en orden:
     *
     * <ol>
     *   <li>La meta tiene que estar en progreso. Aportar a una cumplida no tiene
     *       destino, y a una vencida tampoco.</li>
     *   <li>El aporte no puede pasarse de lo que falta. Dejar acumulado por
     *       encima del objetivo daria un progreso mayor al 100%.</li>
     *   <li>El aporte no puede superar el saldo disponible. Comprometer dinero
     *       que no esta respaldado es exactamente lo que el modelo del sobre
     *       virtual evita.</li>
     * </ol>
     *
     * El @Version de la meta cubre lo que estas comprobaciones no pueden: dos
     * aportes simultaneos leen el mismo acumulado, los dos pasan la validacion y
     * el segundo en escribir falla con un conflicto de bloqueo optimista en vez
     * de pisar al primero.
     */
    @Transactional
    public ContributionResponse create(User user, Long goalId, ContributionRequest request) {
        SavingsGoal goal = goalService.findOwned(user, goalId);
        goalService.applyExpiration(goal);

        if (goal.getStatus() != GoalStatus.IN_PROGRESS) {
            throw new InvalidRequestException(
                    goal.getStatus() == GoalStatus.COMPLETED
                            ? "Esta meta ya esta cumplida."
                            : "Esta meta vencio el " + goal.getDeadline() + ".");
        }

        BigDecimal amount = request.getAmount();
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getAccumulatedAmount());
        if (amount.compareTo(remaining) > 0) {
            throw new InvalidRequestException(
                    "El aporte supera lo que falta para la meta (" + remaining + ").");
        }

        BigDecimal available = savingsService.availableBalance(user);
        if (amount.compareTo(available) > 0) {
            throw new InvalidRequestException(
                    "El aporte supera tu saldo disponible de " + available + ".");
        }

        goal.setAccumulatedAmount(goal.getAccumulatedAmount().add(amount));
        if (goal.getAccumulatedAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
            goal.setCompletedAt(LocalDateTime.now());
        }

        SavingsGoalContribution contribution = SavingsGoalContribution.builder()
                .savingsGoal(goal)
                .amount(amount)
                .source(request.getSource())
                .build();

        // Por el lado dueno de la relacion y el metodo de ayuda de la entidad,
        // que mantiene las dos puntas en orden dentro de la misma sesion.
        goal.addContribution(contribution);

        return mapper.map(contributionRepository.save(contribution), ContributionResponse.class);
    }

    /**
     * Aportes de una meta, del mas reciente al mas antiguo.
     *
     * Resuelve la meta con findOwned antes de consultar: asi una meta ajena da
     * 404 en vez de devolver la lista de aportes de otro usuario.
     */
    @Transactional(readOnly = true)
    public List<ContributionResponse> list(User user, Long goalId) {
        SavingsGoal goal = goalService.findOwned(user, goalId);
        return contributionRepository.findBySavingsGoalIdOrderByCreatedAtDesc(goal.getId())
                .stream()
                .map(contribution -> mapper.map(contribution, ContributionResponse.class))
                .toList();
    }

}
