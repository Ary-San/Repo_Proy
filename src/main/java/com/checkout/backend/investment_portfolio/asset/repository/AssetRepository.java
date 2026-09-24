package com.checkout.backend.investment_portfolio.asset.repository;

import com.checkout.backend.investment_portfolio.asset.model.Asset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso al catalogo de activos.
 */
public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * Solo los activos operables. Un activo dado de baja sigue en la tabla
     * porque las posiciones y las ordenes historicas lo referencian, pero no
     * debe aparecer en la lista donde el usuario elige que comprar.
     */
    List<Asset> findByActiveTrueOrderBySymbolAsc();

    Optional<Asset> findBySymbolIgnoreCase(String symbol);

    boolean existsBySymbolIgnoreCase(String symbol);
}
