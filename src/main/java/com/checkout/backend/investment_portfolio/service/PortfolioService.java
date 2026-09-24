package com.checkout.backend.investment_portfolio.service;

import com.checkout.backend.exceptions.InvalidRequestException;
import com.checkout.backend.investment_portfolio.asset.model.Asset;
import com.checkout.backend.investment_portfolio.asset.service.AssetService;
import com.checkout.backend.investment_portfolio.dto.PortfolioResponse;
import com.checkout.backend.investment_portfolio.model.InvestmentPortfolio;
import com.checkout.backend.investment_portfolio.position.dto.PositionResponse;
import com.checkout.backend.investment_portfolio.position.model.PortfolioPosition;
import com.checkout.backend.investment_portfolio.position.repository.PortfolioPositionRepository;
import com.checkout.backend.investment_portfolio.repository.InvestmentPortfolioRepository;
import com.checkout.backend.user.model.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cartera simulada del usuario y las posiciones que la componen.
 *
 * Igual que el monedero, no tiene escritura por API. Una posicion no se crea ni
 * se edita: aparece porque se ejecuto una orden de compra y desaparece cuando se
 * vende entera. Permitir editarla directamente seria poder darse acciones sin
 * pagarlas.
 *
 * El valor de mercado se calcula al leer, con la cotizacion del momento, en vez
 * de guardarse actualizado. Guardarlo obligaria a recorrer todas las carteras
 * cada vez que cambia un precio, y bastaria con que una actualizacion fallara
 * para que un usuario viera un valor que ya no es cierto.
 */
@Service
public class PortfolioService {

    private final InvestmentPortfolioRepository portfolioRepository;
    private final PortfolioPositionRepository positionRepository;
    private final AssetService assetService;
    private final ModelMapper mapper;

