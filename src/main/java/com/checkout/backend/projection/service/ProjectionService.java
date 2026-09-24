package com.checkout.backend.projection.service;

import com.checkout.backend.exceptions.ResourceNotFoundException;
import com.checkout.backend.projection.dto.ProjectionRequest;
import com.checkout.backend.projection.dto.ProjectionResponse;
import com.checkout.backend.projection.model.Projection;
import com.checkout.backend.projection.repository.ProjectionRepository;
import com.checkout.backend.savings.goal.model.SavingsGoal;
import com.checkout.backend.savings.goal.service.SavingsGoalService;
import com.checkout.backend.user.model.User;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proyecciones de ahorro: cuanto llega a ser un capital con aportes periodicos.
 *
 * Calcula dos escenarios sobre los mismos datos para que la diferencia entre
 * ambos sea visible, que es justo lo que la pantalla quiere ensenar: el interes
 * compuesto no se entiende con una definicion, se entiende viendo la brecha que
 * abre contra el simple a los mismos anos.
 *
 * Los periodos son meses, por el @Max(600) del modelo, que son cincuenta anos.
 * La tasa que llega es anual y esta entre 0 y 1, asi que el paso a mensual es
 * una division entre doce.
 */
@Service
public class ProjectionService {

    /** Precision de trabajo. Los resultados se redondean a dos decimales al final. */
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final ProjectionRepository projectionRepository;
    private final SavingsGoalService goalService;
    private final ModelMapper mapper;

    public ProjectionService(ProjectionRepository projectionRepository,
                             SavingsGoalService goalService,
                             ModelMapper mapper) {
        this.projectionRepository = projectionRepository;
        this.goalService = goalService;
        this.mapper = mapper;
    }

    /**
     * Calcula una proyeccion y la guarda.
     *
     * La meta asociada es opcional. Cuando viene, se resuelve con findOwned, de
     * modo que nadie pueda enlazar su proyeccion a la meta de otro usuario y
     * averiguar asi que ese id existe.
     */
    @Transactional
    public ProjectionResponse create(User user, ProjectionRequest request) {
        SavingsGoal goal = request.getSavingsGoalId() == null
                ? null
                : goalService.findOwned(user, request.getSavingsGoalId());

        Projection projection = Projection.builder()
                .user(user)
                .savingsGoal(goal)
                .name(request.getName())
                .initialCapital(request.getInitialCapital())
                .monthlyContribution(request.getMonthlyContribution())
                .annualRate(request.getAnnualRate())
                .periods(request.getPeriods())
                .build();

        projection.setSimpleFinalAmount(simpleFinalAmount(projection));
        projection.setCompoundFinalAmount(compoundFinalAmount(projection));

        return toResponse(projectionRepository.save(projection));
    }

    @Transactional(readOnly = true)
    public List<ProjectionResponse> list(User user) {
        return projectionRepository.findByUserIdOrderByCalculatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectionResponse get(User user, Long id) {
        return toResponse(findOwned(user, id));
    }

    /**
     * Borra la proyeccion.
     *
     * No hay actualizacion: una proyeccion es el resultado de un calculo con
     * unos datos concretos, y cambiarle los datos no la corrige, produce otra
     * distinta. Guardar la anterior tiene valor, porque el usuario puede querer
     * comparar dos escenarios.
     */
    @Transactional
    public void delete(User user, Long id) {
        projectionRepository.delete(findOwned(user, id));
    }

    private Projection findOwned(User user, Long id) {
        return projectionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyeccion", id));
    }

    /**
     * Interes simple: los intereses se calculan siempre sobre el capital y no se
     * reinvierten.
     *
     * El capital inicial acumula durante los n meses completos. Cada aporte
     * acumula solo desde el mes en que entra, asi que el primero gana intereses
     * por n-1 meses y el ultimo por ninguno.
     */
    private static BigDecimal simpleFinalAmount(Projection projection) {
        BigDecimal monthlyRate = monthlyRate(projection);
        int periods = projection.getPeriods();

        BigDecimal fromCapital = projection.getInitialCapital()
                .multiply(BigDecimal.ONE.add(monthlyRate.multiply(BigDecimal.valueOf(periods), MC)), MC);

        // Suma de los meses que acumula cada aporte: (n-1) + (n-2) + ... + 0,
        // que es n*(n-1)/2. Se usa la formula en vez de un bucle porque con 600
        // periodos la diferencia en precision y en tiempo no es despreciable.
        BigDecimal accruedMonths = BigDecimal.valueOf((long) periods * (periods - 1) / 2);

        BigDecimal fromContributions = projection.getMonthlyContribution()
                .multiply(BigDecimal.valueOf(periods), MC)
                .add(projection.getMonthlyContribution().multiply(monthlyRate, MC)
                        .multiply(accruedMonths, MC), MC);

        return scale(fromCapital.add(fromContributions, MC));
    }

    /**
     * Interes compuesto: los intereses se reinvierten y generan intereses.
     *
     * Capital inicial por (1+i)^n, mas el valor futuro de una renta constante,
     * que es PMT * ((1+i)^n - 1) / i.
     *
     * Con tasa cero esa formula se indefine por la division, y el resultado
     * correcto es simplemente la suma de los aportes: se trata aparte en vez de
     * dejar que reviente.
     */
    private static BigDecimal compoundFinalAmount(Projection projection) {
        BigDecimal monthlyRate = monthlyRate(projection);
        int periods = projection.getPeriods();

        if (monthlyRate.signum() == 0) {
            return scale(projection.getInitialCapital()
                    .add(projection.getMonthlyContribution().multiply(BigDecimal.valueOf(periods), MC), MC));
        }

        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(periods, MC);

        BigDecimal fromCapital = projection.getInitialCapital().multiply(growth, MC);
        BigDecimal fromContributions = projection.getMonthlyContribution()
                .multiply(growth.subtract(BigDecimal.ONE, MC).divide(monthlyRate, MC), MC);

        return scale(fromCapital.add(fromContributions, MC));
    }

    private static BigDecimal monthlyRate(Projection projection) {
        return projection.getAnnualRate().divide(MONTHS_PER_YEAR, MC);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Completa los dos campos que el mapper deja en null porque no existen en la
     * entidad: lo aportado en total y la brecha entre los dos escenarios, que es
     * el numero que da sentido a toda la pantalla.
     */
    private ProjectionResponse toResponse(Projection projection) {
        ProjectionResponse response = mapper.map(projection, ProjectionResponse.class);

        response.setTotalContributed(scale(projection.getInitialCapital()
                .add(projection.getMonthlyContribution()
                        .multiply(BigDecimal.valueOf(projection.getPeriods()), MC), MC)));

        response.setDifference(scale(projection.getCompoundFinalAmount()
                .subtract(projection.getSimpleFinalAmount())));

        return response;
    }

}
