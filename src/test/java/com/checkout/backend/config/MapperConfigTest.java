package com.checkout.backend.config;

import com.checkout.backend.investment_portfolio.asset.model.Asset;
import com.checkout.backend.investment_portfolio.asset.model.AssetType;
import com.checkout.backend.investment_portfolio.position.dto.PositionResponse;
import com.checkout.backend.investment_portfolio.position.model.PortfolioPosition;
import com.checkout.backend.minigame.dto.MinigameResponse;
import com.checkout.backend.minigame.model.Minigame;
import com.checkout.backend.minigame.model.MinigameStatus;
import com.checkout.backend.minigame.model.MinigameType;
import com.checkout.backend.savings.goal.dto.SavingsGoalResponse;
import com.checkout.backend.savings.goal.model.SavingsGoal;
import com.checkout.backend.savings.income.dto.IncomeResponse;
import com.checkout.backend.savings.income.model.Income;
import com.checkout.backend.user.dto.AuthResponse;
import com.checkout.backend.user.dto.UserResponse;
import com.checkout.backend.user.model.Role;
import com.checkout.backend.user.model.User;
import com.checkout.backend.user.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.lang.reflect.Method;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for the shared ModelMapper bean.
 *
 * These run without a Spring context or a database: the point is to catch a
 * broken type map at build time rather than on the first request.
 */
class MapperConfigTest {

    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        modelMapper = new MapperConfig().modelMapper();
    }

    @Test
    @DisplayName("maps matching properties straight across")
    void mapsFlatProperties() {
        Income income = Income.builder()
                .id(7L)
                .amount(new BigDecimal("1200.00"))
                .source("Internship")
                .date(LocalDate.of(2026, 9, 5))
                .description("September payment")
                .build();

        IncomeResponse response = modelMapper.map(income, IncomeResponse.class);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getAmount()).isEqualByComparingTo("1200.00");
        assertThat(response.getSource()).isEqualTo("Internship");
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 9, 5));
    }

    @Test
    @DisplayName("never leaks the password hash into the user response")
    void doesNotExposePasswordHash() {
        User user = User.builder()
                .id(1L)
                .name("Ary")
                .email("ary@example.com")
                .passwordHash("$2a$10$notarealhash")
                .roles(EnumSet.of(Role.USER))
                .status(UserStatus.ACTIVE)
                .build();

        UserResponse response = modelMapper.map(user, UserResponse.class);

        assertThat(response.getEmail()).isEqualTo("ary@example.com");
        assertThat(response.getRoles()).containsExactly(Role.USER);
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(UserResponse.class.getDeclaredFields())
                .noneMatch(field -> field.getName().toLowerCase().contains("password"));
    }

    @Test
    @DisplayName("flattens the nested asset into the position response")
    void flattensNestedAsset() {
        Asset asset = Asset.builder()
                .id(3L)
                .symbol("VOO")
                .name("Vanguard S&P 500")
                .type(AssetType.ETF)
                .currency("USD")
                .active(true)
                .build();

        PortfolioPosition position = PortfolioPosition.builder()
                .id(11L)
                .asset(asset)
                .quantity(new BigDecimal("0.41200000"))
                .averageCost(new BigDecimal("512.4000"))
                .build();

        PositionResponse response = modelMapper.map(position, PositionResponse.class);

        assertThat(response.getSymbol()).isEqualTo("VOO");
        assertThat(response.getAssetName()).isEqualTo("Vanguard S&P 500");
        assertThat(response.getQuantity()).isEqualByComparingTo("0.412");
        assertThat(response.getAverageCost()).isEqualByComparingTo("512.40");
    }

    @Test
    @DisplayName("keeps the token amounts of a minigame as decimals")
    void mapsMinigameTokenAmounts() {
        Minigame minigame = Minigame.builder()
                .id(3L)
                .title("Interes compuesto")
                .type(MinigameType.QUIZ)
                .tokenCost(new BigDecimal("2.50"))
                .maxTokenReward(new BigDecimal("12.75"))
                .status(MinigameStatus.PUBLISHED)
                .build();

        MinigameResponse response = modelMapper.map(minigame, MinigameResponse.class);

        assertThat(response.getTokenCost()).isEqualByComparingTo("2.50");
        assertThat(response.getMaxTokenReward()).isEqualByComparingTo("12.75");
        assertThat(response.getStatus()).isEqualTo(MinigameStatus.PUBLISHED);
    }

    @Test
    @DisplayName("leaves the derived fields untouched, because the service computes them")
    void doesNotInventDerivedFields() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(11L)
                .name("Laptop")
                .targetAmount(new BigDecimal("3500.00"))
                .accumulatedAmount(new BigDecimal("875.00"))
                .deadline(LocalDate.of(2027, 6, 30))
                .build();

        SavingsGoalResponse response = modelMapper.map(goal, SavingsGoalResponse.class);

        assertThat(response.getAccumulatedAmount()).isEqualByComparingTo("875.00");
        // progressPercent has no source in the entity: the mapper must not guess it.
        assertThat(response.getProgressPercent()).isNull();
    }

    @Test
    @DisplayName("the auth responses cannot be mutated after the service builds them")
    void authResponsesExposeNoSetter() {
        for (Class<?> dto : new Class<?>[]{UserResponse.class, AuthResponse.class}) {
            assertThat(dto.getDeclaredMethods())
                    .as("%s must stay read-only once built", dto.getSimpleName())
                    .extracting(Method::getName)
                    .noneMatch(name -> name.startsWith("set"));
        }
    }
}
