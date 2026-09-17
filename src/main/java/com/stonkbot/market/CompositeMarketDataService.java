package com.stonkbot.market;

import java.util.Optional;

public final class CompositeMarketDataService implements MarketDataService {
    private final MarketDataService primary;
    private final MarketDataService fallback;

    public CompositeMarketDataService(MarketDataService primary, MarketDataService fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public Optional<Quote> quote(String symbol) {
        Optional<Quote> quote = primary.quote(symbol);
        return quote.isPresent() ? quote : fallback.quote(symbol);
    }
}
