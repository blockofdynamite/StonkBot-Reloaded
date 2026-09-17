package com.stonkbot.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuote;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class YahooMarketDataService implements MarketDataService {
    private static final Logger log = LoggerFactory.getLogger(YahooMarketDataService.class);
    private static final long CACHE_TTL_MS = 30_000L;

    private record Cached(Quote quote, long fetchedAt) {}

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Override
    public synchronized Optional<Quote> quote(String symbol) {
        String key = symbol.toUpperCase(Locale.ROOT);
        Cached cached = cache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.fetchedAt() < CACHE_TTL_MS) {
            return Optional.of(cached.quote());
        }
        try {
            Stock stock = YahooFinance.get(key);
            StockQuote q = stock.getQuote();
            if (q == null || q.getPrice() == null) {
                return Optional.empty();
            }
            Quote quote = new Quote(
                    key,
                    stock.getName() != null ? stock.getName() : key,
                    q.getPrice(),
                    q.getChangeInPercent(),
                    stock.getCurrency() != null ? stock.getCurrency() : "USD");
            cache.put(key, new Cached(quote, now));
            return Optional.of(quote);
        } catch (Exception ex) {
            log.warn("Failed to fetch quote for {}", key, ex);
            return cached != null ? Optional.of(cached.quote()) : Optional.empty();
        }
    }
}
