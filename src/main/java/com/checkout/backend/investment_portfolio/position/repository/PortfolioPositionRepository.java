package com.checkout.backend.investment_portfolio.position.repository;

import com.checkout.backend.investment_portfolio.position.model.PortfolioPosition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a las posiciones de una cartera. */
public interface PortfolioPositionRepository extends JpaRepository<PortfolioPosition, Long> {

    List<PortfolioPosition> findByPortfolioId(Long portfolioId);

    Optional<PortfolioPosition> findByPortfolioIdAndAssetId(Long portfolioId, Long assetId);
}
