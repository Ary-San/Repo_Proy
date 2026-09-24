package com.checkout.backend.investment_portfolio.asset.quote.repository;

import com.checkout.backend.investment_portfolio.asset.quote.model.AssetQuote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la cotizacion viva de cada activo.
 *
 * Hay como mucho una fila por activo, por el UNIQUE sobre asset_id: esta tabla
 * guarda el precio actual y se sobrescribe. El historico vive en
 * asset_price_history.
 */
public interface AssetQuoteRepository extends JpaRepository<AssetQuote, Long> {

    Optional<AssetQuote> findByAssetId(Long assetId);
}
