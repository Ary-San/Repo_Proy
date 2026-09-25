package com.checkout.backend.investment_portfolio.asset.history.repository;

import com.checkout.backend.investment_portfolio.asset.history.model.AssetPriceHistory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso al cierre diario de cada activo.
 *
 * Es lo que permite dibujar un grafico: la cotizacion viva se sobrescribe, asi
 * que sin estas filas el valor pasado de una posicion no se puede reconstruir.
 */
public interface AssetPriceHistoryRepository extends JpaRepository<AssetPriceHistory, Long> {

    List<AssetPriceHistory> findByAssetIdAndDateBetweenOrderByDateAsc(
            Long assetId, LocalDate from, LocalDate to);

    List<AssetPriceHistory> findByAssetIdOrderByDateAsc(Long assetId);
}
