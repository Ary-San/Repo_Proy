package com.checkout.backend.model;

import com.checkout.backend.investment_portfolio.model.InvestmentPortfolio;
import com.checkout.backend.investment_portfolio.position.model.PortfolioPosition;
import com.checkout.backend.investment_portfolio.asset.model.Asset;
import com.checkout.backend.investment_portfolio.asset.model.AssetType;
import com.checkout.backend.savings.goal.contribution.model.ContributionSource;
import com.checkout.backend.savings.goal.contribution.model.SavingsGoalContribution;
import com.checkout.backend.savings.goal.model.SavingsGoal;
import com.checkout.backend.token_wallet.model.TokenWallet;
import com.checkout.backend.token_wallet.tktransaction.model.TokenReason;
import com.checkout.backend.token_wallet.tktransaction.model.TokenTransaction;
import com.checkout.backend.user.model.Role;
import com.checkout.backend.user.model.User;
import com.checkout.backend.user.model.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Boots Hibernate against an in-memory database so the mappings are exercised
 * instead of only reviewed. The schema for the seventeen entities is generated
 * by the context itself; these tests cover the parts a schema cannot prove.
 */
@DataJpaTest
class EntityMappingTest {

    @Autowired
    private EntityManager em;

    private User persistedUser() {
        User user = User.builder()
                .name("Ary")
                .email("ary@utec.edu.pe")
                .passwordHash("$2a$10$notarealhashbutlongenoughtolooklikeone")
                .roles(EnumSet.of(Role.USER))
                .status(UserStatus.ACTIVE)
                .birthDate(LocalDate.of(2003, 5, 14))
                .build();
        em.persist(user);
        em.flush();
        return user;
    }

    @Test
    @DisplayName("A user with its wallet and a ledger entry survives a flush and reload")
    void persistsTheTokenLedger() {
        User user = persistedUser();

        TokenWallet wallet = TokenWallet.builder()
                .user(user)
                .tokenBalance(new BigDecimal("100.00"))
                .build();
        em.persist(wallet);

        TokenTransaction movement = TokenTransaction.builder()
                .tokenWallet(wallet)
                .amount(new BigDecimal("-226.14"))
                .reason(TokenReason.INVESTMENT)
                .build();
        em.persist(movement);
        em.flush();
        em.clear();

        TokenTransaction reloaded = em.find(TokenTransaction.class, movement.getId());
        assertThat(reloaded.getAmount()).isEqualByComparingTo("-226.14");
        assertThat(reloaded.getReason()).isEqualTo(TokenReason.INVESTMENT);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getTokenWallet().getId()).isEqualTo(wallet.getId());
    }

    @Test
    @DisplayName("@Version is populated so the wallet is protected against a lost update")
    void versionsTheWallet() {
        TokenWallet wallet = TokenWallet.builder().user(persistedUser()).build();
        em.persist(wallet);
        em.flush();

        assertThat(wallet.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("A token movement of zero is rejected")
    void rejectsAZeroMovement() {
        TokenWallet wallet = TokenWallet.builder().user(persistedUser()).build();
        em.persist(wallet);

        TokenTransaction noop = TokenTransaction.builder()
                .tokenWallet(wallet)
                .amount(BigDecimal.ZERO)
                .reason(TokenReason.ADJUSTMENT)
                .build();

        // Hibernate runs bean validation on persist, before the flush.
        assertThatThrownBy(() -> em.persist(noop))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("TokenTransaction");
    }

    @Test
    @DisplayName("addContribution sets the owning side, so the foreign key is never null")
    void keepsBothSidesOfTheGoalRelation() {
        SavingsGoal goal = SavingsGoal.builder()
                .user(persistedUser())
                .name("Laptop")
                .targetAmount(new BigDecimal("3500.00"))
                .deadline(LocalDate.now().plusMonths(8))
                .build();

        SavingsGoalContribution contribution = SavingsGoalContribution.builder()
                .amount(new BigDecimal("250.00"))
                .source(ContributionSource.MANUAL)
                .build();

        goal.addContribution(contribution);
        assertThat(contribution.getSavingsGoal()).isSameAs(goal);

        em.persist(goal);   // cascade = ALL carries the contribution along
        em.flush();

        assertThat(contribution.getId()).isNotNull();
    }

    @Test
    @DisplayName("addPosition sets the owning side of the portfolio relation")
    void keepsBothSidesOfThePortfolioRelation() {
        Asset asset = Asset.builder()
                .symbol("VOO")
                .name("Vanguard S&P 500 ETF")
                .type(AssetType.ETF)
                .currency("USD")
                .build();
        em.persist(asset);

        InvestmentPortfolio portfolio = InvestmentPortfolio.builder()
                .user(persistedUser())
                .build();

        PortfolioPosition position = PortfolioPosition.builder()
                .asset(asset)
                .quantity(new BigDecimal("0.41200000"))
                .averageCost(new BigDecimal("548.9000"))
                .build();

        portfolio.addPosition(position);
        assertThat(position.getPortfolio()).isSameAs(portfolio);

        em.persist(portfolio);
        em.flush();

        assertThat(position.getId()).isNotNull();
    }

    @Test
    @DisplayName("An entity stays findable in a Set after the id is assigned")
    void equalsSurvivesPersist() {
        User user = User.builder()
                .name("Angel")
                .email("angel@utec.edu.pe")
                .passwordHash("$2a$10$notarealhashbutlongenoughtolooklikeone")
                .build();

        Set<User> set = new HashSet<>();
        set.add(user);

        em.persist(user);
        em.flush();

        assertThat(set).contains(user);
        assertThat(user).isNotEqualTo(new User());
    }
}
