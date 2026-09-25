package com.checkout.backend.investment_portfolio.repository;

import com.checkout.backend.investment_portfolio.model.InvestmentPortfolio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la cartera, que es una por usuario. */
public interface InvestmentPortfolioRepository extends JpaRepository<InvestmentPortfolio, Long> {

    Optional<InvestmentPortfolio> findByUserId(Long userId);
}