    public PortfolioService(InvestmentPortfolioRepository portfolioRepository,
                            PortfolioPositionRepository positionRepository,
                            AssetService assetService,
                            ModelMapper mapper) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.assetService = assetService;
        this.mapper = mapper;
    }

    @Transactional
    public InvestmentPortfolio getOrCreate(User user) {
        return portfolioRepository.findByUserId(user.getId())
                .orElseGet(() -> portfolioRepository.save(
                        InvestmentPortfolio.builder()
                                .user(user)
                                .investedTokens(BigDecimal.ZERO)
                                .simulatedValue(BigDecimal.ZERO)
                                .build()));
    }

    /**
     * La cartera con sus posiciones ya valoradas.
     *
     * simulatedValue se recalcula y se guarda de paso: asi el dato persistido no
     * se queda atras respecto a lo que el usuario acaba de ver.
     */
    @Transactional
    public PortfolioResponse getSummary(User user) {
        InvestmentPortfolio portfolio = getOrCreate(user);

        List<PositionResponse> positions = positionRepository.findByPortfolioId(portfolio.getId())
                .stream()
                .map(this::toPositionResponse)
                .toList();

        BigDecimal marketValue = positions.stream()
                .map(PositionResponse::getMarketValue)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        portfolio.setSimulatedValue(marketValue);

        PortfolioResponse response = mapper.map(portfolio, PortfolioResponse.class);
        response.setPositions(positions);
        // El mapper deja unrealizedPnl en null porque no existe en la entidad:
        // es la diferencia entre lo que valen hoy las posiciones y lo que costo
        // abrirlas.
        response.setUnrealizedPnl(marketValue.subtract(portfolio.getInvestedTokens()));
        return response;
    }

    @Transactional
    public List<PositionResponse> listPositions(User user) {
        return positionRepository.findByPortfolioId(getOrCreate(user).getId())
                .stream()
                .map(this::toPositionResponse)
                .toList();
    }

    /**
     * Suma una compra a la posicion del activo, creandola si es la primera.
     *
     * El coste medio se recalcula ponderando lo que ya habia con lo que entra.
     * Es lo que permite despues decir si se gana o se pierde: sin coste medio, el
     * valor de mercado es un numero sin referencia.
     */
    @Transactional
    public void applyBuy(User user, Asset asset, BigDecimal quantity, BigDecimal price,
                         BigDecimal tokensSpent) {
        InvestmentPortfolio portfolio = getOrCreate(user);

        PortfolioPosition position = positionRepository
                .findByPortfolioIdAndAssetId(portfolio.getId(), asset.getId())
                .orElse(null);

        if (position == null) {
            position = PortfolioPosition.builder()
                    .portfolio(portfolio)
                    .asset(asset)
                    .quantity(quantity)
                    .averageCost(price)
                    .build();
            portfolio.addPosition(position);
            positionRepository.save(position);
        } else {
            BigDecimal previousCost = position.getQuantity().multiply(position.getAverageCost());
            BigDecimal addedCost = quantity.multiply(price);
            BigDecimal newQuantity = position.getQuantity().add(quantity);

            position.setAverageCost(
                    previousCost.add(addedCost).divide(newQuantity, 4, RoundingMode.HALF_UP));
            position.setQuantity(newQuantity);
        }

        portfolio.setInvestedTokens(portfolio.getInvestedTokens().add(tokensSpent));
    }

    /**
     * Descuenta una venta de la posicion.
     *
     * Cuando la cantidad llega a cero la posicion se elimina: una fila con
     * cantidad cero no es informacion, es ruido en el listado.
     *
     * De investedTokens se retira la parte proporcional al coste medio, no lo que
     * se cobro por la venta. Si se restara el importe de la venta, vender con
     * ganancia dejaria el invertido por debajo de lo que realmente queda puesto y
     * el resultado no cuadraria.
     */
    @Transactional
    public void applySell(User user, Asset asset, BigDecimal quantity) {
        InvestmentPortfolio portfolio = getOrCreate(user);

        PortfolioPosition position = positionRepository
                .findByPortfolioIdAndAssetId(portfolio.getId(), asset.getId())
                .orElseThrow(() -> new InvalidRequestException(
                        "No tienes ninguna posicion en " + asset.getSymbol() + "."));

        if (position.getQuantity().compareTo(quantity) < 0) {
            throw new InvalidRequestException(
                    "Solo tienes " + position.getQuantity() + " de " + asset.getSymbol() + ".");
        }

        BigDecimal releasedCost = quantity.multiply(position.getAverageCost())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal remaining = position.getQuantity().subtract(quantity);
        if (remaining.signum() == 0) {
            portfolio.removePosition(position);
            positionRepository.delete(position);
        } else {
            position.setQuantity(remaining);
        }

        portfolio.setInvestedTokens(
                portfolio.getInvestedTokens().subtract(releasedCost).max(BigDecimal.ZERO));
    }

    /**
     * Valora una posicion con la cotizacion actual.
     *
     * Los tres campos derivados los deja el mapper en null porque no existen en
     * la entidad. Si el activo no tiene cotizacion se quedan asi en vez de
     * inventar un cero: un valor desconocido y un valor nulo son cosas distintas,
     * y mostrar cero haria creer que la posicion no vale nada.
     */
    private PositionResponse toPositionResponse(PortfolioPosition position) {
        PositionResponse response = mapper.map(position, PositionResponse.class);

        try {
            BigDecimal price = assetService.requireQuote(position.getAsset()).getPrice();
            BigDecimal marketValue = position.getQuantity().multiply(price)
                    .setScale(2, RoundingMode.HALF_UP);

            response.setCurrentPrice(price);
            response.setMarketValue(marketValue);
            response.setUnrealizedPnl(marketValue.subtract(
                    position.getQuantity().multiply(position.getAverageCost())
                            .setScale(2, RoundingMode.HALF_UP)));
        } catch (InvalidRequestException withoutQuote) {
            // Un activo sin precio no puede tumbar la lectura de toda la cartera.
        }

        return response;
    }

}
