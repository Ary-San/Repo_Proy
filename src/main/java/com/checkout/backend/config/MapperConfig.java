package com.checkout.backend.config;

import com.checkout.backend.investment_portfolio.asset.quote.dto.AssetQuoteResponse;
import com.checkout.backend.investment_portfolio.asset.quote.model.AssetQuote;
import com.checkout.backend.investment_portfolio.position.dto.PositionResponse;
import com.checkout.backend.investment_portfolio.position.model.PortfolioPosition;
import com.checkout.backend.investment_portfolio.trade_order.dto.TradeOrderResponse;
import com.checkout.backend.investment_portfolio.trade_order.model.TradeOrder;
import com.checkout.backend.minigame.session.dto.MinigameSessionResponse;
import com.checkout.backend.minigame.session.model.MinigameSession;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single ModelMapper bean shared by every service.
 *
 * The strategy is STRICT on purpose: it only maps properties whose names match
 * exactly, so a renamed entity field fails loudly instead of silently leaving a
 * DTO field null. The price of that safety is that flattened fields such as
 * "symbol" coming from "asset.symbol" have to be declared here, which is what
 * the type maps below do.
 *
 * Note for callers: the explicit maps walk LAZY associations, so map inside the
 * transaction or fetch the association first, otherwise Hibernate throws
 * LazyInitializationException.
 *
 * These response fields have no source in the entity and stay null after the
 * mapping; the service that builds the response has to compute them:
 *
 * <ul>
 *   <li>PortfolioResponse.unrealizedPnl</li>
 *   <li>PositionResponse.currentPrice, marketValue, unrealizedPnl</li>
 *   <li>ProjectionResponse.totalContributed, difference</li>
 *   <li>SavingsGoalResponse.progressPercent</li>
 * </ul>
 */
@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE)
                .setSkipNullEnabled(true);

        addFlattenedMappings(modelMapper);
        return modelMapper;
    }

    /** Maps the response fields that come from a nested entity. */
    private void addFlattenedMappings(ModelMapper modelMapper) {
        modelMapper.typeMap(AssetQuote.class, AssetQuoteResponse.class)
                .addMappings(map -> {
                    map.map(src -> src.getAsset().getSymbol(), AssetQuoteResponse::setSymbol);
                    map.map(src -> src.getAsset().getCurrency(), AssetQuoteResponse::setCurrency);
                });

        modelMapper.typeMap(PortfolioPosition.class, PositionResponse.class)
                .addMappings(map -> {
                    map.map(src -> src.getAsset().getSymbol(), PositionResponse::setSymbol);
                    map.map(src -> src.getAsset().getName(), PositionResponse::setAssetName);
                });

        modelMapper.typeMap(TradeOrder.class, TradeOrderResponse.class)
                .addMappings(map ->
                        map.map(src -> src.getAsset().getSymbol(), TradeOrderResponse::setSymbol));

        modelMapper.typeMap(MinigameSession.class, MinigameSessionResponse.class)
                .addMappings(map ->
                        map.map(src -> src.getMinigame().getTitle(),
                                MinigameSessionResponse::setMinigameTitle));
    }
}
