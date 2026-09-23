package com.checkout.backend.dto;

import com.checkout.backend.investment_portfolio.asset.dto.AssetRequest;
import com.checkout.backend.investment_portfolio.asset.model.AssetType;
import com.checkout.backend.investment_portfolio.trade_order.dto.TradeOrderRequest;
import com.checkout.backend.investment_portfolio.trade_order.model.OrderSide;
import com.checkout.backend.minigame.dto.MinigameRequest;
import com.checkout.backend.minigame.model.MinigameStatus;
import com.checkout.backend.minigame.model.MinigameType;
import com.checkout.backend.minigame.session.dto.MinigameSessionRequest;
import com.checkout.backend.projection.dto.ProjectionRequest;
import com.checkout.backend.savings.expense.dto.ExpenseRequest;
import com.checkout.backend.savings.expense.model.ExpenseCategory;
import com.checkout.backend.savings.goal.contribution.dto.ContributionRequest;
import com.checkout.backend.savings.goal.contribution.model.ContributionSource;
import com.checkout.backend.savings.goal.dto.SavingsGoalRequest;
import com.checkout.backend.savings.income.dto.IncomeRequest;
import com.checkout.backend.user.dto.LoginRequest;
import com.checkout.backend.user.dto.RefreshTokenRequest;
import com.checkout.backend.user.dto.RegisterUserRequest;
import com.checkout.backend.user.dto.UpdateUserRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Exercises the Bean Validation rules declared on every Request DTO.
 *
 * It runs the validator directly instead of going through MockMvc, for two
 * reasons: there are no controllers yet, and covering every constraint over
 * HTTP would mean one Spring context and one JSON round trip per case. What
 * this class proves is that the rule itself is right — the correct annotation
 * on the correct field, with the correct bound.
 *
 * What it deliberately does NOT prove is that the rules are wired up. A request
 * only gets validated when something invokes the validator, which in practice
 * means @Valid on a controller parameter. That belongs in the controller tests,
 * where a couple of cases per endpoint are enough to show the wiring works.
 */
class DtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    /** Longer than any @Size(max) in the project, for the upper bound cases. */
    private static String tooLong(int length) {
        return "x".repeat(length);
    }

    // ------------------------------------------------------------------
    //  A well formed payload must pass cleanly. If one of these ever fails,
    //  a constraint is stricter than the use case it is supposed to allow.
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("validPayloads")
    @DisplayName("accepts a well formed payload")
    void acceptsAValidPayload(String name, Object payload) {
        assertThat(validator.validate(payload))
                .as("%s should be valid but was rejected", name)
                .isEmpty();
    }

    static Stream<Arguments> validPayloads() {
        return Stream.of(
                arguments("LoginRequest", validLogin()),
                arguments("RefreshTokenRequest", validRefreshToken()),
                arguments("RegisterUserRequest", validRegister()),
                arguments("UpdateUserRequest", validUpdateUser()),
                arguments("IncomeRequest", validIncome()),
                arguments("ExpenseRequest", validExpense()),
                arguments("SavingsGoalRequest", validGoal()),
                arguments("ContributionRequest", validContribution()),
                arguments("ProjectionRequest", validProjection()),
                arguments("AssetRequest", validAsset()),
                arguments("TradeOrderRequest", validOrder()),
                arguments("MinigameRequest", validMinigame()),
                arguments("MinigameSessionRequest", validSession()));
    }

    // ------------------------------------------------------------------
    //  Every rejected case names the field it is meant to catch, so a
    //  constraint sitting on the wrong field fails here instead of shipping.
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPayloads")
    @DisplayName("rejects the payload and blames the right field")
    void rejectsAnInvalidPayload(String name, Object payload, String field) {
        Set<ConstraintViolation<Object>> violations = validator.validate(payload);

        assertThat(violations)
                .as("%s should have been rejected", name)
                .isNotEmpty();
        assertThat(violations)
                .as("%s should blame the field '%s'", name, field)
                .extracting(v -> v.getPropertyPath().toString())
                .contains(field);
    }

    static Stream<Arguments> invalidPayloads() {
        return Stream.of(
                // --- autenticacion -------------------------------------
                arguments("login sin correo", mutate(validLogin(), d -> d.setEmail("  ")), "email"),
                arguments("login con correo mal formado",
                        mutate(validLogin(), d -> d.setEmail("ary.sanchez.utec.edu.pe")), "email"),
                arguments("login sin contrasena", mutate(validLogin(), d -> d.setPassword("")), "password"),
                arguments("refresh con token vacio",
                        mutate(validRefreshToken(), d -> d.setRefreshToken("")), "refreshToken"),

                // --- registro ------------------------------------------
                arguments("registro con correo mal formado",
                        mutate(validRegister(), d -> d.setEmail("arroba-faltante")), "email"),
                arguments("registro con contrasena de 7 caracteres",
                        mutate(validRegister(), d -> d.setPassword("1234567")), "password"),
                arguments("registro sin nombre", mutate(validRegister(), d -> d.setName(" ")), "name"),
                arguments("registro con nombre de 121 caracteres",
                        mutate(validRegister(), d -> d.setName(tooLong(121))), "name"),
                arguments("registro con correo de 181 caracteres",
                        mutate(validRegister(), d -> d.setEmail(tooLong(170) + "@utec.edu.pe")), "email"),
                arguments("registro con fecha de nacimiento futura",
                        mutate(validRegister(), d -> d.setBirthDate(LocalDate.now().plusDays(1))), "birthDate"),

                // --- actualizacion de perfil ---------------------------
                arguments("perfil con nombre de 121 caracteres",
                        mutate(validUpdateUser(), d -> d.setName(tooLong(121))), "name"),
                arguments("perfil con fecha de nacimiento futura",
                        mutate(validUpdateUser(), d -> d.setBirthDate(LocalDate.now().plusYears(1))), "birthDate"),

                // --- ingresos y gastos ---------------------------------
                arguments("ingreso de cero", mutate(validIncome(), d -> d.setAmount(new BigDecimal("0.00"))), "amount"),
                arguments("ingreso negativo",
                        mutate(validIncome(), d -> d.setAmount(new BigDecimal("-50.00"))), "amount"),
                arguments("ingreso sin monto", mutate(validIncome(), d -> d.setAmount(null)), "amount"),
                arguments("ingreso sin origen", mutate(validIncome(), d -> d.setSource("")), "source"),
                arguments("ingreso con origen de 101 caracteres",
                        mutate(validIncome(), d -> d.setSource(tooLong(101))), "source"),
                arguments("ingreso con descripcion de 256 caracteres",
                        mutate(validIncome(), d -> d.setDescription(tooLong(256))), "description"),
                arguments("gasto sin categoria", mutate(validExpense(), d -> d.setCategory(null)), "category"),
                arguments("gasto de cero", mutate(validExpense(), d -> d.setAmount(BigDecimal.ZERO)), "amount"),
                arguments("gasto sin fecha", mutate(validExpense(), d -> d.setDate(null)), "date"),

                // --- metas y aportes -----------------------------------
                arguments("meta sin nombre", mutate(validGoal(), d -> d.setName("")), "name"),
                arguments("meta con objetivo de cero",
                        mutate(validGoal(), d -> d.setTargetAmount(BigDecimal.ZERO)), "targetAmount"),
                arguments("meta con fecha limite en el pasado",
                        mutate(validGoal(), d -> d.setDeadline(LocalDate.now().minusDays(1))), "deadline"),
                arguments("aporte de cero",
                        mutate(validContribution(), d -> d.setAmount(BigDecimal.ZERO)), "amount"),
                arguments("aporte sin origen", mutate(validContribution(), d -> d.setSource(null)), "source"),

                // --- proyecciones --------------------------------------
                arguments("proyeccion con tasa mayor a 100%",
                        mutate(validProjection(), d -> d.setAnnualRate(new BigDecimal("1.5"))), "annualRate"),
                arguments("proyeccion con tasa negativa",
                        mutate(validProjection(), d -> d.setAnnualRate(new BigDecimal("-0.01"))), "annualRate"),
                arguments("proyeccion de cero periodos",
                        mutate(validProjection(), d -> d.setPeriods(0)), "periods"),
                arguments("proyeccion de 601 periodos",
                        mutate(validProjection(), d -> d.setPeriods(601)), "periods"),
                arguments("proyeccion con capital negativo",
                        mutate(validProjection(), d -> d.setInitialCapital(new BigDecimal("-1"))), "initialCapital"),

                // --- catalogo de activos -------------------------------
                arguments("activo sin simbolo", mutate(validAsset(), d -> d.setSymbol(" ")), "symbol"),
                arguments("activo con simbolo de 16 caracteres",
                        mutate(validAsset(), d -> d.setSymbol(tooLong(16))), "symbol"),
                arguments("activo sin tipo", mutate(validAsset(), d -> d.setType(null)), "type"),
                arguments("activo con moneda en minusculas",
                        mutate(validAsset(), d -> d.setCurrency("usd")), "currency"),
                arguments("activo con moneda de dos letras",
                        mutate(validAsset(), d -> d.setCurrency("US")), "currency"),

                // --- ordenes -------------------------------------------
                arguments("orden de cantidad cero",
                        mutate(validOrder(), d -> d.setQuantity(BigDecimal.ZERO)), "quantity"),
                arguments("orden de cantidad negativa",
                        mutate(validOrder(), d -> d.setQuantity(new BigDecimal("-0.5"))), "quantity"),
                arguments("orden sin identificador de cliente",
                        mutate(validOrder(), d -> d.setClientOrderId(null)), "clientOrderId"),
                arguments("orden sin lado", mutate(validOrder(), d -> d.setSide(null)), "side"),

                // --- minijuegos ----------------------------------------
                arguments("minijuego sin titulo", mutate(validMinigame(), d -> d.setTitle("")), "title"),
                arguments("minijuego con costo negativo",
                        mutate(validMinigame(), d -> d.setTokenCost(new BigDecimal("-1.00"))), "tokenCost"),
                arguments("minijuego con recompensa negativa",
                        mutate(validMinigame(), d -> d.setMaxTokenReward(new BigDecimal("-0.01"))), "maxTokenReward"),
                arguments("minijuego sin estado", mutate(validMinigame(), d -> d.setStatus(null)), "status"),
                arguments("partida con puntaje negativo",
                        mutate(validSession(), d -> d.setScore(-1)), "score"),
                arguments("partida sin minijuego",
                        mutate(validSession(), d -> d.setMinigameId(null)), "minigameId"));
    }

    // ------------------------------------------------------------------
    //  Casos que documentan una decision, no un limite
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an empty profile update is valid, because it is a partial update")
    void acceptsAnEmptyProfileUpdate() {
        // UpdateUserRequest carries no @NotNull on purpose: null means "leave it
        // alone", so sending nothing is a no-op rather than a bad request.
        assertThat(validator.validate(new UpdateUserRequest())).isEmpty();
    }

    @Test
    @DisplayName("a password of exactly eight characters is accepted")
    void acceptsThePasswordLowerBound() {
        // Guards the boundary itself: min = 8 must mean eight are enough.
        assertThat(validator.validate(mutate(validRegister(), d -> d.setPassword("12345678"))))
                .isEmpty();
    }

    @Test
    @DisplayName("a goal whose deadline is today is rejected, because @Future excludes it")
    void rejectsAGoalDueToday() {
        // Documents the choice of @Future over @FutureOrPresent: a goal that
        // expires the same day it is created has no runway.
        assertThat(validator.validate(mutate(validGoal(), d -> d.setDeadline(LocalDate.now()))))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("deadline");
    }

    // ------------------------------------------------------------------
    //  Fabricas de payloads validos
    // ------------------------------------------------------------------

    /** Applies a single change to a valid payload, so each case breaks one rule only. */
    private static <T> T mutate(T payload, java.util.function.Consumer<T> change) {
        change.accept(payload);
        return payload;
    }

    private static LoginRequest validLogin() {
        return LoginRequest.builder()
                .email("ary.sanchez@utec.edu.pe")
                .password("unaClaveLarga")
                .build();
    }

    private static RefreshTokenRequest validRefreshToken() {
        return RefreshTokenRequest.builder()
                .refreshToken("eyJhbGciOiJIUzI1NiJ9.payload.firma")
                .build();
    }

    private static RegisterUserRequest validRegister() {
        return RegisterUserRequest.builder()
                .name("Ary Sanchez")
                .email("ary.sanchez@utec.edu.pe")
                .password("unaClaveLarga")
                .birthDate(LocalDate.of(2003, 5, 14))
                .build();
    }

    private static UpdateUserRequest validUpdateUser() {
        return UpdateUserRequest.builder()
                .name("Ary S.")
                .birthDate(LocalDate.of(2003, 5, 14))
                .build();
    }

    private static IncomeRequest validIncome() {
        return IncomeRequest.builder()
                .amount(new BigDecimal("1200.00"))
                .source("Practicas")
                .date(LocalDate.now().minusDays(3))
                .description("Pago de septiembre")
                .build();
    }

    private static ExpenseRequest validExpense() {
        return ExpenseRequest.builder()
                .category(ExpenseCategory.FOOD)
                .amount(new BigDecimal("28.50"))
                .date(LocalDate.now().minusDays(1))
                .description("Almuerzo")
                .build();
    }

    private static SavingsGoalRequest validGoal() {
        return SavingsGoalRequest.builder()
                .name("Laptop")
                .targetAmount(new BigDecimal("3500.00"))
                .deadline(LocalDate.now().plusMonths(8))
                .build();
    }

    private static ContributionRequest validContribution() {
        return ContributionRequest.builder()
                .amount(new BigDecimal("250.00"))
                .source(ContributionSource.MANUAL)
                .build();
    }

    private static ProjectionRequest validProjection() {
        return ProjectionRequest.builder()
                .name("Plan laptop")
                .initialCapital(new BigDecimal("500.00"))
                .monthlyContribution(new BigDecimal("150.00"))
                .annualRate(new BigDecimal("0.082000"))
                .periods(24)
                .build();
    }

    private static AssetRequest validAsset() {
        return AssetRequest.builder()
                .symbol("VOO")
                .name("Vanguard S&P 500 ETF")
                .type(AssetType.ETF)
                .currency("USD")
                .active(Boolean.TRUE)
                .build();
    }

    private static TradeOrderRequest validOrder() {
        return TradeOrderRequest.builder()
                .clientOrderId(UUID.randomUUID())
                .assetId(1L)
                .side(OrderSide.BUY)
                .quantity(new BigDecimal("0.41200000"))
                .build();
    }

    private static MinigameRequest validMinigame() {
        return MinigameRequest.builder()
                .title("Interes compuesto")
                .type(MinigameType.QUIZ)
                .topic("Ahorro")
                .tokenCost(new BigDecimal("2.50"))
                .maxTokenReward(new BigDecimal("12.75"))
                .status(MinigameStatus.PUBLISHED)
                .build();
    }

    private static MinigameSessionRequest validSession() {
        return MinigameSessionRequest.builder()
                .minigameId(1L)
                .score(80)
                .build();
    }
}
